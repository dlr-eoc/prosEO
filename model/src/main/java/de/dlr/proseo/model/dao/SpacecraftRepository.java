/**
 * SpacecraftRepository.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Spacecraft;

/**
 * Data Access Object for the Spacecraft class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface SpacecraftRepository extends JpaRepository<Spacecraft, Long> {

	/**
	 * Get the spacecraft with the given mission and code
	 * 
	 * @param missionCode the mission code
	 * @param spacecraftCode the spacecraft code
	 * @return the unique spacecraft identified by the code
	 */
	@Query("select sc from Spacecraft sc where sc.mission.code = ?1 and sc.code = ?2")
	public Spacecraft findByMissionAndCode(String missionCode, String spacecraftCode);
}
