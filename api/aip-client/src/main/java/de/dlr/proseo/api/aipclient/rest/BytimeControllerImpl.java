/**
 * BytimeControllerImpl.java
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
 * Retrieve a single product from a remote Long-term Archive by product type and time interval
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class BytimeControllerImpl implements BytimeController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(BytimeControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.AIP_CLIENT);

	/** Download Manager */
	@Autowired
	private DownloadManager downloadManager;

	/** Security configuration for AIP client */
	@Autowired
	private AipClientSecurityConfig securityConfig;

	/**
	 * Provide the product with the given product type and the exact sensing start and stop times at the given processing facility.
	 * If it already is available there, do nothing and just return the product metadata. If it is not available locally, query all
	 * configured LTAs for a product with the given search criteria. The first response is evaluated: If multiple products
	 * fulfilling the criteria are found in the LTA, the product with the most recent generation time will be used. In the
	 * (unlikely) case of several products having the same generation time, the product with the greatest file name (alphanumeric
	 * string comparison) will be used. The product metadata is returned to the caller, then the product is downloaded from the LTA
	 * and ingested at the given processing facility.
	 *
	 * @param productType The product type
	 * @param startTime   The start of the sensing time interval
	 * @param stopTime    The end of the sensing time interval
	 * @param facility    The processing facility to store the downloaded product files in
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return HTTP status "OK" and a Json representation of the product provided or 
	 *         HTTP status "NOT_FOUND", if no product matching the given selection criteria was found, or 
	 *         HTTP status "BAD_REQUEST", if an invalid facility name, product type or sensing time was given, or 
	 *         HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Ingestor failed or an unexpected exception occurred
	 */
	@Override
	public ResponseEntity<RestProduct> downloadBySensingTime(String productType, String startTime, String stopTime, String facility,
			HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadBySensingTime({}, {}, {}, {})", productType, startTime, stopTime, facility);

		// Get username and password from HTTP Authentication header for authentication with Ingestor
		String[] userPassword = securityConfig.parseAuthenticationHeader(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));

		try {
			return new ResponseEntity<>(
					downloadManager.downloadBySensingTime(productType, startTime, stopTime, facility, userPassword[1]),
					HttpStatus.OK);
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