/**
 * ProductEntityProcessor.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.api.prip.ProductionInterfaceConfiguration;
import de.dlr.proseo.api.prip.ProductionInterfaceSecurity;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.PripMessage;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.UserRole;

/**
 * Retrieve product information from the prosEO metadata database (via the Ingestor component) and download product data from the
 * prosEO Storage Manager
 *
 * @author Dr. Thomas Bassler
 */
@Component
@Transactional
public class ProductEntityProcessor implements EntityProcessor, MediaEntityProcessor {

	// Unformatted message
	private static final String MSG_CANNOT_FILTER_SERIALIZED_OUTPUT = "Cannot filter serialized output";

	/* Other string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";

	/** The cached OData factory object */
	private OData odata;
	/** The cached metadata of the OData service */
	private ServiceMetadata serviceMetadata;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

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
	private static ProseoLogger logger = new ProseoLogger(ProductEntityProcessor.class);

	/**
	 * Initializes the processor for each HTTP request - response cycle (Copied from interface definition)
	 *
	 * @param odata           Olingo's root object, acting as a factory for various object types
	 * @param serviceMetadata metadata of the OData service like the EDM that have to be created before the OData request handling
	 *                        takes place
	 */
	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		if (logger.isTraceEnabled())
			logger.trace(">>> init({}, {})", odata, serviceMetadata);

		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	/**
	 * Get the metadata for a single product from the prosEO Ingestor service
	 *
	 * @param productUuid the UUID of the product to retrieve
	 *
	 * @return a product object
	 * @throws NoResultException if a product with the requested UUID could not be found in the database
	 * @throws SecurityException if the logged in user is not authorized to access the requested product
	 */
	private Product getProduct(String productUuid) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProduct({})", productUuid);

		// Request product metadata from database
		Query query = em.createQuery("select p from Product p where p.uuid = :uuid", Product.class);
		query.setParameter("uuid", UUID.fromString(productUuid));
		Object resultObject;
		try {
			resultObject = query.getSingleResult();
			if (null == resultObject || !(resultObject instanceof Product)) {
				throw new NoResultException();
			}
		} catch (NoResultException e) {
			String message = logger.log(PripMessage.MSG_PRODUCT_NOT_FOUND, productUuid);
			throw new NoResultException(message);
		}
		Product modelProduct = (Product) resultObject;

		// Check mission
		if (!securityConfig.getMission().equals(modelProduct.getProductClass().getMission().getCode())) {
			String message = logger.log(PripMessage.MSG_NOT_AUTHORIZED_FOR_PRODUCT, productUuid);
			throw new SecurityException(message);
		}

		// Check access permission to product
		switch (modelProduct.getProductClass().getVisibility()) {
		case PUBLIC:
			// OK for all users
			break;
		case RESTRICTED:
			if (securityConfig.hasRole(UserRole.PRODUCT_READER_RESTRICTED)) {
				break;
			}
			// Fall through
		case INTERNAL:
			if (securityConfig.hasRole(UserRole.PRODUCT_READER_ALL)) {
				break;
			}
			String message = logger.log(PripMessage.MSG_NOT_AUTHORIZED_FOR_PRODUCT,
					securityConfig.getMission() + "\\" + securityConfig.getUser(), productUuid);
			throw new SecurityException(message);
		}

		if (logger.isDebugEnabled())
			logger.debug("... product found: " + modelProduct.getId());
		return modelProduct;
	}

	/**
	 * Download the requested product from the prosEO Storage Manager
	 *
	 * @param productUuid the UUID of the product to retrieve
	 *
	 * @return a binary stream containing the product data
	 * @throws URISyntaxException       if a valid URI cannot be generated from any product UUID
	 * @throws IllegalArgumentException if mandatory information is missing from the prosEO interface product
	 * @throws NoResultException        if a product with the requested UUID could not be found in the database
	 * @throws SecurityException        if the logged in user is not authorized to access the requested product
	 */
	private Entity getProductAsEntity(String productUuid)
			throws URISyntaxException, IllegalArgumentException, NoSuchElementException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProductAsEntity({})", productUuid);

		// Get the product information from the Database
		Product modelProduct = getProduct(productUuid);

		// Create output product
		Entity product = ProductUtil.toPripProduct(modelProduct);

		if (logger.isTraceEnabled())
			logger.trace("<<< getProductAsEntity()");
		return product;
	}

	/**
	 * Retrieve a download token for the requested product ID and file name from the Ingestor service
	 *
	 * @param id              the product ID
	 * @param productFileName the product file name
	 * @return a JSON Web Token for authentication with the Storage Manager
	 * @throws HttpClientErrorException if an error is returned from the Ingestor service
	 * @throws RestClientException      if the request to the Ingestor fails for some other reason
	 * @throws RuntimeException         if any other exception occurs
	 * @throws SecurityException        if the logged in user is not authorized to access the requested product
	 */
	private String retrieveDownloadToken(long id, String productFileName)
			throws HttpClientErrorException, RestClientException, RuntimeException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> retrieveDownloadToken({}, {})", id, productFileName);

		// Request product metadata from Ingestor service

		// Attempt connection to service
		ResponseEntity<String> entity = null;
		try {
			RestTemplate restTemplate = rtb
				.basicAuthentication(securityConfig.getMission() + "-" + securityConfig.getUser(), securityConfig.getPassword())
				.build();
			String requestUrl = config.getIngestorUrl() + "/products/" + id + "/download/token?fileName=" + productFileName;
			if (logger.isTraceEnabled())
				logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, String.class);
		} catch (HttpClientErrorException.Unauthorized e) {
			String message = logger.log(PripMessage.MSG_NOT_AUTHORIZED_FOR_SERVICE, securityConfig.getUser());
			throw new SecurityException(message);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.log(PripMessage.MSG_SERVICE_REQUEST_FAILED, e.getStatusCode().value(), e.getStatusCode().toString(),
					e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING));
		} catch (RestClientException e) {
			String message = logger.log(PripMessage.MSG_HTTP_REQUEST_FAILED, e.getMessage());
			throw new RestClientException(message, e);
		} catch (Exception e) {
			logger.log(PripMessage.MSG_EXCEPTION, e.getMessage(), e);
			throw new RuntimeException(e);
		}

		// All GET requests should return HTTP status OK
		if (!HttpStatus.OK.equals(entity.getStatusCode())) {
			String message = logger.log(PripMessage.MSG_SERVICE_REQUEST_FAILED, entity.getStatusCodeValue(),
					entity.getStatusCode().toString(), entity.getHeaders().getFirst(HTTP_HEADER_WARNING));
			throw new RuntimeException(message);
		}

		String downloadToken = entity.getBody();
		if (logger.isDebugEnabled())
			logger.debug("... token generated: " + downloadToken);
		return downloadToken;
	}

	/**
	 * Reads entities data from persistence and puts serialized content and status into the response.
	 *
	 * @param request        OData request object containing raw HTTP information
	 * @param response       OData response object for collecting response data
	 * @param uriInfo        information of a parsed OData URI
	 * @param responseFormat requested content type after content negotiation
	 * @throws ODataApplicationException if the service implementation encounters a failure
	 * @throws ODataLibraryException     if an error during serialization occurs
	 */
	@Override
	public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> readEntity({}, {}, {}, {})", request, response, uriInfo, responseFormat);

		// Prepare the output
		ODataSerializer serializer = odata.createSerializer(responseFormat);

		// [1] Retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first
																									// segment is the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// [2] Fetch the data from backend for this requested EntitySetName (has to be delivered as Entity object)
		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();

		Entity entity;
		if (edmEntitySet.getEntityType().getFullQualifiedName().equals(ProductEdmProvider.ET_PRODUCT_FQN)) {
			try {
				entity = getProductAsEntity(keyPredicates.get(0).getText());
			} catch (SecurityException e) {
				response.setContent(
						serializer.error(LogUtil.oDataServerError(HttpStatusCode.UNAUTHORIZED.getStatusCode(), e.getMessage()))
							.getContent());
				response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
				return;
			} catch (URISyntaxException e) {
				String message = logger.log(PripMessage.MSG_URI_GENERATION_FAILED, e.getMessage());
				response.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), message))
					.getContent());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			} catch (IllegalArgumentException e) {
				response.setContent(
						serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getMessage()))
							.getContent());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
				return;
			} catch (NoResultException e) {
				response
					.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.NOT_FOUND.getStatusCode(), e.getMessage()))
						.getContent());
				response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
				return;
			} catch (Exception e) {
				String message = logger.log(PripMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
				e.printStackTrace();
				response.setContent(
						serializer.error(LogUtil.oDataServerError(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), message))
							.getContent());
				response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			}
		} else {
			String message = logger.log(PripMessage.MSG_INVALID_ENTITY_TYPE, edmEntitySet.getEntityType().getFullQualifiedName());
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), message)).getContent());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}

		if (logger.isDebugEnabled())
			logger.debug("... preparing data for response");

		// [3] Check for system query options
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();

		// [4] Create a serializer based on the requested format (json)
		if (!ContentType.APPLICATION_JSON.isCompatible(responseFormat)) {
			// Any other format currently throws an exception (see Github issue #122)
			String message = logger.log(PripMessage.MSG_UNSUPPORTED_FORMAT, responseFormat.toContentTypeString());
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), message)).getContent());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}

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

		// Filter out elements with "null" content (i. e. empty optional fields like Footprint and GeoFootprint)
		// Workaround because there is no way to get Olingo to do this (see also https://issues.apache.org/jira/browse/OLINGO-1361)
		InputStream intermediateContent = serializerResult.getContent();
		InputStream serializedContent = null;

		try {
			// Deserialize JSON output
			ObjectMapper om = new ObjectMapper();
			Map<?, ?> intermediateMap = om.readValue(intermediateContent, Map.class);

			// Remove all fields with null values
			Iterator<?> productMapKeyIter = intermediateMap.keySet().iterator();
			while (productMapKeyIter.hasNext()) {
				if (null == intermediateMap.get(productMapKeyIter.next())) {
					productMapKeyIter.remove();
				}
			}

			// Re-serialize into JSON
			ByteArrayOutputStream cleanedOutput = new ByteArrayOutputStream();
			om.writeValue(cleanedOutput, intermediateMap);

			serializedContent = new ByteArrayInputStream(cleanedOutput.toByteArray());
		} catch (IOException e) {
			// Highly unlikely given that we transform JSON to Map to JSON using the same ObjectMapper
			throw new ODataApplicationException(MSG_CANNOT_FILTER_SERIALIZED_OUTPUT, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					Locale.ROOT, e);
		}

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

		if (logger.isTraceEnabled())
			logger.trace("<<< readEntity()");
	}

	/**
	 * Download a product from the prosEO Storage Manager (any Storage Manager instance will do, because in the case of multiple
	 * copies of the product on various facilities, these should be identical); the Storage Manager is identified via the Facility
	 * Manager component
	 * <p>
	 * This method does not actually return the data stream for the product, but rather redirects to the download URI at the Storage
	 * Manager
	 *
	 * @param request        OData request object containing raw HTTP information
	 * @param response       OData response object for collecting response data
	 * @param uriInfo        information of a parsed OData URI
	 * @param responseFormat requested content type after content negotiation
	 * @throws ODataApplicationException if the service implementation encounters a failure
	 * @throws ODataLibraryException     if an error during serialization occurs
	 */
	@Override
	public void readMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> readMediaEntity({}, {}, {}, {})", request, response, uriInfo, responseFormat);

		// Find the requested product
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0); // in our example,
																													// the first
																													// segment is
																													// the EntitySet
		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		Product modelProduct;

		ODataSerializer serializer = odata.createSerializer(ContentType.JSON); // Serializer for error messages only
		try {
			modelProduct = getProduct(keyPredicates.get(0).getText());
		} catch (NoResultException e) {
			response.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.NOT_FOUND.getStatusCode(), e.getMessage()))
				.getContent());
			response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (SecurityException e) {
			response
				.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.UNAUTHORIZED.getStatusCode(), e.getMessage()))
					.getContent());
			response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (Exception e) {
			String message = logger.log(PripMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
			e.printStackTrace();
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), message))
						.getContent());
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}

		// Check whether the product is actually available on some processing facility
		if (modelProduct.getProductFile().isEmpty()) {
			String message = logger.log(PripMessage.MSG_PRODUCT_NOT_AVAILABLE, keyPredicates.get(0).getText());
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.NOT_FOUND.getStatusCode(), message)).getContent());
			response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
		}
		// Select the first product file to transfer (they should be identical anyway)
		ProductFile productFile = modelProduct.getProductFile().iterator().next();

		// Retrieve a download token from the Ingestor
		String productFileName = (null == productFile.getZipFileName() ? productFile.getProductFileName()
				: productFile.getZipFileName());
		String downloadToken;
		try {
			downloadToken = retrieveDownloadToken(modelProduct.getId(), productFileName);
		} catch (HttpClientErrorException e) {
			response.setContent(serializer.error(LogUtil.oDataServerError(e.getRawStatusCode(), e.getMessage())).getContent());
			response.setStatusCode(e.getRawStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (RestClientException e) {
			response
				.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getMessage()))
					.getContent());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (SecurityException e) {
			response
				.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.UNAUTHORIZED.getStatusCode(), e.getMessage()))
					.getContent());
			response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (Exception e) {
			String message = logger.log(PripMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
			e.printStackTrace();
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), message))
						.getContent());
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}

		// Get the service URI of the Storage Manager service
		String storageManagerUrl = productFile.getProcessingFacility().getExternalStorageManagerUrl();

		// Build the download URI: Set pathInfo to zipped file if available, to product file otherwise
		URIBuilder uriBuilder = null;
		try {
			uriBuilder = new URIBuilder(storageManagerUrl + "/products/download");
			uriBuilder.addParameter("pathInfo", productFile.getFilePath() + "/" + productFileName);
			uriBuilder.addParameter("token", downloadToken);
		} catch (URISyntaxException e) {
			String message = logger.log(PripMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
			e.printStackTrace();
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), message))
						.getContent());
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
								logger.log(PripMessage.MSG_INVALID_RANGE_HEADER, rangeHeader);
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
						logger.log(PripMessage.MSG_INVALID_RANGE_HEADER, rangeHeader);
					}
				} else {
					logger.log(PripMessage.MSG_INVALID_RANGE_HEADER, rangeHeader);
				}
			} else {
				logger.log(PripMessage.MSG_INVALID_RANGE_HEADER, rangeHeader);
			}
		}
		// Redirect the request to the download URI
		logger.log(PripMessage.MSG_REDIRECT, uriBuilder.toString());
		response.setStatusCode(HttpStatusCode.TEMPORARY_REDIRECT.getStatusCode());
		response.setHeader(HttpHeader.LOCATION, uriBuilder.toString());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

		if (logger.isTraceEnabled())
			logger.trace("<<< readMediaEntity()");
	}

	/*
	 * ------------------------------------------------------------------------------------ 
	 * The methods below are not available on the PRIP, since this is a read-only interface
	 * ------------------------------------------------------------------------------------
	 */

	/**
	 * Creates an entity.
	 *
	 * @param request        The OData request.
	 * @param response       The OData response.
	 * @param uriInfo        The URI information.
	 * @param requestFormat  The request content type.
	 * @param responseFormat The response content type.
	 * @throws ODataApplicationException If there is an error in the OData application.
	 * @throws ODataLibraryException     If there is an error in the OData library.
	 */
	@Override
	public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);

		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logger.log(PripMessage.MSG_FORBIDDEN));
	}

	/**
	 * Deletes an entity.
	 *
	 * @param request  The OData request.
	 * @param response The OData response.
	 * @param uriInfo  The URI information.
	 * @throws ODataApplicationException If there is an error in the OData application.
	 * @throws ODataLibraryException     If there is an error in the OData library.
	 */
	@Override
	public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteEntity({}, {}, {})", request, response, uriInfo);

		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logger.log(PripMessage.MSG_FORBIDDEN));
	}

	/**
	 * Updates an entity.
	 *
	 * @param request        The OData request.
	 * @param response       The OData response.
	 * @param uriInfo        The URI information.
	 * @param requestFormat  The request content type.
	 * @param responseFormat The response content type.
	 * @throws ODataApplicationException If there is an error in the OData application.
	 * @throws ODataLibraryException     If there is an error in the OData library.
	 */
	@Override
	public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);

		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logger.log(PripMessage.MSG_FORBIDDEN));
	}

	/**
	 * Creates a media entity.
	 *
	 * @param request        The OData request.
	 * @param response       The OData response.
	 * @param uriInfo        The URI information.
	 * @param requestFormat  The request content type.
	 * @param responseFormat The response content type.
	 * @throws ODataApplicationException If there is an error in the OData application.
	 * @throws ODataLibraryException     If there is an error in the OData library.
	 */
	@Override
	public void createMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createMediaEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);

		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logger.log(PripMessage.MSG_FORBIDDEN));
	}

	/**
	 * Deletes a media entity.
	 *
	 * @param request  The OData request.
	 * @param response The OData response.
	 * @param uriInfo  The URI information.
	 * @throws ODataApplicationException If there is an error in the OData application.
	 * @throws ODataLibraryException     If there is an error in the OData library.
	 */
	@Override
	public void deleteMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteMediaEntity({}, {}, {})", request, response, uriInfo);

		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logger.log(PripMessage.MSG_FORBIDDEN));
	}

	/**
	 * Updates a media entity.
	 *
	 * @param request        The OData request.
	 * @param response       The OData response.
	 * @param uriInfo        The URI information.
	 * @param requestFormat  The request content type.
	 * @param responseFormat The response content type.
	 * @throws ODataApplicationException If there is an error in the OData application.
	 * @throws ODataLibraryException     If there is an error in the OData library.
	 */
	@Override
	public void updateMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateMediaEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);

		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logger.log(PripMessage.MSG_FORBIDDEN));
	}

}