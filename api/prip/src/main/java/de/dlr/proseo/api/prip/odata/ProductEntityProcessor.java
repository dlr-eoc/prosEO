/**
 * ProductEntityProcessor.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import de.dlr.proseo.api.prip.ProductionInterfaceConfiguration;
import de.dlr.proseo.interfaces.rest.model.RestProduct;
import de.dlr.proseo.interfaces.rest.model.RestProductFile;
import de.dlr.proseo.model.rest.model.RestProcessingFacility;


/**
 * Retrieve product collections from the prosEO metadata database (via the Ingestor component) with additional information 
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
	private static final int MSG_ID_AUTH_MISSING_OR_INVALID = 5006;
	private static final int MSG_ID_EXCEPTION = 5007;
	private static final int MSG_ID_FORBIDDEN = 5100;
	private static final int MSG_ID_PRODUCT_NOT_AVAILABLE = 5101;

	/* Message string constants */
	private static final String MSG_INVALID_ENTITY_TYPE = "(E%d) Invalid entity type %s referenced in service request";
	private static final String MSG_URI_GENERATION_FAILED = "(E%d) URI generation from product UUID failed (cause: %s)";
	private static final String MSG_HTTP_REQUEST_FAILED = "(E%d) HTTP request failed (cause: %s)";
	private static final String MSG_SERVICE_REQUEST_FAILED = "(E%d) Service request failed with status %d (%s), cause: %s";
	private static final String MSG_NOT_AUTHORIZED_FOR_SERVICE = "(E%d) User %s not authorized for requested service";
	private static final String MSG_AUTH_MISSING_OR_INVALID = "(E%d) Basic authentication missing or invalid: %s";
	private static final String MSG_EXCEPTION = "(E%d) Request failed (cause %s: %s)";
	private static final String MSG_FORBIDDEN = "(E%d) Creation, update and deletion of products not allowed through PRIP";
	private static final String MSG_PRODUCT_NOT_AVAILABLE = "(E%d) Product %s not available on any Processing Facility";
	
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
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductEntityProcessor.class);

	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String logInfo(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.info(message);

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
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);

		return message;
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
	 * Parse an HTTP authentication header into username and password
	 * @param authHeader the authentication header to parse
	 * @return a string array containing the username and the password
	 * @throws IllegalArgumentException if the authentication header cannot be parsed
	 */
	private String[] parseAuthenticationHeader(String authHeader) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> parseAuthenticationHeader({})", authHeader);

		if (null == authHeader) {
			String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException (message);
		}
		String[] authParts = authHeader.split(" ");
		if (2 != authParts.length || !"Basic".equals(authParts[0])) {
			String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException (message);
		}
		String[] missionUserPassword = (new String(Base64.getDecoder().decode(authParts[1]))).split("\\\\"); // --> regex "\\" --> matches "\"
		if (2 != missionUserPassword.length) {
			String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException (message);
		}
		String[] userPassword = missionUserPassword[1].split(":"); // guaranteed to work as per BasicAuth specification
		return userPassword;
	}

	/**
	 * Get the metadata for a single product from the prosEO Ingestor service
	 * 
	 * @param authHeader the Basic Authorization header for logging in to prosEO
	 * @param productUuid the UUID of the product to retrieve
	 * @return a product object
	 * @throws IllegalArgumentException if the authorization header cannot be parsed
	 * @throws HttpClientErrorException if an error is returned from the Ingestor service
	 * @throws Unauthorized if the user named in the Basic Auth header is not authorized to access the requested product
	 * @throws RestClientException if the request to the Ingestor fails for some other reason
	 * @throws RuntimeException if any other exception occurs
	 */
	private RestProduct getProduct(String authHeader, String productUuid)
			throws IllegalArgumentException, HttpClientErrorException, Unauthorized, RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProduct({}, {})", authHeader, productUuid);

		// Parse authentication header
		String[] userPassword = parseAuthenticationHeader(authHeader);
		
		// Request product metadata from Ingestor service
		
		// Attempt connection to service
		ResponseEntity<RestProduct> entity = null;
		try {
			RestTemplate restTemplate = ( null == authHeader ? rtb.build() : rtb.basicAuthentication(userPassword[0], userPassword[1]).build() );
			String requestUrl = config.getIngestorUrl() + "/products/uuid/" + productUuid;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, RestProduct.class);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.error(String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING)));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING));
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(String.format(MSG_NOT_AUTHORIZED_FOR_SERVICE, MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, e.getMessage()), e);
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
		
		RestProduct restProduct = entity.getBody();
		if (logger.isDebugEnabled()) logger.debug("... product found: " + restProduct.getId());
		return restProduct;
	}

	/**
	 * Get the URL of the storage manager for a given processing facility from the prosEO Facility Manager service
	 * 
	 * @param authHeader the Basic Authorization header for logging in to prosEO
	 * @param facilityName the name of the processing facility
	 * @return the URL of the Storage Manager associated with the given processing facility
	 * @throws IllegalArgumentException if the authorization header cannot be parsed
	 * @throws HttpClientErrorException if an error is returned from the Facility Manager service
	 * @throws Unauthorized if the user named in the Basic Auth header is not authorized to access the requested processing facility
	 * @throws RestClientException if the request to the Facility Manager fails for some other reason
	 * @throws RuntimeException if any other exception occurs
	 */
	private String getStorageManagerUrl(String authHeader, String facilityName)
			throws IllegalArgumentException, HttpClientErrorException, Unauthorized, RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProduct({}, {})", authHeader, facilityName);

		// Parse authentication header
		String[] userPassword = parseAuthenticationHeader(authHeader);
		
		// Request processing facility data from Facility Manager service
		
		// Attempt connection to service
		ResponseEntity<RestProcessingFacility> entity = null;
		try {
			RestTemplate restTemplate = ( null == authHeader ? rtb.build() : rtb.basicAuthentication(userPassword[0], userPassword[1]).build() );
			String requestUrl = config.getFacilityManagerUrl() + "/facilities/" + facilityName;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, RestProcessingFacility.class);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.error(String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING)));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING));
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(String.format(MSG_NOT_AUTHORIZED_FOR_SERVICE, MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, e.getMessage()), e);
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
		
		RestProcessingFacility restProcessingFacility = entity.getBody();
		if (logger.isDebugEnabled()) logger.debug("... processing facility found: " + restProcessingFacility.getId());
		return restProcessingFacility.getStorageManagerUrl();
	}

	/**
	 * Download the requested product from the prosEO Storage Manager
	 * 
	 * @param authHeader the Basic Authorization header for logging in to prosEO
	 * @param productUuid the UUID of the product to retrieve
	 * @return a binary stream containing the product data
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 * @throws IllegalArgumentException if the authorization header cannot be parsed
	 */
	private Entity getProductAsEntity(String authHeader, String productUuid) throws URISyntaxException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductAsEntity({}, {})", authHeader, productUuid);
		
		// Get the product information from the Ingestor service
		RestProduct restProduct = getProduct(authHeader, productUuid);

		// Create output product
		Entity product = new Entity()
				.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, UUID.fromString(restProduct.getUuid())))
				.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, restProduct.getProductClass()))
				.addProperty(new Property(null, "ContentType", ValueType.PRIMITIVE,
						"application/octet-stream"));
		// TODO Add remaining properties
		product.setId(new URI(ProductEdmProvider.ET_PRODUCT_NAME + "('" + restProduct.getUuid() + "')"));

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
		
		// 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// 2nd: fetch the data from backend for this requested EntitySetName
		// it has to be delivered as Entity object
	    List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
	    
		Entity entity;
		if (edmEntitySet.getEntityType().getFullQualifiedName().equals(ProductEdmProvider.ET_PRODUCT_FQN)) {
			try {
				// Extract UUID from keyPredicates (only one key element defined for "Products")				
				entity = getProductAsEntity(request.getHeader(HttpHeaders.AUTHORIZATION), keyPredicates.get(0).getText());
			} catch (URISyntaxException e) {
				String message = logError(MSG_URI_GENERATION_FAILED, MSG_ID_URI_GENERATION_FAILED, e.getMessage());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			} catch (IllegalArgumentException e) {
				response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
				return;
			} catch (HttpClientErrorException e) {
				response.setStatusCode(e.getRawStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
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
		
		// 3rd: create a serializer based on the requested format (json)
		ODataSerializer serializer = odata.createSerializer(responseFormat);

		// 4th: Now serialize the content: transform from the EntitySet object to InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

		EntitySerializerOptions opts = EntitySerializerOptions.with().contextURL(contextUrl).build();
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
		RestProduct restProduct = getProduct(request.getHeader(HttpHeaders.AUTHORIZATION), keyPredicates.get(0).getText());
		
		// Check whether the product is actually available on some processing facility
		if (restProduct.getProductFile().isEmpty()) {
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, logError(MSG_PRODUCT_NOT_AVAILABLE, MSG_ID_PRODUCT_NOT_AVAILABLE, keyPredicates.get(0).getText()));
		}
		// Select a random product file to transfer
		int randomProductIndex = (int) (Math.random() * restProduct.getProductFile().size() - 0.5);
		RestProductFile productFile = restProduct.getProductFile().get(randomProductIndex);
		
		// Get the service URI of the Storage Manager service
		String storageManagerUrl = getStorageManagerUrl(request.getHeader(HttpHeaders.AUTHORIZATION), productFile.getProcessingFacilityName());
		
		// Build the download URI
		String productDownloadUri = storageManagerUrl + "/storage/products/" + restProduct.getId();

		// --- TEST Just download "something" ---
		// TODO Remove test code!!
		productDownloadUri = "https://www.visit-a-church.info/fileadmin/images/id/rantepao/ri_rantepao_theresia_aussen1280x960_tuk_bassler_20160413.jpg";
		
		// Redirect the request to the download URI
		response.setStatusCode(HttpStatusCode.FOUND.getStatusCode());
		response.setHeader(HttpHeader.LOCATION, productDownloadUri);
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
