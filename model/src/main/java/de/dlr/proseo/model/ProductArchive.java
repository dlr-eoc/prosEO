/**
 * ProductArchive.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import de.dlr.proseo.model.enums.ArchiveType;

/**
 * Available archive locations for input products or auxiliary files. Product archives are by design mission-neutral, but 
 * the credentials may actually restrict an archive to a specific mission. In such a case, if the same endpoint shall be used
 * for multiple missions, the product archives must be differentiated in their short codes.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = { @Index(unique = true, columnList = "code") })
public class ProductArchive extends PersistentObject {
	
	/** Short code for the archive (unique key) */
	@Column(nullable = false)
	private String code;
	
	/** Descriptive name for the archive */
	private String name;
	
	/** Interface protocol for this archive */
	@Enumerated(EnumType.STRING)
	private ArchiveType archiveType;
	
	/** Base URI to access the archive (of the form "http[s]://<hostname>[:<port>]", no trailing slash) */
	private String baseUri;
	
	/** The context path of the archive service endpoint (excluding "odata/v1" where applicable, no leading or trailing slashes) */
	private String context;
	
	/** Flag indicating whether token-based authentication shall be used */
	private Boolean tokenRequired;
	
	/** The full URI for token requests (may be routed to a different host from the archive service itself) */
	private String tokenUri;
	
	/**
	 * The username for Basic Authentication
	 * (either for a token request or for a service request, if token-based authentication is not used)
	 */
	private String username;
	
	/** Password for Basic Authentication (CAUTION: Clear text) */
	// TODO Check whether we can at least use a configured key for scrambling the passwords
	private String password;
	
	/** Client-ID for OpenID-based token requests */
	private String clientId;
	
	/** Client secret for OpenID-based token requests */
	private String clientSecret;
	
	/** Flag indicating whether credentials for OpenID-based token requests shall be sent in request body */
	private Boolean sendAuthInBody;
	
	/** List of product classes retrievable from this archive */
	@ManyToMany
	private Set<ProductClass> availableProductClasses = new HashSet<>();

	/**
	 * Gets the archive short code
	 * 
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Sets the archive short code
	 * 
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Gets the descriptive name for the archive
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the descriptive name for the archive
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the archive type
	 * 
	 * @return the archive type
	 */
	public ArchiveType getArchiveType() {
		return archiveType;
	}

	/**
	 * Sets the archive type
	 * 
	 * @param archiveType the archive type to set
	 */
	public void setArchiveType(ArchiveType archiveType) {
		this.archiveType = archiveType;
	}

	/**
	 * Gets the base URI for the archive endpoint
	 * 
	 * @return the base URI
	 */
	public String getBaseUri() {
		return baseUri;
	}

	/**
	 * Sets the base URI for the archive endpoint
	 * 
	 * @param baseUri the base URI to set
	 */
	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}

	/**
	 * Gets the URI context for the archive endpoint
	 * 
	 * @return the URI context
	 */
	public String getContext() {
		return context;
	}

	/**
	 * Sets the URI context for the archive endpoint
	 * 
	 * @param context the URI context to set
	 */
	public void setContext(String context) {
		this.context = context;
	}

	/**
	 * Indicates whether token-based authentication shall be used
	 * 
	 * @return true, if token-based authentication is required, false otherwise
	 */
	public Boolean getTokenRequired() {
		return tokenRequired;
	}

	/**
	 * Indicates whether token-based authentication shall be used (alias for {@link #getTokenRequired()})
	 * 
	 * @return true, if token-based authentication is required, false otherwise
	 */
	public Boolean isTokenRequired() {
		return getTokenRequired();
	}

	/**
	 * Sets whether token-based authentication shall be used
	 * 
	 * @param tokenRequired set to true, if token-based authentication is required, and to false otherwise
	 */
	public void setTokenRequired(Boolean tokenRequired) {
		this.tokenRequired = tokenRequired;
	}

	/**
	 * Gets the URI for the token request endpoint
	 * 
	 * @return the token request URI
	 */
	public String getTokenUri() {
		return tokenUri;
	}

	/**
	 * Sets the URI for the token request endpoint
	 * 
	 * @param tokenUri the token request URI to set
	 */
	public void setTokenUri(String tokenUri) {
		this.tokenUri = tokenUri;
	}

	/**
	 * Gets the user name for Basic Authentication
	 * 
	 * @return the user name
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the user name for Basic Authentication
	 * 
	 * @param username the user name to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the password for Basic Authentication
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password for Basic Authentication
	 * 
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Gets the client ID for OpenID token requests
	 * 
	 * @return the client ID
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * Sets the client ID for OpenID token requests
	 * 
	 * @param clientId the client ID to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Gets the client secret for OpenID token requests
	 * 
	 * @return the client secret
	 */
	public String getClientSecret() {
		return clientSecret;
	}

	/**
	 * Sets the client secret for OpenID token requests
	 * 
	 * @param clientSecret the client secret to set
	 */
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	/**
	 * Indicates whether credentials for OpenID-based token requests shall be sent in request body
	 * 
	 * @return true, if credentials shall be sent in the request body, false otherwise
	 */
	public Boolean getSendAuthInBody() {
		return sendAuthInBody;
	}

	/**
	 * Indicates whether credentials for OpenID-based token requests shall be sent in request body
	 * (alias for {@link #getSendAuthInBody()})
	 * 
	 * @return true, if credentials shall be sent in the request body, false otherwise
	 */
	public Boolean hasSendAuthInBody() {
		return sendAuthInBody;
	}

	/**
	 * Sets whether credentials for OpenID-based token requests shall be sent in request body
	 * 
	 * @param sendAuthInBody set to true, if credentials shall be sent in the request body, and to false otherwise
	 */
	public void setSendAuthInBody(Boolean sendAuthInBody) {
		this.sendAuthInBody = sendAuthInBody;
	}

	/**
	 * Gets the list of product classes available from this archive
	 * 
	 * @return a list of available product classes
	 */
	public Set<ProductClass> getAvailableProductClasses() {
		return availableProductClasses;
	}

	/**
	 * Sets the list of product classes available from this archive
	 * 
	 * @param availableProductClasses the list of available product classes to set
	 */
	public void setAvailableProductClasses(Set<ProductClass> availableProductClasses) {
		this.availableProductClasses = availableProductClasses;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(code);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof ProductArchive))
			return false;
		ProductArchive other = (ProductArchive) obj;
		return Objects.equals(code, other.code);
	}

	@Override
	public String toString() {
		return "ProductArchive [code=" + code + ", name=" + name + ", archiveType=" + archiveType + ", baseUri=" + baseUri
				+ ", username=" + username + ", availableProductClasses=" + availableProductClasses + "]";
	}
	
}
