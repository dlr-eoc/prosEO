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
import de.dlr.proseo.dtmgr.rest.model.DatatakeUtil;
import de.dlr.proseo.dtmgr.rest.model.RestDataSegment;
import de.dlr.proseo.dtmgr.rest.model.RestDatatake;
import de.dlr.proseo.dtmgr.rest.model.RestGroundStation;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.DatatakeMgrMessage;
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
	private static ProseoLogger logger = new ProseoLogger(DatatakeManager.class);
	
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
				throw new IllegalArgumentException(logger.log(DatatakeMgrMessage.BOUNDS_MISMATCH, recordTo, recordFrom));
			}
		}
		
		Instant sensingStartTimeFrom = null, sensingStartTimeTo = null;
		try {
			sensingStartTimeFrom = OrbitTimeFormatter.parseDateTime(startTimeFrom);
			sensingStartTimeTo = OrbitTimeFormatter.parseDateTime(startTimeTo);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException(logger.log(DatatakeMgrMessage.INVALID_START_TIMES,
					startTimeFrom, startTimeTo, datatakeId));
		}
		
		// Find the spacecraft
		Spacecraft spacecraft = spacecraftRepo.findByMissionAndCode(securityService.getMission(), spacecraftCode);
		if (null == spacecraft) {
			throw new EntityNotFoundException(logger.log(DatatakeMgrMessage.SPACECRAFT_NOT_FOUND,
					spacecraftCode, securityService.getMission()));
		}
		
		// Get the datatake list
		List<Datatake> datatakes = selectDatatakes(spacecraftCode, payload, datatakeId, sensingStartTimeFrom, sensingStartTimeTo,
				recordFrom, recordTo, orderBy);
		
		// Convert the list to RestDatatakes
		List<RestDatatake> result = new ArrayList<>();
		
		for (Datatake modelDatatake: datatakes) {
			RestDatatake restDatatake = DatatakeUtil.toRestDatatake(modelDatatake);
			
			result.add(restDatatake);
		}
		
		logger.log(DatatakeMgrMessage.DATATAKES_RETRIEVED, result.size(), spacecraftCode);
		
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
			throw new EntityNotFoundException(logger.log(DatatakeMgrMessage.DATATAKE_NOT_FOUND,
					id, datatake.get().getSpacecraft().getCode(), securityService.getMission()));
		}
		
		
		if (null == datatake.get().getProcessingTime()) {
			datatake.get().setProcessingTime(Instant.now());
			
			logger.log(DatatakeMgrMessage.DATATAKE_PROCESSED,
					datatake.get().getDatatakeId(), datatake.get().getSpacecraft().getCode());
		} else {
			logger.log(DatatakeMgrMessage.DATATAKE_ALREADY_PROCESSED,
					datatake.get().getDatatakeId(), datatake.get().getSpacecraft().getCode());
		}
		
		return DatatakeUtil.toRestDatatake(datatake.get());
	}
}
