/**
 * AllbytimeControllerImpl.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

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
 * Retrieve products from a remote Long-term Archive by product type and time interval
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class AllbytimeControllerImpl implements AllbytimeController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(AllbytimeControllerImpl.class);
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
     * Provide all products with the given product type at the given processing facility, whose sensing times intersect with 
     * the given sensing time interval.
     * Query all configured LTAs for products with the given search criteria, the first response is evaluated.
     * The product metadata is returned to the caller, then the products are downloaded from the LTA and ingested
     * at the given processing facility, unless they are already available there.
     * 
     * @param productType The product type
     * @param startTime The start of the sensing time interval
     * @param stopTime The end of the sensing time interval
     * @param facility The processing facility to use
     * @return HTTP status "OK" and a list of Json representations of the products provided or
     *         HTTP status "BAD_REQUEST", if an invalid processing facility was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
     *         HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Ingestor failed
     */
	@Override
	public ResponseEntity<List<RestProduct>> downloadAllBySensingTime(String productType, String startTime, String stopTime,
			String facility) {
		if (logger.isTraceEnabled()) logger.trace(">>> downloadAllBySensingTime({}, {}, {}, {})",
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
			return new ResponseEntity<List<RestProduct>>(downloadManager.downloadAllBySensingTime(
					productType, startTime, stopTime, processingFacility), HttpStatus.CREATED);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
