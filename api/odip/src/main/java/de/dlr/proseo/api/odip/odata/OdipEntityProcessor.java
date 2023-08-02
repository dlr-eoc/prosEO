/**
 * OdipEntityProcessor.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.odata;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

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
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.api.odip.OdipApplicationBase;
import de.dlr.proseo.api.odip.OdipConfiguration;
import de.dlr.proseo.api.odip.OdipSecurity;
import de.dlr.proseo.api.odip.odata.OdipUtilBase.OdipException;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.OdipMessage;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.model.rest.model.RestOrder;

/**
 * Retrieve information from the prosEO metadata database
 *
 * @author Dr. Thomas Bassler
 *
 */
@Component
@Transactional
public class OdipEntityProcessor implements EntityProcessor {

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
	private OdipConfiguration config;

	/** The security utilities for the ODIP API */
	@Autowired
	private OdipSecurity securityConfig;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OdipEntityProcessor.class);

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
	 * Get the metadata for a single production order from the prosEO Order Manager service
	 *
	 * @param productionOrderUuid the UUID of the production order to retrieve
	 *
	 * @return a production order object
	 * @throws NoResultException if a production order with the requested UUID could not be found in the database
	 * @throws SecurityException if the logged in user is not authorized to access the requested production order
	 */
	private ProcessingOrder getProductionOrder(String productionOrderUuid) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProductionOrder({})", productionOrderUuid);

		// Request production order metadata from database
		Query query = em.createQuery("select p from ProcessingOrder p where p.uuid = :uuid", ProcessingOrder.class);
		query.setParameter("uuid", UUID.fromString(productionOrderUuid));
		Object resultObject;
		try {
			resultObject = query.getSingleResult();
			if (null == resultObject || !(resultObject instanceof ProcessingOrder)) {
				throw new NoResultException();
			}
		} catch (NoResultException e) {
			String message = logger.log(OdipMessage.MSG_PRODUCTIONORDER_NOT_FOUND, productionOrderUuid);
			throw new NoResultException(message);
		}
		ProcessingOrder modelOrder = (ProcessingOrder) resultObject;

		// Check mission
		if (!securityConfig.getMission().equals(modelOrder.getMission().getCode())) {
			String message = logger.log(OdipMessage.MSG_NOT_AUTHORIZED_FOR_PRODUCTIONORDER, productionOrderUuid);
			throw new SecurityException(message);
		}

		// Check access permission to production order
		if (securityConfig.hasRole(UserRole.ORDER_READER) || securityConfig.hasRole(UserRole.ORDER_MGR)) {
		} else {
			String message = logger.log(OdipMessage.MSG_NOT_AUTHORIZED_FOR_PRODUCTIONORDER,
					securityConfig.getMission() + "\\" + securityConfig.getUser(), productionOrderUuid);
			throw new SecurityException(message);
		}

		if (logger.isDebugEnabled())
			logger.debug("... production order found: " + modelOrder.getId());
		return modelOrder;
	}

	/**
	 * Get the metadata for a single production order from the prosEO Order Manager service
	 *
	 * @param productionOrderIdentifier the UUID of the production order to retrieve
	 * @return a production order object
	 * @throws NoResultException if a production order with the requested UUID could not be found in the database
	 * @throws SecurityException if the logged in user is not authorized to access the requested production order
	 */
	public ProcessingOrder getProductionOrderByIdentifier(String productionOrderIdentifier)
			throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProductionOrderByIdentifier({})", productionOrderIdentifier);

		// Request production order metadata from database
		Query query = em.createNativeQuery(
				"select p from processing_order p where p.identifier = '" + productionOrderIdentifier + "'", ProcessingOrder.class);
		Object resultObject;
		try {
			resultObject = query.getSingleResult();
			if (null == resultObject || !(resultObject instanceof ProcessingOrder)) {
				throw new NoResultException();
			}
		} catch (NoResultException e) {
			String message = logger.log(OdipMessage.MSG_PRODUCTIONORDER_NOT_FOUND, productionOrderIdentifier);
			throw new NoResultException(message);
		}
		ProcessingOrder modelOrder = (ProcessingOrder) resultObject;

		// Check mission
		if (!securityConfig.getMission().equals(modelOrder.getMission().getCode())) {
			String message = logger.log(OdipMessage.MSG_NOT_AUTHORIZED_FOR_PRODUCTIONORDER, productionOrderIdentifier);
			throw new SecurityException(message);
		}

		// Check access permission to production order
		if (securityConfig.hasRole(UserRole.ORDER_READER) || securityConfig.hasRole(UserRole.ORDER_MGR)) {
		} else {
			String message = logger.log(OdipMessage.MSG_NOT_AUTHORIZED_FOR_PRODUCTIONORDER,
					securityConfig.getMission() + "\\" + securityConfig.getUser(), productionOrderIdentifier);
			throw new SecurityException(message);
		}

		if (logger.isDebugEnabled())
			logger.debug("... production order found: " + modelOrder.getId());
		return modelOrder;
	}

	/**
	 * Get the metadata for a single workflow from the prosEO service
	 *
	 * @param productUuid the UUID of the workflow to retrieve
	 *
	 * @return a workflow object
	 * @throws NoResultException if a workflow with the requested UUID could not be found in the database
	 * @throws SecurityException if the logged in user is not authorized to access the requested workflow
	 */
	private Workflow getWorkflow(String workflowUuid) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getWorkflow({})", workflowUuid);

		// Request workflow metadata from database
		Query query = em.createQuery("select p from Workflow p where p.uuid = :uuid and (enabled is null or enabled = TRUE)",
				Workflow.class);
		query.setParameter("uuid", UUID.fromString(workflowUuid));
		Object resultObject;
		try {
			resultObject = query.getSingleResult();
			if (null == resultObject || !(resultObject instanceof Workflow)) {
				throw new NoResultException();
			}
		} catch (NoResultException e) {
			String message = logger.log(OdipMessage.MSG_WORKFLOW_NOT_FOUND, workflowUuid);
			throw new NoResultException(message);
		}
		Workflow modelWorkflow = (Workflow) resultObject;

		// Check access permission to production order
		if (securityConfig.hasRole(UserRole.ORDER_READER) || securityConfig.hasRole(UserRole.ORDER_MGR)) {
		} else {
			String message = logger.log(OdipMessage.MSG_NOT_AUTHORIZED_FOR_WORKFLOW,
					securityConfig.getMission() + "\\" + securityConfig.getUser(), workflowUuid);
			throw new SecurityException(message);
		}

		if (logger.isDebugEnabled())
			logger.debug("... workflow found: " + modelWorkflow.getId());
		return modelWorkflow;
	}

	/**
	 * Download the requested production order from the prosEO service
	 *
	 * @param productionOrderUuid the UUID of the production order to retrieve
	 *
	 * @return a binary stream containing the production order data
	 * @throws URISyntaxException       if a valid URI cannot be generated from any production order UUID
	 * @throws IllegalArgumentException if mandatory information is missing from the prosEO interface production order
	 * @throws NoResultException        if a production order with the requested UUID could not be found in the database
	 * @throws SecurityException        if the logged in user is not authorized to access the requested production order
	 */
	private Entity getProductionOrderAsEntity(String productionOrderUuid)
			throws URISyntaxException, IllegalArgumentException, NoSuchElementException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProductionOrderAsEntity({})", productionOrderUuid);

		// Get the production order information from the Database
		ProcessingOrder modelOrder = getProductionOrder(productionOrderUuid);
		// Create output production order
		Entity productionOrder = OdipApplicationBase.util.toOdipProductionOrder(modelOrder);

		if (logger.isTraceEnabled())
			logger.trace("<<< getProductionOrderAsEntity()");
		return productionOrder;
	}

	/**
	 * Download the requested workflow from the prosEO Storage Manager
	 *
	 * @param workflowUuid the UUID of the workflow to retrieve
	 *
	 * @return a binary stream containing the workflow data
	 * @throws URISyntaxException       if a valid URI cannot be generated from any workflow UUID
	 * @throws IllegalArgumentException if mandatory information is missing from the prosEO interface workflow
	 * @throws NoResultException        if a workflow with the requested UUID could not be found in the database
	 * @throws SecurityException        if the logged in user is not authorized to access the requested workflow
	 */
	private Entity getWorkflowAsEntity(String workflowUuid)
			throws URISyntaxException, IllegalArgumentException, NoSuchElementException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getWorkflowAsEntity({})", workflowUuid);

		// Get the workflow information from the Database
		Workflow modelWorkflow = getWorkflow(workflowUuid);
		// Create output workflow
		Entity workflow = OdipApplicationBase.util.toOdipWorkflow(modelWorkflow);

		if (logger.isTraceEnabled())
			logger.trace("<<< getWorkflowAsEntity()");
		return workflow;
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
			String message = logger.log(OdipMessage.MSG_NOT_AUTHORIZED_FOR_SERVICE, securityConfig.getUser());
			throw new SecurityException(message);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.log(OdipMessage.MSG_SERVICE_REQUEST_FAILED, e.getStatusCode().value(), e.getStatusCode().toString(),
					e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst(HTTP_HEADER_WARNING));
		} catch (RestClientException e) {
			String message = logger.log(OdipMessage.MSG_HTTP_REQUEST_FAILED, e.getMessage());
			throw new RestClientException(message, e);
		} catch (Exception e) {
			logger.log(OdipMessage.MSG_EXCEPTION, e.getMessage(), e);
			throw new RuntimeException(e);
		}

		// All GET requests should return HTTP status OK
		if (!HttpStatus.OK.equals(entity.getStatusCode())) {
			String message = logger.log(OdipMessage.MSG_SERVICE_REQUEST_FAILED, entity.getStatusCodeValue(),
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
		try {
			if (edmEntitySet.getEntityType().getFullQualifiedName().equals(OdipEdmProvider.ET_PRODUCTIONORDER_FQN)
					|| edmEntitySet.getEntityType().getFullQualifiedName().equals(OdipEdmProvider.ET_ORDER_FQN)) {
				// Query the backend services for the requested objects, passing on user, password and mission
				entity = getProductionOrderAsEntity(keyPredicates.get(0).getText());
			} else if (edmEntitySet.getEntityType().getFullQualifiedName().equals(OdipEdmProvider.ET_WORKFLOW_FQN)) {
				entity = getWorkflowAsEntity(keyPredicates.get(0).getText());
			} else {
				String message = logger.log(OdipMessage.MSG_INVALID_ENTITY_TYPE,
						edmEntitySet.getEntityType().getFullQualifiedName());
				response.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), message))
					.getContent());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader(HTTP_HEADER_WARNING, message);
				return;
			}
		} catch (SecurityException e) {
			response
				.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.UNAUTHORIZED.getStatusCode(), e.getMessage()))
					.getContent());
			response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (URISyntaxException e) {
			String message = logger.log(OdipMessage.MSG_URI_GENERATION_FAILED, e.getMessage());
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), message)).getContent());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		} catch (IllegalArgumentException e) {
			response
				.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getMessage()))
					.getContent());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (NoResultException e) {
			response.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.NOT_FOUND.getStatusCode(), e.getMessage()))
				.getContent());
			response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (Exception e) {
			String message = logger.log(OdipMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
			e.printStackTrace();
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), message))
						.getContent());
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
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
			String message = logger.log(OdipMessage.MSG_UNSUPPORTED_FORMAT, responseFormat.toContentTypeString());
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
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

		if (logger.isTraceEnabled())
			logger.trace("<<< readEntity()");
	}

	/* ------------------------------------------------------------------------------------
	 * The methods below are not available on the ODIP, since this is a read-only interface 
	 * ------------------------------------------------------------------------------------ */

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
	public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);

		// 1. Retrieve the entity type from the URI
		EdmEntitySet edmEntitySet = OdipApplicationBase.util.getEdmEntitySet(uriInfo);
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		// 2. create the data in backend
		// 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = null;
		try {
			result = deserializer.entity(requestInputStream, edmEntityType);
		} catch (Exception e) {
			response.setStatusCode(HttpStatusCode.NOT_ACCEPTABLE.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, logger.log(OdipMessage.MSG_JSON_PARSE_ERROR,
					e.getMessage() + (e.getCause() == null ? "" : (": " + e.getCause().getMessage()))));
			return;
		}
		Entity requestEntity = result.getEntity();
		RestOrder modelOrder = null;
		try {
			modelOrder = OdipApplicationBase.util.toModelOrder(requestEntity);
		} catch (OdipException e) {
			response.setStatusCode((e.getHttpStatus() != null ? e.getHttpStatus() : HttpStatusCode.NOT_FOUND).getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage());
			return;
		} catch (Exception e) {
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, logger.log(OdipMessage.MSG_EXCEPTION,
					e.getMessage() + (e.getCause() == null ? "" : (": " + e.getCause().getMessage()))));
			return;
		}
		// the rest order is created, now create the processing order
		try {
			modelOrder = OdipApplicationBase.util.sendAndReleaseOrder(modelOrder);
		} catch (OdipException e) {
			response.setStatusCode((e.getHttpStatus() != null ? e.getHttpStatus() : HttpStatusCode.NOT_FOUND).getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage());
			return;
		} catch (Exception e) {
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, logger.log(OdipMessage.MSG_EXCEPTION,
					e.getMessage() + (e.getCause() == null ? "" : (": " + e.getCause().getMessage()))));
			return;
		}

		// 2.2 do the creation in backend, which returns the newly created entity
		Entity createdEntity = null;
		ODataSerializer serializer = this.odata.createSerializer(responseFormat);
		try {
			createdEntity = OdipApplicationBase.util.toOdipProductionOrder(modelOrder);
		} catch (SecurityException e) {
			response
				.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.UNAUTHORIZED.getStatusCode(), e.getMessage()))
					.getContent());
			response.setStatusCode(HttpStatusCode.UNAUTHORIZED.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (URISyntaxException e) {
			String message = logger.log(OdipMessage.MSG_URI_GENERATION_FAILED, e.getMessage());
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), message)).getContent());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		} catch (IllegalArgumentException e) {
			response
				.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getMessage()))
					.getContent());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (NoResultException e) {
			response.setContent(serializer.error(LogUtil.oDataServerError(HttpStatusCode.NOT_FOUND.getStatusCode(), e.getMessage()))
				.getContent());
			response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (Exception e) {
			String message = logger.log(OdipMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
			e.printStackTrace();
			response.setContent(
					serializer.error(LogUtil.oDataServerError(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), message))
						.getContent());
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, message);
			return;
		}

		// 3. serialize the response (we have to return the created entity)
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
		EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build(); // expand and select
																											// currently not
																											// supported

		SerializerResult serializedResponse = serializer.entity(serviceMetadata, edmEntityType, createdEntity, options);

		// 4. configure the response object
		response.setContent(serializedResponse.getContent());
		response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
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
	public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteEntity({}, {}, {})", request, response, uriInfo);

		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logger.log(OdipMessage.MSG_FORBIDDEN));
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
	public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateEntity({}, {}, {}, {}, {})", request, response, uriInfo, requestFormat, responseFormat);

		response.setStatusCode(HttpStatusCode.FORBIDDEN.getStatusCode());
		response.setHeader(HTTP_HEADER_WARNING, logger.log(OdipMessage.MSG_FORBIDDEN));
	}

}