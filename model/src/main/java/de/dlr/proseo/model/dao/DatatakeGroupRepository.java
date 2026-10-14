/**
 * DatatakeGroupRepository.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.DatatakeGroup;

/**
 * Data Access Object for the DatatakeGroup class
 * 
 * @author Dr. Thomas Bassler
 */
public interface DatatakeGroupRepository extends JpaRepository<DatatakeGroup, Long> {
	
	/**
	 * Get the datatake group with the given identifier in the given mission
	 * 
	 * @param missionCode the mission code
	 * @param dtGroupIdentifier the identifier of the datatake group
	 * @return the unique datatake group matching the selection criteria or an empty Optional, if no such group exists
	 */
	@Query("select dtg from DatatakeGroup dtg where dtg.mission.code = ?1 and dtg.dtGroupIdentifier = ?2")
	public Optional<DatatakeGroup> findByMissionCodeAndIdentifier(String missionCode, String dtGroupIdentifier);

}
