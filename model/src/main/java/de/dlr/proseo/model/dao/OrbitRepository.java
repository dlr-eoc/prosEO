/**
 * OrbitRepository.java
 */
package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Orbit;

/**
 * Data Access Object for the Orbit class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface OrbitRepository extends JpaRepository<Orbit, Long> {

	/**
	 * Get the orbit with the given spacecraft and orbit number
	 * 
	 * @param spacecraftCode the spacecraft code
	 * @param orbitNumber the orbit number
	 * @return the unique orbit identified by the spacecraft code and the orbit number
	 */
	@Query("select o from Orbit o where o.spacecraft.code = ?1 and o.orbitNumber = ?2")
	public Orbit findBySpacecraftCodeAndOrbitNumber(String spacecraftCode, Integer orbitNumber);
	
	/**
	 * Get all orbits for the given spacecraft and orbit number range
	 * 
	 * @param spacecraftCode the spacecraft code
	 * @param orbitNumberFrom the first orbit number
	 * @param orbitNumberTo the last orbit number
	 * @return a list of orbits satisfying the spacecraft code and the orbit number range
	 */
	@Query("select o from Orbit o where o.spacecraft.code = ?1 and o.orbitNumber between ?2 and ?3")
	public List<Orbit> findBySpacecraftCodeAndOrbitNumberBetween(String spacecraftCode, Integer orbitNumberFrom, Integer orbitNumberTo);
	
	/**
	 * Get all orbits for a given spacecraft that start in the given time interval (inclusive)
	 * @param spacecraftCode the spacecraft code
	 * @param startTimeFrom the earliest start time
	 * @param startTimeTo the latest start time
	 * @return a list of orbits satisfying the selection criteria
	 */
	@Query("select o from Orbit o where o.spacecraft.code = ?1 and o.startTime between ?2 and ?3")
	public List<Orbit> findBySpacecraftCodeAndStartTimeBetween(String spacecraftCode, Instant startTimeFrom, Instant startTimeTo);
	
	/**
	 * Get all orbits for a given spacecraft that start and stop time intersects the given time interval (inclusive)
	 * @param spacecraftCode the spacecraft code
	 * @param startTime the start time
	 * @param stopTime the stop time
	 * @return a list of orbits satisfying the selection criteria
	 */
	@Query("select o from Orbit o where o.spacecraft.code = ?1 and o.stopTime > ?2 and o.startTime < ?3")
	public List<Orbit> findBySpacecraftCodeAndTimeIntersect(String spacecraftCode, Instant startTime, Instant stopTime);
}
