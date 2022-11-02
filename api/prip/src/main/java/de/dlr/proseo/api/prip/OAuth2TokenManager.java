/**
 * OAuth2TokenManager.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import de.dlr.proseo.api.prip.rest.model.OAuth2Response;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.PripMessage;

/**
 * Class for managing OAuth2 tokens and to access user information based on OAuth2 tokens
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class OAuth2TokenManager {

	private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
	private static final String GRANT_TYPE_PASSWORD = "password";
	
	/* Submessages for token evaluation */
	private static final String MSG_TOKEN_PAYLOAD_INVALID = "The payload of the JWT doesn't represent a valid JSON object and a JWT claims set";
	private static final String MSG_TOKEN_NOT_VERIFIABLE = "The JWS object couldn't be verified";
	private static final String MSG_TOKEN_STATE_INVALID = "The JWS object is not in a signed or verified state, actual state: ";
	private static final String MSG_TOKEN_VERIFICATION_FAILED = "Verification of the JWT failed";
	private static final String MSG_TOKEN_NOT_PARSEABLE = "Token not parseable";
	private static final String MSG_USER_INVALID = "Invalid user: ";
	private static final String MSG_TOKEN_PAYLOAD_INCOMPLETE = "User name or expiration date missing in the payload of the JWT";

	/* OAuth2 signing secret (tokens are invalidated when service is restarted) */
	// We need exactly 256 bits (32 bytes) of key length, so a shorter key will be filled with blanks, a longer key will be truncated
	private static final byte[] TOKEN_SECRET = Arrays.copyOf(
			(String.valueOf(Math.random()) + "                                ").getBytes(),
			32);
	
	/**
	 * Map for user information
	 */
	private Map<String, UserInfo> userInfoMap = new HashMap<>();
	
	/** The PRIP configuration to use */
	@Autowired
	private ProductionInterfaceConfiguration config;
	
	/** The security configuration to use */
	@Autowired
	private ProductionInterfaceSecurity securityConfig;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OAuth2TokenManager.class);
	
	/**
	 * Information about a user including cached password for authentication with prosEO backend services and authorities
	 * to determine access rights to product classes depending on their visibility
	 */
	public static class UserInfo {
		public String missionCode;
		public String username;
		public String password;
		public List<String> authorities = new ArrayList<>();
		
		public UserInfo() {
			super();
		}
		
		public UserInfo(String missionCode, String username, String password, List<String> authorities) {
			this.missionCode = missionCode;
			this.username = username;
			this.password = password;
			this.authorities.addAll(authorities);
		}
	}
	
	/**
	 * Gets the user information for the given username
	 * 
	 * @param username the username (format &lt;mission&gt;\&lt;user&gt;) to check
	 * @return a UserInfo object or null, if no entry for the given username exists
	 */
	/* package */ synchronized UserInfo getUser(String username) {
		return userInfoMap.get(username);
	}
	
	/**
	 * Set the user information for the given user
	 * 
	 * @param userInfo the UserInfo object to get the user information from (including username)
	 */
	private synchronized void addUser(UserInfo userInfo) {
		userInfoMap.put(userInfo.missionCode + "\\" + userInfo.username, userInfo);
	}
	
	/**
	 * Authenticate the user with the given credentials, create an OAuth2 token for them and associate the token with the user
	 * 
	 * @param userInfo a UserInfo object containing the user credentials (will be updated with their authorities)
	 * @return the OAuth2 JSON Web Token generated 
	 * @throws SecurityException if the authenticated client is not authorized to use the PRIP API
	 */
	private String createToken(UserInfo userInfo) throws SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> createToken({})", userInfo.username);
		
		// Authorize the user
		userInfo = securityConfig.authenticateUser(userInfo);
		
		// Build the token
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
				.type(JOSEObjectType.JWT)
				.build();
		
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(userInfo.missionCode + "\\" + userInfo.username)
				.expirationTime(new Date(new Date().getTime() + config.getTokenExpirationPeriod() * 1000))
				.build();
		
		JWSSigner signer = null;
		try {
			// We need exactly 256 bits (32 bytes) of key length, so a shorter key will be filled with blanks, a longer key will be truncated
			signer = new MACSigner(TOKEN_SECRET);
		} catch (KeyLengthException e) {
			throw new RuntimeException(logger.log(PripMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage()));
		}
		
		SignedJWT signedJWT = new SignedJWT(header, claims);
		try {
			signedJWT.sign(signer);
		} catch (JOSEException e) {
			throw new RuntimeException(logger.log(PripMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage()));
		}
		
		// Everything OK, save the user info
		addUser(userInfo);
		
		return signedJWT.serialize();
	}
	
	/**
	 * @param grantType type of grant requested (value must be set to "password" for "Resource Owner Password Credentials Grant" flow
     *    or to "client_credentials" for "Client Credentials Grant" flow as per RFC 6749; REQUIRED)
	 * @param username the PRIP username (as per RFC 6749; REQUIRED for "Resource Owner Password Credentials Grant" flow, must not be set otherwise)
	 * @param password the PRIP password (as per RFC 6749; REQUIRED for "Resource Owner Password Credentials Grant" flow, must not be set otherwise)
	 * @param headers the HTTP request headers (for extraction of the Authentication header)
	 * @return an OAuth2 token grant response
	 * @throws IllegalArgumentException if the request is missing a required parameter, includes an unsupported parameter value 
	 * 				(other than grant type), repeats a parameter, includes multiple credentials, utilizes more than one mechanism
	 * 				for authenticating the client, or is otherwise malformed.
	 * @throws UnsupportedOperationException if the authorization grant type is not supported by the PRIP service
	 * @throws SecurityException if the authenticated client is not authorized to use this authorization grant type
	 */
	public OAuth2Response getToken(String grantType, String username, String password, HttpHeaders headers) 
		throws IllegalArgumentException, UnsupportedOperationException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getToken({}, {}, ********, HttpHeaders)", grantType, username);
		
		// Check authentication header
		UserInfo userInfo = null;
		try {
			userInfo = securityConfig.parseAuthenticationHeader(headers.getFirst(HttpHeaders.AUTHORIZATION));
		} catch (IllegalArgumentException e) {
			throw new SecurityException(e.getMessage()); // already formatted and logged
		}
		
		// Check arguments
		switch (grantType) {
		case GRANT_TYPE_PASSWORD:
			// Split username parameter into mission code and actual user name
			String[] usernameParts = username.split("\\\\");
			if (2 != usernameParts.length) {
				throw new IllegalArgumentException(logger.log(PripMessage.MSG_USERNAME_INVALID, username));
			}
			// User name and password must match information from Authorization header
			if (!userInfo.missionCode.equals(usernameParts[0]) || !userInfo.username.equals(usernameParts[1]) || !userInfo.password.equals(password)) {
				throw new IllegalArgumentException(logger.log(PripMessage.MSG_CREDENTIAL_MISMATCH, username));
			}
			break;
		case GRANT_TYPE_CLIENT_CREDENTIALS:
			if (null != username || null != password) {
				throw new IllegalArgumentException(logger.log(PripMessage.MSG_SUPERFLUOUS_PARAMETERS));
			}
			break;
		default:
			throw new UnsupportedOperationException(logger.log(PripMessage.MSG_GRANT_TYPE_INVALID, grantType));
		}
		
		// Create token
		String token = createToken(userInfo);
		
		OAuth2Response response = new OAuth2Response();
		response.setAccessToken(token);
		response.setExpiresIn(config.getTokenExpirationPeriod());
		response.setTokenType("bearer");
		
		logger.log(PripMessage.MSG_TOKEN_CREATED, username);
		
		return response;
	}

	/**
	 * Analyze a given OAuth2 token (JSON Web Token format) and return the user information associated with the token
	 * 
	 * @param token the OAuth2 token to analyze
	 * @return the UserInfo object associated with this token
	 * @throws SecurityException if the token cannot be associated with a user or is expired
	 */
	public UserInfo getUserInfoFromToken(String token) throws SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getUserInfoFromToken({})", token);
		
		// Parse token
		SignedJWT signedJWT = null;
		try {
			signedJWT = SignedJWT.parse(token);
		} catch (ParseException e) {
			throw new SecurityException(logger.log(PripMessage.MSG_TOKEN_INVALID, token, MSG_TOKEN_NOT_PARSEABLE));
		}
		
		JWSVerifier verifier = null;
		try {
			verifier = new MACVerifier(TOKEN_SECRET);
		} catch (JOSEException e) {
			throw new RuntimeException(logger.log(PripMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage()));
		}

		try {
			if (!signedJWT.verify(verifier)) {
				throw new SecurityException(logger.log(PripMessage.MSG_TOKEN_INVALID, token, MSG_TOKEN_VERIFICATION_FAILED));
			};
		} catch (IllegalStateException e) {
			throw new SecurityException(logger.log(PripMessage.MSG_TOKEN_INVALID, token, 
					MSG_TOKEN_STATE_INVALID + signedJWT.getState()));
		} catch (JOSEException e) {
			throw new SecurityException(logger.log(PripMessage.MSG_TOKEN_INVALID, token, MSG_TOKEN_NOT_VERIFIABLE));
		}
		
		// Retrieve / verify the JWT claims according to the app requirements
		JWTClaimsSet claimsSet = null;
		try {
			claimsSet = signedJWT.getJWTClaimsSet();
		} catch (ParseException e) {
			throw new SecurityException(logger.log(PripMessage.MSG_TOKEN_INVALID, token, MSG_TOKEN_PAYLOAD_INVALID));
		}
		if (null == claimsSet.getSubject() || null == claimsSet.getExpirationTime()) {
			throw new SecurityException(logger.log(PripMessage.MSG_TOKEN_INVALID, token, MSG_TOKEN_PAYLOAD_INCOMPLETE));
		}
		
		// Check token payload (user and expiration time)
		UserInfo userInfo = getUser(claimsSet.getSubject());
		if (null == userInfo) {
			throw new SecurityException(logger.log(PripMessage.MSG_TOKEN_INVALID, token,
					MSG_USER_INVALID + claimsSet.getSubject()));
		}
		
		if ((new Date()).after(claimsSet.getExpirationTime())) {
			throw new SecurityException(logger.log(PripMessage.MSG_TOKEN_EXPIRED, claimsSet.getExpirationTime()));
		}
		
		return userInfo;
	}
}
