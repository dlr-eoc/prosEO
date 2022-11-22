/**
 * OdipUtil.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.odata;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;

/**
 * Utility class to convert product objects from prosEO database model to ODIP (OData) REST API
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class OdipUtil {

	private static final String ERR_NO_PRODUCT_FILES_FOUND = "No product files found in product ";
	private static final Date START_OF_MISSION = Date.from(Instant.parse("1970-01-01T00:00:00.000Z"));
	private static final Date END_OF_MISSION = Date.from(Instant.parse("9999-12-31T23:59:59.999Z"));
	

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OdipUtil.class);
	
	/**
	 * Create a ODIP interface product from a prosEO interface product; when setting ODIP product attributes the product
	 * metadata attributes are overridden by product parameters, if a product parameter with the intended attribute name exists
	 * 
	 * @param modelOrder the prosEO model product to convert
	 * @return an OData entity object representing the prosEO interface production order
	 * @throws IllegalArgumentException if any mandatory information is missing from the prosEO interface production order 
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 */
	public static Entity toOdipProductionOrder(ProcessingOrder modelOrder) throws IllegalArgumentException, URISyntaxException {
		if (logger.isTraceEnabled()) logger.trace(">>> toPripProduct({})", modelOrder.getId());
		
		// Select a product file (we just take the first one, since they are assumed to be identical, even if stored on
		// different processing facilities)
//		if (modelProduct.getProductFile().isEmpty()) {
//			throw new IllegalArgumentException(ERR_NO_PRODUCT_FILES_FOUND + modelOrder.getId());
//		}
//		ProductFile modelProductFile = modelProduct.getProductFile().iterator().next();
//		Mission modelMission = modelProduct.getProductClass().getMission();
//		
//		// Determine production type
//		ProductionType modelProductionType = modelProduct.getProductionType();
//		int productionType = OdipEdmProvider.EN_PRODUCTIONTYPE_SYSTEMATIC_VAL; // Default, also in case of no production type
//		if (ProductionType.ON_DEMAND_DEFAULT.equals(modelProductionType)) {
//			productionType = OdipEdmProvider.EN_PRODUCTIONTYPE_ONDEMDEF_VAL;
//		} else if (ProductionType.ON_DEMAND_NON_DEFAULT.equals(modelProductionType)) {
//			productionType = OdipEdmProvider.EN_PRODUCTIONTYPE_ONDEMNODEF_VAL;
//		}
//		
//		// Create product entity
		Entity order = new Entity();
		order.setType(OdipEdmProvider.ET_PRODUCTIONORDER_FQN.getFullQualifiedNameAsString());
		order.addProperty(new Property(null, OdipEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, modelOrder.getUuid()))
			.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_STATUS, ValueType.ENUM, 
				(OdipUtil.getProductionOrderStateFrom(modelOrder))))
			.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_STATUSMESSAGE, ValueType.PRIMITIVE,
				modelOrder.getStateMessage()));
			// TODO
		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_ORDEROUTPUTSIZE, ValueType.PRIMITIVE,
				0));
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
		inputProductReference.getValue().add(new Property(null, OdipEdmProvider.CT_INPUTPRODUCTREFERENCE_PROP_REFERENCE, ValueType.PRIMITIVE,
				(modelOrder.getInputProductReference() != null && modelOrder.getInputProductReference().getInputFileName() != null) 
				? modelOrder.getInputProductReference().getInputFileName() : "null"));

		if (modelOrder.getInputProductReference() != null && (
				modelOrder.getInputProductReference().getSensingStartTime() != null && 
				modelOrder.getInputProductReference().getSensingStopTime() != null
				)
				) {
			ComplexValue contentDate = new ComplexValue();
			if (modelOrder.getInputProductReference().getSensingStartTime() != null) {
				contentDate.getValue().add(new Property(null, OdipEdmProvider.CT_CONTENTDATE_PROP_START, ValueType.PRIMITIVE,
						Date.from(modelOrder.getInputProductReference().getSensingStartTime())));
			}
			if (modelOrder.getInputProductReference().getSensingStopTime() != null) {
				contentDate.getValue().add(new Property(null, OdipEdmProvider.CT_CONTENTDATE_PROP_END, ValueType.PRIMITIVE,
						Date.from(modelOrder.getInputProductReference().getSensingStopTime())));
			}
			inputProductReference.getValue().add(new Property(null, OdipEdmProvider.CT_CONTENTDATE_NAME, ValueType.COMPLEX, 
					contentDate));
		}

		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_INPUTPRODUCTREFERENCE, ValueType.COMPLEX, 
				inputProductReference));

		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWID, ValueType.PRIMITIVE,
				(modelOrder.getWorkflow() != null && modelOrder.getWorkflow().getUuid() != null) ? modelOrder.getWorkflow().getUuid() : "00000000-0000-0000-0000-000000000000"));

		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWNAME, ValueType.PRIMITIVE,
				(modelOrder.getWorkflow() != null && modelOrder.getWorkflow().getName() != null) ? modelOrder.getWorkflow().getName() : "null"));

	
		// workflow options
		// use dynamic processing parameters
		// TODO shall missing options added using options in workflow?

		List<ComplexValue> workflowOptions = new ArrayList<>();
		for (String paramName : modelOrder.getDynamicProcessingParameters().keySet()) {
			ComplexValue workflowOption = new ComplexValue();
			Parameter param = modelOrder.getDynamicProcessingParameters().get(paramName);
			workflowOption.getValue().add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_NAME, ValueType.PRIMITIVE,
					paramName));
			workflowOption.getValue().add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_VALUE, ValueType.PRIMITIVE,
					param.getParameterValue()));
			workflowOptions.add(workflowOption);
		}
		order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWOPTIONS, ValueType.COLLECTION_COMPLEX, 
				workflowOptions));
		if (modelOrder.getNotificationEndpoint() != null) {
			if (modelOrder.getNotificationEndpoint().getUri() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONENDPOINT, ValueType.PRIMITIVE,
					modelOrder.getNotificationEndpoint().getUri()));
			}
			if (modelOrder.getNotificationEndpoint().getUsername() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPUSERNAME, ValueType.PRIMITIVE,
					modelOrder.getNotificationEndpoint().getUsername()));
			}
			if (modelOrder.getNotificationEndpoint().getPassword() != null) {
			order.addProperty(new Property(null, OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPPASSWORD, ValueType.PRIMITIVE,
					modelOrder.getNotificationEndpoint().getPassword()));
			}
		}
		return order;
	}

	public static Entity toOdipWorkflow(Workflow modelWorkflow) throws IllegalArgumentException, URISyntaxException {
		if (logger.isTraceEnabled()) logger.trace(">>> toOdipWorkflow({})", modelWorkflow.getId());
		
		// Select a product file (we just take the first one, since they are assumed to be identical, even if stored on
		// different processing facilities)
//		if (modelProduct.getProductFile().isEmpty()) {
//			throw new IllegalArgumentException(ERR_NO_PRODUCT_FILES_FOUND + modelOrder.getId());
//		}
//		ProductFile modelProductFile = modelProduct.getProductFile().iterator().next();
//		Mission modelMission = modelProduct.getProductClass().getMission();
//		
//		// Determine production type
//		ProductionType modelProductionType = modelProduct.getProductionType();
//		int productionType = OdipEdmProvider.EN_PRODUCTIONTYPE_SYSTEMATIC_VAL; // Default, also in case of no production type
//		if (ProductionType.ON_DEMAND_DEFAULT.equals(modelProductionType)) {
//			productionType = OdipEdmProvider.EN_PRODUCTIONTYPE_ONDEMDEF_VAL;
//		} else if (ProductionType.ON_DEMAND_NON_DEFAULT.equals(modelProductionType)) {
//			productionType = OdipEdmProvider.EN_PRODUCTIONTYPE_ONDEMNODEF_VAL;
//		}
//		
//		// Create product entity
		Entity workflow = new Entity();
		workflow.setType(OdipEdmProvider.ET_WORKFLOW_FQN.getFullQualifiedNameAsString());
		workflow.addProperty(new Property(null, OdipEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, modelWorkflow.getUuid()))
			.addProperty(new Property(null, OdipEdmProvider.ET_WORKFLOW_PROP_NAME, ValueType.PRIMITIVE, 
				modelWorkflow.getName()));
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
		// TODO shall missing options added using options in workflow?

		List<ComplexValue> workflowOptions = new ArrayList<>();
		for (WorkflowOption opt : modelWorkflow.getWorkflowOptions()) {
			ComplexValue workflowOption = new ComplexValue();
			if (opt.getName() != null) {
			workflowOption.getValue().add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_NAME, ValueType.PRIMITIVE,
					opt.getName()));
			}
			if (opt.getDescription() != null) {
			workflowOption.getValue().add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_DESCRIPTION, ValueType.PRIMITIVE,
					opt.getDescription()));
			}
			if (opt.getType() != null) {
			workflowOption.getValue().add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_TYPE, ValueType.PRIMITIVE,
					opt.getType()));
			}
			if (opt.getDefaultValue() != null) {
			workflowOption.getValue().add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_DEFAULT, ValueType.PRIMITIVE,
					opt.getDefaultValue()));
			}

			List<Object> values = new ArrayList<Object>();
			for (String elem : opt.getValueRange()) {
				values.add(elem);
			}
			workflowOption.getValue().add(new Property(null, OdipEdmProvider.CT_WORKFLOWOPTION_PROP_VALUE, ValueType.COLLECTION_PRIMITIVE,
					values));
			workflowOptions.add(workflowOption);
		}
		workflow.addProperty(new Property(null, OdipEdmProvider.ET_WORKFLOW_PROP_WORKFLOWOPTIONS, ValueType.COLLECTION_COMPLEX, 
				workflowOptions));
		return workflow;
	}
	
	public static int getProductionOrderStateFrom(ProcessingOrder order) {
		// States are
		//INITIAL, APPROVED, PLANNING, PLANNING_FAILED, PLANNED, RELEASING, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED

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
}
