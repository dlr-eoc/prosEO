/**
 * ProductEntityProcessor.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.http.client.utils.URIBuilder;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.processor.MediaEntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.Unauthorized;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.api.prip.ProductionInterfaceConfiguration;
import de.dlr.proseo.api.prip.ProductionInterfaceSecurity;
import de.dlr.proseo.interfaces.rest.model.RestProduct;
import de.dlr.proseo.interfaces.rest.model.RestProductFile;
import de.dlr.proseo.model.rest.model.RestProcessingFacility;


/**
 * Retrieve product information from the prosEO metadata database (via the Ingestor component) and download product data 
 * from the prosEO Storage Manager
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductEntityProcessor implements EntityProcessor, MediaEntityProcessor {

	/* Message ID constants */
	private static final int MSG_ID_INVALID_ENTITY_TYPE = 5001;
	private static final int MSG_ID_URI_GENERATION_FAILED = 5002;
	private static final int MSG_ID_HTTP_REQUEST_FAILED = 5003;
	private static final int MSG_ID_SERVICE_REQUEST_FAILED = 5004;
	private static final int MSG_ID_NOT_AUTHORIZED_FOR_SERVICE = 5005;
	private static final int MSG_ID_UNSUPPORTED_FORMAT = 5006;
	private static final int MSG_ID_EXCEPTION = 5007;
	private static final int MSG_ID_FORBIDDEN = 5100;
	private static final int MSG_ID_PRODUCT_NOT_AVAILABLE = 5101;
	private static final int MSG_ID_INVALID_RANGE_HEADER = 5102;
	private static final int MSG_ID_CANNOT_DESERIALIZE_RESPONSE = 5103;
	private static final int MSG_ID_REDIRECT = 5104;

	/* Message string constants */
	private static final String MSG_INVALID_ENTITY_TYPE = "(E%d) Invalid entity type %s referenced in service request";
	private static final String MSG_URI_GENERATION_FAILED = "(E%d) URI generation from product UUID failed (cause: %s)";
	private static final String MSG_HTTP_REQUEST_FAILED = "(E%d) HTTP request failed (cause: %s)";
	private static final String MSG_SERVICE_REQUEST_FAILED = "(E%d) Service request failed with status %d (%s), cause: %s";
	private static final String MSG_NOT_AUTHORIZED_FOR_SERVICE = "(E%d) User %s not authorized for requested service";
	private static final String MSG_EXCEPTION = "(E%d) Request failed (cause %s: %s)";
	private static final String MSG_FORBIDDEN = "(E%d) Creation, update and deletion of products not allowed through PRIP";
	private static final String MSG_PRODUCT_NOT_AVAILABLE = "(E%d) Product %s not available on any Processing Facility";
	private static final String MSG_CANNOT_DESERIALIZE_RESPONSE = "(E%d) Cannot deserialize HTTP response";
	private static final String MSG_UNSUPPORTED_FORMAT = "(E%d) Unsupported response format %s";

	private static final String MSG_INVALID_RANGE_HEADER = "(W%d) Ignoring invalid HTTP range header %s";

	private static final String MSG_REDIRECT = "(I%d) Redirecting download request to Storage Manger URL %s";
	
	/* Other string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";

	/** The cached OData factory object */
	private OData odata;
	/** The cached metadata of the OData service */
	private ServiceMetadata serviceMetadata;

	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtb;
	
	/** The configuration for the PRIP API */
	@Autowired
	private ProductionInterfaceConfiguration config;
	
	/** The security utilities for the PRIP API */
	@Autowired
	private ProductionInterfaceSecurity securityConfig;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductEntityProcessor.class);

	/**
	 * Create and log a formatted message at the given level
	 * 
	 * @param level the logging level to use
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String log(Level level, String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		if (Level.ERROR.equals(level)) {
			logger.error(message);
		} else if (Level.WARN.equals(level)) {
			logger.warn(message);
		} else {
			logger.info(message);
		}

		return message;
	}

	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private String logError(String messageFormat, int messageId, Object... messageParameters) {
		return log(Level.ERROR, messageFormat, messageId, messageParameters);
	}

	/**
	 * Initializes the processor for each HTTP request - response cycle
	 * (Copied from interface definition)
	 * 
	 * @param odata Olingo's root object, acting as a factory for various object types
	 * @param serviceMetadata metadata of the OData service like the EDM that have to be created before the OData request handling takes place
	 */
	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		if (logger.isTraceEnabled()) logger.trace(">>> init({}, {})", odata, serviceMetadata);
		
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}


	/**
	 * Get the metadata for a single product from the prosEO Ingestor service
	 * @param productUuid the UUID of the product to retrieve
	 * 
	 * @return a product object
	 * @throws HttpClientErrorException if an error is returned from the Ingestor service
	 * @throws RestClientException if the request to the Ingestor fails for some other reason
	 * @throws RuntimeException if any other exception occurs
	 * @throws SecurityException if the logged in user is not authorized to access the requested product
	 */
	private RestProduct getProduct(String productUuid)
			throws HttpClientErrorException, RestClientException, RuntimeException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProduct({})", productUuid);

		// Request product metadata from Ingestor service
		
		// Attempt connection to service
		ResponseEntity<RestProduct> entity = null;
		try {
			RestTemplate restTemplate = rtb.basicAuthentication(
					securityConfig.getMission() + "-" + securityConfig.getUser(), securityConfig.getPassword())
				.build();
			String requestUrl = config.getIngestorUrl() + "/products/uuid/" + productUuid;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, RestProduct.class);
		} catch (HttpClientErrorException.Unauthorized e) {
			String message = String.format(MSG_NOT_AUTHORIZED_FOR_SERVICE, MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, securityConfig.getUser());
			logger.error(message);
			throw new SecurityException(message);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.error(String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING)));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING));
		} catch (RestClientException e) {
			String message = String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		
		// All GET requests should return HTTP status OK
		if (!HttpStatus.OK.equals(entity.getStatusCode())) {
			String message = String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED, 
					entity.getStatusCodeValue(), entity.getStatusCode().toString(), entity.getHeaders().getFirst(HTTP_HEADER_WARNING));
			logger.error(message);
			throw new RuntimeException(message);
		}
		
		RestProduct restProduct = entity.getBody();
		if (logger.isDebugEnabled()) logger.debug("... product found: " + restProduct.getId());
		return restProduct;
	}

	/**
	 * Get the URL of the storage manager for a given processing facility from the prosEO Facility Manager service
	 * @param facilityName the name of the processing facility
	 * 
	 * @return the URL of the Storage Manager associated with the given processing facility
	 * @throws IllegalArgumentException if the authorization header cannot be parsed
	 * @throws HttpClientErrorException if an error is returned from the Facility Manager service
	 * @throws Unauthorized if the user named in the Basic Auth header is not authorized to access the requested processing facility
	 * @throws RestClientException if the request to the Facility Manager fails for some other reason
	 * @throws RuntimeException if any other exception occurs
	 */
	private String getStorageManagerUrl(String facilityName)
			throws IllegalArgumentException, HttpClientErrorException, Unauthorized, RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> getStorageManagerUrl({})", facilityName);

		// Request processing facility data from Facility Manager service
		
		// Attempt connection to service
		ResponseEntity<?> entity = null;
		try {
			RestTemplate restTemplate = rtb.basicAuthentication(
					securityConfig.getMission() + "-" + config.getFacilityManagerUser(), config.getFacilityManagerPassword())
				.build();
			String requestUrl = config.getFacilityManagerUrl() + "/facilities?name=" + facilityName;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, List.class);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.error(String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING)));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING));
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(String.format(MSG_NOT_AUTHORIZED_FOR_SERVICE, MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, securityConfig.getUser()), e);
			throw e;
		} catch (RestClientException e) {
			String message = String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		
		// All GET requests should return HTTP status OK
		if (!HttpStatus.OK.equals(entity.getStatusCode())) {
			String message = String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED, 
					entity.getStatusCodeValue(), entity.getStatusCode().toString(), entity.getHeaders().getFirst(HTTP_HEADER_WARNING));
			logger.error(message);
			throw new RuntimeException(message);
		}
		
		if (entity.getBody() instanceof List) {
			List<?> body = (List<?>) entity.getBody();
			if (1 == body.size()) {
				RestProcessingFacility restProcessingFacility = (new ObjectMapper()).convertValue(body.get(0), RestProcessingFacility.class);
				if (logger.isDebugEnabled()) logger.debug("... processing facility found: " + restProcessingFacility.getId());
				return restProcessingFacility.getStorageManagerUrl();
			}
		}
		
		String message = String.format(MSG_CANNOT_DESERIALIZE_RESPONSE, MSG_ID_CANNOT_DESERIALIZE_RESPONSE);
		logger.error(message);
		throw new RuntimeException(message);
	}

	/**
	 * Download the requested product from the prosEO Storage Manager
	 * 
	 * @param authHeader the Basic Authorization header for logging in to prosEO
	 * @param productUuid the UUID of the product to retrieve
	 * @return a binary stream containing the product data
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 * @throws IllegalArgumentException if mandatory information is missing from the prosEO interface product
	 * @throws HttpClientErrorException if an error is returned from the Ingestor service
	 * @throws RestClientException if the request to the Ingestor fails for some other reason
	 * @throws RuntimeException if any other exception occurs
	 * @throws SecurityException if the logged in user is not authorized to access the requested product
	 */
	private Entity getProductAsEntity(String authHeader, String productUuid) throws URISyntaxException, IllegalArgumentException,
			HttpClientErrorException, RestClientException, RuntimeException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductAsEntity({}, {})", authHeader, productUuid);
		
		// Get the product information from the Ingestor service
		RestProduct restProduct = getProduct(productUuid);

		// Create output product
		Entity product = ProductUtil.toPripProduct(restProduct);

		if (logger.isTraceEnabled()) logger.trace("<<< downloadProduct()");
		return product;
	}

	/**
	 * Reads entities data from persistence and puts serialized content and status into the response.
	 * 
	 * @param request OData request object containing raw HTTP information
	 * @param response OData response object for collecting response data
	 * @param uriInfo information of a parsed OData URI
	 * @param responseFormat requested content type after content negotiation
	 * @throws ODataApplicationException if the service implementation encounters a failure
	 * @throws ODataLibraryException if an error during serialization occurs
	 */
	@Override
	public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled()) logger.trace(">>> readEntity({}, {}, {}, {})", request, response, uriInfo, responseFormat);
		
		// [1] Retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// [2] Fetch the data from backend for this requested EntitySetName (has to be delivered as Entity object)
	    List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
	    
		Entity entity;
		if (edmEntitySet.getEntityType().getFullQualifiedName().equals(ProductEdmProvider.ET_PRODUCT_FQN)) {
			try {
				entity = getProductAsEntity(request.getHeader(HttpHeaders.AUTHORIZATION), keyPredicates.get(0).getText());
			} catch (HttpClientErrorException e) {
				response.setStatusCode(e.getRawStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
				return;
			} catch (SecurityException e) {
				response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
				return;
			} catch (URISyntaxException e) {
				String message = logError(MSG_URI_GENERATION_FAILED, MSG_ID_URI_GENERATION_FAILED, e.getMessage());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			} catch (Exception e) {
				String message = logError(MSG_EXCEPTION, MSG_ID_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
				e.printStackTrace();
				response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			}
		} else {
			String message = logError(MSG_INVALID_ENTITY_TYPE, MSG_ID_INVALID_ENTITY_TYPE, edmEntitySet.getEntityType().getFullQualifiedName());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}

		if (logger.isDebugEnabled()) logger.debug("... preparing data for response");
		
		// [3] Check for system query options
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();

		// [4] Create a serializer based on the requested format (json)
		if (!ContentType.APPLICATION_JSON.isCompatible(responseFormat)) {
			// Any other format currently throws an exception (see Github issue #122)
			String message = logError(MSG_UNSUPPORTED_FORMAT, MSG_ID_UNSUPPORTED_FORMAT, responseFormat.toContentTypeString());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}
		ODataSerializer serializer = odata.createSerializer(responseFormat);

		// [5] Now serialize the content: transform from the Entity object to InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with()
				.entitySet(edmEntitySet)
				.selectList(odata.createUriHelper().buildContextURLSelectList(edmEntityType, expandOption, selectOption))
				.suffix(Suffix.ENTITY)
				.build();

		EntitySerializerOptions opts = EntitySerializerOptions.with()
				.contextURL(contextUrl)
				.expand(expandOption)
				.select(selectOption)
				.build();
		SerializerResult serializerResult = serializer.entity(serviceMetadata, edmEntityType, entity, opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		
		if (logger.isTraceEnabled()) logger.trace("<<< readEntity()");
	}

	/**
	 * Download a product from the prosEO Storage Manager (any Storage Manager instance will do, because in the case
	 * of multiple copies of the product on various facilities, these should be identical); the Storage Manager is
	 * identified via the Facility Manager component
	 * <p>
	 * This method does not actually return the data stream for the product, but rather redirects to the download
	 * URI at the Storage Manager
	 * 
	 * @param request OData request object containing raw HTTP information
	 * @param response OData response object for collecting response data
	 * @param uriInfo information of a parsed OData URI
	 * @param responseFormat requested content type after content negotiation
	 * @throws ODataApplicationException if the service implementation encounters a failure
	 * @throws ODataLibraryException if an error during serialization occurs
	 */
	@Override
	public void readMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled()) logger.trace(">>> readMediaEntity({}, {}, {}, {})", request, response, uriInfo, responseFormat);
		
		// Find the requested product
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0); // in our example, the first segment is the EntitySet
	    List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		RestProduct restProduct;

		try {
			restProduct = getProduct(keyPredicates.get(0).getText());
		} catch (HttpClientErrorException e) {
			response.setStatusCode(e.getRawStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (SecurityException e) {
			response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (Exception e) {
			String message = logError(MSG_EXCEPTION, MSG_ID_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
			e.printStackTrace();
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}
		
		// Check whether the product is actually available on some processing facility
		if (restProduct.getProductFile().isEmpty()) {
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, logError(MSG_PRODUCT_NOT_AVAILABLE, MSG_ID_PRODUCT_NOT_AVAILABLE, keyPredicates.get(0).getText()));
		}
		// Select a random product file to transfer
		int randomProductIndex = (int) (Math.random() * restProduct.getProductFile().size() - 0.5);
		RestProductFile productFile = restProduct.getProductFile().get(randomProductIndex);
		
		// Get the service URI of the Storage Manager service
		String storageManagerUrl = getStorageManagerUrl(productFile.getProcessingFacilityName());
		
		// Build the download URI: Set pathInfo to zipped file if available, to product file otherwise
		URIBuilder uriBuilder = null;
		try {
			uriBuilder = new URIBuilder(storageManagerUrl + "/products/download");
			uriBuilder.addParameter("pathInfo", productFile.getFilePath() + "/" +
				(null == productFile.getZipFileName() ? productFile.getProductFileName() : productFile.getZipFileName()));
		} catch (URISyntaxException e) {
			String message = logError(MSG_EXCEPTION, MSG_ID_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
			e.printStackTrace();
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}

		// Evaluate HTTP Range Header
		String rangeHeader = request.getHeader(HttpHeaders.RANGE);
		if (null != rangeHeader) {
			if (rangeHeader.startsWith("bytes=")) {
				String[] rangeParts = rangeHeader.substring(6).split("-", 2);
				if (2 == rangeParts.length) {
					int fromByte = 0;
					int toByte = (null == productFile.getZipFileName() ? productFile.getFileSize().intValue()
							: productFile.getZipFileSize().intValue()) - 1;
					try {
						if (rangeParts[0].isBlank()) {
							if (rangeParts[1].isBlank()) {
								log(Level.WARN, MSG_INVALID_RANGE_HEADER, MSG_ID_INVALID_RANGE_HEADER, rangeHeader);
							} else {
								// Range header format "bytes=-nnnn", i. e. last nnnn bytes to transfer
								fromByte = toByte - Integer.parseInt(rangeParts[1]);
							}
						} else {
							// Range header format "bytes=nnnn-[mmmm]", i. e. transfer starts from byte nnnn
							fromByte = Integer.parseInt(rangeParts[0]);
							if (!rangeParts[1].isBlank()) {
								// Range header format "bytes=nnnn-mmmm", i. e. transfer ends at byte nnnn
								toByte = Integer.parseInt(rangeParts[1]);
							}
						}
						uriBuilder.addParameter("fromByte", String.valueOf(fromByte));
						uriBuilder.addParameter("toByte", String.valueOf(toByte));
					} catch (NumberFormatException e) {
						log(Level.WARN, MSG_INVALID_RANGE_HEADER, MSG_ID_INVALID_RANGE_HEADER, rangeHeader);
					}
				} else {
					log(Level.WARN, MSG_INVALID_RANGE_HEADER, MSG_ID_INVALID_RANGE_HEADER, rangeHeader);
				}
			} else {
				log(Level.WARN, MSG_INVALID_RANGE_HEADER, MSG_ID_INVALID_RANGE_HEADER, rangeHeader);
			} 
		}
		// Redirect the request to the download URI
		log(Level.INFO, MSG_REDIRECT, MSG_ID_REDIRECT, uriBuilder.toString());
		response.setStatusCode(HttpStatusCode.TEMPORARY_REDIRECT.getStatusCode());
		response.setHeader(HttpHeader.LOCATION, uriBuilder.toString());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		
		if (logger.isTraceEnabled()) logger.trace("<<< readMediaEntity()");
	}

	/* ------------------------------------------------------------------------------------
	 * The methods below are not available on the PRIP, since this is a read-only interface 
	 * ------------------------------------------------------------------------------------ */
	
	@Override
	public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled()) logger.trace(">>> createEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);
		
		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logError(MSG_FORBIDDEN, MSG_ID_FORBIDDEN));
	}

	@Override
	public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteEntity({}, {}, {})", request, response, uriInfo);
		
		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logError(MSG_FORBIDDEN, MSG_ID_FORBIDDEN));
	}

	@Override
	public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled()) logger.trace(">>> updateEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);
		
		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logError(MSG_FORBIDDEN, MSG_ID_FORBIDDEN));
	}

	@Override
	public void createMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled()) logger.trace(">>> createMediaEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);
		
		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logError(MSG_FORBIDDEN, MSG_ID_FORBIDDEN));
	}

	@Override
	public void deleteMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteMediaEntity({}, {}, {})", request, response, uriInfo);
		
		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logError(MSG_FORBIDDEN, MSG_ID_FORBIDDEN));
	}

	@Override
	public void updateMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled()) logger.trace(">>> updateMediaEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);
		
		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logError(MSG_FORBIDDEN, MSG_ID_FORBIDDEN));
	}

}
