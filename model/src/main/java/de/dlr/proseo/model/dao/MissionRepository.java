/**
 * MissionRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Mission;

/**
 * Data Access Object for the Mission class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

	/**
	 * Get the mission with the given code
	 * 
	 * @param missionCode the mission code
	 * @return the unique mission identified by the code
	 */
	public Mission findByCode(String missionCode);
}
