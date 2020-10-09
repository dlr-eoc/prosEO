/**
 * OrderRepository.java
 */
package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ProcessingOrder;

/**
 * Data Access Object for the ProcessingOrder class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Transactional
@Repository
public interface OrderRepository extends JpaRepository<ProcessingOrder, Long> {

	/**
	 * Get the processing order with the given identifier
	 * 
	 * @param identifier the identifier of the processing order
	 * @return the unique processing order identified by the given identifier
	 */
	public ProcessingOrder findByIdentifier(String identifier);
	
	/**
	 * Get the processing order with the given UUID
	 * 
	 * @param uuid the UUID of the processing order
	 * @return the unique processing order identified by the given UUID
	 */
	public ProcessingOrder findByUuid(UUID uuid);
	
	/**
	 * Get all processing orders scheduled for execution within the given time range
	 * 
	 * @param executionTimeFrom the earliest execution time
	 * @param executionTimeTo the latest execution time
	 * @return a list of processing orders matching the selection criteria
	 */
	public List<ProcessingOrder> findByExecutionTimeBetween(Instant executionTimeFrom, Instant executionTimeTo);
}
