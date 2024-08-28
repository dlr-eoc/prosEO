/**
 * ProcessingOrderHistoryRepository.java
 * 
 * (C) 2024 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ProcessingOrderHistory;
/**
 * Data Access Object for the ProcessingOrderHistory class
 * 
 * @author Ernst Melchinger
 *
 */
@Transactional
@Repository
public interface ProcessingOrderHistoryRepository extends JpaRepository<ProcessingOrderHistory, Long> {

	/**
	 * Get the processing order history with the given mission code and identifier
	 * 
	 * @param missionCode the mission code of the processing order
	 * @param identifier  the identifier of the processing order
	 * @return the unique processing order history identified by the given identifier
	 */
	@Query("select po from ProcessingOrderHistory po where po.missionCode = ?1 and po.identifier = ?2")
	public ProcessingOrderHistory findByMissionCodeAndIdentifier(String missionCode, String identifier);

}
