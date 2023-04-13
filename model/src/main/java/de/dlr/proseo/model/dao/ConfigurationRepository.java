/**
 * ConfigurationRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Configuration;

/**
 * Data Access Object for the Processor class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

	/**
	 * Get the configuration with the given mission, class name and version
	 * 
	 * @param mission the mission code
	 * @param processorName the processor class name
	 * @param configurationVersion the configuration version
	 * @return the unique configuration identified by the search criteria
	 */
	@Query("select c from Configuration c where c.processorClass.mission.code = ?1 and c.processorClass.processorName = ?2 and c.configurationVersion = ?3")
	public Configuration findByMissionCodeAndProcessorNameAndConfigurationVersion(String mission, String processorName, String configurationVersion);

}
