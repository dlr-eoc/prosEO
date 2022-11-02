/**
 * BytimeControllerImpl.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.api.aipclient.AipClientConfiguration;
import de.dlr.proseo.api.aipclient.rest.model.RestProduct;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.IngestorMessage;
import de.dlr.proseo.model.ProcessingFacility;

/**
 * Retrieve a single product from a remote Long-term Archive by product type and time interval
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class BytimeControllerImpl implements BytimeController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(BytimeControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.INGESTOR);
	
	/** AIP Client configuration */
	@Autowired
	AipClientConfiguration config;
	
	/** Facility Manager */
	@Autowired
	ProcessingFacilityManager processingFacilityManager;
			
	/** Download Manager */
	@Autowired
	DownloadManager downloadManager;
			
    /**
     * Provide the product with the given product type and the exact sensing start and stop times at the given processing facility.
     * If it already is available there, do nothing and just return the product metadata.
     * If it is not available locally, query all configured LTAs for a product with the given search criteria.
     * The first response is evaluated: If multiple products fulfilling the criteria are found in the LTA, the product with the
     * most recent generation time will be used. In the (unlikely) case of several products having the same generation time,
     * the product with the greatest file name (alphanumeric string comparison) will be used.
     * The product metadata is returned to the caller, then the product is downloaded from the LTA and ingested
     * at the given processing facility.
     * 
     * @param productType The product type
     * @param startTime The start of the sensing time interval
     * @param stopTime The end of the sensing time interval
     * @param facility The processing facility to use
     * @return HTTP status "OK" and a Json representation of the product provided or
     *         HTTP status "BAD_REQUEST", if an invalid processing facility was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
     *         HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Ingestor failed
     */
	@Override
	public ResponseEntity<RestProduct> downloadBySensingTime(String productType, String startTime, String stopTime,
			String facility) {
		if (logger.isTraceEnabled()) logger.trace(">>> downloadBySensingTime({}, {}, {}, {})",
				productType, startTime, stopTime, facility);

		// Check whether the given processing facility is valid
		try {
			facility = URLDecoder.decode(facility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					http.errorHeaders(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e)), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility processingFacility = processingFacilityManager.getFacilityByName(facility);
		if (null == processingFacility) {
			return new ResponseEntity<>(
					http.errorHeaders(logger.log(IngestorMessage.INVALID_FACILITY, processingFacility)), 
					HttpStatus.BAD_REQUEST);
		}

		try {
			return new ResponseEntity<>(downloadManager.downloadBySensingTime(
					productType, startTime, stopTime, processingFacility), HttpStatus.CREATED);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}
}
