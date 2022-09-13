/**
 * OrderRepository.java
 */
package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ProductionType;

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
	 * Get the processing order with the given mission code and identifier
	 * 
	 * @param missionCode the mission code of the processing order
	 * @param identifier the identifier of the processing order
	 * @return the unique processing order identified by the given identifier
	 */
	@Query("select po from ProcessingOrder po where po.mission.code = ?1 and po.identifier = ?2")
	public ProcessingOrder findByMissionCodeAndIdentifier(String missionCode, String identifier);
	
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
	
	/**
	 * Get all processing orders of state orderState and eviction time older (less) than evictionTime
	 * 
	 * @param orderState the state of order
	 * @param evictionTime the time to compare
	 * @return a list of processing orders matching the selection criteria
	 */
	@Query("select p from ProcessingOrder p where p.orderState = ?1 and p.evictionTime is not null and p.evictionTime < ?2")
	public List<ProcessingOrder> findByOrderStateAndEvictionTimeLessThan(OrderState orderState, Instant evictionTime);

	/**
	 * Get the processing order with the given order state
	 * 
	 * @param missionCode the mission code of the processing order
	 * @return the unique processing order identified by the given identifier
	 */
	@Query("select po from ProcessingOrder po where po.orderState = ?1")
	public List<ProcessingOrder> findByOrderState(OrderState orderState);
	
}
