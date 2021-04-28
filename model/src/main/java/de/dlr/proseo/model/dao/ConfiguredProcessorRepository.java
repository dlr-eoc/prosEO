/**
 * ConfiguredProcessorRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.ConfiguredProcessor;

/**
 * Data Access Object for the ConfiguredProcessor class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ConfiguredProcessorRepository extends JpaRepository<ConfiguredProcessor, Long> {

	/**
	 * Get the configured processor associated with the given mission code and identifier
	 * 
	 * @param missionCode the mission code of the configured processor
	 * @param identifier the identifier of the configured processor
	 * @return the unique configured processor identified by the given identifier 
	 */
	@Query("select cp from ConfiguredProcessor cp where cp.processor.processorClass.mission.code = ?1 and cp.identifier = ?2")
	public ConfiguredProcessor findByMissionCodeAndIdentifier(String missionCode, String identifier);

	/**
	 * Get the configured processor associated with the given UUID
	 * 
	 * @param uuid the UUID of the configured processor
	 * @return the unique configured processor identified by the given UUID 
	 */
	public ConfiguredProcessor findByUuid(UUID uuid);
}
