/**
 * DownloadManager.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.persistence.NoResultException;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.api.aipclient.AipClientConfiguration;
import de.dlr.proseo.api.aipclient.rest.model.RestProduct;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.IngestorMessage;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Class to handle product downloads from remote Long-term Archives
 * 
 * @author Dr. Thomas Bassler
 */
@Component
@Transactional
public class DownloadManager {
	
	/** AIP Client configuration */
	@Autowired
	private AipClientConfiguration config;
	
	/** RestTemplate for calling the LTA */
	@Autowired
	RestTemplate restTemplate;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DownloadManager.class);
	
	/**
	 * Read the processing facility with the given name from the metadata database
	 * 
	 * @param facility the processing facility name
	 * @return the processing facility found
	 * @throws IllegalArgumentException if the facility name was illegal or no such facility exists
	 */
	private ProcessingFacility readProcessingFacility(String facility) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> readProcessingFacility({})", facility);

		// Check whether the given processing facility is valid
		try {
			facility = URLDecoder.decode(facility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e));
		}
		
		final ProcessingFacility processingFacility = RepositoryService.getFacilityRepository().findByName(facility);
		if (null == processingFacility) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.INVALID_FACILITY, facility));
		}
		return processingFacility;
	}

    /**
     * Provide the product with the given file name at the given processing facility. If it already is available there, do
     * nothing and just return the product metadata. If it is not available locally, query all configured LTAs for a product
     * with the given file name, the first response is returned to the caller, then download from the LTA and ingested
     * at the given processing facility.
     * 
     * @param filename The (unique) product file name to search for
     * @param facility The processing facility to use
     * @return the product provided
     * @throws NoResultException if no products matching the given selection criteria were found
     * @throws IllegalArgumentException if an invalid processing facility name was given
     * @throws RuntimeException if the communication to the Ingestor failed TODO Check whether needed!
     */
	public RestProduct downloadByName(String filename, String facility) throws NoResultException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> downloadByName({}, {})", filename, facility);

		final ProcessingFacility processingFacility = readProcessingFacility(facility);
		
		// TODO Auto-generated method stub
		return null;
	}

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
     * @return the product provided
     * @throws NoResultException if no products matching the given selection criteria were found
     * @throws IllegalArgumentException if an invalid facility name, product type or sensing time was given
     * @throws RuntimeException if the communication to the Ingestor failed TODO Check whether needed!
     */
	public RestProduct downloadBySensingTime(String productType, String startTime, String stopTime, String facility)
			throws NoResultException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> downloadBySensingTime({}, {}, {})", startTime, stopTime, facility);

		final ProcessingFacility processingFacility = readProcessingFacility(facility);
		
		// --- Test REST service stub ---
		
		String response = restTemplate.getForObject("http://localhost:9090/info", String.class);
		RestProduct result = new RestProduct();
		result.setProductClass(response);
		
		// --- End Test REST service stub ---
		
		
		// TODO Auto-generated method stub
		return result;
	}

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
     * @return a list of the products provided from the LTA
     * @throws NoResultException if no products matching the given selection criteria were found
     * @throws IllegalArgumentException if an invalid facility name, product type or sensing time was given
     * @throws RuntimeException if the communication to the Ingestor failed TODO Check whether needed!
     */
	public List<RestProduct> downloadAllBySensingTime(String productType, String startTime, String stopTime, String facility)
			throws NoResultException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> downloadAllBySensingTime({}, {}, {}, {})", 
				productType, startTime, stopTime, facility);

		final ProcessingFacility processingFacility = readProcessingFacility(facility);
		
		// TODO Auto-generated method stub
		return null;
	}

}
