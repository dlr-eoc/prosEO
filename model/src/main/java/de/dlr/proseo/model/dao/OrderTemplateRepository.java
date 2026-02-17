/**
 * OrderTemplateRepository.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.OrderTemplate;

/**
 * Data Access Object for the Workflow class
 * 
 * @author Dr. Thomas Bassler
 *
 */
public interface OrderTemplateRepository extends JpaRepository<OrderTemplate, Long> {

	/**
	 * Get all order templates for the given mission
	 * 
	 * @param missionCode the mission code
	 * @return the list of order templates for this mission
	 */
	@Query("select ot from OrderTemplate ot where ot.mission.code = ?1")
	public List<OrderTemplate> findByMissionCode(String missionCode);

	/**
	 * Get the order template with the given mission and name
	 * 
	 * @param missionCode     the mission code
	 * @param name            the order template name
	 * @return the unique order template identified by the search criteria
	 */
	@Query("select ot from OrderTemplate ot where ot.mission.code = ?1 and ot.name = ?2")
	public OrderTemplate findByMissionCodeAndName(String missionCode, String name);
}
