/**
 * OdipUtil.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.odata;

import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import org.joda.time.DateTime;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;
import de.dlr.proseo.model.rest.model.RestInputReference;
import de.dlr.proseo.model.rest.model.RestNotificationEndpoint;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Utility class to convert product objects from prosEO database model to ODIP (OData) REST API
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class OdipUtil {

	private static final Date START_OF_MISSION = Date.from(Instant.parse("1970-01-01T00:00:00.000Z"));
	private static final Date END_OF_MISSION = Date.from(Instant.parse("9999-12-31T23:59:59.999Z"));

	private static final String ORDER_PROCESSING_MODE = "OFFL";
	private static final String ORDER_SLICING_TYPE = "NONE";
	

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OdipUtil.class);

	public static EdmEntitySet getEdmEntitySet(UriInfoResource uriInfo) throws ODataApplicationException {

		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// To get the entity set we have to interpret all URI segments
		if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
			// Here we should interpret the whole URI but in this example we do not support navigation so we throw an
			// exception
			throw new ODataApplicationException("Invalid resource type for first segment.", HttpStatusCode.NOT_IMPLEMENTED
					.getStatusCode(), Locale.ENGLISH);
		}

		UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);

		return uriResource.getEntitySet();
	}

	
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
	
	public static RestOrder toModelOrder(Entity order) throws ODataApplicationException {
		if (null == order)
			return null;

		RestOrder restOrder = new RestOrder();
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_INPUTPRODUCTREFERENCE) != null) {
			if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_INPUTPRODUCTREFERENCE).getValueType() == ValueType.COMPLEX) {
				// handle input product reference
				// find input file
				ComplexValue ipr = (ComplexValue)order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_INPUTPRODUCTREFERENCE).getValue();
				String reference = null;
				Instant start = null;
				Instant end = null;
				List<Property> iprProps = ipr.getValue();
				for (Property prop : iprProps) {
					if (prop.getName().equals(OdipEdmProvider.CT_CONTENTDATE_NAME)) {
						// TODO
						if (prop.getValue() instanceof ComplexValue) {
							ComplexValue cd = (ComplexValue)prop.getValue();
							for (Property cdprop : cd.getValue()) {
								if (cdprop.getName().equals(OdipEdmProvider.CT_CONTENTDATE_PROP_START)) {
									if (cdprop.getType().equals(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName().toString())) {
										Timestamp ts = (Timestamp)(cdprop.getValue());
										start = ts.toInstant();
									}
								} else if (cdprop.getName().equals(OdipEdmProvider.CT_CONTENTDATE_PROP_END)) {
									if (cdprop.getType().equals(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName().toString())) {
										Timestamp ts = (Timestamp)(cdprop.getValue());
										end = ts.toInstant();
									}
								}
							}

						}
					} else if (prop.getName().equals(OdipEdmProvider.CT_INPUTPRODUCTREFERENCE_PROP_REFERENCE)) {
						reference = (String)prop.getValue();
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
				}				
			}
		}
		restOrder.setProcessingMode(ORDER_PROCESSING_MODE);
		restOrder.setSlicingType(ORDER_SLICING_TYPE);
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWID) != null) {
			restOrder.setWorkflowUuid((String)order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWID).getValue());
		}
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWNAME) != null) {
			restOrder.setWorkflowName((String)order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWNAME).getValue());
		}
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_PRIORITY) != null) {
			restOrder.setPriority((Long)order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_PRIORITY).getValue());
		}
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONENDPOINT) != null) {
			RestNotificationEndpoint rnep = new RestNotificationEndpoint();
			rnep.setUri((String)order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONENDPOINT).getValue());
			if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPUSERNAME) != null) {
				rnep.setUsername((String)order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPUSERNAME).getValue());
			}			
			if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPPASSWORD) != null) {
				rnep.setUsername((String)order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_NOTIFICATIONEPPASSWORD).getValue());
			}	
			restOrder.setNotificationEndpoint(rnep);
		}
		if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWOPTIONS) != null) {
			List<RestParameter> workflowOptions = new ArrayList<RestParameter>();
			if (order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWOPTIONS).getValueType() == ValueType.COLLECTION_COMPLEX) {
				for (Object obj : (List)(order.getProperty(OdipEdmProvider.ET_PRODUCTIONORDER_PROP_WORKFLOWOPTIONS).getValue())) {
					String name = null;
					String value = null;
					if (obj instanceof ComplexValue) {
						ComplexValue cv = (ComplexValue) obj;
						for (Object paramObj : (List)(cv.getValue())) {
							Property param = (Property)paramObj;
							if (param.getName().equals(OdipEdmProvider.ET_WORKFLOWOPTION_PROP_NAME)) {
								name = (String)(param.getValue());
							} else if (param.getName().equals(OdipEdmProvider.ET_WORKFLOWOPTION_PROP_VALUE)) {
								value = (String)(param.getValue());
							}
						}
					}
					if (name != null && value != null) {
						// find type of value
						
					}
				}
			}
		}		
		
		// generate an order name
		
		// set the mission
		Date d = Date.from(Instant.now());
		restOrder.setSubmissionTime(d);
		return restOrder;
	}
}
