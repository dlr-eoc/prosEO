/**
 * DownlinkRepository.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.Downlink;

/**
 * Data Access Object for the Downlink class
 * 
 * @author Dr. Thomas Bassler
 */
public interface DownlinkRepository extends JpaRepository<Downlink, Long> {
	
	/**
	 * Gets the downlink with the given downlink ID and spacecraft association
	 * 
	 * 
	 */
	@Query("select dl from Downlink dl where dl.spacecraft.mission.code = ?1 and dl.spacecraft.code = ?2 and dl.downlinkId = ?3")
	public Optional<Downlink> findByMissionAndSpacecraftAndId(String missionCode, String spacecraftCode, String downlinkId);

}
