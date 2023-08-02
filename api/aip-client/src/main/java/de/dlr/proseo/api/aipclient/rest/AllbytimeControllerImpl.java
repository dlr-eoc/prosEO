/**
 * AllbytimeControllerImpl.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient.rest;

import java.util.List;

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
 * Retrieve products from a remote Long-term Archive by product type and time interval
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class AllbytimeControllerImpl implements AllbytimeController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(AllbytimeControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.AIP_CLIENT);

	/** Download Manager */
	@Autowired
	private DownloadManager downloadManager;

	/** Security configuration for AIP client */
	@Autowired
	private AipClientSecurityConfig securityConfig;

	/**
	 * Provide all products with the given product type at the given processing facility, whose sensing times intersect with the
	 * given sensing time interval. Query all configured LTAs for products with the given search criteria, the first response is
	 * evaluated. The product metadata is returned to the caller, then the products are downloaded from the LTA and ingested at the
	 * given processing facility, unless they are already available there.
	 *
	 * @param productType The product type
	 * @param startTime   The start of the sensing time interval
	 * @param stopTime    The end of the sensing time interval
	 * @param facility    The processing facility to store the downloaded product files in
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return HTTP status "OK" and a list of Json representations of the products provided or 
	 *         HTTP status "NOT_FOUND", if no products matching the given selection criteria were found, or 
	 *         HTTP status "BAD_REQUEST", if an invalid facility name, product type or sensing time was given, or 
	 *         HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Ingestor failed or an unexpected exception occurred
	 */
	@Override
	public ResponseEntity<List<RestProduct>> downloadAllBySensingTime(String productType, String startTime, String stopTime,
			String facility, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadAllBySensingTime({}, {}, {}, {})", productType, startTime, stopTime, facility);

		// Get username and password from HTTP Authentication header for authentication with Ingestor
		String[] userPassword = securityConfig.parseAuthenticationHeader(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));

		try {
			return new ResponseEntity<>(
					downloadManager.downloadAllBySensingTime(productType, startTime, stopTime, facility, userPassword[1]),
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