/**
 * DatatakeUtil.java
 *
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr.rest.model;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.IngestorMessage;
import de.dlr.proseo.model.DataSegment;
import de.dlr.proseo.model.Datatake;
import de.dlr.proseo.model.GroundStation;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Utility methods for datatakes, e. g. for conversion between prosEO model and REST model
 *
 * @author Dr. Thomas Bassler
 */
public class DatatakeUtil {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DatatakeUtil.class);

	/**
	 * Converts a datatake in database format to a REST interface datatake representation
	 * 
	 * @param modelDatatake the datatake in database format
	 * @return a REST interface representation of a datatake
	 * @throws DateTimeException if an error occurs during formatting of any of the timestamps
	 */
	public static RestDatatake toRestDatatake(Datatake modelDatatake) throws DateTimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestDatatake({})",
				(null == modelDatatake ? "null" : modelDatatake.getDatatakeId()));
		
		if (null == modelDatatake)
			return null;
		
		RestDatatake restDatatake = new RestDatatake();
		restDatatake.setId(modelDatatake.getId());
		restDatatake.setVersion(Long.valueOf(modelDatatake.getVersion()));
		
		Spacecraft spacecraft = modelDatatake.getSpacecraft();
		restDatatake.setMissionCode(spacecraft.getMission().getCode());
		restDatatake.setSpacecraftCode(spacecraft.getCode());
		
		restDatatake.setPayloadName(modelDatatake.getPayload().getName());
		
		if (null != modelDatatake.getOrbit()) {
			restDatatake.setOrbitNumber(Long.valueOf(modelDatatake.getOrbit().getOrbitNumber()));
		}
		if (null != modelDatatake.getDatatakeGroup()) {
			restDatatake.setDtGroupIdentifier(modelDatatake.getDatatakeGroup().getDtGroupIdentifier());
		}
		
		restDatatake.setDatatakeId(modelDatatake.getDatatakeId());
		restDatatake.setInstrumentMode(modelDatatake.getInstrumentMode());
		restDatatake.setSensingStartTime(OrbitTimeFormatter.format(modelDatatake.getSensingStartTime()));
		restDatatake.setDatatakeDuration(modelDatatake.getDatatakeDuration().toMillis());
		restDatatake.setIsComplete(modelDatatake.isComplete());
		
		if (null != modelDatatake.getDeliveryStopTime()) {
			restDatatake.setDeliveryStopTime(OrbitTimeFormatter.format(modelDatatake.getDeliveryStopTime()));
		}
		if (null != modelDatatake.getProcessingTime()) {
			restDatatake.setProcessingTime(OrbitTimeFormatter.format(modelDatatake.getProcessingTime()));
		}
		
		Map<String, Parameter> modelParameters = modelDatatake.getParameters();
		for (String key: modelParameters.keySet()) {
			RestParameter restParameter = new RestParameter(key, modelParameters.get(key).getParameterType().toString(), 
					modelParameters.get(key).getParameterValue().toString());
			restDatatake.getParameters().add(restParameter);
		}
		
		for (DataSegment modelDataSegment: modelDatatake.getDataSegments()) {
			restDatatake.getDataSegments().add(DataSegmentUtil.toRestDataSegment(modelDataSegment));
		}
		
		return restDatatake;
	}

	
	/**
	 * Convert a REST datatake into a prosEO model datatake (scalar and embedded attributes only, no object references);
	 * embedded data segments are restored also, but also without object references.
	 *
	 * @param restDatatake the REST datatake
	 * @return a (roughly) equivalent model datatake or null, if no REST datatake was given
	 * @throws IllegalArgumentException if the REST datatake violates syntax rules for date, enum or numeric values
	 */
	public static Datatake toModelDatatake(RestDatatake restDatatake) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> to ModelDatatake({})", (null == restDatatake ? "null" : 
				restDatatake.getSpacecraftCode() + "/" + restDatatake.getDatatakeId()));
		
		if (null == restDatatake)
			return null;
		
		Datatake modelDatatake = new Datatake();
		
		if (null != restDatatake.getId() && 0 != restDatatake.getId()) {
			modelDatatake.setId(restDatatake.getId());
			while (modelDatatake.getVersion() < restDatatake.getVersion()) {
				modelDatatake.incrementVersion();
			}
		}
		
		modelDatatake.setDatatakeId(restDatatake.getDatatakeId());
		modelDatatake.setInstrumentMode(restDatatake.getInstrumentMode());
		modelDatatake.setSensingStartTime(Instant.from(OrbitTimeFormatter.parseDateTime(restDatatake.getSensingStartTime())));
		modelDatatake.setDatatakeDuration(Duration.ofMillis(restDatatake.getDatatakeDuration()));
		modelDatatake.setIsComplete(restDatatake.getIsComplete());
		
		if (null != restDatatake.getDeliveryStopTime()) {
			modelDatatake.setDeliveryStopTime(Instant.from(OrbitTimeFormatter.parseDateTime(restDatatake.getDeliveryStopTime())));
		}
		if (null != restDatatake.getProcessingTime()) {
			modelDatatake.setProcessingTime(Instant.from(OrbitTimeFormatter.parseDateTime(restDatatake.getProcessingTime())));
		}
		
		for (RestParameter restParameter: restDatatake.getParameters()) {
			Parameter modelParameter = new Parameter();
			try {
				modelParameter.init(ParameterType.valueOf(restParameter.getParameterType()), restParameter.getParameterValue());
			} catch (Exception e) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.INVALID_PARAMETER_TYPE, restParameter.getParameterType()));
			}

			modelDatatake.getParameters().put(restParameter.getKey(), modelParameter);
		}
		
		for (RestDataSegment restDataSegment: restDatatake.getDataSegments()) {
			DataSegment modelDataSegment = DataSegmentUtil.toModelDataSegment(restDataSegment);
			modelDataSegment.setDatatake(modelDatatake);
			modelDatatake.getDataSegments().add(modelDataSegment);
		}
		
		return modelDatatake;
	}
}
