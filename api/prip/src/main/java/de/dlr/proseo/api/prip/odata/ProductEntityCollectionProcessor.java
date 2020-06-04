/**
 * ProductEntityCollectionProcessor.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmProperty;
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
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
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
	private static final int MSG_ID_EXCEPTION = 5007;
	private static final int MSG_ID_PRODUCT_WITHOUT_UUID = 5008;
	private static final int MSG_ID_INVALID_FILTER_CONDITION = 5009;

	/* Message string constants */
	private static final String MSG_INVALID_ENTITY_TYPE = "(E%d) Invalid entity type %s referenced in service request";
	private static final String MSG_URI_GENERATION_FAILED = "(E%d) URI generation from product UUID failed (cause: %s)";
	private static final String MSG_HTTP_REQUEST_FAILED = "(E%d) HTTP request failed (cause: %s)";
	private static final String MSG_SERVICE_REQUEST_FAILED = "(E%d) Service request failed with status %d (%s), cause: %s";
	private static final String MSG_NOT_AUTHORIZED_FOR_SERVICE = "(E%d) User %s not authorized for requested service";
	private static final String MSG_AUTH_MISSING_OR_INVALID = "(E%d) Basic authentication missing or invalid: %s";
	private static final String MSG_EXCEPTION = "(E%d) Request failed (cause %s: %s)";
	private static final String MSG_PRODUCT_WITHOUT_UUID = "(W%d) Product with database ID %d has no UUID";
	private static final String MSG_INVALID_FILTER_CONDITION = "(E%d) Invalid filter condition (cause: %s)";

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
		if (logger.isTraceEnabled()) logger.trace(">>> init({}, {})", odata, serviceMetadata);
		
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	/**
	 * Filter the given product list according to the conditions given in the filter option
	 * 
	 * @param productList the product list to filter
	 * @param filterOption the filtering conditions
	 * @throws ODataApplicationException if an error occurs during filter option evaluation
	 */
	private void filterProductList(List<Entity> productList, FilterOption filterOption) throws ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> filterProductList({}, {})", productList, filterOption);

		Expression filterExpression = filterOption.getExpression();
		
		// Loop over all entities in the product list and remove those that do not match the filtering conditions
		try {
			Iterator<Entity> entityIterator = productList.iterator();

			// Evaluate the expression for each entity
			// If the expression is evaluated to "true", keep the entity otherwise remove it from
			// the entityList
			while (entityIterator.hasNext()) {
				// To evaluate the the expression, create an instance of the Filter Expression
				// Visitor and pass the current entity to the constructor
				Entity currentEntity = entityIterator.next();
				FilterExpressionVisitor expressionVisitor = new FilterExpressionVisitor(currentEntity);

				// Evaluating the expression
				Object visitorResult = filterExpression.accept(expressionVisitor);		
				// The result of the filter expression must be of type Edm.Boolean
				if(visitorResult instanceof Boolean) {
					if(!Boolean.TRUE.equals(visitorResult)) {
						// The expression evaluated to false (or null), so we have to remove the
						// currentEntity from entityList
						entityIterator.remove();
					}
				} else {
					throw new ODataApplicationException("A filter expression must evaulate to type Edm.Boolean", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
				}
			} // End while
		} catch (ExpressionVisitException e) {
			throw new ODataApplicationException("Exception in filter evaluation",
					HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
		}
	}

	/**
	 * Sort the given product list by the given ordering criteria
	 * 
	 * @param productList the product list to sort
	 * @param orderByOption the ordering criteria to apply
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void sortProductList(List<Entity> productList, OrderByOption orderByOption) {
		if (logger.isTraceEnabled()) logger.trace(">>> sortProductList({}, {})", productList, orderByOption);

		List<OrderByItem> orderItemList = orderByOption.getOrders();
		final OrderByItem orderByItem = orderItemList.get(0); // TODO Support list of ordering items (recursive comparison function below)
		Expression expression = orderByItem.getExpression();

		if(expression instanceof Member){
			UriInfoResource resourcePath = ((Member)expression).getResourcePath();
			UriResource uriResource = resourcePath.getUriResourceParts().get(0);
			if (uriResource instanceof UriResourcePrimitiveProperty) {
				EdmProperty edmProperty = ((UriResourcePrimitiveProperty)uriResource).getProperty();
				final String sortPropertyName = edmProperty.getName();

				// do the sorting for the list of entities  
				Collections.sort(productList, (entity1, entity2) -> {
					int compareResult = 0;
					if (null == entity1.getProperty(sortPropertyName)) {
						compareResult = -1;
					} else if (null == entity2.getProperty(sortPropertyName)) {
						compareResult = +1;
					} else {
						compareResult = ((Comparable) entity1.getProperty(sortPropertyName).getValue())
								.compareTo((Comparable) entity2.getProperty(sortPropertyName).getValue());
					}

					// if 'desc' is specified in the URI, change the order
					if(orderByItem.isDescending()){
						return -compareResult; // just reverse order
					}

					return compareResult;
				});
			}
		}
	}
	
	/**
	 * Creates an "orderby" specifying ascending order by publication date
	 * @return the prepared "orderby" option
	 */
	private OrderByOption createPublicationDateAscendingOption() {
		// TODO
		return null;
		
	}

	/**
	 * Read the requested products from the prosEO kernel components
	 * 
	 * @param username the username for logging in to prosEO
	 * @param password the password for the user
	 * @param mission the mission to login to
	 * @param uriInfo additional URI parameters to consider in the request
	 * @return a collection of entities representing products
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 * @throws ODataApplicationException if an error occurs during evaluation of a filtering condition
	 */
	private EntityCollection queryProducts(String username, String password, String mission, UriInfo uriInfo) throws URISyntaxException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> queryProducts({}, ********, {})", username, mission);
		
		EntityCollection productsCollection = new EntityCollection();
		List<Entity> productList = new ArrayList<>();

		// Request product list from Ingestor service
		
		// Attempt connection to service
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> httpResponseEntity = null;
		try {
			RestTemplate restTemplate = ( null == username ? rtb.build() : rtb.basicAuthentication(mission + "-" + username, password).build() );
			String requestUrl = config.getIngestorUrl() + "/products?mission=" + mission;
			
			// TODO Add filter conditions from $filter option, if set (performance improvement)
			// Requires manual parsing of filter text --> expensive!
			
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			httpResponseEntity = restTemplate.getForEntity(requestUrl, List.class);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.error(String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst("Warning")));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst("Warning"));
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(String.format(MSG_NOT_AUTHORIZED_FOR_SERVICE, MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, username), e);
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
		if (!HttpStatus.OK.equals(httpResponseEntity.getStatusCode())) {
			String message = String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED, 
					httpResponseEntity.getStatusCodeValue(), httpResponseEntity.getStatusCode().toString(), httpResponseEntity.getHeaders().getFirst("Warning"));
			logger.error(message);
			throw new RuntimeException(message);
		}
		
		List<?> restProducts = httpResponseEntity.getBody();
		if (logger.isDebugEnabled()) logger.debug("... products found: " + restProducts.size());
		ObjectMapper mapper = new ObjectMapper();
		for (Object object: restProducts) {
			RestProduct restProduct = mapper.convertValue(object, RestProduct.class);
			
			// Check applicability
			if (null == restProduct.getUuid() || restProduct.getUuid().isEmpty()) {
				// ignore products without valid UUID
				logger.warn(String.format(MSG_PRODUCT_WITHOUT_UUID, MSG_ID_PRODUCT_WITHOUT_UUID, restProduct.getId()));
				continue;
			}
			
			// Filter products not yet generated
			if (null == restProduct.getGenerationTime() || restProduct.getProductFile().isEmpty()) {
				if (logger.isTraceEnabled()) logger.trace("... skipping product {} without product files", restProduct.getId());
				continue;
			}
			
			// Create output product
			Entity product = ProductUtil.toPripProduct(restProduct);
			productList.add(product);
		}
		
		// Check $filter option
		FilterOption filterOption = uriInfo.getFilterOption();
		if (null != filterOption) {
			filterProductList(productList, filterOption);
		}
		
		// Check $orderby option
		OrderByOption orderByOption = uriInfo.getOrderByOption();
		if (null == orderByOption) {
			// If nothing is specified, the output shall be ordered ascending by publication date (PRIP spec. v1.4, sec. 3.3)
			orderByOption = createPublicationDateAscendingOption();
		}
		if (null != orderByOption) {
			sortProductList(productList, orderByOption);
		}
		
		// Check $skip option
		SkipOption skipOption = uriInfo.getSkipOption();
		if (skipOption != null) {
		    int skipNumber = skipOption.getValue();
		    if (logger.isTraceEnabled()) logger.trace("... skipping {} products due to $skip option", skipNumber);
		    if (skipNumber >= 0) {
		        if(skipNumber <= productList.size()) {
		        	productList = productList.subList(skipNumber, productList.size());
		        } else {
		            // The client skipped all entities
		        	productList.clear();
		        }
		    } else {
		        throw new ODataApplicationException("Invalid value for $skip", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
		    }
		}
		
		// Check $top option
		TopOption topOption = uriInfo.getTopOption();
		if (topOption != null) {
		    int topNumber = topOption.getValue();
		    if (logger.isTraceEnabled()) logger.trace("... returning max. {} products due to $top option", topNumber);
		    if (topNumber >= 0) {
		        if(topNumber <= productList.size()) {
		        	productList = productList.subList(0, topNumber);
		        }  // else the client has requested more entities than available => return what we have
		    } else {
		        throw new ODataApplicationException("Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
		    }
		}
		
		// Add the product list to the product collection
		productsCollection.getEntities().addAll(productList);
		
		// Check $count option
		CountOption countOption = uriInfo.getCountOption();
		if (null != countOption && countOption.getValue()) {
		    productsCollection.setCount(productList.size());
		}

		if (logger.isTraceEnabled()) logger.trace("... returning " + productsCollection.getEntities().size() + " product entries");
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
		if (logger.isTraceEnabled()) logger.trace(">>> readEntityCollection({}, {}, {}, {})", request, response, uriInfo, responseFormat);
		
		// [1] Retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// [2] Fetch the data from backend for this requested EntitySetName (has to be delivered as EntityCollection object)
		EntityCollection entityCollection;
		if (edmEntitySet.getEntityType().getFullQualifiedName().equals(ProductEdmProvider.ET_PRODUCT_FQN)) {
			try {
				// Retrieve mission, user name and password from Authorization HTTP header
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

				// Query the backend services for the requested products, passing on user, password and mission
				entityCollection = queryProducts(userPassword[0], userPassword[1], missionUserPassword[0], uriInfo);
			} catch (URISyntaxException e) {
				String message = logError(MSG_URI_GENERATION_FAILED, MSG_ID_URI_GENERATION_FAILED, e.getMessage());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader("Warning", message);
				return;
			} catch (ODataApplicationException e) {
				String message = logError(MSG_INVALID_FILTER_CONDITION, MSG_ID_INVALID_FILTER_CONDITION, e.getMessage());
				response.setStatusCode(e.getStatusCode());
				response.setHeader("Warning", message);
				return;
			} catch (HttpClientErrorException e) {
				response.setStatusCode(e.getRawStatusCode());
				response.setHeader("Warning", e.getMessage()); // Message already logged and formatted
				return;
			} catch (Exception e) {
				String message = logError(MSG_EXCEPTION, MSG_ID_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
				e.printStackTrace();
				response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
				response.setHeader("Warning", message);
				return;
			}
		} else {
			String message = logError(MSG_INVALID_ENTITY_TYPE, MSG_ID_INVALID_ENTITY_TYPE, edmEntitySet.getEntityType().getFullQualifiedName());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader("Warning", message);
			return;
		}

		if (logger.isDebugEnabled()) logger.debug("... preparing data for response");
		
		// [3] Check for system query options
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();
		
		// [4] Create a serializer based on the requested format (json)
		ODataSerializer serializer = odata.createSerializer(responseFormat);

		// [5] Now serialize the content: transform from the EntitySet object to InputStream, taking into account system query options
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType,
				expandOption, selectOption);

		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).selectList(selectList).build();

		final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
		EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
				.id(id).contextURL(contextUrl).expand(expandOption).select(selectOption).build();
		SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entityCollection, opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		
		if (logger.isTraceEnabled()) logger.trace("<<< readEntityCollection()");
	}

}
