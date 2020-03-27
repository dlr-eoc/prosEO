/**
 * ConfiguredProcessorRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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
	 * Get the configured processor associated with the given identifier
	 * 
	 * @param identifier the identifier of the configured processor
	 * @return the unique configured processor identified by the given identifier 
	 */
	public ConfiguredProcessor findByIdentifier(String identifier);

	/**
	 * Get the configured processor associated with the given UUID
	 * 
	 * @param uuid the UUID of the configured processor
	 * @return the unique configured processor identified by the given UUID 
	 */
	public ConfiguredProcessor findByUuid(UUID uuid);
}
