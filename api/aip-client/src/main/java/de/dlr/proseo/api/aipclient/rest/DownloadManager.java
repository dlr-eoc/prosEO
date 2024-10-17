/**
 * DownloadManager.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.validation.constraints.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.api.aipclient.AipClientConfiguration;
import de.dlr.proseo.api.aipclient.rest.model.IngestorProduct;
import de.dlr.proseo.api.aipclient.rest.model.Orbit;
import de.dlr.proseo.api.aipclient.rest.model.RestConfiguredProcessor;
import de.dlr.proseo.api.aipclient.rest.model.RestDownloadHistory;
import de.dlr.proseo.api.aipclient.rest.model.RestParameter;
import de.dlr.proseo.api.aipclient.rest.model.RestProduct;
import de.dlr.proseo.api.aipclient.rest.model.RestProductFile;
import de.dlr.proseo.basewrap.MD5Util;
import de.dlr.proseo.interfaces.rest.model.RestMessage;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.AipClientMessage;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OAuthMessage;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.DownloadHistory;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.ArchiveType;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.ProductQuality;
import de.dlr.proseo.model.enums.StorageType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

/**
 * Class to handle product downloads from remote Long-term Archives
 * <br>
 * Archive queries are restricted to the logged-in mission using the first three characters of the file name,
 * as per the EO GS File Format Standard (PE-TN-ESA-GS-0001), issue 3.01, sec. 4.1 
 *
 * @author Dr. Thomas Bassler
 */
@Component
@Transactional
public class DownloadManager {

	private static final String FAILURE_NOTIFICATION_SUBJECT = "AIP Client Failure";
	// OData URL components for AIP and PRIP
	private static final String ODATA_CONTEXT = "odata/v1";
	private static final String ODATA_ENTITY_ORDERS = "Orders";
	private static final String ODATA_ENTITY_PRODUCTS = "Products";
	private static final String ODATA_FILTER_NAME = "Name eq ";
	private static final String ODATA_FILTER_ID = "Id eq ";
	private static final String ODATA_EXPAND_ATTRIBUTES = "Attributes";
	private static final int ODATA_TOP_COUNT = 1000;
	private static final String ODATA_CSC_ORDER = "OData.CSC.Order";

	// OData response properties
	private static final String ODATA_PROPERTY_ID = "Id";
	private static final String ODATA_PROPERTY_STATUS = "Status";
	private static final String ODATA_PROPERTY_ATTRIBUTES = "Attributes";
	private static final String ODATA_PROPERTY_CHECKSUM_VALUE = "Value";
	private static final String ODATA_PROPERTY_CHECKSUM_ALGORITHM = "Algorithm";
	private static final String ODATA_PROPERTY_CHECKSUM = "Checksum";
	private static final String ODATA_PROPERTY_CONTENT_LENGTH = "ContentLength";
	private static final String ODATA_PROPERTY_FILENAME = "Name";
	private static final String ODATA_PROPERTY_ONLINE = "Online";
	private static final String ODATA_PROPERTY_PUBLICATION_DATE = "PublicationDate";
	private static final String ODATA_PROPERTY_CONTENTDATE_END = "End";
	private static final String ODATA_PROPERTY_CONTENTDATE_START = "Start";
	private static final String ODATA_PROPERTY_CONTENT_DATE = "ContentDate";
	private static final String ODATA_PROPERTY_ATTRIBUTE_VALUE = "Value";
	private static final String ODATA_PROPERTY_ATTRIBUTE_VALUE_TYPE = "ValueType";
	private static final String ODATA_PROPERTY_ATTRIBUTE_NAME = "Name";

	// OData attribute names
	private static final String PRODUCT_ATTRIBUTE_PRODUCT_TYPE = "productType";
	private static final String PRODUCT_ATTRIBUTE_PROCESSING_DATE = "processingDate";
	
	// OData property values
	private static final String ORDER_STATUS_COMPLETED = "completed";
	private static final String PRODUCT_CHECKSUM_MD5 = "MD5";
	
	/** Maximum number of retries for product download */
	private static final int DOWNLOAD_MAX_RETRIES = 3;
	/** Retry interval for product downloads in ms */
	private static final int DOWNLOAD_RETRY_INTERVAL = 5000;
	
	/** OData request body for production order creation */
	private static final String ODATA_ORDER_REQUEST_BODY = "{ \"Priority\": 50 }";
	
	/** OData time format (UTC to millisecond precision with "Z" time zone) */
	private static final DateTimeFormatter ODATA_DF = 
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.of("UTC"));

	/** Mapping between OData attribute types and prosEO parameter types */
	private static final Map<String, String> ODATA_TO_PARAMTYPE_MAP = new HashMap<>();

	/** AIP Client configuration */
	@Autowired
	private AipClientConfiguration config;

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** Lookup table for products currently being downloaded from some archive */
	private static ConcurrentSkipListSet<String> productDownloads = new ConcurrentSkipListSet<>();

	/** Semaphore to limit number of parallel order requests to archive */
	private static Semaphore orderSemaphore = null;
	/** Semaphore to limit number of parallel download requests to archive */
	private static Semaphore downloadSemaphore = null;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DownloadManager.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.AIP_CLIENT);

	/**
	 * Extension to RestProduct class to include the "Online" parameter
	 */
	private static class AipRestProduct extends RestProduct {
		private static final long serialVersionUID = 1L;
		
		public Boolean online = false;
		
		public Boolean isOnline() { return online; }

		public void setOnline(Boolean online) { this.online = online; }
	}
	
	/**
	 * Fill mapping between OData types and prosEO parameter types
	 */
	{
		ODATA_TO_PARAMTYPE_MAP.put("String", ParameterType.STRING.toString());
		ODATA_TO_PARAMTYPE_MAP.put("Integer", ParameterType.INTEGER.toString());
		ODATA_TO_PARAMTYPE_MAP.put("Int64", ParameterType.INTEGER.toString());
		ODATA_TO_PARAMTYPE_MAP.put("Double", ParameterType.DOUBLE.toString());
		ODATA_TO_PARAMTYPE_MAP.put("Boolean", ParameterType.BOOLEAN.toString());
		ODATA_TO_PARAMTYPE_MAP.put("DateTimeOffset", ParameterType.INSTANT.toString());
	}

	/**
	 * Send a (failure) notification message to the configured recipient
	 *
	 * @param message the message to send
	 */
	private void notifyUser(String message) {
		if (logger.isTraceEnabled()) logger.trace(">>> notifyUser({})", message);

		try {
			// Create a request
			WebClient webClient = WebClient.create(config.getNotificationUrl() + "/notify");
			RequestBodySpec request = webClient.post().contentType(MediaType.APPLICATION_JSON).accept(MediaType.ALL);

			// Build message body
			RestMessage newMessage = new RestMessage(
					config.getNotificationRecipient(), 
					null, null,
					FAILURE_NOTIFICATION_SUBJECT,
					null, null, true,
					message,
					config.getNotificationSender());
			
			String jsonMessage = (new ObjectMapper()).writeValueAsString(newMessage);

			// Send message to notification service
			String notificationResponse = request.syncBody(jsonMessage).retrieve().bodyToMono(String.class).block();
			
			if (logger.isTraceEnabled()) logger.trace("... notification response: ", notificationResponse);
			
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass().getName() + "/" + e.getMessage());
			if (logger.isDebugEnabled()) logger.debug("Stack trace: ", e);
			// otherwise ignore (if the message does not get sent, we cannot help it, since we are in failure handling anyway)
		}
	}

	/**
	 * Read the processing facility with the given name from the metadata database
	 *
	 * @param facility the processing facility name
	 * @return the processing facility found
	 * @throws IllegalArgumentException if the facility name was illegal or no such facility exists
	 */
	private ProcessingFacility readProcessingFacility(String facility) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> readProcessingFacility({})", facility);

		// Check whether the given processing facility is valid
		try {
			facility = URLDecoder.decode(facility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e));
		}

		final ProcessingFacility processingFacility = RepositoryService.getFacilityRepository().findByName(facility);
		if (null == processingFacility) {
			throw new IllegalArgumentException(logger.log(AipClientMessage.INVALID_FACILITY, facility));
		}
		return processingFacility;
	}

	/**
	 * Query the database for a product class of the given type
	 *
	 * @param productType the product type to look for
	 * @return the product class from the metadata database
	 * @throws IllegalArgumentException if no product class with the given type can be found for the user's mission
	 */
	private ProductClass readProductClass(String productType) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> readProductClass({})", productType);

		ProductClass productClass = RepositoryService.getProductClassRepository()
			.findByMissionCodeAndProductType(securityService.getMission(), productType);

		if (null == productClass) {
			throw new IllegalArgumentException(
					logger.log(AipClientMessage.INVALID_PRODUCT_TYPE, securityService.getMission(), productType));
		}

		return productClass;
	}

	/**
	 * Query the database for products of the given type, which exactly match the given sensing time period and are available at the
	 * given processing facility.
	 *
	 * @param productType        the product type to query for
	 * @param earliestStart      the beginning of the requested or sensing time period (to millisecond precision)
	 * @param earliestStop       the end of the requested or sensing time period (to millisecond precision)
	 * @param processingFacility the processing facility to look for
	 * @return the (first) product found or null, if no product can be found for the given criteria
	 * @throws IllegalArgumentException if the start and/or stop time cannot be parsed
	 */
	private Product findProductBySensingTime(String productType, Instant earliestStart, Instant earliestStop,
			final ProcessingFacility processingFacility) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> findProductBySensingTime({}, {}, {}, {})", productType, earliestStart, earliestStop,
					(null == processingFacility ? "NULL" : processingFacility.getName()));

		Product modelProduct = null;

		// Try requested (nominal) product start and stop times
		List<Product> productList = RepositoryService.getProductRepository()
			.findByMissionCodeAndProductTypeAndRequestedStartTimeBetween(securityService.getMission(), productType, earliestStart,
					earliestStart.plusNanos(999999L));

		OUTER1: for (Product product : productList) {
			if (!product.getRequestedStopTime().isBefore(earliestStop)
					&& earliestStop.plusMillis(1L).isAfter(product.getRequestedStopTime())) {
				// Time frame OK, check availability at processing facility
				for (ProductFile productFile : product.getProductFile()) {
					if (productFile.getProcessingFacility().equals(processingFacility)) {
						modelProduct = product;
						break OUTER1;
					}
				}
			}
		}

		if (null == modelProduct) {
			// Try actual (sensing data) start and stop times
			productList = RepositoryService.getProductRepository()
				.findByMissionCodeAndProductTypeAndSensingStartTimeBetween(securityService.getMission(), productType, earliestStart,
						earliestStart.plusNanos(999999L));

			OUTER2: for (Product product : productList) {
				if (!product.getSensingStopTime().isBefore(earliestStop)
						&& earliestStop.plusMillis(1L).isAfter(product.getSensingStopTime())) {
					// Time frame OK, check availability at processing facility
					for (ProductFile productFile : product.getProductFile()) {
						if (productFile.getProcessingFacility().equals(processingFacility)) {
							modelProduct = product;
							break OUTER2;
						}
					}
				}
			}
		}
		return modelProduct;
	}

	/**
	 * Query the database for products of the given type, which intersect the given sensing time period and are available at the
	 * given processing facility.
	 *
	 * @param productType        the product type to query for
	 * @param earliestStart      the beginning of the requested or sensing time period (to millisecond precision)
	 * @param earliestStop       the end of the requested or sensing time period (to millisecond precision)
	 * @param processingFacility the processing facility to look for
	 * @return a list of products found (may be empty, if no product can be found for the given criteria)
	 * @throws IllegalArgumentException if the start and/or stop time cannot be parsed
	 */
	private List<Product> findAllProductsBySensingTime(String productType, Instant earliestStart, Instant earliestStop,
			ProcessingFacility processingFacility) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findAllProductsBySensingTime({}, {}, {}, {})", productType, earliestStart, earliestStop,
					(null == processingFacility ? "NULL" : processingFacility.getName()));

		// Try requested (nominal) and actual (sensing data) product start and stop times
		List<Product> productList = RepositoryService.getProductRepository()
			.findByMissionCodeAndProductTypeAndRequestedStartTimeLessAndRequestedStopTimeGreater(securityService.getMission(),
					productType, earliestStop.plusNanos(999999L), earliestStart);
		productList.addAll(RepositoryService.getProductRepository()
			.findByMissionCodeAndProductTypeAndSensingStartTimeLessAndSensingStopTimeGreater(securityService.getMission(),
					productType, earliestStop.plusNanos(999999L), earliestStart));

		Iterator<Product> productIter = productList.iterator();

		// Remove products not available at the requested processing facility
		while (productIter.hasNext()) {
			Product product = productIter.next();

			boolean found = false;
			for (ProductFile productFile : product.getProductFile()) {
				if (productFile.getProcessingFacility().equals(processingFacility)) {
					found = true;
					break;
				}
			}
			if (!found) {
				productIter.remove();
			}
		}

		return productList;
	}
	
	/**
	 * Check the given list of products against another product and return the product, which matches the other product and
	 * has a product file at the given processing facility; it can be assumed that all products in the list are of the same
	 * mission and product class as the other product (due to selection by findAllProductsBySensingTime()).
	 * 
	 * @param modelProducts the list of products to check
	 * @param restProduct the product to check against
	 * @param processingFacility the processing facility, at which the product file shall be present
	 * @return the product found converted to a REST product or null, if no such product exists
	 */
	private RestProduct findLocalProductAtFacility(List<Product> modelProducts, RestProduct restProduct, ProcessingFacility processingFacility) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findLocalProductAtFacility(Product[{}], {}, {})",
					(null == modelProducts ? "MISSING" : modelProducts.size()),
					(null == restProduct ? "null" : restProduct.getProductClass()),
					(null == processingFacility ? "null" : processingFacility.getName()));
		
		// Get sensing start and stop times and generation time to check against
		Instant sensingStartTime = OrbitTimeFormatter.parseDateTime(restProduct.getSensingStartTime());
		// Instant sensingStopTime = OrbitTimeFormatter.parseDateTime(restProduct.getSensingStopTime());
		Instant generationTime = OrbitTimeFormatter.parseDateTime(restProduct.getGenerationTime());
		
		// Travel through the list of products
		for (Product modelProduct: modelProducts) {
			if (sensingStartTime.equals(modelProduct.getSensingStartTime())
					// && sensingStopTime.equals(modelProduct.getSensingStopTime())
					&& generationTime.equals(modelProduct.getGenerationTime())) {
				
				// Candidate product found, check product files
				for (ProductFile modelProductFile: modelProduct.getProductFile()) {
					if (modelProductFile.getProcessingFacility().equals(processingFacility)) {
						return toRestProduct(modelProduct);
					}
				}
				
			}
		}

		// No matching product or product file found
		return null;
	}

	/**
	 * Convert a prosEO model product file into a REST product file
	 *
	 * @param modelProductFile the prosEO model product
	 * @return an equivalent REST product or null, if no model product was given
	 */
	private static RestProductFile toRestProductFile(ProductFile modelProductFile) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toRestProductFile({})", (null == modelProductFile ? "MISSING" : modelProductFile.getId()));

		if (null == modelProductFile)
			return null;

		RestProductFile restProductFile = new RestProductFile();

		restProductFile.setId(modelProductFile.getId());
		restProductFile.setProductId(modelProductFile.getProduct().getId());
		restProductFile.setVersion(Long.valueOf(modelProductFile.getVersion()));
		restProductFile.setProcessingFacilityName(modelProductFile.getProcessingFacility().getName());
		restProductFile.setProductFileName(modelProductFile.getProductFileName());
		restProductFile.setFilePath(modelProductFile.getFilePath());
		restProductFile.setStorageType(modelProductFile.getStorageType().toString());
		restProductFile.setFileSize(modelProductFile.getFileSize());
		if (null != modelProductFile.getChecksumTime()) {
			restProductFile.setChecksum(modelProductFile.getChecksum());
			restProductFile.setChecksumTime(OrbitTimeFormatter.format(modelProductFile.getChecksumTime()));
		}
		restProductFile.getAuxFileNames().addAll(modelProductFile.getAuxFileNames());

		if (null != modelProductFile.getZipFileName()) {
			restProductFile.setZipFileName(modelProductFile.getZipFileName());
			restProductFile.setZipFileSize(modelProductFile.getZipFileSize());
			restProductFile.setZipChecksum(modelProductFile.getZipChecksum());
			restProductFile.setZipChecksumTime(OrbitTimeFormatter.format(modelProductFile.getZipChecksumTime()));
		}

		return restProductFile;
	}

	/**
	 * Convert a database model product to a REST interface product
	 *
	 * @param modelProduct the product to convert
	 * @return the corresponding REST interface product
	 */
	private RestProduct toRestProduct(Product modelProduct) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toRestProduct({})", (null == modelProduct ? "MISSING" : modelProduct.getId()));

		if (null == modelProduct)
			return null;

		RestProduct restProduct = new RestProduct();

		restProduct.setId(modelProduct.getId());
		restProduct.setVersion(Long.valueOf(modelProduct.getVersion()));
		if (null != modelProduct.getUuid()) {
			restProduct.setUuid(modelProduct.getUuid().toString());
		}
		if (null != modelProduct.getProductClass()) {
			if (null != modelProduct.getProductClass().getMission()) {
				restProduct.setMissionCode(modelProduct.getProductClass().getMission().getCode());
			}
			restProduct.setProductClass(modelProduct.getProductClass().getProductType());
		}
		restProduct.setFileClass(modelProduct.getFileClass());
		restProduct.setMode(modelProduct.getMode());
		if (null != modelProduct.getProductClass().getProcessingLevel()) {
			restProduct.setProcessingLevel(modelProduct.getProductClass().getProcessingLevel().toString());
		}
		if (null != modelProduct.getProductQuality()) {
			restProduct.setProductQuality(modelProduct.getProductQuality().toString());
		}
		if (null != modelProduct.getSensingStartTime()) {
			restProduct.setSensingStartTime(OrbitTimeFormatter.format(modelProduct.getSensingStartTime()));
		}
		if (null != modelProduct.getSensingStopTime()) {
			restProduct.setSensingStopTime(OrbitTimeFormatter.format(modelProduct.getSensingStopTime()));
		}
		if (null != modelProduct.getRawDataAvailabilityTime()) {
			restProduct.setRawDataAvailabilityTime(OrbitTimeFormatter.format(modelProduct.getRawDataAvailabilityTime()));
		}
		if (null != modelProduct.getGenerationTime()) {
			restProduct.setGenerationTime(OrbitTimeFormatter.format(modelProduct.getGenerationTime()));
		}
		if (null != modelProduct.getPublicationTime()) {
			restProduct.setPublicationTime(OrbitTimeFormatter.format(modelProduct.getPublicationTime()));
		}
		if (null != modelProduct.getEvictionTime()) {
			restProduct.setEvictionTime(OrbitTimeFormatter.format(modelProduct.getEvictionTime()));
		}
		for (DownloadHistory historyEntry : modelProduct.getDownloadHistory()) {
			RestDownloadHistory restHistoryEntry = new RestDownloadHistory();
			restHistoryEntry.setUsername(historyEntry.getUsername());
			restHistoryEntry.setProductFileName(historyEntry.getProductFileName());
			restHistoryEntry.setProductFileSize(historyEntry.getProductFileSize());
			restHistoryEntry.setDateTime(OrbitTimeFormatter.format(historyEntry.getDateTime()));
			restProduct.getDownloadHistory().add(restHistoryEntry);
		}
		if (null != modelProduct.getProductionType()) {
			restProduct.setProductionType(modelProduct.getProductionType().toString());
		}
		for (Product componentProduct : modelProduct.getComponentProducts()) {
			restProduct.getComponentProductIds().add(componentProduct.getId());
		}
		if (null != modelProduct.getEnclosingProduct()) {
			restProduct.setEnclosingProductId(modelProduct.getEnclosingProduct().getId());
		}
		if (null != modelProduct.getOrbit()) {
			Orbit restOrbit = new Orbit();
			de.dlr.proseo.model.Orbit modelOrbit = modelProduct.getOrbit();
			restOrbit.setOrbitNumber(Long.valueOf(modelOrbit.getOrbitNumber()));
			restOrbit.setSpacecraftCode(modelOrbit.getSpacecraft().getCode());
			restProduct.setOrbit(restOrbit);
		}
		for (ProductFile modelFile : modelProduct.getProductFile()) {
			restProduct.getProductFile().add(toRestProductFile(modelFile));
		}
		if (null != modelProduct.getConfiguredProcessor()) {
			ConfiguredProcessor modelConfiguredProcessor = modelProduct.getConfiguredProcessor();
			RestConfiguredProcessor restConfiguredProcessor = new RestConfiguredProcessor();
			restConfiguredProcessor.setId(modelConfiguredProcessor.getId());
			restConfiguredProcessor.setVersion(Long.valueOf(modelConfiguredProcessor.getVersion()));
			restConfiguredProcessor.setIdentifier(modelConfiguredProcessor.getIdentifier());
			restConfiguredProcessor
				.setProcessorName(modelConfiguredProcessor.getProcessor().getProcessorClass().getProcessorName());
			restConfiguredProcessor.setProcessorVersion(modelConfiguredProcessor.getProcessor().getProcessorVersion());
			restConfiguredProcessor.setConfigurationVersion(modelConfiguredProcessor.getConfiguration().getConfigurationVersion());
			restProduct.setConfiguredProcessor(restConfiguredProcessor);
		}
		for (String productParameterKey : modelProduct.getParameters().keySet()) {
			RestParameter restParameter = new RestParameter(productParameterKey,
					modelProduct.getParameters().get(productParameterKey).getParameterType().toString(),
					modelProduct.getParameters().get(productParameterKey).getParameterValue().toString());
			restProduct.getParameters().add(restParameter);
		}

		return restProduct;
	}

	/**
	 * Convert an OData product representation to a REST interface product
	 *
	 * @param product  the product to convert
	 * @param facility the processing facility to link the REST interface product to
	 * @param attributes flag to fill attributes
	 * @return the corresponding REST interface product
	 */
	private AipRestProduct toRestProduct(ClientEntity product, ProcessingFacility facility, Boolean attributes) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toRestProduct({})", (null == product ? "MISSING" : product.getProperty(ODATA_PROPERTY_ID)));

		if (null == product)
			return null;

		AipRestProduct restProduct = new AipRestProduct();

		try {
			try {
				restProduct.setUuid(product.getProperty(ODATA_PROPERTY_ID).getPrimitiveValue().toCastValue(String.class));
			} catch (EdmPrimitiveTypeException | NullPointerException e) {
				logger.log(AipClientMessage.PRODUCT_UUID_MISSING, product.toString());
				return null;
			}

			try {
				restProduct.setSensingStartTime(OrbitTimeFormatter.format(
					Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(
						product.getProperty(ODATA_PROPERTY_CONTENT_DATE)
							.getComplexValue()
							.get(ODATA_PROPERTY_CONTENTDATE_START)
							.getPrimitiveValue()
							.toCastValue(String.class)))));
			} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
				logger.log(AipClientMessage.PRODUCT_VAL_START_MISSING, product.toString());
				return null;
			}
			try {
				restProduct.setSensingStopTime(OrbitTimeFormatter.format(
					Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(
						product.getProperty(ODATA_PROPERTY_CONTENT_DATE)
							.getComplexValue()
							.get(ODATA_PROPERTY_CONTENTDATE_END)
							.getPrimitiveValue()
							.toCastValue(String.class)))));
			} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
				logger.log(AipClientMessage.PRODUCT_VAL_STOP_MISSING, product.toString());
				return null;
			}
			try {
				restProduct.setPublicationTime(OrbitTimeFormatter
					.format(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(
							product.getProperty(ODATA_PROPERTY_PUBLICATION_DATE).getPrimitiveValue().toCastValue(String.class)))));
			} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
				logger.log(AipClientMessage.PRODUCT_PUBLICATION_MISSING, product.toString());
				return null;
			}
			try {
				restProduct.setOnline(product.getProperty(ODATA_PROPERTY_ONLINE).getPrimitiveValue().toCastValue(Boolean.class));
			} catch (EdmPrimitiveTypeException | NullPointerException e) {
				logger.log(AipClientMessage.PRODUCT_ONLINEFLAG_MISSING, product.toString());
				return null;
			}

			// Create product file sub-structure
			RestProductFile restProductFile = new RestProductFile();
			try {
				restProductFile.setProductFileName(product.getProperty(ODATA_PROPERTY_FILENAME).getPrimitiveValue().toCastValue(String.class));
			} catch (EdmPrimitiveTypeException | NullPointerException e) {
				logger.log(AipClientMessage.PRODUCT_FILENAME_MISSING, product.toString());
				return null;
			}
			try {
				restProductFile.setFileSize(product.getProperty(ODATA_PROPERTY_CONTENT_LENGTH).getPrimitiveValue().toCastValue(Long.class));
			} catch (EdmPrimitiveTypeException | NullPointerException e) {
				logger.log(AipClientMessage.PRODUCT_SIZE_MISSING, product.toString());
				return null;
			}
			restProductFile.setProcessingFacilityName(facility.getName());

			restProductFile.setChecksum(null);
			try {
				product.getProperty(ODATA_PROPERTY_CHECKSUM).getCollectionValue().forEach(clientValue -> {
					try {
						if (PRODUCT_CHECKSUM_MD5.equals(clientValue.asComplex().get(ODATA_PROPERTY_CHECKSUM_ALGORITHM).getPrimitiveValue().toCastValue(String.class))) {
							restProductFile
								.setChecksum(clientValue.asComplex().get(ODATA_PROPERTY_CHECKSUM_VALUE).getPrimitiveValue().toCastValue(String.class));
							restProductFile.setChecksumTime(OrbitTimeFormatter.format(Instant
								.parse(clientValue.asComplex().get("ChecksumDate").getPrimitiveValue().toCastValue(String.class))));
						}
					} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
						logger.log(AipClientMessage.PRODUCT_HASH_MISSING, product.toString());
					}
				});
			} catch (NullPointerException e) {
				logger.log(AipClientMessage.PRODUCT_HASH_MISSING, product.toString());
				return null;
			}
			if (null == restProductFile.getChecksum()) {
				logger.log(AipClientMessage.PRODUCT_HASH_MISSING, product.toString());
				return null;
			}
			if (logger.isTraceEnabled())
				logger.trace("... checksum = {}", restProductFile.getChecksum());

			restProduct.getProductFile().add(restProductFile);

			// Gather values from "Attributes" list
			if (attributes) {
				try {
					if (logger.isTraceEnabled())
						logger.trace("... Attributes = {} ", product.getProperty(ODATA_PROPERTY_ATTRIBUTES));
					if (logger.isTraceEnabled())
						logger.trace("... collection value = {} ", product.getProperty(ODATA_PROPERTY_ATTRIBUTES).getCollectionValue());

					product.getProperty(ODATA_PROPERTY_ATTRIBUTES).getCollectionValue().forEach(clientValue -> {
						String attributeName = clientValue.asComplex().get(ODATA_PROPERTY_ATTRIBUTE_NAME).getValue().toString();
						String attributeType = clientValue.asComplex().get(ODATA_PROPERTY_ATTRIBUTE_VALUE_TYPE).getValue().toString();
						String attributeValue = clientValue.asComplex().get(ODATA_PROPERTY_ATTRIBUTE_VALUE).getValue().toString();
						if (logger.isTraceEnabled())
							logger.trace("... found attribute {}", clientValue);
						if (logger.isTraceEnabled())
							logger.trace("    ... with Name {}", attributeName);
						switch (attributeName) {
						case PRODUCT_ATTRIBUTE_PROCESSING_DATE:
							try {
								restProduct.setGenerationTime(OrbitTimeFormatter.format(
										Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(attributeValue))));
							} catch (DateTimeParseException e) {
								logger.log(AipClientMessage.PRODUCT_GENERATION_MISSING, product.toString());
							}
							break;
						case PRODUCT_ATTRIBUTE_PRODUCT_TYPE:
							restProduct.setProductClass(attributeValue);
							break;
						default:
							RestParameter param = new RestParameter(attributeName, ODATA_TO_PARAMTYPE_MAP.get(attributeType),
									attributeValue);
							restProduct.getParameters().add(param);
						}

					});
				} catch (NullPointerException e) {
					logger.log(AipClientMessage.PRODUCT_ATTRIBUTES_MISSING, product.toString());
					return null;
				}
			}
		} catch (DateTimeException e) {
			logger.log(AipClientMessage.DATE_NOT_PARSEABLE, product);
			return null;
		} catch (NullPointerException e) {
			logger.log(AipClientMessage.MANDATORY_ELEMENT_MISSING, product);
			return null;
		}
		
		// Make sure generation time exists (retrieved from list of attributes, so no systematic property access)
		if (null == restProduct.getGenerationTime()) {
			logger.log(AipClientMessage.PRODUCT_GENERATION_MISSING, product.toString());
			return null;
		}

		return restProduct;
	}

	/**
	 * Request a bearer token from the given product archive
	 *
	 * @param archive the archive to request the token from
	 * @return the bearer token as received from the archive, or null, if the request failed
	 */
	private String getBearerToken(ProductArchive archive) {
		if (logger.isTraceEnabled()) logger.trace(">>> getBearerToken()");

		// Create a request
		WebClient webClient = WebClient.create(archive.getBaseUri());
		RequestBodySpec request = webClient.post().uri(archive.getTokenUri()).accept(MediaType.APPLICATION_JSON);

		// Set username and password as query parameters
		MultiValueMap<String, String> queryVariables = new LinkedMultiValueMap<>();

		queryVariables.add("grant_type", "password");
		queryVariables.add("username", archive.getUsername());
		queryVariables.add("password", archive.getPassword());

		// Add client credentials, if OpenID is required for login, otherwise prepare Basic Auth with username/password
		if (null == archive.getClientId()) {
			String base64Auth = new String(
					Base64.getEncoder().encode((archive.getUsername() + ":" + archive.getPassword()).getBytes()));
			request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + base64Auth);
			logger.trace("... Auth: '{}'", base64Auth);
		} else {
			queryVariables.add("scope", "openid");
			if (archive.getSendAuthInBody()) {
				queryVariables.add("client_id", archive.getClientId());
				queryVariables.add("client_secret", URLEncoder.encode(archive.getClientSecret(), Charset.defaultCharset()));
			} else {
				String base64Auth = new String(
						Base64.getEncoder().encode((archive.getClientId() + ":" + archive.getClientSecret()).getBytes()));
				request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + base64Auth);
				logger.trace("... Auth: '{}'", base64Auth);
			}
		}
		if (logger.isTraceEnabled())
			logger.trace("... using query variables '{}'", queryVariables);

		// Perform token request
		String tokenResponse;
		try {
			tokenResponse = request.body(BodyInserters.fromFormData(queryVariables)).retrieve().bodyToMono(String.class).block();
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass().getName() + "/" + e.getMessage());
			if (logger.isDebugEnabled())
				logger.debug("Stack trace: ", e);
			return null;
		}
		if (null == tokenResponse) {
			logger.log(OAuthMessage.TOKEN_REQUEST_FAILED, archive.getBaseUri() + "/" + archive.getTokenUri());
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... got token response '{}'", tokenResponse);

		// Analyse the result
		ObjectMapper om = new ObjectMapper();
		Map<?, ?> tokenResponseMap = null;
		try {
			tokenResponseMap = om.readValue(tokenResponse, Map.class);
		} catch (IOException e) {
			logger.log(OAuthMessage.TOKEN_RESPONSE_INVALID, tokenResponse, archive.getBaseUri() + "/" + archive.getTokenUri(),
					e.getMessage());
			return null;
		}
		if (null == tokenResponseMap || tokenResponseMap.isEmpty()) {
			logger.log(OAuthMessage.TOKEN_RESPONSE_EMPTY, tokenResponse, archive.getBaseUri() + "/" + archive.getTokenUri());
			return null;
		}
		Object accessToken = tokenResponseMap.get("access_token");
		if (null == accessToken || !(accessToken instanceof String)) {
			logger.log(OAuthMessage.ACCESS_TOKEN_MISSING, tokenResponse, archive.getBaseUri() + "/" + archive.getTokenUri());
			return null;
		} else {
//			if (logger.isTraceEnabled()) logger.trace("... found access token {}", accessToken);
			return (String) accessToken;
		}
	}

	/**
	 * Send a product order request to the given archive
	 *
	 * @param archive    the archive to address
	 * @param requestUri the URI for the request (only part after 'odata/v1')
	 * @return the request response as key-value map
	 * @throws IOException if an error occurred during
	 */
	private Map<?, ?> createOrder(ProductArchive archive, String requestUri) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createOrder({}, {})", (null == archive ? "NULL" : archive.getCode()), requestUri);

		// Create a request
		String fullRequestUri = archive.getBaseUri() 
				+ (null == archive.getContext() || archive.getContext().isBlank() ? "" : "/" + archive.getContext())
				+ "/" + ODATA_CONTEXT
				+ "/" + requestUri;
		
		WebClient webClient = WebClient.create(archive.getBaseUri());
		RequestBodySpec request = webClient.post()
				.uri(fullRequestUri)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
		
		String authorizationHeader = archive.isTokenRequired() ?
				"Bearer " + getBearerToken(archive) : 
    			"Basic " + Base64.getEncoder().encodeToString((archive.getUsername() + ":" + archive.getPassword()).getBytes());
		request = request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);

		// Perform product order request
		Map<?, ?> createResponse;
		
		// Check whether parallel execution is allowed
		try {
			downloadSemaphore.acquire();
			if (logger.isDebugEnabled())
				logger.debug("... file download semaphore {} acquired, {} permits remaining",
						downloadSemaphore, downloadSemaphore.availablePermits());
		} catch (InterruptedException e) {
			throw new IOException(logger.log(ApiMonitorMessage.ABORTING_TASK, e.toString()));
		}
		
		try {
			if (logger.isDebugEnabled()) logger.debug("... sending OData request '{}'", fullRequestUri);
			
			createResponse = request.body(BodyInserters.fromObject(ODATA_ORDER_REQUEST_BODY))
				.retrieve()
				.bodyToMono(Map.class)
				.block();
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass().getName() + "/" + e.getMessage());
			if (logger.isDebugEnabled())
				logger.debug("Stack trace: ", e);
			throw new IOException(message);
		} finally {
			// Release parallel thread
			downloadSemaphore.release();
			if (logger.isDebugEnabled())
				logger.debug("... file download semaphore {} released, {} permits now available",
						downloadSemaphore, downloadSemaphore.availablePermits());
		}

		if (null == createResponse) {
			throw new IOException(
					logger.log(AipClientMessage.ORDER_REQUEST_FAILED, archive.getBaseUri() + "/" + archive.getBaseUri()));
		}
		if (logger.isTraceEnabled())
			logger.trace("... got create response '{}'", createResponse);

		return createResponse;
	}
	
	/**
	 * Mission specific additional query filters to inject into OData query
	 * 
	 * This method is intended for overriding by mission-specific extensions of this class.
	 * 
	 * @param missionCode the applicable mission
	 * @param productType the product type to query data for
	 * @return a string containing additional filter elements (default implementation is empty string)
	 */
	protected String getMissionSpecificFilters(String missionCode, String productType) {
		if (logger.isTraceEnabled()) logger.trace(">>> getMissionSpecificFilters({}, {})", missionCode, productType);
		
		return "";
	}

	/**
	 * Send the given request to an archive
	 *
	 * @param archive     the archive to address
	 * @param queryEntity the entity to query for
	 * @param queryFilter the query filter to send
	 * @param expandAttributes flag to expand the attributes
	 * @return a possibly empty list of products (as attribute maps) or null, if an invalid response was received
	 * @throws IOException in case of a communication error
	 */
	private List<ClientEntity> queryArchive(ProductArchive archive, String queryEntity, String queryFilter,
			Boolean expandAttributes) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> queryArchive({}, {}, {}, {})", (null == archive ? "NULL" : archive.getCode()), 
					queryEntity, queryFilter, expandAttributes);

		// Prepare OData request
		ODataClient oDataClient = ODataClientFactory.getClient();
		oDataClient.getConfiguration().setDefaultPubFormat(ContentType.APPLICATION_JSON);

		String oDataServiceRoot = archive.getBaseUri() + "/" + (archive.getContext().isBlank() ? "" : archive.getContext() + "/")
				+ ODATA_CONTEXT;

		URIBuilder uriBuilder = oDataClient.newURIBuilder(oDataServiceRoot)
			.appendEntitySetSegment(queryEntity)
			.addQueryOption(QueryOption.FILTER, queryFilter.toString())
			.addQueryOption(QueryOption.COUNT, "true")
			.top(ODATA_TOP_COUNT);

		if (expandAttributes && ODATA_ENTITY_PRODUCTS.equals(queryEntity)) {
			uriBuilder = uriBuilder.expand(ODATA_EXPAND_ATTRIBUTES);
		}

		String authorizationHeader = archive.isTokenRequired() ?
				"Bearer " + getBearerToken(archive) : 
    			"Basic " + Base64.getEncoder().encodeToString((archive.getUsername() + ":" + archive.getPassword()).getBytes());
		
		// Retrieve products
		if (logger.isTraceEnabled())
			logger.trace("... requesting {} list at URL '{}'", queryEntity, oDataServiceRoot);

		ODataEntitySetRequest<ClientEntitySet> request = oDataClient.getRetrieveRequestFactory()
			.getEntitySetRequest(uriBuilder.build());
		request.addCustomHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

		if (logger.isTraceEnabled())
			logger.trace("... sending OData request '{}'", request.getURI());

		Future<ODataRetrieveResponse<ClientEntitySet>> futureResponse = request.asyncExecute();
		ODataRetrieveResponse<ClientEntitySet> response = null;
		try {
			response = futureResponse.get(config.getArchiveTimeout(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new IOException(
					logger.log(AipClientMessage.ODATA_REQUEST_ABORTED, request.getURI(), e.getClass().getName(), e.getMessage()));
		}

		if (HttpStatus.OK.value() != response.getStatusCode()) {
			String message = null;
			try {
				message = logger.log(AipClientMessage.ODATA_REQUEST_FAILED, request.getURI(), response.getStatusCode(),
						new String(response.getRawResponse().readAllBytes()));
			} catch (IOException e) {
				message = logger.log(AipClientMessage.ODATA_RESPONSE_UNREADABLE);
			}
			throw new IOException(message);
		}

		ClientEntitySet entitySet = response.getBody();
		logger.log(AipClientMessage.RETRIEVAL_RESULT, entitySet.getEntities().size(), entitySet.getCount());

		return entitySet.getEntities();
	}

	/**
	 * Send the given request to an archive
	 *
	 * @param archive     the archive to address
	 * @param queryEntity the entity to query for
	 * @param uuid the uuid to send
	 * @param expandAttributes flag to expand the attributes
	 * @return a possibly empty list of products (as attribute maps) or null, if an invalid response was received
	 * @throws IOException in case of a communication error
	 */
	private ClientEntity queryArchiveForSingleEntity(ProductArchive archive, String queryEntity, String uuid,
			Boolean expandAttributes) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> queryArchiveForSingleEntity({}, {}, {}, {})", (null == archive ? "NULL" : archive.getCode()), 
					queryEntity, uuid, expandAttributes);

		// Prepare OData request
		ODataClient oDataClient = ODataClientFactory.getClient();
		oDataClient.getConfiguration().setDefaultPubFormat(ContentType.APPLICATION_JSON);

		String oDataServiceRoot = archive.getBaseUri() + "/" + (archive.getContext().isBlank() ? "" : archive.getContext() + "/")
				+ ODATA_CONTEXT
				+ "/" + queryEntity + "(" + uuid + ")";

		URIBuilder uriBuilder = oDataClient.newURIBuilder(oDataServiceRoot);

		if (expandAttributes && ODATA_ENTITY_PRODUCTS.equals(queryEntity)) {
			uriBuilder = uriBuilder.expand(ODATA_EXPAND_ATTRIBUTES);
		}

		String authorizationHeader = archive.isTokenRequired() ?
				"Bearer " + getBearerToken(archive) : 
    			"Basic " + Base64.getEncoder().encodeToString((archive.getUsername() + ":" + archive.getPassword()).getBytes());
		
		// Retrieve products
		if (logger.isTraceEnabled())
			logger.trace("... requesting {} list at URL '{}'", queryEntity, oDataServiceRoot);

		ODataEntityRequest<ClientEntity> request = oDataClient.getRetrieveRequestFactory()
			.getEntityRequest(uriBuilder.build());
		request.addCustomHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

		if (logger.isTraceEnabled())
			logger.trace("... sending OData request '{}'", request.getURI());

		Future<ODataRetrieveResponse<ClientEntity>> futureResponse = request.asyncExecute();
		ODataRetrieveResponse<ClientEntity> response = null;
		try {
			response = futureResponse.get(config.getArchiveTimeout(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new IOException(
					logger.log(AipClientMessage.ODATA_REQUEST_ABORTED, request.getURI(), e.getClass().getName(), e.getMessage()));
		}

		if (HttpStatus.OK.value() != response.getStatusCode()) {
			String message = null;
			try {
				message = logger.log(AipClientMessage.ODATA_REQUEST_FAILED, request.getURI(), response.getStatusCode(),
						new String(response.getRawResponse().readAllBytes()));
			} catch (IOException e) {
				message = logger.log(AipClientMessage.ODATA_RESPONSE_UNREADABLE);
			}
			throw new IOException(message);
		}

		ClientEntity entity = response.getBody();
		logger.log(AipClientMessage.RETRIEVAL_RESULT, "1", entity==null?"0":"1");

		return entity;
	}

	/**
	 * Create a product order with the given long-term archive and wait for its completion
	 *
	 * @param archive     the long-term archive to address
	 * @param productUuid the UUID of the product to fetch
	 * @throws InterruptedException if the process is interrupted while waiting for order completion
	 * @throws IOException          if an error in the communication with the archive occurs
	 */
	private void createProductOrderAndWait(ProductArchive archive,
			@Pattern(regexp = "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$") String productUuid)
			throws InterruptedException, IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createProductOrderAndWait({}, {})", (null == archive ? "NULL" : archive.getCode()), productUuid);

		// Build the request URL
		StringBuilder requestUrl = new StringBuilder(ODATA_ENTITY_PRODUCTS);
		requestUrl.append('(').append(productUuid).append(')').append('/').append(ODATA_CSC_ORDER);

		// Check whether parallel execution is allowed
		try {
			orderSemaphore.acquire();
			if (logger.isDebugEnabled())
				logger.debug("... archive order semaphore {} acquired, {} permits remaining",
						orderSemaphore, orderSemaphore.availablePermits());
		} catch (InterruptedException e) {
			throw new IOException(logger.log(ApiMonitorMessage.ABORTING_TASK, e.toString()));
		}

		try {
			
			// Start the product order request
			Map<?, ?> response;
			try {
				
				response = createOrder(archive, requestUrl.toString());
				
			} catch (IOException e) {
				// Already logged
				throw e;
			} catch (Exception e) {
				if (logger.isDebugEnabled())
					logger.debug("Stack trace: ", e);
				throw new IOException(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass().getName() + "/" + e.getMessage()));
			}

			String orderUuid = response.get(ODATA_PROPERTY_ID).toString();
			String orderStatus = response.get(ODATA_PROPERTY_STATUS).toString();

			// Wait for the product order to complete
			while (!ORDER_STATUS_COMPLETED.equals(orderStatus)) {
				logger.log(AipClientMessage.WAITING_FOR_PRODUCT_ORDER, orderUuid, orderStatus);

				try {
					Thread.sleep(config.getOrderCheckInterval());
				} catch (InterruptedException e) {
					logger.log(AipClientMessage.ORDER_WAIT_INTERRUPTED, orderUuid);
					throw e;
				}

				// Check order status
				List<ClientEntity> orderList = queryArchive(archive, ODATA_ENTITY_ORDERS, ODATA_FILTER_ID + orderUuid, true);
				
				if (null == orderList || 1 != orderList.size()) {
					throw new RuntimeException(logger.log(AipClientMessage.INVALID_ODATA_RESPONSE, orderList, archive.getCode()));
				}
				ClientEntity order = orderList.get(0);
				
				if (logger.isTraceEnabled()) logger.trace("... evaluating result object: {}", order);

				try {
					orderUuid = order.getProperty(ODATA_PROPERTY_ID).getPrimitiveValue().toCastValue(String.class);
					orderStatus = order.getProperty(ODATA_PROPERTY_STATUS).getPrimitiveValue().toCastValue(String.class);
					
					if (logger.isTraceEnabled()) logger.trace("... found order UUID {} and status {}", orderUuid, orderStatus);
				} catch (NullPointerException | EdmPrimitiveTypeException e) {
					throw new IOException(logger.log(AipClientMessage.ORDER_DATA_MISSING, order.toString()));
				}
			}
			logger.log(AipClientMessage.PRODUCT_ORDER_COMPLETED, orderUuid);
			
		} finally {
			// Release parallel thread
			orderSemaphore.release();
			if (logger.isDebugEnabled())
				logger.debug("... order archive semaphore {} released, {} permits now available",
						orderSemaphore, orderSemaphore.availablePermits());
			
		}

	}

	/**
	 * Create product representation for Ingestor (mission-specific subclasses may want to override this method
	 * to set the attributes from inspecting the downloaded product)
	 * 
	 * This method is intended for overriding by mission-specific extensions of this class.
	 * 
	 * @param product the product metadata as received from the archive
	 * @param missionCode the code of the mission to ingest to
	 * @return a product representation suitable for ingestion
	 */
	protected IngestorProduct setProductMetadata(RestProduct product, String missionCode) {
		if (logger.isTraceEnabled()) logger.trace(">>> setProductMetadata({}, {})", product, missionCode);

		IngestorProduct ingestorProduct = new IngestorProduct();
		ingestorProduct.setMissionCode(missionCode);
		ingestorProduct.setUuid(product.getUuid());
		ingestorProduct.setProductClass(product.getProductClass());
		ingestorProduct.setProductQuality(ProductQuality.NOMINAL.toString());
		ingestorProduct.setSensingStartTime(product.getSensingStartTime());
		ingestorProduct.setSensingStopTime(product.getSensingStopTime());
		ingestorProduct.setGenerationTime(product.getGenerationTime());
		ingestorProduct.setPublicationTime(product.getPublicationTime());
		ingestorProduct.setProductionType(product.getProductionType());
		ingestorProduct.getParameters().addAll(product.getParameters());

		RestProductFile restProductFile = product.getProductFile().get(0);
		ingestorProduct.setProductFileName(restProductFile.getProductFileName());
		ingestorProduct.setFileSize(restProductFile.getFileSize());
		ingestorProduct.setChecksum(restProductFile.getChecksum());
		ingestorProduct.setChecksumTime(restProductFile.getChecksumTime());
		
		return ingestorProduct;
	}

	/**
	 * Download a product by UUID from the given archive
	 *
	 * @param archive the archive to download from
	 * @param product the product to download
	 * @param missionCode the mission code to set for ingestion
	 * @return a product representation useful for product ingestion (including the target file path of the download)
	 * @throws IOException if any error occurs during the download process
	 */
	private IngestorProduct downloadProduct(ProductArchive archive, RestProduct product, String missionCode) throws IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> downloadProduct({}, {})",
				(null == archive ? "NULL" : archive.getCode()),
				(null == product ? "NULL" : product.getUuid()));

		if (logger.isTraceEnabled()) logger.trace("... download of product with class {} and file name {} requested",
				product.getProductClass(), product.getProductFile().get(0).getProductFileName());
		
		// Build the request URL
		StringBuilder requestUrl = new StringBuilder(archive.getBaseUri());
		requestUrl.append('/')
			.append(archive.getContext())
			.append(archive.getContext().isEmpty()?"":'/')
			.append(ODATA_CONTEXT)
			.append('/')
			.append(ODATA_ENTITY_PRODUCTS)
			.append('(')
			.append(product.getUuid())
			.append(')')
			.append('/')
			.append("$value");

		RestProductFile restProductFile = product.getProductFile().get(0);
		File productFile = new File(config.getClientTargetDir() + File.separator + restProductFile.getProductFileName());

		for (int i = 0; i < DOWNLOAD_MAX_RETRIES; i++) {
			try {
				
				// Check whether parallel execution is allowed
				try {
					downloadSemaphore.acquire();
					if (logger.isDebugEnabled())
						logger.debug("... file download semaphore {} acquired, {} permits remaining",
								downloadSemaphore, downloadSemaphore.availablePermits());
				} catch (InterruptedException e) {
					throw new IOException(logger.log(ApiMonitorMessage.ABORTING_TASK, e.toString()));
				}
				
				try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
					logger.trace("... starting request for URL '{}'", requestUrl);

					HttpGet httpGet = new HttpGet(requestUrl.toString());

					if (archive.getTokenRequired()) {
						httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getBearerToken(archive));
					} else {
						httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
								.encodeToString((archive.getUsername() + ":" + archive.getPassword()).getBytes()));
					}

					CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
					HttpEntity httpEntity = httpResponse.getEntity();

					if (httpEntity != null) {
						FileUtils.copyInputStreamToFile(httpEntity.getContent(), productFile);
					}

					httpResponse.close();
					
				} catch (FileNotFoundException e) {
					throw new IOException(logger.log(AipClientMessage.FILE_NOT_WRITABLE, productFile));
				} catch (HttpResponseException e) {
					throw new IOException(logger.log(AipClientMessage.PRODUCT_DOWNLOAD_FAILED, product.getUuid(),
							e.getMessage() + " / " + e.getReasonPhrase()));
				} catch (Exception e) {
					throw new IOException(logger.log(AipClientMessage.PRODUCT_DOWNLOAD_FAILED, product.getUuid(), e.getMessage()));
				} finally {
					// Release parallel thread
					downloadSemaphore.release();
					if (logger.isDebugEnabled())
						logger.debug("... file download semaphore {} released, {} permits now available",
								downloadSemaphore, downloadSemaphore.availablePermits());
				}

				// Compare file size with value given by external archive
				Long productFileLength = productFile.length();
				if (!productFileLength.equals(restProductFile.getFileSize())) {
					throw new IOException(logger.log(AipClientMessage.FILE_SIZE_MISMATCH, product.getUuid(),
							restProductFile.getFileSize(), productFileLength));
				}

				// Compute checksum and compare with value given by external archive
				String md5Hash = MD5Util.md5Digest(productFile);
				if (!md5Hash.equalsIgnoreCase(restProductFile.getChecksum())) {
					throw new IOException(logger.log(AipClientMessage.CHECKSUM_MISMATCH, product.getUuid(),
							restProductFile.getChecksum(), md5Hash));
				}
				
				break; // Leave retry loop!

			} catch (Exception e) {
				if ((i + 1) < DOWNLOAD_MAX_RETRIES) {
					if (logger.isTraceEnabled())
						logger.trace("... download attempt {} of {} failed, waiting {} s", i + 1, DOWNLOAD_MAX_RETRIES,
								(double) DOWNLOAD_RETRY_INTERVAL / 1000.0);
					try {
						Thread.sleep(DOWNLOAD_RETRY_INTERVAL);
					} catch (InterruptedException e1) {
						logger.log(AipClientMessage.DOWNLOAD_RETRY_INTERRUPTED, restProductFile.getProductFileName());
						throw e;
					}
				} else {
					throw e;
				}
			} 
		}
		// Log download with UUID, file name, size, checksum, publication date
		logger.log(AipClientMessage.PRODUCT_TRANSFER_COMPLETED, product.getUuid(), restProductFile.getProductFileName(),
				restProductFile.getFileSize(), restProductFile.getChecksum(), product.getPublicationTime());

		// Set the product metadata (mission-specific subclasses may inspect the downloaded product)
		IngestorProduct ingestorProduct = setProductMetadata(product, missionCode);
		
		if (logger.isTraceEnabled()) logger.trace("... ingestor product metadata with product class {} and file name {} created",
				ingestorProduct.getProductClass(), ingestorProduct.getProductFileName());

		return ingestorProduct;
	}

	/**
	 * Ingest a product to a given processing facility
	 *
	 * @param product  the metadata of the product to ingest
	 * @param facility the processing facility to ingest to
	 * @param user     username for Ingestor service (no security context available in spawned thread)
	 * @param password password for Ingestor service
	 * @throws IOException if any error occurs during the ingestion process
	 */
	private void ingestProduct(IngestorProduct product, ProcessingFacility facility, String user, String password)
			throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> ingestProduct({}, {}, {}, ********)", (null == product ? "NULL" : product.getUuid()),
					(null == facility ? "NULL" : facility.getName()), user);
		
		if (logger.isTraceEnabled()) logger.trace("... ingestion requested for product of class {} and file name {}",
				product.getProductClass(), product.getProductFileName());
		
		// Set ingestion-specific attributes
		product.setSourceStorageType(StorageType.POSIX.toString());
		product.setMountPoint(config.getStorageMgrMountPoint());
		product.setFilePath(config.getStorageMgrSourceDir());

		// Remove UUID and have Ingestor generate a UUID of its own
		// (otherwise ingesting the same AUX products into two missions will fail!)
		product.setUuid(null);

		// Set product eviction time, if requested
		if (0 < config.getIngestorProductRetention()) {
			product.setEvictionTime(OrbitTimeFormatter.format(
					Instant.now().plus(config.getIngestorProductRetention(), ChronoUnit.DAYS)));
		}

		// Upload product via Ingestor
		String ingestorRestUrl = "/ingest/" + facility.getName();

		ObjectMapper obj = new ObjectMapper();
		String jsonRequest = null;
		try {
			jsonRequest = obj.writeValueAsString(Arrays.asList(product)); // Ingestion expects list of products
		} catch (JsonProcessingException e) {
			logger.log(AipClientMessage.ERROR_CONVERTING_INGESTOR_PRODUCT, product.getProductClass(), e.getMessage());
			throw e;
		}

		String basicAuth = "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
		HttpClient httpClient = HttpClient.create().wiretap(logger.isDebugEnabled());
		WebClient webClient = WebClient.builder()
			.baseUrl(config.getIngestorUrl())
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.build();
		if (logger.isTraceEnabled())
			logger.trace("... calling Ingestor on {} with auth '{}' and body '{}'", config.getIngestorUrl() + ingestorRestUrl,
					basicAuth, jsonRequest);

		try {
			Flux<String> result = webClient.post().uri(ingestorRestUrl).headers(httpHeaders -> {
				httpHeaders.add(HttpHeaders.AUTHORIZATION, basicAuth);
				httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
			}).body(BodyInserters.fromObject(jsonRequest)).retrieve().bodyToFlux(String.class);
			result.blockLast(Duration.ofSeconds(config.getIngestorTimeout()));
		} catch (WebClientResponseException e) {
			logger.log(AipClientMessage.ERROR_REGISTERING_PRODUCT, product.getProductClass(), e.getStatusCode().value(),
					http.extractProseoMessage(e.getHeaders().getFirst(HttpHeaders.WARNING)));
			throw e;
		}

		logger.log(AipClientMessage.PRODUCT_REGISTERED, product.getProductFileName());
	}

	/**
	 * Start a thread to download the named product from the given archive, and to ingest it to the given processing facility
	 *
	 * @param archive  the archive to download from
	 * @param product  the metadata of the product to download
	 * @param facility the processing facility to ingest the product to
	 * @param password Ingestor password
	 */
	private void downloadAndIngest(final ProductArchive archive, final AipRestProduct product, final ProcessingFacility facility,
			String password) {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadAndIngest({}, {}, {}, ********)", (null == archive ? "NULL" : archive.getCode()),
					(null == product ? "NULL" : product.getUuid()), (null == facility ? "NULL" : facility.getName()));

		// Avoid duplicate downloads
		if (productDownloads.contains(product.getUuid())) {
			logger.log(AipClientMessage.PRODUCT_DOWNLOAD_ONGOING, product.getUuid());
			return;
		}

		// Restrict number of parallel archive orders
		if (null == orderSemaphore) {
			orderSemaphore = new Semaphore(config.getArchiveOrderThreads(), true);
			if (logger.isDebugEnabled())
				logger.debug("... archive order semaphore {} created", orderSemaphore);
		}
		
		// Restrict number of parallel downloads
		if (null == downloadSemaphore) {
			downloadSemaphore = new Semaphore(config.getArchiveThreads(), true);
			if (logger.isDebugEnabled())
				logger.debug("... file download semaphore {} created", downloadSemaphore);
		}
		
		// Get the user and mission code from the current security context (not preserved to spawned thread)
		String user = securityService.getUser();
		String missionCode = securityService.getMission();
		
		// Create a new thread
		Thread downloadThread = new Thread() {

			@Override
			public void run() {
				if (logger.isTraceEnabled())
					logger.trace(">>> downloadAndIngest:run({}, {}, {})", (null == archive ? "NULL" : archive.getCode()),
							(null == product ? "NULL" : product.getUuid()), (null == facility ? "NULL" : facility.getName()));
				
				if (logger.isTraceEnabled())
					logger.trace("... download and ingest requested for product of class {} and file name {}",
							product.getProductClass(), product.getProductFile().get(0).getProductFileName());

				// Log download in lookup table
				productDownloads.add(product.getUuid());

				try {
					// For long-term archives, create a product order first and wait for its completion
					if (!product.isOnline() && (
							ArchiveType.AIP.equals(archive.getArchiveType()) 
							|| ArchiveType.SIMPLEAIP.equals(archive.getArchiveType()))) {
						createProductOrderAndWait(archive, product.getUuid());
					}

					// Download the product by product UUID
					IngestorProduct ingestorProduct = downloadProduct(archive, product, missionCode);
					
					// Ingest product to prosEO
					ingestProduct(ingestorProduct, facility, user, password);
				} catch (Exception e) {
					String message = null;
					if (e instanceof InterruptedException || e instanceof IOException) {
						// Already logged
						message = e.getMessage();
					} else {
						message = logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass().getName() + "/" + e.getMessage());
						if (logger.isDebugEnabled()) logger.debug("Stack trace: ", e);
					}
					notifyUser(message);
				} finally {
					// Remove download from lookup table
					productDownloads.remove(product.getUuid());
				}
			}
		};
		downloadThread.start();

	}

	/**
	 * Query the named archive for the requested file and download it to the given processing facility
	 *
	 * @param archive  the product archive to query
	 * @param filename the name of the product file to find
	 * @param facility the processing facility to download the product to
	 * @param password Ingestor password
	 * @return a REST interface representation of the product found or null, if no product was found
	 */
	private RestProduct downloadByName(ProductArchive archive, String filename, ProcessingFacility facility, String password) {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadByName({}, {}, {}, ********)", (null == archive ? "NULL" : archive.getCode()), filename,
					(null == facility ? "NULL" : facility.getName()));

		// Request the product from the archive
		List<ClientEntity> productList = null;
		try {
			productList = queryArchive(archive, ODATA_ENTITY_PRODUCTS, ODATA_FILTER_NAME + "'" + filename + "'", true);
		} catch (IOException e) {
			// Already logged
			return null;
		}

		if (null == productList || 0 == productList.size()) {
			logger.log(AipClientMessage.PRODUCT_NOT_FOUND_BY_NAME, filename, archive.getName());
			return null;
		}
		if (1 < productList.size()) {
			logger.log(AipClientMessage.MULTIPLE_PRODUCTS_FOUND_BY_NAME, filename, archive.getName());
		}

		AipRestProduct restProduct = toRestProduct(productList.get(0), facility, true);

		// Start download and ingestion thread
		downloadAndIngest(archive, restProduct, facility, password);

		// Return product metadata
		if (logger.isTraceEnabled())
			logger.trace("Found product " + restProduct);
		return restProduct;
	}

	/**
	 * Query the given archive for the first product matching the given product type and sensing start/stop times, download it and
	 * ingest it to the prosEO Storage Manager backend storage.<br>
	 * Queries are restricted to the logged-in mission using the first three characters of the file name,
	 * as per the EO GS File Format Standard (PE-TN-ESA-GS-0001), issue 3.01, sec. 4.1 
	 *
	 * @param archive            the archive to query
	 * @param productType        the product type to look for
	 * @param earliestStart          sensing start time at millisecond precision
	 * @param earliestStop           sensing stop time at millisecond precision
	 * @param processingFacility the processing facility to store the result in
	 * @param password           password for Ingestor login
	 * @return the product metadata in REST interface format or null, if no product was found
	 */
	private RestProduct downloadBySensingTime(ProductArchive archive, String productType, Instant earliestStart, Instant earliestStop,
			ProcessingFacility processingFacility, String password) {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadBySensingTime({}, {}, {}, {}, {}, ********)", (null == archive ? "NULL" : archive.getCode()),
					productType, earliestStart, earliestStop, (null == processingFacility ? "NULL" : processingFacility.getName()));

		// Request the product from the archive
		List<ClientEntity> productList = null;
		Boolean expandAttributes = true;
		
		String queryFilter = "";
		switch (archive.getArchiveType()) {
		case SIMPLEAIP:
			// CAUTION: Code for SIMPLEAIP is Sentinel-1-specific!
			expandAttributes = false;
			queryFilter =
				"(startswith(Name,'" + securityService.getMission() + "')"
					+ " or startswith(Name,'" + securityService.getMission().substring(0, 2) + "_'))"
				+ " and (contains(Name,'" + productType + "')"
				+ getMissionSpecificFilters(securityService.getMission(), productType)
				+ ")"
				+ " and ContentDate/Start lt " + ODATA_DF.format(earliestStop) 
				+ " and ContentDate/End gt " + ODATA_DF.format(earliestStart);
			break;
		default:
			queryFilter = 
				"(startswith(Name,'" + securityService.getMission() + "')"
					+ " or startswith(Name,'" + securityService.getMission().substring(0, 2) + "_'))"
				+ " and ContentDate/Start lt " + ODATA_DF.format(earliestStop) 
				+ " and ContentDate/End gt " + ODATA_DF.format(earliestStart)
				+ " and Attributes/OData.CSC.StringAttribute/any(" + "att:att/Name eq 'productType' "
				+ "and att/OData.CSC.StringAttribute/Value eq '" + productType + "')";
			break;
		}
		try {
			productList = queryArchive(archive, ODATA_ENTITY_PRODUCTS, queryFilter, expandAttributes);
		} catch (IOException e) {
			// Already logged
			return null;
		}

		if (null == productList || 0 == productList.size()) {
			logger.log(AipClientMessage.PRODUCT_NOT_FOUND_BY_TIME, productType, earliestStart, earliestStop, archive.getName());
			return null;
		}
		if (1 < productList.size()) {
			logger.log(AipClientMessage.MULTIPLE_PRODUCTS_FOUND_BY_TIME, productType, earliestStart, earliestStop, archive.getName());
		}
		ClientEntity odataProduct = productList.get(0);
		AipRestProduct restProduct = null; 
		if (archive.getArchiveType().equals(ArchiveType.SIMPLEAIP)) {
			// the attributes where not expanded in first query cause slow reaction
			// query the product directly and expand the attributes
			restProduct = toRestProduct(odataProduct, processingFacility, false);
			try {
				ClientEntity secondOdataProduct = queryArchiveForSingleEntity(archive, ODATA_ENTITY_PRODUCTS, restProduct.getUuid(), true);
				restProduct = toRestProduct(secondOdataProduct, processingFacility, true);
			} catch (IOException e) {
				// already logged
			}
		} else {
			restProduct = toRestProduct(odataProduct, processingFacility, true);
		}
		// Start download and ingestion thread
		downloadAndIngest(archive, restProduct, processingFacility, password);

		// Return product metadata
		if (logger.isTraceEnabled())
			logger.trace("Found product " + restProduct);
		return restProduct;
	}

	/**
	 * Query the given archive for all products matching the given product type and intersecting the given sensing start/stop time
	 * interval, download them and ingest them to the prosEO Storage Manager backend storage.<br>
	 * Queries are restricted to the logged-in mission using the first three characters of the file name,
	 * as per the EO GS File Format Standard (PE-TN-ESA-GS-0001), issue 3.01, sec. 4.1 
	 *
	 * @param archive            the archive to query
	 * @param productType        the product type to look for
	 * @param earliestStart      sensing start time at millisecond precision
	 * @param earliestStop       sensing stop time at millisecond precision
	 * @param processingFacility the processing facility to store the result in
	 * @param password           password for Ingestor login
	 * @param modelProducts 	 list of products already available locally
	 * @return a list of product metadata in REST interface format or an empty list, if no product was found
	 */
	private List<RestProduct> downloadAllBySensingTime(ProductArchive archive, String productType, Instant earliestStart,
			Instant earliestStop, ProcessingFacility processingFacility, String password, List<Product> modelProducts) {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadAllBySensingTime({}, {}, {}, {}, {}, ********)",
					(null == archive ? "NULL" : archive.getCode()), productType, earliestStart, earliestStop,
					(null == processingFacility ? "NULL" : processingFacility.getName()));
		// Request the product from the archive
		List<ClientEntity> productList = null;

		Boolean expandAttributes = true;
		
		String queryFilter = "";
		switch (archive.getArchiveType()) {
		case SIMPLEAIP:
			expandAttributes = false;
			queryFilter =
				"(startswith(Name,'" + securityService.getMission() + "')"
					+ " or startswith(Name,'" + securityService.getMission().substring(0, 2) + "_'))"
				+ " and (contains(Name,'" + productType + "')"
				+ getMissionSpecificFilters(securityService.getMission(), productType)
				+ ")"
				+ " and ContentDate/Start lt " + ODATA_DF.format(earliestStop) 
				+ " and ContentDate/End gt " + ODATA_DF.format(earliestStart);
			break;
		default:
			queryFilter =
				"(startswith(Name,'" + securityService.getMission() + "')"
					+ " or startswith(Name,'" + securityService.getMission().substring(0, 2) + "_'))"
				+ " and ContentDate/Start lt " + ODATA_DF.format(earliestStop) 
				+ " and ContentDate/End gt " + ODATA_DF.format(earliestStart)
				+ " and Attributes/OData.CSC.StringAttribute/any(" + "att:att/Name eq 'productType' "
				+ "and att/OData.CSC.StringAttribute/Value eq '" + productType + "')";
			break;
		}
		try {
			productList = queryArchive(archive, ODATA_ENTITY_PRODUCTS, queryFilter, expandAttributes);
		} catch (IOException e) {
			// Already logged
			return new ArrayList<>();
		}

		if (null == productList || 0 == productList.size()) {
			logger.log(AipClientMessage.NO_PRODUCTS_FOUND_BY_TIME, productType, earliestStart, earliestStop, archive.getName());
			return new ArrayList<>();
		}

		List<RestProduct> restProducts = new ArrayList<>();

		for (ClientEntity odataProduct : productList) {
			AipRestProduct restProduct = null; 
			if (archive.getArchiveType().equals(ArchiveType.SIMPLEAIP)) {
				// the attributes where not expanded in first query cause slow reaction
				// query the product directly and expand the attributes
				restProduct = toRestProduct(odataProduct, processingFacility, false);
				try {
					ClientEntity secondOdataProduct = queryArchiveForSingleEntity(archive, ODATA_ENTITY_PRODUCTS, restProduct.getUuid(), true);
					restProduct = toRestProduct(secondOdataProduct, processingFacility, true);
				} catch (IOException e) {
					// already logged
					break;
				}
				// For SIMPLEAIP an exact match between requested product type and product type found is not guaranteed
				if (!productType.equals(restProduct.getProductClass())) {
					logger.log(AipClientMessage.PRODUCT_TYPE_MISMATCH, restProduct.getProductClass(), productType);
					restProduct.setProductClass(productType);
				}
			} else {
				restProduct = toRestProduct(odataProduct, processingFacility, true);
			}
			
			// Check whether product file is already available locally
			RestProduct localProduct = findLocalProductAtFacility(modelProducts, restProduct, processingFacility);
			
			if (null == localProduct) {
				// Not available locally: Start download and ingestion thread
				downloadAndIngest(archive, restProduct, processingFacility, password);
				if (logger.isTraceEnabled())
					logger.trace("Found product " + restProduct);
	
				restProducts.add(restProduct);
			} else {
				// Skip download and return locally available product
				restProducts.add(localProduct);
				if (logger.isTraceEnabled())
					logger.trace("Skipping locally available product " + restProduct);
			}
		}

		// Return list of product metadata
		return restProducts;
	}

	/**
	 * Provide the product with the given file name at the given processing facility. If it already is available there, do nothing
	 * and just return the product metadata. If it is not available locally, query all configured LTAs for a product with the given
	 * file name, the first response is returned to the caller, then download from the LTA and ingested at the given processing
	 * facility.
	 *
	 * @param filename the (unique) product file name to search for
	 * @param facility the processing facility to store the downloaded product files in
	 * @param password password for Ingestor login
	 * @return the product provided
	 * @throws NoResultException        if no products matching the given selection criteria were found
	 * @throws IllegalArgumentException if an invalid processing facility name was given
	 */
	public RestProduct downloadByName(String filename, String facility, String password)
			throws NoResultException, IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadByName({}, {}, ********)", filename, facility);

		// Find the requested processing facility
		final ProcessingFacility processingFacility = readProcessingFacility(facility);

		// Check local availability of product
		List<ProductFile> productFiles = RepositoryService.getProductFileRepository().findByFileName(filename);

		for (ProductFile productFile : productFiles) {
			if (processingFacility.equals(productFile.getProcessingFacility())) {
				return toRestProduct(productFile.getProduct());
			}
		}

		// Try all available archives (since product type is unknown) until one succeeds
		List<ProductArchive> productArchives = RepositoryService.getProductArchiveRepository().findAll();

		for (ProductArchive archive : productArchives) {
			RestProduct result = downloadByName(archive, filename, processingFacility, password);
			if (null != result) {
				return result;
			}
		}

		// Fall-through: All archive requests failed
		throw new NoResultException(logger.log(AipClientMessage.INPUT_FILE_NOT_FOUND, filename));
	}

	/**
	 * Provide the product with the given product type and the exact sensing start and stop times (at millisecond precision) at the
	 * given processing facility. If it already is available there, do nothing and just return the product metadata. If it is not
	 * available locally, query all configured LTAs for a product with the given search criteria. The first response is evaluated:
	 * If multiple products fulfilling the criteria are found in the LTA, the product with the most recent generation time will be
	 * used. In the (unlikely) case of several products having the same generation time, the product with the greatest file name
	 * (alphanumeric string comparison) will be used. The product metadata is returned to the caller, then the product is downloaded
	 * from the LTA and ingested at the given processing facility.
	 *
	 * @param productType the product type
	 * @param startTime   start of the sensing time interval (at millisecond precision)
	 * @param stopTime    end of the sensing time interval (at millisecond precision)
	 * @param facility    the processing facility to store the downloaded product files in
	 * @param password    password for Ingestor login
	 * @return the product provided
	 * @throws NoResultException        if no products matching the given selection criteria were found
	 * @throws IllegalArgumentException if an invalid facility name, product type or sensing time was given
	 */
	public RestProduct downloadBySensingTime(String productType, String startTime, String stopTime, String facility,
			String password) throws NoResultException, IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadBySensingTime({}, {}, {}, ********)", startTime, stopTime, facility);

		// Check input parameters
		final ProcessingFacility processingFacility = readProcessingFacility(facility);
		final ProductClass productClass = readProductClass(productType);

		// Ensure millisecond precision for start and stop time
		Instant earliestStart, earliestStop;
		try {
			// Ensure millisecond precision for start and stop time
			earliestStart = OrbitTimeFormatter.parseDateTime(startTime);
			earliestStart.minusNanos(earliestStart.getNano() - earliestStart.get(ChronoField.MILLI_OF_SECOND) * 1000000);
			earliestStop = OrbitTimeFormatter.parseDateTime(stopTime);
			earliestStop.minusNanos(earliestStop.getNano() - earliestStop.get(ChronoField.MILLI_OF_SECOND) * 1000000);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException(logger.log(AipClientMessage.INVALID_SENSING_TIME, e.getMessage()));
		}

		// Check availability of products
		Product modelProduct = findProductBySensingTime(productType, earliestStart, earliestStop, processingFacility);

		if (null != modelProduct) {
			// Found (at least) one suitable product
			return toRestProduct(modelProduct);
		}

		// Query the available archives for the given product type
		List<ProductArchive> productArchives = RepositoryService.getProductArchiveRepository().findAll();

		for (ProductArchive archive : productArchives) {
			if (archive.getAvailableProductClasses().contains(productClass)) {
				RestProduct result = downloadBySensingTime(archive, productType, earliestStart, earliestStop,
						processingFacility, password);
				if (null != result) {
					return result;
				}
			}
		}

		// Fall-through: All archive requests failed
		throw new NoResultException(logger.log(AipClientMessage.INPUT_FILE_NOT_FOUND_BY_TIME, productType, startTime, stopTime));
	}

	/**
	 * Provide all products with the given product type at the given processing facility, whose sensing times intersect with the
	 * given sensing time interval. Query all configured LTAs for products with the given search criteria, the first response is
	 * evaluated. The product metadata is returned to the caller, then the products are downloaded from the LTA and ingested at the
	 * given processing facility, unless they are already available there.
	 *
	 * @param productType the product type
	 * @param startTime   the start of the sensing time interval
	 * @param stopTime    the end of the sensing time interval
	 * @param facility    the processing facility to store the downloaded product files in
	 * @param password    password for Ingestor login
	 * @return a list of the products provided from the LTA
	 * @throws NoResultException        if no products matching the given selection criteria were found
	 * @throws IllegalArgumentException if an invalid facility name, product type or sensing time was given
	 */
	public List<RestProduct> downloadAllBySensingTime(String productType, String startTime, String stopTime, String facility,
			String password) throws NoResultException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadAllBySensingTime({}, {}, {}, {}, ********)", productType, startTime, stopTime, facility);

		// Check input parameters
		final ProcessingFacility processingFacility = readProcessingFacility(facility);
		final ProductClass productClass = readProductClass(productType);

		// Ensure millisecond precision for start and stop time
		Instant earliestStart, earliestStop;
		try {
			// Ensure millisecond precision for start and stop time
			earliestStart = OrbitTimeFormatter.parseDateTime(startTime);
			earliestStart.minusNanos(earliestStart.getNano() - earliestStart.get(ChronoField.MILLI_OF_SECOND) * 1000000);
			earliestStop = OrbitTimeFormatter.parseDateTime(stopTime);
			earliestStop.minusNanos(earliestStop.getNano() - earliestStop.get(ChronoField.MILLI_OF_SECOND) * 1000000);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException(logger.log(AipClientMessage.INVALID_SENSING_TIME, e.getMessage()));
		}

		// Check availability of products
		List<Product> modelProducts = findAllProductsBySensingTime(productType, earliestStart, earliestStop, processingFacility);

		if (modelProducts.isEmpty()) {
			if (logger.isTraceEnabled()) logger.trace("No products found locally");
		}

		// Query the available archives for the given product type
		List<ProductArchive> productArchives = RepositoryService.getProductArchiveRepository().findAll();

		if (logger.isTraceEnabled()) logger.trace("Turning to archives {}",
				Arrays.asList(productArchives.stream().map(a -> { return a.getCode(); }).toArray()));
		
		for (ProductArchive archive : productArchives) {
			if (logger.isTraceEnabled()) logger.trace("Checking archive {} with product classes {}", archive.getCode(),
					Arrays.asList(archive.getAvailableProductClasses().stream().map(pc -> { return pc.getProductType(); }).toArray()));
			if (archive.getAvailableProductClasses().contains(productClass)) {
				if (logger.isTraceEnabled()) logger.trace("Querying archive for product class {}", productClass.getProductType());
				List<RestProduct> result = downloadAllBySensingTime(archive, productType, earliestStart, earliestStop,
						processingFacility, password, modelProducts);
				if (!result.isEmpty()) {
					return result;
				}
			}
		}

		// Fall-through: All archive requests failed
		throw new NoResultException(logger.log(AipClientMessage.INPUT_FILE_NOT_FOUND_BY_TIME, productType, startTime, stopTime));
	}

}