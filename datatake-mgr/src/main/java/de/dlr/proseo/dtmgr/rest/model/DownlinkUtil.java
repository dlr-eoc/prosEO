/**
 * DownlinkUtil.java
 *
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr.rest.model;

import java.time.DateTimeException;
import java.time.Instant;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.DataSegment;
import de.dlr.proseo.model.Downlink;
import de.dlr.proseo.model.GroundStation;
import de.dlr.proseo.model.OrbitalPass;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Utility methods for downlinks, e. g. for conversion between prosEO model and REST model
 *
 * @author Dr. Thomas Bassler
 */
public class DownlinkUtil {
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DownlinkUtil.class);

	/**
	 * Converts a downlink in database format to a REST interface downlink representation
	 * 
	 * @param modelDownlink the downlink in database format
	 * @return a REST interface representation of a downlink
	 * @throws DateTimeException if an error occurs during formatting of any of the timestamps
	 */
	public static RestDownlink toRestDownlink(Downlink modelDownlink) throws DateTimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestDownlink({})",
				(null == modelDownlink ? "null" : modelDownlink.getDownlinkId()));
		
		if (null == modelDownlink)
			return null;
		
		RestDownlink restDownlink = new RestDownlink();
		
		restDownlink.setId(modelDownlink.getId());
		restDownlink.setVersion(Long.valueOf(modelDownlink.getVersion()));
		
		Spacecraft spacecraft = modelDownlink.getSpacecraft();
		restDownlink.setMissionCode(spacecraft.getMission().getCode());
		restDownlink.setSpacecraftCode(spacecraft.getCode());
		restDownlink.setDownlinkId(modelDownlink.getDownlinkId());
		
		for (OrbitalPass pass: modelDownlink.getPasses()) {
			RestOrbitalPass restPass = new RestOrbitalPass();
			
			restPass.setGroundStation(new RestGroundStation(
				pass.getGroundStation().getId(), Long.valueOf(pass.getGroundStation().getVersion()),
				pass.getGroundStation().getStationId(), pass.getGroundStation().getStationName()));
			
			if (null != pass.getAcquisitionStartTime()) {
				restPass.setAcquisitionStartTime(OrbitTimeFormatter.format(pass.getAcquisitionStartTime()));
			}
			if (null != pass.getAcquisitionStopTime()) {
				restPass.setAcquisitionStopTime(OrbitTimeFormatter.format(pass.getAcquisitionStopTime()));
			}
			if (null != pass.getDataStartTime()) {
				restPass.setDataStartTime(OrbitTimeFormatter.format(pass.getDataStartTime()));
			}
			if (null != pass.getDataStopTime()) {
				restPass.setDataStopTime(OrbitTimeFormatter.format(pass.getDataStopTime()));
			}
			if (null != pass.getPickupAvailabilityTime()) {
				restPass.setPickupAvailabilityTime(OrbitTimeFormatter.format(pass.getPickupAvailabilityTime()));
			}
			
			restDownlink.getPasses().add(restPass);
		}
		
		for (DataSegment modelDataSegment: modelDownlink.getDataSegments()) {
			restDownlink.getDataSegments().add(DataSegmentUtil.toRestDataSegment(modelDataSegment));
		}
		
		restDownlink.getChannels().addAll(modelDownlink.getChannels());
		restDownlink.setIsReceived(modelDownlink.isReceived());
		restDownlink.setReceivedSize(modelDownlink.getReceivedSize());
		
		if (null != modelDownlink.getReceptionTime()) {
			restDownlink.setReceptionTime(OrbitTimeFormatter.format(modelDownlink.getReceptionTime()));
		}

		return restDownlink;
	}
	
	/**
	 * Convert a REST dpwnlink into a prosEO model downlink (scalar and embedded attributes only, no object references
	 * except for data segments and [embedded] orbital passes; data segments in turn get no links to their datatakes)
	 * 
	 * @param restDownlink the REST downlink
	 * @return a (roughly) equivalent model downlink or null, if no REST datatake was given
	 * @throws IllegalArgumentException if the REST downlink violates syntax rules for date, enum or numeric values
	 */
	public static Downlink toModelDownlink(RestDownlink restDownlink) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelDownlink({})", (null == restDownlink ? "null" :
				restDownlink.getSpacecraftCode() + "/" + restDownlink.getDownlinkId()));
		
		if (null == restDownlink)
			return null;
		
		Downlink modelDownlink = new Downlink();
		
		if (null != restDownlink.getId() && 0 != restDownlink.getId()) {
			modelDownlink.setId(restDownlink.getId());
			while (modelDownlink.getVersion() < restDownlink.getVersion()) {
				modelDownlink.incrementVersion();
			}
		}
		
		modelDownlink.setDownlinkId(restDownlink.getDownlinkId());
		modelDownlink.getChannels().addAll(restDownlink.getChannels());
		
		for (RestOrbitalPass restPass: restDownlink.getPasses()) {
			OrbitalPass pass = new OrbitalPass();
			
			RestGroundStation restGroundStation = restPass.getGroundStation();
			GroundStation groundStation = new GroundStation();
			
			if (null != restGroundStation.getId() && 0 != restGroundStation.getId()) {
				modelDownlink.setId(restDownlink.getId());
				while (groundStation.getVersion() < restGroundStation.getVersion()) {
					groundStation.incrementVersion();
				}
			}
			groundStation.setStationId(restGroundStation.getStationId());
			groundStation.setStationName(restGroundStation.getStationName());
			
			pass.setGroundStation(groundStation);
			
			if (null != restPass.getAcquisitionStartTime()) {
				pass.setAcquisitionStartTime(Instant.from(OrbitTimeFormatter.parseDateTime(restPass.getAcquisitionStartTime())));
			}
			if (null != restPass.getAcquisitionStopTime()) {
				pass.setAcquisitionStopTime(Instant.from(OrbitTimeFormatter.parseDateTime(restPass.getAcquisitionStopTime())));
			}
			if (null != restPass.getDataStartTime()) {
				pass.setDataStartTime(Instant.from(OrbitTimeFormatter.parseDateTime(restPass.getDataStartTime())));
			}
			if (null != restPass.getDataStopTime()) {
				pass.setDataStopTime(Instant.from(OrbitTimeFormatter.parseDateTime(restPass.getDataStopTime())));
			}
			if (null != restPass.getPickupAvailabilityTime()) {
				pass.setPickupAvailabilityTime(Instant.from(OrbitTimeFormatter.parseDateTime(restPass.getPickupAvailabilityTime())));
			}
			
			modelDownlink.getPasses().add(pass);
		}
		
		for (RestDataSegment restDataSegment: restDownlink.getDataSegments()) {
			modelDownlink.getDataSegments().add(DataSegmentUtil.toModelDataSegment(restDataSegment));
		}
		
		modelDownlink.setIsReceived(restDownlink.getIsReceived());
		modelDownlink.setReceivedSize(restDownlink.getReceivedSize());
		
		if (null != restDownlink.getReceptionTime()) {
			modelDownlink.setReceptionTime(Instant.from(OrbitTimeFormatter.parseDateTime(restDownlink.getReceptionTime())));
		}
		
		return modelDownlink;
	}

}
