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
	 * Get the processor class with the given name
	 * 
	 * @param processorName the processor name
	 * @return the unique processor class identified by the processor name
	 */
	public ProcessorClass findByProcessorName(String processorName);
}
