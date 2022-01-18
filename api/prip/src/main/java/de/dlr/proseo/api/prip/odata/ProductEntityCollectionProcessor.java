/**
 * ProductEntityCollectionProcessor.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.api.prip.ProductionInterfaceSecurity;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.enums.ProductVisibility;
import de.dlr.proseo.model.enums.UserRole;


/**
 * Retrieve product collections from the prosEO metadata database (via the Ingestor component) with additional information 
 * from the prosEO Storage Manager
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
@Transactional
public class ProductEntityCollectionProcessor implements EntityCollectionProcessor {

	/* Message ID constants */
	private static final int MSG_ID_INVALID_ENTITY_TYPE = 5001;
	private static final int MSG_ID_URI_GENERATION_FAILED = 5002;
	private static final int MSG_ID_UNSUPPORTED_FORMAT = 5008;
	private static final int MSG_ID_EXCEPTION = 5009;
	private static final int MSG_ID_INVALID_FILTER_CONDITION = 5010;
	private static final int MSG_ID_INVALID_QUERY_RESULT = 5011;

	/* Message string constants */
	private static final String MSG_INVALID_ENTITY_TYPE = "(E%d) Invalid entity type %s referenced in service request";
	private static final String MSG_URI_GENERATION_FAILED = "(E%d) URI generation from product UUID failed (cause: %s)";
	private static final String MSG_EXCEPTION = "(E%d) Request failed (cause %s: %s)";
	private static final String MSG_INVALID_QUERY_CONDITION = "(E%d) Invalid query condition (cause: %s)";
	private static final String MSG_UNSUPPORTED_FORMAT = "(E%d) Unsupported response format %s";
	private static final String MSG_INVALID_QUERY_RESULT = "(E%d) Invalid result for 'count(*)' query: %s";

	/* Other string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";

	/** The cached OData factory object */
	private OData odata;
	/** The cached metadata of the OData service */
	private ServiceMetadata serviceMetadata;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	/** The security utilities for the PRIP API */
	@Autowired
	private ProductionInterfaceSecurity securityConfig;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductEntityCollectionProcessor.class);

	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private String logError(String messageFormat, int messageId, Object... messageParameters) {
		return LogUtil.logError(logger, messageFormat, messageId, messageParameters);
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
	 * Create an SQL command with a "WHERE" clause derived from the "$filter" query parameter in the URI
	 * 
	 * @param uriInfo the URI info to analyze
	 * @param countOnly create a command, which only counts the requested products, but does not return them
	 * @return a native SQL command
	 * @throws ODataApplicationException if any error is encountered in the query options contained in the URI info object
	 */
	private StringBuilder createProductSqlQueryFilter(UriInfo uriInfo, boolean countOnly) throws ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> createProductSqlQueryFilter({})", uriInfo.getUriResourceParts());

		SqlFilterExpressionVisitor expressionVisitor = new SqlFilterExpressionVisitor();
		StringBuilder sqlCommand = new StringBuilder(expressionVisitor.getSqlCommand(countOnly));

		// Test filter option
		FilterOption filterOption = uriInfo.getFilterOption();
		if (null == filterOption) {
			sqlCommand.append("TRUE");
		} else {
			try {
				Expression filterExpression = filterOption.getExpression();
				String result = filterExpression.accept(expressionVisitor);
				logger.trace("accept() returns [" + result + "]");
		        if (null == result) {
					throw new NullPointerException("Unexpected null result from expressionVisitor");
				}
		        sqlCommand = new StringBuilder(expressionVisitor.getSqlCommand(countOnly)); // The number of parameters requested may have changed!
				sqlCommand.append(result);
			} catch (ODataApplicationException | ExpressionVisitException e) {
		        throw new ODataApplicationException("Exception thrown in filter expression: " + e.getMessage(),
		        		HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
			}
		}
		
		// Add filter for mission
		sqlCommand.append("\nAND m.code = '").append(securityConfig.getMission()).append("'");
		
		// Add filter for user's access permissions
		StringBuilder permissionFilter = new StringBuilder("AND pc.visibility IN ('");
		permissionFilter.append(ProductVisibility.PUBLIC.toString()).append("'");
		if (securityConfig.hasRole(UserRole.PRODUCT_READER_RESTRICTED) || securityConfig.hasRole(UserRole.PRODUCT_READER_ALL)) {
			permissionFilter.append(", '").append(ProductVisibility.RESTRICTED.toString()).append("'");
		}
		if (securityConfig.hasRole(UserRole.PRODUCT_READER_ALL)) {
			permissionFilter.append(", '").append(ProductVisibility.INTERNAL.toString()).append("'");
		}
		permissionFilter.append(")");
		sqlCommand.append("\n").append(permissionFilter);
		
		return sqlCommand;
	}

	/**
	 * Convert the given URI info object into a native SQL command to select the requested products. In addition to the URI info
	 * the product class access rights of the logged in user will be respected.
	 * 
	 * @param uriInfo the URI info to analyze
	 * @return a native SQL command
	 * @throws ODataApplicationException if any error is encountered in the query options contained in the URI info object
	 */
	private String createProductSqlQuery(UriInfo uriInfo) throws ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> createProductSqlQuery({})", uriInfo.getUriResourceParts());

		SqlFilterExpressionVisitor expressionVisitor = new SqlFilterExpressionVisitor();
		StringBuilder sqlCommand = new StringBuilder(expressionVisitor.getSqlCommand(false));

		sqlCommand = createProductSqlQueryFilter(uriInfo, false);

		// Test order option
		OrderByOption orderByOption = uriInfo.getOrderByOption();
		if (null != orderByOption) {
			StringBuilder orderByClause = new StringBuilder();
			List<OrderByItem> orderByItems = orderByOption.getOrders();
			boolean first = true;
			for (OrderByItem orderByItem : orderByItems) {
				if (first) {
					orderByClause.append("ORDER BY ");
					first = false;
				} else {
					orderByClause.append(", ");
				}
				try {
					String orderExpression = orderByItem.getExpression().accept(new SqlFilterExpressionVisitor());
					orderByClause.append(orderExpression).append(" ").append(orderByItem.isDescending() ? "DESC" : "ASC");
				} catch (ExpressionVisitException | ODataApplicationException e) {
			        throw new ODataApplicationException("Exception thrown in orderBy expression: " + e.getMessage(),
			        		HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
				}
			} 
			sqlCommand.append("\n").append(orderByClause);
		}
		
		// Test topOption
		TopOption topOption = uriInfo.getTopOption();
		if (null != topOption) {
			sqlCommand.append("\nLIMIT ").append(topOption.getValue());
		}

		// Test skip option
		SkipOption skipOption = uriInfo.getSkipOption();
		if (null != skipOption) {
			sqlCommand.append("\nOFFSET ").append(skipOption.getValue());
		}
		
		logger.trace("<<< createProductSqlQuery() -> SQL command:\n" + sqlCommand);
		return sqlCommand.toString();
	}

	/**
	 * Read the requested products from the prosEO kernel components
	 * @param uriInfo additional URI parameters to consider in the request
	 * 
	 * @return a collection of entities representing products
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 * @throws ODataApplicationException if an error occurs during evaluation of a filtering condition
	 */
	private EntityCollection queryProducts(UriInfo uriInfo) throws URISyntaxException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> queryProducts({})", uriInfo);
		
		EntityCollection productsCollection = new EntityCollection();
		List<Entity> productList = new ArrayList<>();
		
		// Request product list from database
		String sqlCommand = createProductSqlQuery(uriInfo);
		
		Query query = em.createNativeQuery(sqlCommand, Product.class);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Product) {
				// Create output product
				Entity product = ProductUtil.toPripProduct((Product) resultObject);
				productList.add(product);
			}
		}		
		if (logger.isDebugEnabled()) logger.debug("... products found: " + productList.size());
		
		// Add the product list to the product collection
		productsCollection.getEntities().addAll(productList);
		
		// Check $count option
		CountOption countOption = uriInfo.getCountOption();
		if (null != countOption && countOption.getValue()) {
			sqlCommand = createProductSqlQueryFilter(uriInfo, true).toString();
			
			query = em.createNativeQuery(sqlCommand);
			Integer collectionSize = 0;
			String queryResult = query.getSingleResult().toString();
			try {
				collectionSize = Integer.parseInt(queryResult);
			} catch (NumberFormatException e) {
				logError(MSG_INVALID_QUERY_RESULT, MSG_ID_INVALID_QUERY_RESULT, queryResult);
			}
			
		    if (logger.isTraceEnabled()) logger.trace("... returning collection size {} due to $count option", collectionSize);
		    productsCollection.setCount(collectionSize);
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
	 * @throws ODataLibraryException if the Olingo OData library detects an error
	 */
	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled()) logger.trace(">>> readEntityCollection({}, {}, {}, {})", request, response, uriInfo, responseFormat);
		
		// Prepare the output
		ODataSerializer serializer = odata.createSerializer(responseFormat);
		
		// [1] Retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// [2] Fetch the data from backend for this requested EntitySetName (has to be delivered as EntityCollection object)
		EntityCollection entityCollection;
		if (edmEntitySet.getEntityType().getFullQualifiedName().equals(ProductEdmProvider.ET_PRODUCT_FQN)) {
			try {
				// Query the backend services for the requested products, passing on user, password and mission
				entityCollection = queryProducts(uriInfo);
			} catch (URISyntaxException e) {
				String message = logError(MSG_URI_GENERATION_FAILED, MSG_ID_URI_GENERATION_FAILED, e.getMessage());
				response.setContent(serializer.error(
						LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), message)).getContent());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			} catch (ODataApplicationException e) {
				String message = logError(MSG_INVALID_QUERY_CONDITION, MSG_ID_INVALID_FILTER_CONDITION, e.getMessage());
				response.setContent(serializer.error(
						LogUtil.oDataServerError(e.getStatusCode(), message)).getContent());
				response.setStatusCode(e.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			} catch (Exception e) {
				String message = logError(MSG_EXCEPTION, MSG_ID_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
				e.printStackTrace();
				response.setContent(serializer.error(
						LogUtil.oDataServerError(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), message)).getContent());
				response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			}
		} else {
			String message = logError(MSG_INVALID_ENTITY_TYPE, MSG_ID_INVALID_ENTITY_TYPE, edmEntitySet.getEntityType().getFullQualifiedName());
			response.setContent(serializer.error(
					LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), message)).getContent());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}

		if (logger.isDebugEnabled()) logger.debug("... preparing data for response");
		
		// [3] Check for system query options
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();
		CountOption countOption = uriInfo.getCountOption();
		
		InputStream serializedContent = null;
		try {
			// [4] Create a serializer based on the requested format (json)
			if (!ContentType.APPLICATION_JSON.isCompatible(responseFormat)) {
				// Any other format currently throws an exception (see Github issue #122)
				String message = logError(MSG_UNSUPPORTED_FORMAT, MSG_ID_UNSUPPORTED_FORMAT, responseFormat.toContentTypeString());
				response.setContent(serializer.error(
						LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), message)).getContent());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			}

			// [5] Now serialize the content: transform from the EntitySet object to InputStream, taking into account system query options
			EdmEntityType edmEntityType = edmEntitySet.getEntityType();
			String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType,
					expandOption, selectOption);

			ContextURL contextUrl = ContextURL.with()
					.entitySet(edmEntitySet)
					.selectList(selectList)
					.build();

			final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
			EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
					.id(id)
					.contextURL(contextUrl)
					.expand(expandOption)
					.select(selectOption)
					.count(countOption)
					.build();
			SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entityCollection, opts);
			serializedContent = serializerResult.getContent();
		} catch (Exception e) {
			String message = logError(MSG_EXCEPTION, MSG_ID_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
			e.printStackTrace();
			response.setContent(serializer.error(
					LogUtil.oDataServerError(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), message)).getContent());
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		
		if (logger.isTraceEnabled()) logger.trace("<<< readEntityCollection()");
	}

}
