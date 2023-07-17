/**
 * OdipUtilBase.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.odata;

import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

import de.dlr.proseo.api.odip.OdipConfiguration;
import de.dlr.proseo.api.odip.OdipSecurity;
import de.dlr.proseo.api.odip.service.ServiceConnection;
import de.dlr.proseo.api.odip.util.ProductUtil;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.OdipMessage;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;
import de.dlr.proseo.model.WorkflowOption.WorkflowOptionType;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.OrderSource;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.rest.model.RestClassOutputParameter;
import de.dlr.proseo.model.rest.model.RestInputReference;
import de.dlr.proseo.model.rest.model.RestNotificationEndpoint;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.rest.model.RestProduct;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Utility class for the prosEO ODIP
 *
 * @author Dr. Thomas Bassler
 */
public class OdipUtilBase {

//	private final Date START_OF_MISSION = Date.from(Instant.parse("1970-01-01T00:00:00.000Z"));
//	private final Date END_OF_MISSION = Date.from(Instant.parse("9999-12-31T23:59:59.999Z"));

	// TODO where to get the settings?
	private final String ORDER_PROCESSING_MODE = "OPER";
	private final String ORDER_SLICING_TYPE = "NONE";
	private final String ORDER_OUTPUT_FILE_CLASS = "OPER";

//	private final String URI_PATH_ORBITS = "/orbits";
//	private final String ORBITS = "orbits";

	private final String URI_PATH_ORDERS = "/orders";
	private final String ORDERS = "orders";

	private final String URI_PATH_ORDERS_APPROVE = "/orders/approve";
	private final String URI_PATH_ORDERS_PLAN = "/orders/plan";
	private final String URI_PATH_ORDERS_RESUME = "/orders/resume";
	private final String URI_PATH_DOWNLOAD_BYNAME = "/download/byname";
	private final String URI_PATH_DOWNLOAD_ALLBYTIME = "/download/allbytime";

	/** MonitorServices configuration */
	@Autowired
	protected OdipConfiguration config;

	/** The connector service to the prosEO backend services */
	@Autowired
	protected ServiceConnection serviceConnection;

	/** The security utilities for the ODIP API */
	@Autowired
	protected OdipSecurity securityConfig;

	/** JPA entity manager */
	@PersistenceContext
	protected EntityManager em;

	/** A logger for this class */
	private ProseoLogger logger = new ProseoLogger(OdipUtilBase.class);

	/** A date time formatter */
	protected final DateTimeFormatter instantFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSSSSS")
		.withZone(ZoneId.of("UTC"));

	/** Exception for unrecoverable errors during generation of processing orders */
	public class OdipException extends Exception {

		private static final long serialVersionUID = 4396477286380050214L;

		private HttpStatusCode httpStatus = null;

		/**
		 * Get the HTTP status associated with the exception.
		 *
		 * @return the HTTP status
		 */
		public HttpStatusCode getHttpStatus() {
			return httpStatus;
		}

		/**
		 * Constructs a new OdipException with the specified error message.
		 *
		 * @param message the error message
		 */
		public OdipException(String message) {
			super(message);
		}

		/**
		 * Constructs a new OdipException with the specified error message and HTTP status.
		 *
		 * @param message the error message
		 * @param status  the HTTP status
		 */
		public OdipException(String message, HttpStatusCode status) {
			super(message);
			httpStatus = status;
		}

		/**
		 * Constructs a new OdipException with the specified error message, cause, and HTTP status.
		 *
		 * @param message the error message
		 * @param e       the cause of the exception
		 * @param status  the HTTP status
		 */
		public OdipException(String message, Exception e, HttpStatusCode status) {
			super(message, e);
			httpStatus = status;
		}

	}

	/**
	 * Retrieves the EdmEntitySet based on the provided UriInfoResource.
	 *
	 * @param uriInfo the UriInfoResource containing the URI segments
	 * @return the EdmEntitySet
	 * @throws ODataApplicationException if the resource type for the first segment is invalid or not supported
	 */
	public EdmEntitySet getEdmEntitySet(UriInfoResource uriInfo) throws ODataApplicationException {

		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// To get the entity set we have to interpret all URI segments
		if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
			// Here we should interpret the whole URI but in this example we do not support navigation so we throw an
			// exception
			throw new ODataApplicationException("Invalid resource type for first segment.",
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}

		UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);

		return uriResource.getEntitySet();
	}

	/**
	 * Create a ODIP interface production order from a prosEO interface processing order. Replace password by "*****"
	 *
	 * @param modelOrder the prosEO model order to convert
	 * @return an OData entity object representing the prosEO interface production order
	 * @throws IllegalArgumentException if any mandatory information is missing from the prosEO interface production order
	 * @throws URISyntaxException       if a valid URI cannot be generated from any production order UUID
	 */
	public Entity toOdipProductionOrder(ProcessingOrder modelOrder) throws IllegalArgumentException, URISyntaxException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toOdipProductionOrder({})", modelOrder.getId());

		// Create production order entity
		Entity order = new Entity();
		order.setType(OdipEdmProvider.ET_PRODUCTIONORDER_FQN.getFullQualifiedNameAsString());
		order.addProperty(new Property(null, OdipEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, modelOrder.getUuid()))
			.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_STATUS, ValueType.ENUM,
					(this.getProductionOrderStateFrom(modelOrder))))
			.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_STATUSMESSAGE, ValueType.PRIMITIVE,
					modelOrder.getStateMessage()));
		// TODO
		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_ORDEROUTPUTSIZE, ValueType.PRIMITIVE, 0));
		if (modelOrder.getEstimatedCompletionTime() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_ESTIMATEDDATE, ValueType.PRIMITIVE,
					Date.from(modelOrder.getEstimatedCompletionTime())));
		}
		if (modelOrder.getSubmissionTime() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_SUBMISSIONDATE, ValueType.PRIMITIVE,
					Date.from(modelOrder.getSubmissionTime())));
		}
		if (modelOrder.getActualCompletionTime() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_COMPLETEDDATE, ValueType.PRIMITIVE,
					Date.from(modelOrder.getActualCompletionTime())));
		}
		if (modelOrder.getEvictionTime() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_EVICTIONDATE, ValueType.PRIMITIVE,
					Date.from(modelOrder.getEvictionTime())));
		}
		if (modelOrder.getPriority() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_PRIORITY, ValueType.PRIMITIVE,
					modelOrder.getPriority()));
		}

		ComplexValue inputProductReference = new ComplexValue();
		inputProductReference.getValue()
			.add(new Property(null, OdipEdmProvider.CT_INPUTPRODUCTREFERENCE_PROP_REFERENCE, ValueType.PRIMITIVE,
					(modelOrder.getInputProductReference() != null
							&& modelOrder.getInputProductReference().getInputFileName() != null)
									? modelOrder.getInputProductReference().getInputFileName()
									: "null"));

		if (modelOrder.getInputProductReference() != null && (modelOrder.getInputProductReference().getSensingStartTime() != null
				&& modelOrder.getInputProductReference().getSensingStopTime() != null)) {
			ComplexValue contentDate = new ComplexValue();
			if (modelOrder.getInputProductReference().getSensingStartTime() != null) {
				contentDate.getValue()
					.add(new Property(null, OdipEdmProvider.CT_CONTENTDATE_PROP_START, ValueType.PRIMITIVE,
							Date.from(modelOrder.getInputProductReference().getSensingStartTime())));
			}
			if (modelOrder.getInputProductReference().getSensingStopTime() != null) {
				contentDate.getValue()
					.add(new Property(null, OdipEdmProvider.CT_CONTENTDATE_PROP_END, ValueType.PRIMITIVE,
							Date.from(modelOrder.getInputProductReference().getSensingStopTime())));
			}
			inputProductReference.getValue()
				.add(new Property(null, OdipEdmProvider.CT_CONTENTDATE_NAME, ValueType.COMPLEX, contentDate));
		}

		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_INPUTPRODUCTREFERENCE, ValueType.COMPLEX,
				inputProductReference));

		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWID, ValueType.PRIMITIVE,
				(modelOrder.getWorkflow() != null && modelOrder.getWorkflow().getUuid() != null)
						? modelOrder.getWorkflow().getUuid()
						: "00000000-0000-0000-0000-000000000000"));

		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWNAME, ValueType.PRIMITIVE,
				(modelOrder.getWorkflow() != null && modelOrder.getWorkflow().getName() != null)
						? modelOrder.getWorkflow().getName()
						: "null"));

		// workflow options
		// use dynamic processing parameters
		List<ComplexValue> workflowOptions = new ArrayList<>();
		for (String paramName : modelOrder.getDynamicProcessingParameters().keySet()) {
			ComplexValue workflowOption = new ComplexValue();
			Parameter param = modelOrder.getDynamicProcessingParameters().get(paramName);
			workflowOption.getValue()
				.add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_NAME, ValueType.PRIMITIVE, paramName));
			workflowOption.getValue()
				.add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_VALUE, ValueType.PRIMITIVE,
						param.getParameterValue()));
			workflowOptions.add(workflowOption);
		}
		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWOPTIONS, ValueType.COLLECTION_COMPLEX,
				workflowOptions));
		if (modelOrder.getNotificationEndpoint() != null) {
			if (modelOrder.getNotificationEndpoint().getUri() != null) {
				order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONENDPOINT,
						ValueType.PRIMITIVE, modelOrder.getNotificationEndpoint().getUri()));
			}
			if (modelOrder.getNotificationEndpoint().getUsername() != null) {
				order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPUSERNAME,
						ValueType.PRIMITIVE, modelOrder.getNotificationEndpoint().getUsername()));
			}
			if (modelOrder.getNotificationEndpoint().getPassword() != null) {
				order.addProperty(
						new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPPASSWORD, ValueType.PRIMITIVE,
								// modelOrder.getNotificationEndpoint().getPassword()
								"*****"));
			}
		}
		return order;
	}

	/**
	 * Create a ODIP interface production order from a prosEO interface processing order.
	 *
	 * @param restOrder the prosEO model order to convert
	 * @return an OData entity object representing the prosEO interface production order
	 * @throws IllegalArgumentException if any mandatory information is missing from the prosEO interface production order
	 * @throws URISyntaxException       if a valid URI cannot be generated from any production order UUID
	 */
	public Entity toOdipProductionOrder(RestOrder restOrder) throws IllegalArgumentException, URISyntaxException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toOdipProductionOrder({})", restOrder.getId());

		// Create production order entity
		Entity order = new Entity();
		order.setType(OdipEdmProvider.ET_PRODUCTIONORDER_FQN.getFullQualifiedNameAsString());
		order
			.addProperty(
					new Property(null, OdipEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, UUID.fromString(restOrder.getUuid())))
			.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_STATUS, ValueType.ENUM,
					(this.getProductionOrderStateFrom(restOrder))))
			.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_STATUSMESSAGE, ValueType.PRIMITIVE,
					restOrder.getStateMessage()));
		// TODO
		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_ORDEROUTPUTSIZE, ValueType.PRIMITIVE, 0));
		if (restOrder.getEstimatedCompletionTime() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_ESTIMATEDDATE, ValueType.PRIMITIVE,
					restOrder.getEstimatedCompletionTime()));
		}
		if (restOrder.getSubmissionTime() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_SUBMISSIONDATE, ValueType.PRIMITIVE,
					restOrder.getSubmissionTime()));
		}
		if (restOrder.getActualCompletionTime() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_COMPLETEDDATE, ValueType.PRIMITIVE,
					restOrder.getActualCompletionTime()));
		}
		if (restOrder.getEvictionTime() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_EVICTIONDATE, ValueType.PRIMITIVE,
					restOrder.getEvictionTime()));
		}
		if (restOrder.getPriority() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_PRIORITY, ValueType.PRIMITIVE,
					restOrder.getPriority()));
		}

		ComplexValue inputProductReference = new ComplexValue();
		inputProductReference.getValue()
			.add(new Property(null, OdipEdmProvider.CT_INPUTPRODUCTREFERENCE_PROP_REFERENCE, ValueType.PRIMITIVE,
					(restOrder.getInputProductReference() != null
							&& restOrder.getInputProductReference().getInputFileName() != null)
									? restOrder.getInputProductReference().getInputFileName()
									: "null"));

		if (restOrder.getInputProductReference() != null && (restOrder.getInputProductReference().getSensingStartTime() != null
				&& restOrder.getInputProductReference().getSensingStopTime() != null)) {
			ComplexValue contentDate = new ComplexValue();
			if (restOrder.getInputProductReference().getSensingStartTime() != null) {
				contentDate.getValue()
					.add(new Property(null, OdipEdmProvider.CT_CONTENTDATE_PROP_START, ValueType.PRIMITIVE, Date
						.from(Instant.from(OrbitTimeFormatter.parse(restOrder.getInputProductReference().getSensingStartTime())))));
			}
			if (restOrder.getInputProductReference().getSensingStopTime() != null) {
				contentDate.getValue()
					.add(new Property(null, OdipEdmProvider.CT_CONTENTDATE_PROP_END, ValueType.PRIMITIVE, Date
						.from(Instant.from(OrbitTimeFormatter.parse(restOrder.getInputProductReference().getSensingStopTime())))));
			}
			inputProductReference.getValue()
				.add(new Property(null, OdipEdmProvider.CT_CONTENTDATE_NAME, ValueType.COMPLEX, contentDate));
		}

		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_INPUTPRODUCTREFERENCE, ValueType.COMPLEX,
				inputProductReference));

		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWID, ValueType.PRIMITIVE,
				(restOrder.getWorkflowUuid() != null) ? restOrder.getWorkflowUuid() : "00000000-0000-0000-0000-000000000000"));

		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWNAME, ValueType.PRIMITIVE,
				(restOrder.getWorkflowName() != null) ? restOrder.getWorkflowName() : "null"));

		// workflow options
		// use dynamic processing parameters
		List<ComplexValue> workflowOptions = new ArrayList<>();
		for (RestParameter restParam : restOrder.getDynamicProcessingParameters()) {
			ComplexValue workflowOption = new ComplexValue();
			workflowOption.getValue()
				.add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_NAME, ValueType.PRIMITIVE, restParam.getKey()));
			workflowOption.getValue()
				.add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_VALUE, ValueType.PRIMITIVE,
						restParam.getParameterValue()));
			workflowOptions.add(workflowOption);
		}
		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWOPTIONS, ValueType.COLLECTION_COMPLEX,
				workflowOptions));
		if (restOrder.getNotificationEndpoint() != null) {
			if (restOrder.getNotificationEndpoint().getUri() != null) {
				order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONENDPOINT,
						ValueType.PRIMITIVE, restOrder.getNotificationEndpoint().getUri()));
			}
			if (restOrder.getNotificationEndpoint().getUsername() != null) {
				order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPUSERNAME,
						ValueType.PRIMITIVE, restOrder.getNotificationEndpoint().getUsername()));
			}
			if (restOrder.getNotificationEndpoint().getPassword() != null) {
				order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPPASSWORD,
						ValueType.PRIMITIVE, restOrder.getNotificationEndpoint().getPassword()));
			}
		}
		return order;
	}

	/**
	 * Converts a model workflow to an ODIP workflow entity.
	 *
	 * @param modelWorkflow the model workflow to convert
	 * @return the converted ODIP workflow entity
	 * @throws IllegalArgumentException if the provided model workflow is invalid
	 * @throws URISyntaxException       if there is an error in the URI syntax
	 */
	public Entity toOdipWorkflow(Workflow modelWorkflow) throws IllegalArgumentException, URISyntaxException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toOdipWorkflow({})", modelWorkflow.getId());

		// Create w entity
		Entity workflow = new Entity();
		workflow.setType(OdipEdmProvider.ET_WORKFLOW_FQN.getFullQualifiedNameAsString());
		workflow.addProperty(new Property(null, OdipEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, modelWorkflow.getUuid()))
			.addProperty(new Property(null, OdipEdmProvider.ET_WORKFLOW_PROP_NAME, ValueType.PRIMITIVE, modelWorkflow.getName()));
		if (modelWorkflow.getDescription() != null) {
			workflow.addProperty(new Property(null, OdipEdmProvider.ET_WORKFLOW_PROP_DESCRIPTION, ValueType.PRIMITIVE,
					modelWorkflow.getDescription()));
		}
		if (modelWorkflow.getInputProductClass() != null) {
			workflow.addProperty(new Property(null, OdipEdmProvider.ET_WORKFLOW_PROP_INPUTPRODUCTTYPE, ValueType.PRIMITIVE,
					modelWorkflow.getInputProductClass().getProductType()));
		}
		if (modelWorkflow.getOutputProductClass() != null) {
			workflow.addProperty(new Property(null, OdipEdmProvider.ET_WORKFLOW_PROP_OUTPUTPRODUCTTYPE, ValueType.PRIMITIVE,
					modelWorkflow.getOutputProductClass().getProductType()));
		}
		if (modelWorkflow.getWorkflowVersion() != null) {
			workflow.addProperty(new Property(null, OdipEdmProvider.ET_WORKFLOW_PROP_WORKFLOWVERSION, ValueType.PRIMITIVE,
					modelWorkflow.getWorkflowVersion()));
		}

		// workflow options
		// use dynamic processing parameters

		List<ComplexValue> workflowOptions = new ArrayList<>();
		for (WorkflowOption opt : modelWorkflow.getWorkflowOptions()) {
			ComplexValue workflowOption = new ComplexValue();
			if (opt.getName() != null) {
				workflowOption.getValue()
					.add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_NAME, ValueType.PRIMITIVE, opt.getName()));
			}
			if (opt.getDescription() != null) {
				workflowOption.getValue()
					.add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_DESCRIPTION, ValueType.PRIMITIVE,
							opt.getDescription()));
			}
			if (opt.getType() != null) {
				workflowOption.getValue()
					.add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_TYPE, ValueType.PRIMITIVE, opt.getType()));
			}
			if (opt.getDefaultValue() != null) {
				workflowOption.getValue()
					.add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_DEFAULT, ValueType.PRIMITIVE,
							opt.getDefaultValue()));
			}

			List<Object> values = new ArrayList<>();
			for (String elem : opt.getValueRange()) {
				values.add(elem);
			}
			workflowOption.getValue()
				.add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_VALUE, ValueType.COLLECTION_PRIMITIVE, values));
			workflowOptions.add(workflowOption);
		}
		workflow.addProperty(new Property(null, OdipEdmProvider.ET_WORKFLOW_PROP_WORKFLOWOPTIONS, ValueType.COLLECTION_COMPLEX,
				workflowOptions));
		return workflow;
	}

	/**
	 * Retrieves the production order state from the given processing order.
	 *
	 * @param order the processing order
	 * @return the production order state
	 */
	public int getProductionOrderStateFrom(ProcessingOrder order) {
		// States are
		// INITIAL, APPROVED, PLANNING, PLANNING_FAILED, PLANNED, RELEASING, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED,
		// CLOSED

		int productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_QUEUED_VAL; // Default
		switch (order.getOrderState()) {
		case INITIAL:
		case APPROVED:
		case PLANNING:
		case PLANNED:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_QUEUED_VAL;
			break;
		case RELEASING:
		case RELEASED:
		case RUNNING:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_IN_PROGRESS_VAL;
			break;
		case COMPLETED:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_COMPLETED_VAL;
			break;
		case FAILED:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_FAILED_VAL;
			break;
		case CLOSED:
			if (order.hasFailedJobSteps()) {
				productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_FAILED_VAL;
			} else {
				productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_COMPLETED_VAL;
			}
			break;
		default:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_CANCELLED_VAL;
			break;
		}
		return productionType;
	}

	/**
	 * Retrieves the production order state from the given REST order.
	 *
	 * @param order the REST order
	 * @return the production order state
	 */
	public int getProductionOrderStateFrom(RestOrder order) {
		// States are
		// INITIAL, APPROVED, PLANNING, PLANNING_FAILED, PLANNED, RELEASING, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED,
		// CLOSED

		int productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_QUEUED_VAL; // Default
		switch (OrderState.valueOf(order.getOrderState())) {
		case INITIAL:
		case APPROVED:
		case PLANNING:
		case PLANNED:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_QUEUED_VAL;
			break;
		case RELEASING:
		case RELEASED:
		case RUNNING:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_IN_PROGRESS_VAL;
			break;
		case COMPLETED:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_COMPLETED_VAL;
			break;
		case FAILED:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_FAILED_VAL;
			break;
		case CLOSED:
			if (order.getHasFailedJobSteps()) {
				productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_FAILED_VAL;
			} else {
				productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_COMPLETED_VAL;
			}
			break;
		default:
			productionType = OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_CANCELLED_VAL;
			break;
		}
		return productionType;
	}

	/**
	 * Converts an ODIP order entity to a model order.
	 *
	 * @param order the ODIP order entity
	 * @return the converted model order
	 * @throws ODataApplicationException if there is an error in the OData application
	 * @throws OdipException             if there is an error in the ODIP application
	 */
	@Transactional
	public RestOrder toModelOrder(Entity order) throws ODataApplicationException, OdipException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelOrder()");
		if (null == order)
			return null;

		RestOrder restOrder = new RestOrder();
		// get the workflow
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWID) != null) {
			restOrder.setWorkflowUuid((String) order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWID).getValue());
		}
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWNAME) != null) {
			restOrder.setWorkflowName((String) order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWNAME).getValue());
		}
		if (restOrder.getWorkflowUuid() == null && restOrder.getWorkflowName() == null) {
			// no workflow reference, return error
			String message = logger.log(OdipMessage.MSG_WORKFLOW_REFERENCE_MISSING);
			throw new OdipException(message);
		}
		Workflow workflow = null;
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_PRIORITY) != null) {
			restOrder
				.setPriority(((Long) (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_PRIORITY).getValue())).intValue());
		}
		if (restOrder.getWorkflowUuid() != null && restOrder.getWorkflowName() != null) {
			workflow = RepositoryService.getWorkflowRepository().findByUuid(UUID.fromString(restOrder.getWorkflowUuid()));
			if (workflow != null) {
				if (!workflow.getName().equals(restOrder.getWorkflowName())) {
					workflow = null;
				}
			}
		} else if (restOrder.getWorkflowUuid() != null) {
			workflow = RepositoryService.getWorkflowRepository().findByUuid(UUID.fromString(restOrder.getWorkflowUuid()));
		} else {
			workflow = RepositoryService.getWorkflowRepository().findByName(restOrder.getWorkflowName());
		}
		if (workflow == null) {
			// no workflow reference, return error
			String message = logger.log(OdipMessage.MSG_WORKFLOW_REF_NOT_FOUND, restOrder.getWorkflowUuid(),
					restOrder.getWorkflowName());
			throw new OdipException(message);
		}
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_INPUTPRODUCTREFERENCE) != null) {
			if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_INPUTPRODUCTREFERENCE)
				.getValueType() == ValueType.COMPLEX) {
				// handle input production order reference
				// find input file
				ComplexValue ipr = (ComplexValue) order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_INPUTPRODUCTREFERENCE)
					.getValue();
				String reference = null;
				Instant start = null;
				Instant end = null;
				List<Property> iprProps = ipr.getValue();
				for (Property prop : iprProps) {
					if (prop.getName().equals(OdipEdmProvider.CT_CONTENTDATE_NAME)) {
						// TODO
						if (prop.getValue() instanceof ComplexValue) {
							ComplexValue cd = (ComplexValue) prop.getValue();
							for (Property cdprop : cd.getValue()) {
								if (cdprop.getName().equals(OdipEdmProvider.CT_CONTENTDATE_PROP_START)) {
									if (cdprop.getType()
										.equals(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName().toString())) {
										Timestamp ts = (Timestamp) (cdprop.getValue());
										start = ts.toInstant();
									}
								} else if (cdprop.getName().equals(OdipEdmProvider.CT_CONTENTDATE_PROP_END)) {
									if (cdprop.getType()
										.equals(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName().toString())) {
										Timestamp ts = (Timestamp) (cdprop.getValue());
										end = ts.toInstant();
									}
								}
							}

						}
					} else if (prop.getName().equals(OdipEdmProvider.CT_INPUTPRODUCTREFERENCE_PROP_REFERENCE)) {
						reference = (String) prop.getValue();
					}
				}
				if (reference != null || (start != null && end != null)) {
					RestInputReference inputProductReference = new RestInputReference();
					if (reference != null) {
						inputProductReference.setInputFileName(reference);
					}
					if (start != null && end != null) {
						inputProductReference.setSensingStartTime(OrbitTimeFormatter.format(start));
						inputProductReference.setSensingStopTime(OrbitTimeFormatter.format(end));
					}
					restOrder.setInputProductReference(inputProductReference);

					// TODO ensure start and stop are set from reference or start/stop of input product reference
					// search referenced product
					Product product = null;
					if (reference != null) {
						try {
							product = findProductByReference(reference);
							if (product == null) {
								String message = logger.log(OdipMessage.MSG_INPUTREF_NOT_FOUND, reference);
								throw new OdipException(message);
							}
						} catch (Exception e) {
							String message = logger.log(OdipMessage.MSG_EXCEPTION, e.getClass().getCanonicalName(), e.getMessage());
							throw new OdipException(message);
						}
					}

					if ((start == null || end == null) && product != null) {
						start = product.getSensingStartTime();
						end = product.getSensingStopTime();
					}
					if ((start == null || end == null) && reference == null && product == null) {
						String message = logger.log(OdipMessage.MSG_INPUTREF_INVALID);
						throw new OdipException(message);
					}
					// TODO use planner only for testing
//					if (start == null || end == null) {
//						String message = logger.log(OdipMessage.MSG_STARTSTOP_MISSING);
//						throw new OdipException(message);
//					}
//					if (start != null && end != null && product == null) {
//						// search for possible products
//						if (workflow.getInputProductClass() != null) {
//							List<Product> products = findProductsByClassAndStartStop(workflow.getInputProductClass().getProductType(), start, end);
//							if (products.isEmpty()) {
//								String message = logger.log(OdipMessage.MSG_NO_INPUTPRODUCT, workflow.getInputProductClass().getProductType());
//								throw new OdipException(message);
//							}
//						}
//					}
					restOrder.setSlicingType(OrderSlicingType.NONE.toString());
					restOrder.setStartTime(OrbitTimeFormatter.format(start));
					restOrder.setStopTime(OrbitTimeFormatter.format(end));
				}
			}
		}
		if (workflow.getProcessingMode() != null && !workflow.getProcessingMode().isEmpty()) {
			restOrder.setProcessingMode(workflow.getProcessingMode());
		} else {
			restOrder.setProcessingMode(ORDER_PROCESSING_MODE);
		}
		if (workflow.getSlicingType() != null) {
			restOrder.setSlicingType(workflow.getSlicingType().toString());
		} else {
			restOrder.setSlicingType(ORDER_SLICING_TYPE);
		}
		if (workflow.getSliceDuration() != null) {
			restOrder.setSliceDuration(workflow.getSliceDuration().getSeconds());
		}
		if (workflow.getSliceOverlap() != null) {
			restOrder.setSliceOverlap(workflow.getSliceOverlap().getSeconds());
		}

		restOrder.setOrderSource(OrderSource.ODIP.toString());

		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONENDPOINT) != null) {
			RestNotificationEndpoint rnep = new RestNotificationEndpoint();
			rnep.setUri((String) order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONENDPOINT).getValue());
			if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPUSERNAME) != null) {
				rnep.setUsername(
						(String) order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPUSERNAME).getValue());
			}
			if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPPASSWORD) != null) {
				rnep.setPassword(
						(String) order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPPASSWORD).getValue());
			}
			restOrder.setNotificationEndpoint(rnep);
		}
		Map<String, WorkflowOption> workflowOptions = new HashMap<>();

		for (WorkflowOption wo : workflow.getWorkflowOptions()) {
			workflowOptions.put(wo.getName(), wo);
		}
		List<String> definedOptionName = new ArrayList<>();
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWOPTIONS) != null) {
			if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWOPTIONS)
				.getValueType() == ValueType.COLLECTION_COMPLEX) {
				for (Object obj : (List<?>) (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWOPTIONS).getValue())) {
					String name = null;
					String value = null;
					if (obj instanceof ComplexValue) {
						ComplexValue cv = (ComplexValue) obj;
						for (Object paramObj : (List<?>) (cv.getValue())) {
							Property param = (Property) paramObj;
							if (param.getName().equals(OdipEdmProvider.ET_WORKFLOWOPTION_PROP_NAME)) {
								name = (String) (param.getValue());
							} else if (param.getName().equals(OdipEdmProvider.ET_WORKFLOWOPTION_PROP_VALUE)) {
								value = (String) (param.getValue());
							}
						}
					}
					if (name != null && value != null) {
						RestParameter param = new RestParameter();
						// is option defined in workflow
						WorkflowOption wo = workflowOptions.get(name);
						if (wo == null) {
							// option not defined -> error
							String message = logger.log(OdipMessage.MSG_WORKFLOW_OPTION_NOT_DEF, name, workflow.getName());
							throw new OdipException(message);
						}
						switch (wo.getType()) {
						case NUMBER: {
							// check for number type
							try {
								Integer.parseInt(value);
								param.setParameterType(ParameterType.INTEGER.toString());
							} catch (NumberFormatException e) {
								// try double
								try {
									Double.parseDouble(value);
									param.setParameterType(ParameterType.DOUBLE.toString());
								} catch (NumberFormatException ex) {
									// error, value string is not a number
									String message = logger.log(OdipMessage.MSG_WORKFLOW_OPTION_NO_TYPE_MATCH, name, wo.getType(),
											value);
									throw new OdipException(message);
								}
							}
							break;
						}
						case DATENUMBER: {
							/**
							 * Assumption is that this type means the day of year, i. e. it must be an integer number in the range
							 * 1..366
							 */
							try {
								Integer dn = Integer.parseInt(value);
								if (dn < 1 || dn > 366) {
									String message = logger.log(OdipMessage.MSG_WORKFLOW_OPTION_NO_TYPE_MATCH, name, wo.getType(),
											value);
									throw new OdipException(message);
								}
								param.setParameterType(ParameterType.INTEGER.toString());
							} catch (NumberFormatException e) {
								String message = logger.log(OdipMessage.MSG_WORKFLOW_OPTION_NO_TYPE_MATCH, name, wo.getType(),
										value);
								throw new OdipException(message);
							}
							break;
						}
						default: {
							// all others are strings
							param.setParameterType(ParameterType.STRING.toString());
							break;
						}
						}
						// check whether the value is in the value range
						if (wo.getValueRange() != null && !wo.getValueRange().isEmpty()) {
							if (!wo.getValueRange().contains(value)) {
								String message = logger.log(OdipMessage.MSG_WORKFLOW_OPTION_VALUE_NOT_IN_RANGE, name, value);
								throw new OdipException(message);
							}
						}
						param.setKey(name);
						param.setParameterValue(value);
						restOrder.getDynamicProcessingParameters().add(param);
						definedOptionName.add(name);
					}
				}
			}
		}
		for (WorkflowOption wo : workflowOptions.values()) {
			if (!definedOptionName.contains(wo.getName())) {
				if (wo.getDefaultValue() != null) {
					RestParameter param = new RestParameter();
					if (wo.getType().equals(WorkflowOptionType.NUMBER)) {
						// check for number type
						try {
							Integer.parseInt(wo.getDefaultValue());
							param.setParameterType(ParameterType.INTEGER.toString());
						} catch (NumberFormatException e) {
							// try double
							try {
								Double.parseDouble(wo.getDefaultValue());
								param.setParameterType(ParameterType.DOUBLE.toString());
							} catch (NumberFormatException ex) {
								// error, value string is not a number
								String message = logger.log(OdipMessage.MSG_WORKFLOW_OPTION_NO_TYPE_MATCH, wo.getName(),
										wo.getType(), wo.getDefaultValue());
								throw new OdipException(message);
							}
						}

					} else if (wo.getType().equals(WorkflowOptionType.DATENUMBER)) {
						/**
						 * Assumption is that this type means the day of year, i. e. it must be an integer number in the range
						 * 1..366
						 */
						try {
							Integer dn = Integer.parseInt(wo.getDefaultValue());
							if (dn < 1 || dn > 366) {
								String message = logger.log(OdipMessage.MSG_WORKFLOW_OPTION_NO_TYPE_MATCH, wo.getName(),
										wo.getType(), wo.getDefaultValue());
								throw new OdipException(message);
							}
							param.setParameterType(ParameterType.INTEGER.toString());
						} catch (NumberFormatException e) {
							String message = logger.log(OdipMessage.MSG_WORKFLOW_OPTION_NO_TYPE_MATCH, wo.getName(), wo.getType(),
									wo.getDefaultValue());
							throw new OdipException(message);
						}
					} else {
						// all others are strings
						param.setParameterType(ParameterType.STRING.toString());
					}
					param.setKey(wo.getName());
					param.setParameterValue(wo.getDefaultValue());
					restOrder.getDynamicProcessingParameters().add(param);
				}
			}
		}
		if (workflow.getOutputParameters() != null) {
			for (String paramKey : workflow.getOutputParameters().keySet()) {
				restOrder.getOutputParameters()
					.add(new RestParameter(paramKey, workflow.getOutputParameters().get(paramKey).getParameterType().toString(),
							workflow.getOutputParameters().get(paramKey).getParameterValue()));
			}
		}
		if (workflow.getClassOutputParameters() != null) {
			for (ProductClass targetClass : workflow.getClassOutputParameters().keySet()) {
				RestClassOutputParameter restClassOutputParameter = new RestClassOutputParameter();
				restClassOutputParameter.setProductClass(targetClass.getProductType());
				Map<String, Parameter> outputParameters = workflow.getClassOutputParameters()
					.get(targetClass)
					.getOutputParameters();
				for (String paramKey : outputParameters.keySet()) {
					restClassOutputParameter.getOutputParameters()
						.add(new RestParameter(paramKey, outputParameters.get(paramKey).getParameterType().toString(),
								outputParameters.get(paramKey).getParameterValue()));
				}
				restOrder.getClassOutputParameters().add(restClassOutputParameter);
			}
		}

		List<String> requestedProductClasses = new ArrayList<>();
		requestedProductClasses.add(workflow.getOutputProductClass().getProductType());
		restOrder.setRequestedProductClasses(requestedProductClasses);
		if (workflow.getOutputFileClass() != null && !workflow.getOutputFileClass().isEmpty()) {
			restOrder.setOutputFileClass(workflow.getOutputFileClass());
		} else {
			restOrder.setOutputFileClass(ORDER_OUTPUT_FILE_CLASS);
		}
		List<String> confProcs = new ArrayList<>();
		if (workflow.getConfiguredProcessor() != null) {
			confProcs.add(workflow.getConfiguredProcessor().getIdentifier());
		}
		restOrder.setMissionCode(securityConfig.getMission());
		restOrder.setConfiguredProcessors(confProcs);
		
		// generate an order name using workflow name and current time
		String name = workflow.getName() + "_";
		Instant now = Instant.now();
		name += instantFormatter.format(now);
		restOrder.setIdentifier(name);
		
		// set the mission
		Date d = Date.from(now);
		restOrder.setSubmissionTime(d);
		restOrder.setOrderSource(OrderSource.ODIP.toString());
		return restOrder;
	}

	/**
	 * Sends an order to the production planner and releases it.
	 *
	 * @param order The order to be sent and released.
	 * @return The created order after sending and releasing.
	 * @throws OdipException If an error occurs during the process of sending and releasing the order.
	 */
	@Transactional
	public RestOrder sendAndReleaseOrder(RestOrder order) throws OdipException {
		RestOrder createdOrder = null;
		if (order != null) {
			if (logger.isTraceEnabled())
				logger.trace(">>> sendAndReleaseOrder({})", order.getIdentifier());
			// send order to order manager
			int i = 0;
			while (3 > i++) {
				try {
					createdOrder = serviceConnection.postToService(config.getOrderManagerUrl(), URI_PATH_ORDERS, order,
							RestOrder.class, securityConfig.getMission() + "-" + securityConfig.getUser(),
							securityConfig.getPassword());
					break;
				} catch (RestClientResponseException e) {
					String message = null;
					HttpStatusCode status = HttpStatusCode.INTERNAL_SERVER_ERROR;
					switch (e.getRawStatusCode()) {
					case org.apache.http.HttpStatus.SC_BAD_REQUEST:
						status = HttpStatusCode.BAD_REQUEST;
						message = logger.log(OdipMessage.ORDER_DATA_INVALID, e.getStatusText());
						status = HttpStatusCode.BAD_REQUEST;
						break;
					case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					case org.apache.http.HttpStatus.SC_FORBIDDEN:
						status = HttpStatusCode.UNAUTHORIZED;
						message = (null == e.getStatusText()
								? ProseoLogger.format(OdipMessage.NOT_AUTHORIZED, securityConfig.getUser(), ORDERS,
										securityConfig.getMission())
								: e.getStatusText());
						break;
					default:
						message = logger.log(OdipMessage.EXCEPTION, e.getMessage());
					}
					throw new OdipException(message, status);
				} catch (RuntimeException e) {
					String message = ProseoLogger.format(OdipMessage.EXCEPTION, e.getMessage());
					throw new OdipException(message, HttpStatusCode.INTERNAL_SERVER_ERROR);
				}
			}
			// TODO change to > 2
			if (i > 2) {
				return createdOrder;
			}
			// approve, plan, release the order
			i = 0;
			while (3 > i++) {
				try {
					createdOrder = serviceConnection.patchToService(config.getProductionPlannerUrl(),
							URI_PATH_ORDERS_APPROVE + "/" + createdOrder.getId(), createdOrder, RestOrder.class,
							securityConfig.getMission() + "-" + securityConfig.getUser(), securityConfig.getPassword());
					break;
				} catch (RestClientResponseException e) {
					HttpStatusCode status = HttpStatusCode.INTERNAL_SERVER_ERROR;
					String message = null;
					switch (e.getRawStatusCode()) {
					case org.apache.http.HttpStatus.SC_BAD_REQUEST:
						message = logger.log(OdipMessage.ORDER_DATA_INVALID, e.getStatusText());
						status = HttpStatusCode.BAD_REQUEST;
						break;
					case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					case org.apache.http.HttpStatus.SC_FORBIDDEN:
						message = (null == e.getStatusText()
								? ProseoLogger.format(OdipMessage.NOT_AUTHORIZED, securityConfig.getUser(), ORDERS,
										securityConfig.getMission())
								: e.getStatusText());
						status = HttpStatusCode.UNAUTHORIZED;
						break;
					default:
						message = logger.log(OdipMessage.EXCEPTION, e.getMessage());
					}
					throw new OdipException(message, status);
				} catch (RuntimeException e) {
					String message = ProseoLogger.format(OdipMessage.EXCEPTION, e.getMessage());
					throw new OdipException(message, HttpStatusCode.INTERNAL_SERVER_ERROR);
				}
			}
			if (i > 2) {
				return createdOrder;
			}
			
			i = 0;
			while (3 > i++) {
				try {
					createdOrder = serviceConnection.putToService(config.getProductionPlannerUrl(),
							URI_PATH_ORDERS_PLAN + "/" + createdOrder.getId() + "?facility=" + config.getFacility() + "&wait=true",
							RestOrder.class, securityConfig.getMission() + "-" + securityConfig.getUser(),
							securityConfig.getPassword());
					break;
				} catch (RestClientResponseException e) {
					HttpStatusCode status = HttpStatusCode.INTERNAL_SERVER_ERROR;
					String message = null;
					switch (e.getRawStatusCode()) {
					case org.apache.http.HttpStatus.SC_BAD_REQUEST:
						message = logger.log(OdipMessage.ORDER_DATA_INVALID, e.getStatusText());
						status = HttpStatusCode.BAD_REQUEST;
						break;
					case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					case org.apache.http.HttpStatus.SC_FORBIDDEN:
						message = (null == e.getStatusText()
								? ProseoLogger.format(OdipMessage.NOT_AUTHORIZED, securityConfig.getUser(), ORDERS,
										securityConfig.getMission())
								: e.getStatusText());
						status = HttpStatusCode.UNAUTHORIZED;
						break;
					default:
						message = logger.log(OdipMessage.EXCEPTION, e.getMessage());
					}
					throw new OdipException(message, status);
				} catch (RuntimeException e) {
					String message = ProseoLogger.format(OdipMessage.EXCEPTION, e.getMessage());
					throw new OdipException(message, HttpStatusCode.INTERNAL_SERVER_ERROR);
				}
			}
			if (i > 2) {
				return createdOrder;
			}
			
			i = 0;			
			while (3 > i++) {
				try {
					createdOrder = serviceConnection.patchToService(config.getProductionPlannerUrl(),
							URI_PATH_ORDERS_RESUME + "/" + createdOrder.getId() + "?wait=true", createdOrder, RestOrder.class,
							securityConfig.getMission() + "-" + securityConfig.getUser(), securityConfig.getPassword());
					break;
				} catch (RestClientResponseException e) {
					HttpStatusCode status = HttpStatusCode.INTERNAL_SERVER_ERROR;
					String message = null;
					switch (e.getRawStatusCode()) {
					case org.apache.http.HttpStatus.SC_BAD_REQUEST:
						message = logger.log(OdipMessage.ORDER_DATA_INVALID, e.getStatusText());
						status = HttpStatusCode.BAD_REQUEST;
						break;
					case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					case org.apache.http.HttpStatus.SC_FORBIDDEN:
						message = (null == e.getStatusText()
								? ProseoLogger.format(OdipMessage.NOT_AUTHORIZED, securityConfig.getUser(), ORDERS,
										securityConfig.getMission())
								: e.getStatusText());
						status = HttpStatusCode.UNAUTHORIZED;
						break;
					default:
						message = logger.log(OdipMessage.EXCEPTION, e.getMessage());
					}
					System.err.println(message);
					throw new OdipException(message, status);
				} catch (RuntimeException e) {
					String message = ProseoLogger.format(OdipMessage.EXCEPTION, e.getMessage());
					throw new OdipException(message, HttpStatusCode.INTERNAL_SERVER_ERROR);
				}
			}
		}
		
		return createdOrder;
	}

	/**
	 * Finds a product by its reference.
	 *
	 * @param reference The reference of the product.
	 * @return The product matching the reference, or null if not found.
	 */
	public Product findProductByReference(String reference) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findProductByReference({})", reference);
		
		List<ProductFile> files = RepositoryService.getProductFileRepository().findByFileName(reference);
		Product product = null;
		
		for (ProductFile file : files) {
			if (file.getProcessingFacility().getName().equals(config.getFacility())) {
				product = file.getProduct();
				if (logger.isTraceEnabled())
					logger.trace("    product found on facility {}", config.getFacility());
				break;
			}
		}
		
		if (product == null) {
			try {
				RestProduct restProduct = serviceConnection.getFromService(config.getAipUrl(),
						URI_PATH_DOWNLOAD_BYNAME + "?filename=" + reference + "&facility=" + config.getFacility(),
						RestProduct.class, securityConfig.getMission() + "-" + securityConfig.getUser(),
						securityConfig.getPassword());
				if (restProduct != null) {
					product = ProductUtil.toModelProduct(restProduct);
				}
			} catch (HttpClientErrorException.NotFound e) {
				// already logged
				product = null;
			}
		}
		return product;
	}

	/**
	 * Finds products of a specified class within a given time range.
	 *
	 * @param productType the type of product class to search for
	 * @param start       the start time of the range
	 * @param stop        the stop time of the range
	 * @return the list of found products
	 * @throws OdipException if there is an error in the ODIP application
	 */
	public List<Product> findProductsByClassAndStartStop(String productType, Instant start, Instant stop) throws OdipException {
		if (logger.isTraceEnabled())
			logger.trace(">>> findProductByOutputClassAndStartStop({}, {}, {})", productType, start, stop);

		List<Product> products = new ArrayList<>();
		// find product class
		List<ProductClass> productClasses = RepositoryService.getProductClassRepository().findByProductType(productType);
		ProductClass productClass = null;
		for (ProductClass pc : productClasses) {
			if (pc.getMission().getCode().equals(securityConfig.getMission())) {
				productClass = pc;
				if (logger.isTraceEnabled())
					logger.trace("    product class found for mission {}", pc.getMission().getCode());
				break;
			}
		}
		if (productClass == null) {
			// Product class not defined, error
			String message = logger.log(OdipMessage.MSG_PRODUCTCLASS_NOT_DEF, productType, securityConfig.getMission());
			throw new OdipException(message);
		}
		
		try {
			@SuppressWarnings("unchecked")
			List<RestProduct> restProducts = (List<RestProduct>) serviceConnection.getFromService(config.getAipUrl(),
					URI_PATH_DOWNLOAD_ALLBYTIME + "?productType=" + productType + "&startTime=" + OrbitTimeFormatter.format(start)
							+ "&stopTime=" + OrbitTimeFormatter.format(stop) + "&facility=" + config.getFacility(),
					RestProduct.class, securityConfig.getMission() + "-" + securityConfig.getUser(), securityConfig.getPassword());
			if (restProducts != null) {
				for (RestProduct restProduct : restProducts) {
					products.add(ProductUtil.toModelProduct(restProduct));
				}
			}
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
				String message = logger.log(OdipMessage.MSG_NO_INPUTPRODUCT, productType);
				throw new OdipException(message);
			} else {
				String message = logger.log(OdipMessage.MSG_EXCEPTION, e.getMessage(), e);
				throw new OdipException(message);
			}
		} catch (Exception e) {
			String message = logger.log(OdipMessage.MSG_EXCEPTION, e.getMessage(), e);
			throw new OdipException(message);
		}
		
		return products;
	}

}