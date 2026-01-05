/**
 * TriggerUtil.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.util;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.quartz.CronExpression;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.OrderGenMessage;
import de.dlr.proseo.model.CalendarOrderTrigger;
import de.dlr.proseo.model.DataDrivenOrderTrigger;
import de.dlr.proseo.model.DatatakeOrderTrigger;
import de.dlr.proseo.model.OrbitOrderTrigger;
import de.dlr.proseo.model.OrderTrigger;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.TimeIntervalOrderTrigger;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.enums.TriggerType;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestTrigger;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Utility class to work with a trigger
 * 
 * @author Ernst Melchinger
 * 
 */
public class TriggerUtil {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(TriggerUtil.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/**
	 * Convert a prosEO model OrderTrigger into a REST Trigger
	 * 
	 * @param trigger the prosEO model trigger
	 * @return an equivalent REST Trigger or null, if no model Trigger was given
	 */
	public static RestTrigger toRestTrigger(OrderTrigger trigger) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestTrigger({})", (null == trigger ? "MISSING" : trigger.getId()));
		
		if (trigger == null) {
			return null;
		}
		
		RestTrigger restTrigger = new RestTrigger();
		restTrigger.setId(trigger.getId());
		restTrigger.setVersion(Long.valueOf(trigger.getVersion()));
		if (null != trigger.getMission()) {
			restTrigger.setMissionCode(trigger.getMission().getCode());
		}	
		if (null != trigger.getName()) {
			restTrigger.setName(trigger.getName());
		}	
		if (null != trigger.getPriority()) {
			restTrigger.setPriority(trigger.getPriority().longValue());
		}	
		if (null != trigger.getWorkflow()) {
			restTrigger.setWorkflowName(trigger.getWorkflow().getName());
		}	
		if (null != trigger.getExecutionDelay()) {
			restTrigger.setExecutionDelay(trigger.getExecutionDelay().getSeconds());
		}	
		if (trigger instanceof DataDrivenOrderTrigger) {
			DataDrivenOrderTrigger triggerLoc = (DataDrivenOrderTrigger) trigger;
			restTrigger.setType(TriggerType.DataDriven.name());
			if (null != triggerLoc.getParametersToCopy()) {
				restTrigger.getParametersToCopy().addAll(triggerLoc.getParametersToCopy());
			}
		} else if (trigger instanceof TimeIntervalOrderTrigger) {
			TimeIntervalOrderTrigger triggerLoc = (TimeIntervalOrderTrigger) trigger;
			restTrigger.setType(TriggerType.TimeInterval.name());
			if (null != triggerLoc.getTriggerInterval()) {
				restTrigger.setTriggerInterval(triggerLoc.getTriggerInterval().getSeconds());
			}
			if (null != triggerLoc.getNextTriggerTime()) {
				restTrigger.setNextTriggerTime(OrbitTimeFormatter.format(triggerLoc.getNextTriggerTime()));
			}	
		} else if (trigger instanceof CalendarOrderTrigger) {
			CalendarOrderTrigger triggerLoc = (CalendarOrderTrigger) trigger;
			restTrigger.setType(TriggerType.Calendar.name());
			if (null != triggerLoc.getCronExpression()) {
				restTrigger.setCronExpression(triggerLoc.getCronExpression());
			}	
		} else if (trigger instanceof OrbitOrderTrigger) {
			OrbitOrderTrigger triggerLoc = (OrbitOrderTrigger) trigger;
			restTrigger.setType(TriggerType.Orbit.name());
			if (null != triggerLoc.getSpacecraft()) {
				restTrigger.setSpacecraftCode(triggerLoc.getSpacecraft().getCode());
			}	
			if (null != triggerLoc.getDeltaTime()) {
				restTrigger.setDeltaTime(triggerLoc.getDeltaTime().getSeconds());
			}	
		} else if (trigger instanceof DatatakeOrderTrigger) {
			DatatakeOrderTrigger triggerLoc = (DatatakeOrderTrigger) trigger;
			restTrigger.setType(TriggerType.Datatake.name());
			if (null != triggerLoc.getDatatakeType()) {
				restTrigger.setDatatakeType(triggerLoc.getDatatakeType());
			}	
			if (null != triggerLoc.getLastDatatakeStartTime()) {
				restTrigger.setLastDatatakeStartTime(OrbitTimeFormatter.format(triggerLoc.getLastDatatakeStartTime()));
			}	
			if (null != triggerLoc.getParametersToCopy()) {
				restTrigger.getParametersToCopy().addAll(triggerLoc.getParametersToCopy());
			}
			if (null != triggerLoc.getDeltaTime()) {
				restTrigger.setDeltaTime(triggerLoc.getDeltaTime().getSeconds());
			}	
		}
		return restTrigger;
	}
	
	/**
	 * Convert a REST trigger into a prosEO model OrderTrigger
	 * 
	 * @param restTrigger the REST trigger
	 * @return a (roughly) equivalent model trigger
	 * @throws IllegalArgumentException if the REST trigger violates syntax rules for date, enum or numeric values
	 */
	public static OrderTrigger toModelTrigger(RestTrigger restTrigger) throws IllegalArgumentException {		
		if (logger.isTraceEnabled()) logger.trace(">>> toModelTrigger({})", (null == restTrigger ? "MISSING" : restTrigger.getId()));
		
		if (restTrigger == null || restTrigger.getType() == null) {
			return null;
		}
		OrderTrigger trigger = null;

		if (restTrigger.getType().equals(TriggerType.DataDriven.name())) {
			trigger = new DataDrivenOrderTrigger();
		} else if (restTrigger.getType().equals(TriggerType.Calendar.name())) {
			trigger = new CalendarOrderTrigger();
		} else if (restTrigger.getType().equals(TriggerType.Datatake.name())) {
			trigger = new DatatakeOrderTrigger();
		} else if (restTrigger.getType().equals(TriggerType.Orbit.name())) {
			trigger = new OrbitOrderTrigger();
		} else if (restTrigger.getType().equals(TriggerType.TimeInterval.name())) {
			trigger = new TimeIntervalOrderTrigger();
		}
		
		if (null != restTrigger.getId() && 0 != restTrigger.getId()) {
			trigger.setId(restTrigger.getId());
			while (trigger.getVersion() < restTrigger.getVersion()) {
				trigger.incrementVersion();
			} 
		}
		if (!StringUtils.isNullOrEmpty(restTrigger.getMissionCode())) {
			trigger.setMission(RepositoryService.getMissionRepository().findByCode(restTrigger.getMissionCode()));
		}		
		if (!StringUtils.isNullOrEmpty(restTrigger.getWorkflowName()) && !StringUtils.isNullOrEmpty(restTrigger.getMissionCode())) {
			List<Workflow> wfl = RepositoryService.getWorkflowRepository().findByMissionCodeAndName(restTrigger.getMissionCode(), restTrigger.getWorkflowName());
			if (wfl.size() > 0) {
				trigger.setWorkflow(wfl.get(0));
			}
		}		
		if (!StringUtils.isNullOrEmpty(restTrigger.getName())) {
			trigger.setName(restTrigger.getName());
		}
		if (null != restTrigger.getPriority()) {
			trigger.setPriority(restTrigger.getPriority().intValue());
		}
		if (null != restTrigger.getExecutionDelay()) {
			trigger.setExecutionDelay(Duration.ofSeconds(restTrigger.getExecutionDelay()));
		}

		if (restTrigger.getType().equals(TriggerType.DataDriven.name())) {
			if (null != restTrigger.getParametersToCopy()) {
				((DataDrivenOrderTrigger)trigger).getParametersToCopy().addAll(restTrigger.getParametersToCopy());
			}
		} else if (restTrigger.getType().equals(TriggerType.Calendar.name())) {
			if (!StringUtils.isNullOrEmpty(restTrigger.getCronExpression())) {
				((CalendarOrderTrigger)trigger).setCronExpression(restTrigger.getCronExpression());
			}
		} else if (restTrigger.getType().equals(TriggerType.Datatake.name())) {
			if (null != restTrigger.getDeltaTime()) {
				((DatatakeOrderTrigger)trigger).setDeltaTime(Duration.ofSeconds(restTrigger.getDeltaTime()));
			}
			if (null != restTrigger.getParametersToCopy()) {
				((DatatakeOrderTrigger)trigger).getParametersToCopy().addAll(restTrigger.getParametersToCopy());
			}
			if (!StringUtils.isNullOrEmpty(restTrigger.getDatatakeType())) {
				((DatatakeOrderTrigger)trigger).setDatatakeType(restTrigger.getDatatakeType());
			}
			if (!StringUtils.isNullOrEmpty(restTrigger.getLastDatatakeStartTime())) {
				((DatatakeOrderTrigger)trigger).setLastDatatakeStartTime(Instant.from(OrbitTimeFormatter.parse(restTrigger.getLastDatatakeStartTime())));
			}
		} else if (restTrigger.getType().equals(TriggerType.Orbit.name())) {
			if (null != restTrigger.getDeltaTime()) {
				((OrbitOrderTrigger)trigger).setDeltaTime(Duration.ofSeconds(restTrigger.getDeltaTime()));
			}
			if (!StringUtils.isNullOrEmpty(restTrigger.getSpacecraftCode())) {
				((OrbitOrderTrigger)trigger).setSpacecraft(RepositoryService.getSpacecraftRepository().findByMissionAndCode(restTrigger.getMissionCode(), restTrigger.getSpacecraftCode()));
			}
		} else if (restTrigger.getType().equals(TriggerType.TimeInterval.name())) {
			if (null != restTrigger.getTriggerInterval()) {
				((TimeIntervalOrderTrigger)trigger).setTriggerInterval(Duration.ofSeconds(restTrigger.getTriggerInterval()));
			}
			if (!StringUtils.isNullOrEmpty(restTrigger.getNextTriggerTime())) {
				((TimeIntervalOrderTrigger)trigger).setNextTriggerTime(Instant.from(OrbitTimeFormatter.parse(restTrigger.getNextTriggerTime())));
			}
		}
				
		return trigger;
	}
	
	public static OrderTrigger findByMissionCodeAndTriggerNameAndType(String misssionCode, String name, String typeString) {
		if (logger.isTraceEnabled()) logger.trace(">>> findByMissionCodeAndTriggerNameAndType({}, {}, {})", misssionCode, name, typeString);

		TriggerType type = TriggerType.valueOf(typeString);

		switch (type) {
		case Calendar:
			return RepositoryService.getCalendarOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name);
		case DataDriven:
			return RepositoryService.getDataDrivenOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name);
		case Datatake:
			return RepositoryService.getDatatakeOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name);
		case Orbit:
			return RepositoryService.getOrbitOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name);
		case TimeInterval:
			return RepositoryService.getTimeIntervalOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name);
		default:
			// error
			
			return null;
		}
	}


	public static OrderTrigger findOneByMissionCodeAndTriggerNameAndType(String misssionCode, String name, String typeString) {
		if (logger.isTraceEnabled()) logger.trace(">>> findOneByMissionCodeAndTriggerNameAndType({}, {}, {})", misssionCode, name, typeString);

		if (misssionCode == null || misssionCode.isEmpty()) {
			logger.log(OrderGenMessage.TRIGGER_MISSION_MISSING);
			return null;
		}
		if (typeString == null || typeString.isEmpty()) {
			logger.log(OrderGenMessage.TRIGGER_TYPE_MISSING);
			return null;
		}
		if (name == null || name.isEmpty()) {
			logger.log(OrderGenMessage.TRIGGER_NAME_MISSING);
			return null;
		}
		List<OrderTrigger> triggers = TriggerUtil.findAllByMissionCodeAndTriggerNameAndType(misssionCode, name, typeString);
		if (triggers.isEmpty()) {
			return null;
		} else {
			return triggers.get(0);
		}
	}
	
	public static List<OrderTrigger> findAllByMissionCodeAndTriggerNameAndType(String misssionCode, String name, String typeString) {
		if (logger.isTraceEnabled()) logger.trace(">>> findAllByMissionCodeAndTriggerNameAndType({}, {}, {})", misssionCode, name, typeString);
		TriggerType type = null;
		if (typeString != null) {
			type = TriggerType.valueOf(typeString);
		}
		List<OrderTrigger> triggers = new ArrayList<OrderTrigger>();
		if (type == null) {
			if (name == null || name.isEmpty()) {
				triggers.addAll(RepositoryService.getCalendarOrderTriggerRepository().findByMissionCode(misssionCode));
				triggers.addAll(RepositoryService.getDataDrivenOrderTriggerRepository().findByMissionCode(misssionCode));
				triggers.addAll(RepositoryService.getDatatakeOrderTriggerRepository().findByMissionCode(misssionCode));
				triggers.addAll(RepositoryService.getOrbitOrderTriggerRepository().findByMissionCode(misssionCode));
				triggers.addAll(RepositoryService.getTimeIntervalOrderTriggerRepository().findByMissionCode(misssionCode));
			} else {
				triggers.add(RepositoryService.getCalendarOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
				triggers.add(RepositoryService.getDataDrivenOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
				triggers.add(RepositoryService.getDatatakeOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
				triggers.add(RepositoryService.getOrbitOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
				triggers.add(RepositoryService.getTimeIntervalOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
			}
		} else {
			if (name == null || name.isEmpty()) {
				switch (type) {
				case Calendar:
					triggers.addAll(RepositoryService.getCalendarOrderTriggerRepository().findByMissionCode(misssionCode));
					break;
				case DataDriven:
					triggers.addAll(RepositoryService.getDataDrivenOrderTriggerRepository().findByMissionCode(misssionCode));
					break;
				case Datatake:
					triggers.addAll(RepositoryService.getDatatakeOrderTriggerRepository().findByMissionCode(misssionCode));
					break;
				case Orbit:
					triggers.addAll(RepositoryService.getOrbitOrderTriggerRepository().findByMissionCode(misssionCode));
					break;
				case TimeInterval:
					triggers.addAll(RepositoryService.getTimeIntervalOrderTriggerRepository().findByMissionCode(misssionCode));
					break;
				default:
					break;
				}
			} else {
				switch (type) {
				case Calendar:
					triggers.add(RepositoryService.getCalendarOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
					break;
				case DataDriven:
					triggers.add(RepositoryService.getDataDrivenOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
					break;
				case Datatake:
					triggers.add(RepositoryService.getDatatakeOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
					break;
				case Orbit:
					triggers.add(RepositoryService.getOrbitOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
					break;
				case TimeInterval:
					triggers.add(RepositoryService.getTimeIntervalOrderTriggerRepository().findByMissionCodeAndName(misssionCode, name));
					break;
				default:
					break;
				}
			}
		}
		return triggers;
	}
	
	public static void delete(OrderTrigger trigger, String typeString) {

		TriggerType type = TriggerType.valueOf(typeString);

		switch (type) {
		case Calendar:
			RepositoryService.getCalendarOrderTriggerRepository().delete((CalendarOrderTrigger)trigger);
			break;
		case DataDriven:
			RepositoryService.getDataDrivenOrderTriggerRepository().delete((DataDrivenOrderTrigger)trigger);
			break;
		case Datatake:
			RepositoryService.getDatatakeOrderTriggerRepository().delete((DatatakeOrderTrigger)trigger);
			break;
		case Orbit:
			RepositoryService.getOrbitOrderTriggerRepository().delete((OrbitOrderTrigger)trigger);
			break;
		case TimeInterval:
			RepositoryService.getTimeIntervalOrderTriggerRepository().delete((TimeIntervalOrderTrigger)trigger);
			break;
		default:
			break;
		}
	}
	
	public static OrderTrigger save(OrderTrigger trigger) {
		if (logger.isTraceEnabled()) logger.trace(">>> save({})", trigger.getName());

		if (trigger instanceof CalendarOrderTrigger) {
			CalendarOrderTrigger triggerLoc = (CalendarOrderTrigger) trigger;
			return RepositoryService.getCalendarOrderTriggerRepository().save(triggerLoc);
		} else if (trigger instanceof DataDrivenOrderTrigger) {
			DataDrivenOrderTrigger triggerLoc = (DataDrivenOrderTrigger) trigger;
			return RepositoryService.getDataDrivenOrderTriggerRepository().save(triggerLoc);
		} else if (trigger instanceof DatatakeOrderTrigger) {
			DatatakeOrderTrigger triggerLoc = (DatatakeOrderTrigger) trigger;
			return RepositoryService.getDatatakeOrderTriggerRepository().save(triggerLoc);
		} else if (trigger instanceof OrbitOrderTrigger) {
			OrbitOrderTrigger triggerLoc = (OrbitOrderTrigger) trigger;
			return RepositoryService.getOrbitOrderTriggerRepository().save(triggerLoc);
		} else if (trigger instanceof TimeIntervalOrderTrigger) {
			TimeIntervalOrderTrigger triggerLoc = (TimeIntervalOrderTrigger) trigger;
			return RepositoryService.getTimeIntervalOrderTriggerRepository().save(triggerLoc);
		}
		return null;
	}

	public static OrderTrigger check(OrderTrigger modelTrigger) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> check({})", modelTrigger.getName());
		
		if (modelTrigger != null) {
			if (modelTrigger instanceof CalendarOrderTrigger) {
				CalendarOrderTrigger trigger = (CalendarOrderTrigger) modelTrigger;
				try {
					new CronExpression(trigger.getCronExpression());
				} catch (ParseException e) {
					throw new IllegalArgumentException(logger.log(OrderGenMessage.INVALID_CRON_EXPRESSION, trigger.getCronExpression(),
							trigger.getName(), TriggerType.Calendar));
				}
			} else if (modelTrigger instanceof DataDrivenOrderTrigger) {
				// nothing to check
			} else if (modelTrigger instanceof DatatakeOrderTrigger) {
			} else if (modelTrigger instanceof OrbitOrderTrigger) {
				OrbitOrderTrigger trigger = (OrbitOrderTrigger) modelTrigger;
				if (trigger.getSpacecraft() == null) {
					throw new IllegalArgumentException(logger.log(OrderGenMessage.SPACECRAFT_NOT_SET,
							trigger.getName(), TriggerType.Orbit));
				}
			} else if (modelTrigger instanceof TimeIntervalOrderTrigger) {
				TimeIntervalOrderTrigger trigger = (TimeIntervalOrderTrigger) modelTrigger;
				if (trigger.getTriggerInterval() == null) {
					throw new IllegalArgumentException(logger.log(OrderGenMessage.INTERVAL_NOT_SET,
							trigger.getName(), TriggerType.TimeInterval));
				}
			}
		}
		return modelTrigger;
	}
	
}
