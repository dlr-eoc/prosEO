/**
 * ProductEntityCollectionProcessor.java
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
import org.apache.olingo.commons.api.data.EntityCollection;
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
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.api.prip.ProductionInterfaceConfiguration;
import de.dlr.proseo.interfaces.rest.model.RestProduct;


/**
 * Retrieve product collections from the prosEO metadata database (via the Ingestor component) with additional information 
 * from the prosEO Storage Manager
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductEntityCollectionProcessor implements EntityCollectionProcessor {

	/* Message ID constants */
	private static final int MSG_ID_INVALID_ENTITY_TYPE = 5001;
	private static final int MSG_ID_URI_GENERATION_FAILED = 5002;
	private static final int MSG_ID_HTTP_REQUEST_FAILED = 5003;
	private static final int MSG_ID_SERVICE_REQUEST_FAILED = 5004;
	private static final int MSG_ID_NOT_AUTHORIZED_FOR_SERVICE = 5005;
	private static final int MSG_ID_AUTH_MISSING_OR_INVALID = 5006;

	/* Message string constants */
	private static final String MSG_INVALID_ENTITY_TYPE = "(E%d) Invalid entity type %s referenced in service request";
	private static final String MSG_URI_GENERATION_FAILED = "(E%d) URI generation from product UUID failed (cause: %s)";
	private static final String MSG_HTTP_REQUEST_FAILED = "(E%d) HTTP request failed (cause: %s)";
	private static final String MSG_SERVICE_REQUEST_FAILED = "(E%d) Service request failed with status %d (%s), cause: %s";
	private static final String MSG_NOT_AUTHORIZED_FOR_SERVICE = "(E%d) User %s not authorized for requested service";
	private static final String MSG_AUTH_MISSING_OR_INVALID = "(E%d) Basic authentication missing or invalid: %s";

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
	private static Logger logger = LoggerFactory.getLogger(ProductEntityCollectionProcessor.class);

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
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	/**
	 * Read the requested products from the prosEO kernel components
	 * (temporarily: hard coded examples)
	 * 
	 * @return a collection of entities representing products
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 */
	private EntityCollection queryProducts(String username, String password, String mission) throws URISyntaxException {
		EntityCollection productsCollection = new EntityCollection();
		List<Entity> productList = productsCollection.getEntities();

		// Request product list from Ingestor service
		
		// Attempt connection to service
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = null;
		try {
			RestTemplate restTemplate = ( null == username ? rtb.build() : rtb.basicAuthentication(username, password).build() );
			String requestUrl = config.getIngestorUrl() + "?mission=" + mission;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, List.class);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.error(String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst("Warning")));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst("Warning"));
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
					entity.getStatusCodeValue(), entity.getStatusCode().toString(), entity.getHeaders().getFirst("Warning"));
			logger.error(message);
			throw new RuntimeException(message);
		}
		
		List<?> restProducts = entity.getBody();
		ObjectMapper mapper = new ObjectMapper();
		for (Object object: restProducts) {
			RestProduct restProduct = mapper.convertValue(object, RestProduct.class);
			Entity product = new Entity()
					.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, UUID.fromString(restProduct.getUuid())))
					.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, restProduct.getProductClass()))
					.addProperty(new Property(null, "ContentType", ValueType.PRIMITIVE,
							"application/octet-stream"));
			product.setId(new URI(ProductEdmProvider.ET_PRODUCT_NAME + "('" + restProduct.getUuid() + "')"));
			productList.add(product);
		}

		return productsCollection;
	}

	/**
	 * Reads entities data from persistence and puts serialized content and status into the response.
	 * 
	 * @param request OData request object containing raw HTTP information
	 * @param response OData response object for collecting response data
	 * @param uriInfo information of a parsed OData URI
	 * @param responseFormat requested content type after content negotiation
	 * @throws ODataApplicationException if the service implementation encounters a failure
	 * @throws ODataLibraryException
	 */
	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		
		// 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// 2nd: fetch the data from backend for this requested EntitySetName
		// it has to be delivered as EntitySet object
		EntityCollection entitySet;
		if (edmEntitySet.getEntityType().getFullQualifiedName().equals(ProductEdmProvider.ET_PRODUCT_FQN)) {
			try {
				String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
				if (null == authHeader) {
					String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
					response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
					response.setHeader("Warning", message);
					return;
				}
				String[] authParts = authHeader.split(" ");
				if (2 != authParts.length || !"Basic".equals(authParts[0])) {
					String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
					response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
					response.setHeader("Warning", message);
					return;
				}
				String[] missionUserPassword = (new String(Base64.getDecoder().decode(authParts[1]))).split("\\\\"); // --> regex "\\" --> matches "\"
				if (2 != missionUserPassword.length) {
					String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
					response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
					response.setHeader("Warning", message);
					return;
				}
				String[] userPassword = missionUserPassword[1].split(":"); // guaranteed to work as per BasicAuth specification
				entitySet = queryProducts(userPassword[0], userPassword[1], missionUserPassword[0]);
			} catch (URISyntaxException e) {
				String message = logError(MSG_URI_GENERATION_FAILED, MSG_ID_URI_GENERATION_FAILED, e.getMessage());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader("Warning", message);
				return;
			} catch (HttpClientErrorException e) {
				response.setStatusCode(e.getRawStatusCode());
				response.setHeader("Warning", e.getMessage());
				return;
			} catch (Exception e) {
				response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
				response.setHeader("Warning", e.getMessage());
				return;
			}
		} else {
			String message = logError(MSG_INVALID_ENTITY_TYPE, MSG_ID_INVALID_ENTITY_TYPE, edmEntitySet.getEntityType().getFullQualifiedName());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader("Warning", message);
			return;
		}

		// 3rd: create a serializer based on the requested format (json)
		ODataSerializer serializer = odata.createSerializer(responseFormat);

		// 4th: Now serialize the content: transform from the EntitySet object to InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

		final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
		EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl).build();
		SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet, opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

}
