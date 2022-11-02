/**
 * BynameControllerImpl.java
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
 * Retrieve a single product from a remote Long-term Archive by file name
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class BynameControllerImpl implements BynameController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(BynameControllerImpl.class);
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
     * Provide the product with the given file name at the given processing facility. If it already is available there, do
     * nothing and just return the product metadata. If it is not available locally, query all configured LTAs for a product
     * with the given file name, the first response is returned to the caller, then download from the LTA and ingested
     * at the given processing facility.
     * 
     * @param filename The (unique) product file name to search for
     * @param facility The processing facility to use
     * @return HTTP status "OK" and a Json representation of the product provided or
     *         HTTP status "BAD_REQUEST", if an invalid processing facility was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
     *         HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Ingestor failed
     */
	@Override
	public ResponseEntity<RestProduct> downloadByName(String filename, String facility) {
		if (logger.isTraceEnabled()) logger.trace(">>> downloadByName({}, {})", filename, facility);
		
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
			return new ResponseEntity<>(downloadManager.downloadByName(filename, processingFacility), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
