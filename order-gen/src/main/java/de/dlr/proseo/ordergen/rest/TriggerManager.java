/**
 * TriggerManager.java
 *
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OrderGenMessage;
import de.dlr.proseo.model.CalendarOrderTrigger;
import de.dlr.proseo.model.DataDrivenOrderTrigger;
import de.dlr.proseo.model.DatatakeOrderTrigger;
import de.dlr.proseo.model.OrbitOrderTrigger;
import de.dlr.proseo.model.OrderTrigger;
import de.dlr.proseo.model.TimeIntervalOrderTrigger;
import de.dlr.proseo.model.enums.TriggerType;
import de.dlr.proseo.model.rest.model.RestTrigger;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.ordergen.util.TriggerUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * TriggerManager to create, modify, list or delete triggers.
 *
 * @author Ernst Melchinger
 *
 */
@Component
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class TriggerManager {
	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	
	@Autowired
	private TriggerUtil triggerUtil;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(TriggerManager.class);


	/**
	 * Create a new trigger (version)
	 *
	 * @param trigger a Json representation of the new trigger
	 * @return a Json representation of the trigger after creation (with ID and version number)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public RestTrigger createTrigger(RestTrigger trigger) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createTrigger({})", (null == trigger ? "MISSING" : trigger.getName()));

		if (null == trigger) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_MISSING));
		}

		// Ensure user is authorized for the mission of the trigger
		if (!securityService.isAuthorizedForMission(trigger.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, trigger.getMissionCode(),
					securityService.getMission()));
		}

		// Ensure mandatory attributes are set
		if (null == trigger.getName() || trigger.getName().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "triggerName", "trigger creation"));
		}
		OrderTrigger modelTrigger = triggerUtil.toModelTrigger(trigger);
		triggerUtil.check(modelTrigger);

		// Make sure a trigger with the same trigger name and trigger version does not yet exist
		if (null != triggerUtil.findByMissionCodeAndTriggerNameAndType(trigger.getMissionCode(), trigger.getName(),
					trigger.getType())) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.DUPLICATE_TRIGGER, trigger.getMissionCode(),
					trigger.getName(), trigger.getType()));
		}

		modelTrigger = triggerUtil.save(modelTrigger);
		RestTrigger restTrigger = triggerUtil.toRestTrigger(modelTrigger);
		logger.log(OrderGenMessage.TRIGGER_CREATED,
				restTrigger.getName(), restTrigger.getType(), restTrigger.getMissionCode());

		return restTrigger;
	}


	/**
	 * Delete a trigger 
	 *
	 * @param mission 			the mission code
	 * @param name				the trigger name
	 * @param type				the trigger type
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public void deleteTrigger(String mission, String name, String type) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteTrigger({}, {}, {})", mission, name, type);

		if (null == mission || mission.isEmpty()) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_MISSION_MISSING));
		}
		if (null == name || name.isEmpty()) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_NAME_MISSING));
		}
		if (null == type || type.isEmpty()) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_TYPE_MISSING));
		}

		// Ensure user is authorized for the mission of the trigger
		if (!securityService.isAuthorizedForMission(mission)) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission,
					securityService.getMission()));
		}

		OrderTrigger trigger = triggerUtil.findByMissionCodeAndTriggerNameAndType(mission, name,
				type);
		// Make sure a trigger with the trigger name and trigger version does exist
		if (null == trigger) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_NOT_EXIST, mission,
					name, type));
		}

		triggerUtil.delete(trigger, type);
		logger.log(OrderGenMessage.TRIGGER_DELETED, mission, name, type);

	}

	/**
	 * Reload all triggers 
	 *
	 * @param mission 			not used
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public void reloadTriggers(String mission) throws IllegalArgumentException, SecurityException, SchedulerException {
		if (logger.isTraceEnabled())
			logger.trace(">>> reloadTriggers({}, {}, {})", mission);

		if (null == mission || mission.isEmpty()) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_MISSION_MISSING));
		}

		// Ensure user is authorized for the mission of the trigger
		if (!securityService.isAuthorizedForMission(mission)) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission,
					securityService.getMission()));
		}

		triggerUtil.reload();
		logger.log(OrderGenMessage.TRIGGERS_RELOADED);

	}

	/**
	 * Get a list of triggers filtered by mission, name and type.
	 *
	 * @param mission 			the mission code
	 * @param name				the trigger name
	 * @param type				the trigger type
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public List<RestTrigger> getTriggers(String mission, String name, String type,
			String workflow, String inputProductClass, String outputProductClass, Integer recordFrom, 
			Integer recordTo, String[] orderBy) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getTriggers({}, {}, {}, {}, {}, {})", mission, name, type, workflow, inputProductClass, outputProductClass);
		

		List<RestTrigger> triggers = new ArrayList<RestTrigger>();

		TriggerType triggerType = null;
		if (type != null) {
			triggerType = TriggerType.valueOf(type);
			triggers.addAll(getTriggersPrim(mission, name, triggerType,
					workflow, inputProductClass, outputProductClass, null, 
					null, null));
		} else {
			for (TriggerType triggerTypeL : TriggerType.values()) {
				triggers.addAll(getTriggersPrim(mission, name, triggerTypeL,
						workflow, inputProductClass, outputProductClass, null, 
						null, null));
			}			
		}
		// sort
		if (orderBy != null) {
			triggers.sort((lhs, rhs) -> {
				// there are max. three attributes to compare: type, name, workflow.name
				int result = 0;
				if (orderBy.length > 0) {
					result = sortPrim(lhs, rhs, orderBy[0]);
					if (result == 0 && orderBy.length > 1) {
						result = sortPrim(lhs, rhs, orderBy[1]);						
					}
					if (result == 0 && orderBy.length > 2) {
						result = sortPrim(lhs, rhs, orderBy[2]);						
					}
				}
				return result;
		    });
		}
		
		// from - to
		if (recordFrom != null && recordFrom >= 0 && recordTo !=  null && recordTo > recordFrom) {
		    triggers =  triggers.subList(recordFrom, Math.min(triggers.size(), recordTo));
		} 
		return triggers;
	}

	private int sortPrim(RestTrigger first, RestTrigger second, String orderBy) {
		String[] sort = orderBy.split(" ");
		int dir = 1;
		if (sort.length > 1 && sort[1].equalsIgnoreCase("DESC")) {
			dir = -1;
		}
		if (sort[0].equalsIgnoreCase("type")) {
			return first.getType().compareTo(second.getType()) * dir;
		} else if (sort[0].equalsIgnoreCase("name")) {
			return first.getName().compareTo(second.getName()) * dir;
		} else if (sort[0].equalsIgnoreCase("workflow.name")) {
			return first.getOrderTemplateName().compareTo(second.getOrderTemplateName()) * dir;
		}
		return 0;
	}
	private List<RestTrigger> getTriggersPrim(String mission, String name, TriggerType type,
			String workflow, String inputProductClass, String outputProductClass, Integer recordFrom, 
			Integer recordTo, String[] orderBy) {
		List<RestTrigger> triggers = new ArrayList<RestTrigger>();

		Query query = createTriggersQuery(mission, name, type, workflow, inputProductClass, outputProductClass, orderBy, false);
		
		List<Object> objList = query.getResultList();
		for (Object obj : objList) {
			if (obj instanceof DataDrivenOrderTrigger) {
				DataDrivenOrderTrigger triggerLoc = (DataDrivenOrderTrigger) obj;
				triggers.add(triggerUtil.toRestTrigger(triggerLoc));
			} else if (obj instanceof TimeIntervalOrderTrigger) {
				TimeIntervalOrderTrigger triggerLoc = (TimeIntervalOrderTrigger) obj;
				triggers.add(triggerUtil.toRestTrigger(triggerLoc));
			} else if (obj instanceof CalendarOrderTrigger) {
				CalendarOrderTrigger triggerLoc = (CalendarOrderTrigger) obj;
				triggers.add(triggerUtil.toRestTrigger(triggerLoc));
			} else if (obj instanceof OrbitOrderTrigger) {
				OrbitOrderTrigger triggerLoc = (OrbitOrderTrigger) obj;
				triggers.add(triggerUtil.toRestTrigger(triggerLoc));
			} else if (obj instanceof DatatakeOrderTrigger) {
				DatatakeOrderTrigger triggerLoc = (DatatakeOrderTrigger) obj;
				triggers.add(triggerUtil.toRestTrigger(triggerLoc));
			}
		}
		
		return triggers;
	}
	public String countTriggers(String mission, String name, String type, String workflow,
			String inputProductClass, String outputProductClass) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> countTriggers({}, {}, {}, {}, {}, {})", mission, name, type, workflow, inputProductClass, outputProductClass);
		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(
						logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission, securityService.getMission()));
			}
		}
		Long count = 0L;
		
		TriggerType triggerType = null;
		if (type != null) {
			triggerType = TriggerType.valueOf(type);
			count = countTriggersPrim(mission, name, triggerType, workflow, inputProductClass, outputProductClass);
		} else {
			for (TriggerType triggerTypeL : TriggerType.values()) {
				count += countTriggersPrim(mission, name, triggerTypeL, workflow, inputProductClass, outputProductClass);
			}
			
		}
		String result = count.toString();
		logger.log(OrderGenMessage.TRIGGERS_COUNTED, result);

		return result;
	}
	
	private Long countTriggersPrim(String mission, String name, TriggerType type, String workflow,
			String inputProductClass, String outputProductClass) {
		Long count = 0L;
		Query query = createTriggersQuery(mission, name, type, workflow, inputProductClass, outputProductClass, null, true);

		Object resultObject = query.getSingleResult();

		if (resultObject instanceof Long) {
			count = ((Long) resultObject);
		}
		if (resultObject instanceof String) {
			count = Long.parseLong((String) resultObject);
		}
		
		return count;
		
	}
	
	private Query createTriggersQuery(String mission, String name, TriggerType type , String workflow,
			String inputProductClass, String outputProductClass, String[] orderBy, Boolean count) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createTriggersQuery({}, {}, {}, {}, {}, {})", mission, name, type.toString(), workflow, inputProductClass, outputProductClass);

		if (type == null) {
			// error
		}
		String tableName = type.toString() + "OrderTrigger";
		// Find using search parameters
		String jpqlQuery = null;
		String andStr = " where ";
		if (count) {
			jpqlQuery = "select count(w) from " + tableName + " w ";
		} else {
			jpqlQuery = "select w from " + tableName + " w ";
		}
		if (null != mission) {
			jpqlQuery += andStr + "mission.code = :missionCode";
			andStr = " and ";
		}
		if (null != name) {
			jpqlQuery += andStr + "upper(name) like :name";
			andStr = " and ";
		}
		if (null != workflow) {
			jpqlQuery += andStr + "upper(workflow.name) like :workflow";
			andStr = " and ";
		}
		if (null != inputProductClass) {
			jpqlQuery += andStr + "inputProductClass.productType = :inputProductClass";
			andStr = " and ";
		}
		if (null != outputProductClass) {
			jpqlQuery += andStr + "outputProductClass.productType = :outputProductClass";
			andStr = " and ";
		}
		if (!count) {
			// order by
			if (null != orderBy && 0 < orderBy.length) {
				jpqlQuery += " order by ";
				for (int i = 0; i < orderBy.length; ++i) {
					if (0 < i)
						jpqlQuery += ", ";
					jpqlQuery += "w.";
					jpqlQuery += orderBy[i];
				}
			}

		}

		Query query = em.createQuery(jpqlQuery);
		if (null != mission) {
			query.setParameter("missionCode", mission);
		}
		if (null != name) {
			query.setParameter("name", name.toUpperCase());
		}
		if (null != workflow) {
			query.setParameter("workflow", workflow.toUpperCase());
		}
		if (null != inputProductClass) {
			query.setParameter("inputProductClass", inputProductClass);
		}
		if (null != outputProductClass) {
			query.setParameter("outputProductClass", outputProductClass);
		}
		return query;
	}

	/**
	 * Get all data driven triggers for workflows having the given product class as input product class
	 * 
	 * @param mission the mission code
	 * @param productType the product type of the requested product class
	 * @return a list of data driven triggers
	 * @throws IllegalArgumentException if the given product type does not belong to any product class of the mission
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public List<RestTrigger> getByProductType(String mission, String productType) 
			throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getByProductType({}, {})", mission, productType);

		if (null == mission || mission.isBlank() || null == productType || productType.isBlank()) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.MISSION_OR_PRODUCT_TYPE_MISSING));
		}
		
		// Ensure user is authorized for the mission of the product type
		if (!securityService.isAuthorizedForMission(mission)) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission,
					securityService.getMission()));
		}

		List<RestTrigger> triggers = new ArrayList<RestTrigger>();
		
		for (DataDrivenOrderTrigger orderTrigger : triggerUtil.findDataDrivenByProductType(mission, productType)) {
			triggers.add(triggerUtil.toRestTrigger(orderTrigger));
		}

		return triggers;
	}

	
	/**
	 * Modify a trigger 
	 *
	 * @param trigger a Json representation of the modified trigger
	 * @return a Json representation of the trigger after creation (with ID and version number)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public RestTrigger modifyTrigger(RestTrigger trigger) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyTrigger({})", (null == trigger ? "MISSING" : trigger.getName()));

		if (null == trigger) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_MISSING));
		}
		String mission = trigger.getMissionCode();
		String name = trigger.getName();
		String type = trigger.getType();
		if (null == mission || mission.isEmpty()) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_MISSION_MISSING));
		}
		if (null == name || name.isEmpty()) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_NAME_MISSING));
		}
		if (null == type || type.isEmpty()) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_TYPE_MISSING));
		}
		
		OrderTrigger modelTrigger = triggerUtil.findOneByMissionCodeAndTriggerNameAndType(mission, name, type);
		if (modelTrigger == null) {
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_NOT_EXIST, mission,
					name, type));
		}
		
		OrderTrigger changedTrigger = triggerUtil.toModelTrigger(trigger);
		Boolean triggerChanged = false;

		if (!Objects.equals(modelTrigger.getOrderTemplate(), changedTrigger.getOrderTemplate())) {
			triggerChanged = true;
			modelTrigger.setOrderTemplate(changedTrigger.getOrderTemplate());
		}
		if (!Objects.equals(modelTrigger.getExecutionDelay(), changedTrigger.getExecutionDelay())) {
			triggerChanged = true;
			modelTrigger.setExecutionDelay(changedTrigger.getExecutionDelay());
		}
		if (!Objects.equals(modelTrigger.getPriority(), changedTrigger.getPriority())) {
			triggerChanged = true;
			modelTrigger.setPriority(changedTrigger.getPriority());
		}
		if (modelTrigger instanceof DataDrivenOrderTrigger && changedTrigger instanceof DataDrivenOrderTrigger) {
			DataDrivenOrderTrigger modelTriggerLoc = (DataDrivenOrderTrigger) modelTrigger;
			DataDrivenOrderTrigger changedTriggerLoc = (DataDrivenOrderTrigger) changedTrigger;
			if (!modelTriggerLoc.getParametersToCopy().equals(changedTriggerLoc.getParametersToCopy())) {
				triggerChanged = true;			
				modelTriggerLoc.setParametersToCopy(changedTriggerLoc.getParametersToCopy());
			}
		} else if (modelTrigger instanceof TimeIntervalOrderTrigger && changedTrigger instanceof TimeIntervalOrderTrigger) {
			TimeIntervalOrderTrigger modelTriggerLoc = (TimeIntervalOrderTrigger) modelTrigger;
			TimeIntervalOrderTrigger changedTriggerLoc = (TimeIntervalOrderTrigger) changedTrigger;
			if (!Objects.equals(modelTriggerLoc.getTriggerInterval(), changedTriggerLoc.getTriggerInterval())) {
				triggerChanged = true;
				modelTriggerLoc.setTriggerInterval(changedTriggerLoc.getTriggerInterval());
			}
			if (!Objects.equals(modelTriggerLoc.getNextTriggerTime(), changedTriggerLoc.getNextTriggerTime())) {
				triggerChanged = true;
				modelTriggerLoc.setNextTriggerTime(changedTriggerLoc.getNextTriggerTime());
			} 	
		} else if (modelTrigger instanceof CalendarOrderTrigger && changedTrigger instanceof CalendarOrderTrigger) {
			CalendarOrderTrigger modelTriggerLoc = (CalendarOrderTrigger) modelTrigger;
			CalendarOrderTrigger changedTriggerLoc = (CalendarOrderTrigger) changedTrigger;
			if (!Objects.equals(modelTriggerLoc.getCronExpression(), changedTriggerLoc.getCronExpression())) {
				triggerChanged = true;
				modelTriggerLoc.setCronExpression(changedTriggerLoc.getCronExpression());
			}	
		} else if (modelTrigger instanceof OrbitOrderTrigger && changedTrigger instanceof OrbitOrderTrigger) {
			OrbitOrderTrigger modelTriggerLoc = (OrbitOrderTrigger) modelTrigger;
			OrbitOrderTrigger changedTriggerLoc = (OrbitOrderTrigger) changedTrigger;
			if (!Objects.equals(modelTriggerLoc.getSpacecraft(), changedTriggerLoc.getSpacecraft())) {
				triggerChanged = true;
				modelTriggerLoc.setSpacecraft(changedTriggerLoc.getSpacecraft());
			}	
			if (!Objects.equals(modelTriggerLoc.getDeltaTime(), changedTriggerLoc.getDeltaTime())) {
				triggerChanged = true;
				modelTriggerLoc.setDeltaTime(changedTriggerLoc.getDeltaTime());
			}	
			if (!Objects.equals(modelTriggerLoc.getLastOrbit(), changedTriggerLoc.getLastOrbit())) {
				triggerChanged = true;
				modelTriggerLoc.setLastOrbit(changedTriggerLoc.getLastOrbit());
			}	
		} else if (modelTrigger instanceof DatatakeOrderTrigger && changedTrigger instanceof DatatakeOrderTrigger) {
			DatatakeOrderTrigger modelTriggerLoc = (DatatakeOrderTrigger) modelTrigger;
			DatatakeOrderTrigger changedTriggerLoc = (DatatakeOrderTrigger) changedTrigger;
			if (!Objects.equals(modelTriggerLoc.getDatatakeType(), changedTriggerLoc.getDatatakeType())) {
				triggerChanged = true;
				modelTriggerLoc.setDatatakeType(changedTriggerLoc.getDatatakeType());
			}	
			if (!Objects.equals(modelTriggerLoc.getLastDatatakeStartTime(), changedTriggerLoc.getLastDatatakeStartTime())) {
				triggerChanged = true;
				modelTriggerLoc.setLastDatatakeStartTime(changedTriggerLoc.getLastDatatakeStartTime());
			}	
			if (!modelTriggerLoc.getParametersToCopy().equals(changedTriggerLoc.getParametersToCopy())) {
				triggerChanged = true;
				modelTriggerLoc.setParametersToCopy(changedTriggerLoc.getParametersToCopy());
			}
			if (!Objects.equals(modelTriggerLoc.getDeltaTime(), changedTriggerLoc.getDeltaTime())) {
				triggerChanged = true;
				modelTriggerLoc.setDeltaTime(changedTriggerLoc.getDeltaTime());
			}	
		} else {
			// types not equal
			throw new IllegalArgumentException(logger.log(OrderGenMessage.TRIGGER_TYPE_DIFFER));
		}
		if (triggerChanged) {
			modelTrigger.incrementVersion();
			triggerUtil.save(modelTrigger);
			logger.log(OrderGenMessage.TRIGGER_MODIFIED, name, type, mission);
		} else {
			logger.log(OrderGenMessage.TRIGGER_NOT_MODIFIED, name, type, mission);
		}
		return triggerUtil.toRestTrigger(modelTrigger);
	}


}
