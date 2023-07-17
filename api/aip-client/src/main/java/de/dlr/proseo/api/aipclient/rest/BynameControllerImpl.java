/**
 * BynameControllerImpl.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient.rest;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.api.aipclient.AipClientSecurityConfig;
import de.dlr.proseo.api.aipclient.rest.model.RestProduct;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;

/**
 * Retrieve a single product from a remote Long-term Archive by file name
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class BynameControllerImpl implements BynameController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(BynameControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.AIP_CLIENT);

	/** Download Manager */
	@Autowired
	private DownloadManager downloadManager;

	/** Security configuration for AIP client */
	@Autowired
	private AipClientSecurityConfig securityConfig;

	/**
	 * Provide the product with the given file name at the given processing facility. If it already is available there, do nothing
	 * and just return the product metadata. If it is not available locally, query all configured LTAs for a product with the given
	 * file name, the first response is returned to the caller, then download from the LTA and ingested at the given processing
	 * facility.
	 *
	 * @param filename    The (unique) product file name to search for
	 * @param facility    The processing facility to use
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return HTTP status "CREATED" and a Json representation of the product provided or HTTP status "BAD_REQUEST", if an invalid
	 *         processing facility was given, or HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Ingestor failed or
	 *         an unexpected exception occurred
	 */
	@Override
	public ResponseEntity<RestProduct> downloadByName(String filename, String facility, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadByName({}, {})", filename, facility);

		// Get username and password from HTTP Authentication header for authentication with Production Planner
		String[] userPassword = securityConfig.parseAuthenticationHeader(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));

		try {
			return new ResponseEntity<>(downloadManager.downloadByName(filename, facility, userPassword[1]), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage())),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}