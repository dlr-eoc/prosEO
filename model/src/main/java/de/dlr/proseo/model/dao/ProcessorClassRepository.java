/**
 * ProcessorClassRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.ProcessorClass;

/**
 * Data Access Object for the ProcessorClass class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ProcessorClassRepository extends CrudRepository<ProcessorClass, Long> {

	/**
	 * Get the processor class with the given mission and name
	 * 
	 * @param missionCode the mission code
	 * @param processorName the processor name
	 * @return the unique processor class identified by the mission code and processor name
	 */
	@Query("select pc from ProcessorClass pc where pc.mission.code = ?1 and pc.processorName = ?2")
	public ProcessorClass findByMissionCodeAndProcessorName(String missionCode, String processorName);
}
