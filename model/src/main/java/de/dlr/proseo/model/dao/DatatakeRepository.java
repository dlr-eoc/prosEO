/**
 * DatatakeRepository.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.Datatake;

/**
 * Data Access Object for the Datatake class
 * 
 * @author Dr. Thomas Bassler
 */
public interface DatatakeRepository extends JpaRepository<Datatake, Long> {
	
	/**
	 * Get the datatake with the given datatake ID and payload
	 * 
	 * @param missionCode the mission code
	 * @param spacecraftCode the spacecraft code
	 * @param payloadName the payload name
	 * @param datatakeID the datatake ID
	 * @return the unique datatake identified by the selection criteria or an empty Optional, if no such datatake exists
	 */
	@Query("select dt from Datatake dt where dt.spacecraft.mission.code = ?1 and dt.spacecraft.code = ?2 and dt.payload.name = ?3 and dt.datatakeId = ?4")
	public Optional<Datatake> findByMissionAndPayloadAndId(
			String missionCode, String spacecraftCode, String payloadName, String datatakeID);
	
	/**
	 * Get all datatakes for a given payload with a sensing start time in a given interval, ordered by ascending sensing start time
	 */
	@Query("select dt from Datatake dt where dt.spacecraft.mission.code = ?1 and dt.spacecraft.code = ?2 and dt.payload.name = ?3 and dt.sensingStartTime between ?4 and ?5 order by dt.sensingStartTime")
	public List<Datatake> findByMissionAndPayloadAndSensingStartBetween(
			String missionCode, String spacecraftCode, String payloadName, Instant sensingStartFrom, Instant sensingStartTo);

}
