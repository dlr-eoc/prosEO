/**
 * DataSegmentUtil.java
 *
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr.rest.model;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.DataSegment;
import de.dlr.proseo.model.GroundStation;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Utility methods for data segments, e. g. for conversion between prosEO model and REST model
 *
 * @author Dr. Thomas Bassler
 */
public class DataSegmentUtil {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DataSegmentUtil.class);

	/**
	 * Converts a data segment in database format to a REST interface data segment representation
	 * 
	 * @param modelDataSegment the data segment in database format
	 * @return a REST interface representation of a data segment
	 * @throws DateTimeException if an error occurs during formatting of any of the timestamps
	 */
	public static RestDataSegment toRestDataSegment(DataSegment modelDataSegment) throws DateTimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestDataSegment({})", (null == modelDataSegment ? "null" : 
			modelDataSegment.getDatatake().getDatatakeId() + "/" + modelDataSegment.getSegmentNumber()));
		
		if (null == modelDataSegment)
			return null;
		
		RestDataSegment restDataSegment = new RestDataSegment();
		
		restDataSegment.setId(modelDataSegment.getId());
		restDataSegment.setVersion(Long.valueOf(modelDataSegment.getVersion()));
		restDataSegment.setMissionCode(modelDataSegment.getDatatake().getSpacecraft().getMission().getCode());
		restDataSegment.setSpacecraftCode(modelDataSegment.getDatatake().getSpacecraft().getCode());
		restDataSegment.setPayloadName(modelDataSegment.getDatatake().getPayload().getName());
		restDataSegment.setDatatakeId(modelDataSegment.getDatatake().getDatatakeId());
		restDataSegment.setSegmentNumber(Long.valueOf(modelDataSegment.getSegmentNumber()));
		
		if (null != modelDataSegment.getDownlink()) {
			restDataSegment.setDownlinkId(modelDataSegment.getDownlink().getDownlinkId());
		}
		
		for (GroundStation groundStation: modelDataSegment.getGroundStations()) {
			RestGroundStation restGroundStation = new RestGroundStation(
					groundStation.getId(), Long.valueOf(groundStation.getVersion()),
					groundStation.getStationId(), groundStation.getStationName());
			restDataSegment.getGroundStations().add(restGroundStation);
		}
		
		restDataSegment.setSensingStartTime(OrbitTimeFormatter.format(modelDataSegment.getSensingStartTime()));
		restDataSegment.setSegmentDuration(modelDataSegment.getSegmentDuration().toMillis());
		if (null != modelDataSegment.getDownlinkStart()) {
			restDataSegment.setDownlinkStart(OrbitTimeFormatter.format(modelDataSegment.getDownlinkStart()));
		}
		if (null != modelDataSegment.getDownlinkStop()) {
			restDataSegment.setDownlinkStop(OrbitTimeFormatter.format(modelDataSegment.getDownlinkStop()));
		}
		restDataSegment.setChannel(modelDataSegment.getChannel());
		
		return restDataSegment;
	}
	
	/**
	 * Convert a REST datatake into a prosEO model datatake (scalar and embedded attributes only, no object references);
	 * embedded data segments are restored also, but also without object references.
	 *
	 * @param restDataSegment the REST datatake
	 * @return a (roughly) equivalent model datatake or null, if no REST datatake was given
	 * @throws IllegalArgumentException if the REST datatake violates syntax rules for date, enum or numeric values
	 */
	public static DataSegment toModelDataSegment(RestDataSegment restDataSegment) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> to toModelDataSegment({})", (null == restDataSegment ? "null" : 
				restDataSegment.getSpacecraftCode() + "/" + restDataSegment.getDatatakeId()
				+ "/" + restDataSegment.getSegmentNumber()));
		
		if (null == restDataSegment)
			return null;
		
		DataSegment modelDataSegment = new DataSegment();
		
		if (null != restDataSegment.getId() && 0 != restDataSegment.getId()) {
			modelDataSegment.setId(restDataSegment.getId());
			while (modelDataSegment.getVersion() < restDataSegment.getVersion()) {
				modelDataSegment.incrementVersion();
			}
		}
		
		modelDataSegment.setSegmentNumber(restDataSegment.getSegmentNumber().intValue());
		modelDataSegment.setSensingStartTime(Instant.from(
				OrbitTimeFormatter.parseDateTime(restDataSegment.getSensingStartTime())));
		modelDataSegment.setSegmentDuration(Duration.ofMillis(restDataSegment.getSegmentDuration()));
		
		if (null != restDataSegment.getDownlinkStart()) {
			modelDataSegment.setDownlinkStart(Instant.from(
					OrbitTimeFormatter.parseDateTime(restDataSegment.getDownlinkStart())));
		}
		if (null != restDataSegment.getDownlinkStop()) {
			modelDataSegment.setDownlinkStop(Instant.from(
					OrbitTimeFormatter.parseDateTime(restDataSegment.getDownlinkStop())));
		}
		
		modelDataSegment.setChannel(restDataSegment.getChannel());
		
		return modelDataSegment;
	}
}
