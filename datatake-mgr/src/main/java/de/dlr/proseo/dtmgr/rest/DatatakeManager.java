/**
 * DatatakeManager.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr.rest;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import de.dlr.proseo.model.DataSegment;
import de.dlr.proseo.model.Datatake;
import de.dlr.proseo.model.GroundStation;
import de.dlr.proseo.model.dao.DatatakeRepository;
import de.dlr.proseo.dtmgr.rest.model.RestDataSegment;
import de.dlr.proseo.dtmgr.rest.model.RestDatatake;
import de.dlr.proseo.dtmgr.rest.model.RestGroundStation;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.dao.SpacecraftRepository;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import jakarta.persistence.EntityNotFoundException;

/**
 * Service methods to query the database about datatakes and to mark data segments as received
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class DatatakeManager {

	/* Message ID constants */
	private static final int MSG_ID_BOUNDS_MISMATCH = 8100;
	private static final int MSG_ID_SPACECRAFT_NOT_FOUND = 8101;
	private static final int MSG_ID_DATATAKE_NOT_FOUND = 8103;
	private static final int MSG_ID_DATATAKES_RETRIEVED = 8105;
	private static final int MSG_ID_DATATAKE_PROCESSED = 8108;
	private static final int MSG_ID_DATATAKE_ALREADY_PROCESSED = 8109;

	/* Message string constants */
	private static final String MSG_BOUNDS_MISMATCH = "(E%d) Upper record number %d is below lower record number %d";
	private static final String MSG_SPACECRAFT_NOT_FOUND = "(E%d) Spacecraft %s not found in mission %s";
	private static final String MSG_DATATAKE_NOT_FOUND = "(E%d) Datatake with database ID %d not found for spacecraft %s and mission %s";
	
	private static final String MSG_DATATAKE_ALREADY_PROCESSED = "(W%d) Datatake %s for spacecraft %s has already been processed";

	private static final String MSG_DATATAKES_RETRIEVED = "(I%d) %d datatakes retrieved for spacecraft %s";
	private static final String MSG_DATATAKE_PROCESSED = "(I%d) Datatake %s for spacecraft %s marked as processed";
	
	/** Repository for spacecrafts */
	@Autowired
	private SpacecraftRepository spacecraftRepo;

	/** Repository for datatakes */
	@Autowired
	private DatatakeRepository datatakeRepo;

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DatatakeManager.class);
	
	/**
	 * Create and log a formatted message at the given level
	 * 
	 * @param level the logging level to use
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String log(Level level, String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		if (Level.ERROR.equals(level)) {
			logger.error(message);
		} else if (Level.WARN.equals(level)) {
			logger.warn(message);
		} else {
			logger.info(message);
		}

		return message;
	}

	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private String logError(String messageFormat, int messageId, Object... messageParameters) {
		return log(Level.ERROR, messageFormat, messageId, messageParameters);
	}
	
	/**
	 * Query the database for datatakes matching the given selection criteria
	 * 
	 * @param spacecraftCode the spacecraft code (optional)
	 * @param payload the spacecraft payload name (optional)
	 * @param datatakeId the (external) datatake ID (optional)
	 * @param sensingStartTimeFrom earliest sensing start time (optional)
	 * @param sensingStartTimeTo latest sensing start time (optional)
	 * @param recordFrom first record of filtered and ordered result to return (optional, default first)
	 * @param recordTo last record of filtered and ordered result to return (optional, default last)
	 * @param orderBy array of strings containing a column name and an optional sort direction (ASC/DESC), may be empty
	 * @return a list of datatakes ordered as requested, reduced to the record range requested
	 */
	private List<Datatake> selectDatatakes(String spacecraftCode, String payload, String datatakeId, Instant sensingStartTimeFrom,
			Instant sensingStartTimeTo, Long recordFrom, Long recordTo, String[] orderBy) {
		
		// TODO Build and execute database query, see Order Manager "/select"
		
		return null;
	}

	/**
	 * Converts a datatake in database format to a REST interface datatake representation
	 * 
	 * @param spacecraft the spacecraft the datatake belongs to
	 * @param modelDatatake the datatake in database format
	 * @return a REST interface representation of a datatake
	 * @throws DateTimeException if an error occurs during formatting of any of the timestamps
	 */
	public RestDatatake toRestDatatake(Spacecraft spacecraft, Datatake modelDatatake) throws DateTimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestDatatake({}, {})", (null == spacecraft ? "null" : spacecraft.getCode()),
				(null == modelDatatake ? "null" : modelDatatake.getDatatakeId()));
		
		RestDatatake restDatatake = new RestDatatake();
		restDatatake.setId(modelDatatake.getId());
		restDatatake.setVersion(Long.valueOf(modelDatatake.getVersion()));
		restDatatake.setMissionCode(spacecraft.getMission().getCode());
		restDatatake.setSpacecraftCode(spacecraft.getCode());
		restDatatake.setDatatakeId(modelDatatake.getDatatakeId());
		restDatatake.setSensingStartTime(OrbitTimeFormatter.format(modelDatatake.getSensingStartTime()));
		restDatatake.setDatatakeDuration(modelDatatake.getDatatakeDuration().toMillis());
		restDatatake.setInstrumentMode(modelDatatake.getInstrumentMode());
		restDatatake.setIsComplete(modelDatatake.isComplete());
		if (null != modelDatatake.getProcessingTime()) {
			restDatatake.setProcessingTime(OrbitTimeFormatter.format(modelDatatake.getProcessingTime()));
		}
		if (null != modelDatatake.getDeliveryStopTime()) {
			restDatatake.setDeliveryStopTime(OrbitTimeFormatter.format(modelDatatake.getDeliveryStopTime()));
		}
		
		for (DataSegment modelDataSegment: modelDatatake.getDataSegments()) {
			RestDataSegment restDataSegment = new RestDataSegment();
			restDataSegment.setId(modelDataSegment.getId());
			restDataSegment.setVersion(Long.valueOf(modelDataSegment.getVersion()));
			restDataSegment.setSensingStartTime(OrbitTimeFormatter.format(modelDataSegment.getSensingStartTime()));
			restDataSegment.setSegmentDuration(modelDataSegment.getSegmentDuration().toMillis());
			if (null != modelDataSegment.getDownlinkStart()) {
				restDataSegment.setDownlinkStart(OrbitTimeFormatter.format(modelDataSegment.getDownlinkStart()));
			}
			if (null != modelDataSegment.getDownlinkStop()) {
				restDataSegment.setDownlinkStop(OrbitTimeFormatter.format(modelDataSegment.getDownlinkStop()));
			}
			restDataSegment.setChannel(modelDataSegment.getChannel());
			for (GroundStation groundStation: modelDataSegment.getGroundStations()) {
				RestGroundStation restGroundStation = new RestGroundStation(groundStation.getId(), Long.valueOf(groundStation.getVersion()),
						groundStation.getStationId(), groundStation.getStationName());
				restDataSegment.getGroundStations().add(restGroundStation);
			}
			
			restDatatake.getDataSegments().add(restDataSegment);
		}
		
		return restDatatake;
	}

	/**
	 * Get the datatakes in the database (optionally using a lower and/or upper bound of the datatake ID)
	 * 
	 * @param spacecraftCode the spacecraft code (optional)
	 * @param payload the spacecraft payload name (optional)
	 * @param datatakeId the (external) datatake ID (optional)
	 * @param startTimeFrom earliest sensing start time (optional)
	 * @param startTimeTo latest sensing start time (optional)
	 * @param recordFrom first record of filtered and ordered result to return (optional, default first)
	 * @param recordTo last record of filtered and ordered result to return (optional, default last)
	 * @param orderBy array of strings containing a column name and an optional sort direction (ASC/DESC), may be empty
	 * @return a list of datatakes ordered as requested, reduced to the record range requested
	 * @throws IllegalArgumentException if the last record number is below the first record number
	 * @throws EntityNotFoundException if no datatakes with the given search criteria can be found
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public List<RestDatatake> getRestDatatakes(String spacecraftCode, String payload, String datatakeId, String startTimeFrom, String startTimeTo, Long recordFrom, Long recordTo, String[] orderBy)
			throws IllegalArgumentException, EntityNotFoundException {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestDatatakes({}, {}, {}, {}, {}, {}, {}, {})", spacecraftCode, payload, datatakeId, startTimeFrom, startTimeTo, recordFrom, recordTo, orderBy);
		
		// Check arguments
		if (null == recordFrom)
			recordFrom = 0L;
		if (null != recordTo) {
			if (recordTo < recordFrom) {
				throw new IllegalArgumentException(logError(MSG_BOUNDS_MISMATCH, MSG_ID_BOUNDS_MISMATCH, recordTo, recordFrom));
			}
		}
		
		Instant sensingStartTimeFrom = null, sensingStartTimeTo = null;
		try {
			sensingStartTimeFrom = OrbitTimeFormatter.parseDateTime(startTimeFrom);
			sensingStartTimeTo = OrbitTimeFormatter.parseDateTime(startTimeTo);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("TODO"); // TODO
		}
		
		// Find the spacecraft
		Spacecraft spacecraft = spacecraftRepo.findByMissionAndCode(securityService.getMission(), spacecraftCode);
		if (null == spacecraft) {
			throw new EntityNotFoundException(logError(MSG_SPACECRAFT_NOT_FOUND, MSG_ID_SPACECRAFT_NOT_FOUND,
					spacecraftCode, securityService.getMission()));
		}
		
		// Get the datatake list
		List<Datatake> datatakes = selectDatatakes(spacecraftCode, payload, datatakeId, sensingStartTimeFrom, sensingStartTimeTo,
				recordFrom, recordTo, orderBy);
		
		// Convert the list to RestDatatakes
		List<RestDatatake> result = new ArrayList<>();
		
		for (Datatake modelDatatake: datatakes) {
			RestDatatake restDatatake = toRestDatatake(spacecraft, modelDatatake);
			
			result.add(restDatatake);
		}
		
		log(Level.INFO, MSG_DATATAKES_RETRIEVED, MSG_ID_DATATAKES_RETRIEVED, result.size(), spacecraftCode);
		
		return result;
		
	}

	/**
	 * Mark the datatake with the given datatake ID as processed (sets L0ASP processing time), assuming the mission
	 * code is set due to a user login
	 * 
	 * @param id the database ID of the datatake
	 * @return the updated datatake
	 * @throws EntityNotFoundException if the spacecraft code or the datatake ID cannot be found
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public RestDatatake markProcessed(Long id) throws EntityNotFoundException {
		if (logger.isTraceEnabled()) logger.trace(">>> markProcessed({})", id);
		
		// Get the datatake
		Optional<Datatake> datatake = datatakeRepo.findById(id);
		if (datatake.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_DATATAKE_NOT_FOUND, MSG_ID_DATATAKE_NOT_FOUND,
					id, datatake.get().getSpacecraft().getCode(), securityService.getMission()));
		}
		
		
		if (null == datatake.get().getProcessingTime()) {
			datatake.get().setProcessingTime(Instant.now());
			
			log(Level.INFO, MSG_DATATAKE_PROCESSED, MSG_ID_DATATAKE_PROCESSED,
					datatake.get().getDatatakeId(), datatake.get().getSpacecraft().getCode());
		} else {
			log(Level.WARN, MSG_DATATAKE_ALREADY_PROCESSED, MSG_ID_DATATAKE_ALREADY_PROCESSED,
					datatake.get().getDatatakeId(), datatake.get().getSpacecraft().getCode());
		}
		
		return toRestDatatake(datatake.get().getSpacecraft(), datatake.get());
	}
}
