package de.dlr.proseo.ordermgr.rest.model;

import java.time.Duration;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Payload;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.rest.model.RestPayload;
import de.dlr.proseo.model.rest.model.RestSpacecraft;

public class MissionUtil {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(MissionUtil.class);

	/**
	 * Convert a prosEO model Mission into a REST Mission
	 * 
	 * @param modelMission
	 *            the prosEO model mission
	 * @return an equivalent REST mission or null, if no model mission was given
	 */
	public static RestMission toRestMission(Mission modelMission) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestMission({})", (null == modelMission ? "MISSING" : modelMission.getId()));

		if (null == modelMission)
			return null;

		RestMission restMission = new RestMission();

		restMission.setId(modelMission.getId());
		restMission.setVersion(Long.valueOf(modelMission.getVersion()));

		if (null != modelMission.getCode()) {
			restMission.setCode(modelMission.getCode());
		}
		if (null != modelMission.getName()) {
			restMission.setName(modelMission.getName());
		}
		if (null != modelMission.getProductFileTemplate()) {
			restMission.setProductFileTemplate(modelMission.getProductFileTemplate());
		}
		if (null != modelMission.getProcessingCentre()) {
			restMission.setProcessingCentre(modelMission.getProcessingCentre());
		}
		if (null != modelMission.getProductRetentionPeriod()) {
			restMission.setProductRetentionPeriod(modelMission.getProductRetentionPeriod().getSeconds());
		}

		if (null != modelMission.getFileClasses()) {
			restMission.getFileClasses().addAll(modelMission.getFileClasses());
		}
		if (null != modelMission.getProcessingModes()) {
			restMission.getProcessingModes().addAll(modelMission.getProcessingModes());
		}

		if (null != modelMission.getSpacecrafts()) {
			List<RestSpacecraft> restSpacecrafts = new ArrayList<RestSpacecraft>();

			for (Spacecraft modelSpacecraft : modelMission.getSpacecrafts()) {
				List<RestPayload> restPayloads = null;
				if (!modelSpacecraft.getPayloads().isEmpty()) {
					restPayloads = new ArrayList<>();
					for (Payload payload : modelSpacecraft.getPayloads()) {
						RestPayload restPayload = new RestPayload(payload.getName(), payload.getDescription());
						restPayloads.add(restPayload);
					}
				}
				RestSpacecraft restFinal = new RestSpacecraft(modelSpacecraft.getId(), Long.valueOf(modelSpacecraft.getVersion()),
						modelSpacecraft.getCode(), modelSpacecraft.getName(), restPayloads);
				restSpacecrafts.add(restFinal);
			}
			restMission.setSpacecrafts(restSpacecrafts);
		}

		return restMission;
	}

	/**
	 * Convert a REST mission into a prosEO model mission (scalar and embedded
	 * attributes only, no mission references)
	 * 
	 * @param restMission
	 *            the REST mission
	 * @return a (roughly) equivalent model mission
	 * @throws IllegalArgumentException
	 *             if the REST mission violates syntax rules for date, enum or
	 *             numeric values
	 */

	public static Mission toModelMission(RestMission restMission) throws IllegalArgumentException {

		if (logger.isTraceEnabled()) logger.trace(">>> toModelMission({})", (null == restMission ? "MISSING" : restMission.getId()));

		Mission modelMission = new Mission();

		if (null != restMission.getId() && 0 != restMission.getId()) {
			modelMission.setId(restMission.getId());
			while (modelMission.getVersion() < restMission.getVersion()) {
				modelMission.incrementVersion();
			}
		}
		modelMission.setCode(restMission.getCode());
		modelMission.setName(restMission.getName());
		modelMission.setProductFileTemplate(restMission.getProductFileTemplate());
		
		if (null != restMission.getProcessingCentre()) {
			modelMission.setProcessingCentre(restMission.getProcessingCentre());
		}
		if (null != restMission.getProductRetentionPeriod()) {
			modelMission.setProductRetentionPeriod(Duration.ofSeconds(restMission.getProductRetentionPeriod()));
		}

		modelMission.getFileClasses().addAll(restMission.getFileClasses());
		modelMission.getProcessingModes().addAll(restMission.getProcessingModes());

		return modelMission;
	}

}
