/**
 * ProcessorRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Processor;

/**
 * Data Access Object for the Processor class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ProcessorRepository extends JpaRepository<Processor, Long> {

	/**
	 * Get the processor with the given mission, class name and version
	 * 
	 * @param mission the misson code
	 * @param processorName the processor class name
	 * @param processorVersion the processor version
	 * @return the unique processor identified by the search criteria
	 */
	@Query("select p from Processor p where p.processorClass.mission.code = ?1 and p.processorClass.processorName = ?2 and p.processorVersion = ?3")
	public Processor findByMissionCodeAndProcessorNameAndProcessorVersion(String mission, String processorName, String processorVersion);
}
