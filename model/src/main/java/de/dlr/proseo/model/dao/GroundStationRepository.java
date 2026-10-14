/**
 * GroundStationRepository.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.dlr.proseo.model.GroundStation;

/**
 * Data Access Object for the GroundStation class
 * 
 * @author Dr. Thomas Bassler
 */
public interface GroundStationRepository extends JpaRepository<GroundStation, Long> {

	/**
	 * Get the ground station with the given station ID
	 *
	 * @param stationId the ground station ID
	 * @return the unique ground station with the given ID or an empty Optional, if no such station exists
	 */
	public Optional<GroundStation> findByStationId(String stationId);
}
