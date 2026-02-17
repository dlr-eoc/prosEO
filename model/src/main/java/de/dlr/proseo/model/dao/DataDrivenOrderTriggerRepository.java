/**
 * DataDrivenOrderTriggerRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.DataDrivenOrderTrigger;
import de.dlr.proseo.model.ProductClass;

/**
 * Data Access Object for the DataDrivenOrderTrigger class
 * 
 * @author Ernst Melchinger
 *
 */
public interface DataDrivenOrderTriggerRepository extends JpaRepository<DataDrivenOrderTrigger, Long> {

	/**
	 * Get all triggers for the given mission
	 * 
	 * @param missionCode the mission code
	 * @return the list of triggers for this mission
	 */
	@Query("select t from DataDrivenOrderTrigger t where t.mission.code = ?1")
	public List<DataDrivenOrderTrigger> findByMissionCode(String missionCode);

	/**
	 * Get trigger within a mission with the given name
	 * 
	 * @param missionCode the mission code
	 * @param name the name of the workflow
	 * 
	 * @return the trigger for this mission having the given name
	 */
	@Query("select t from DataDrivenOrderTrigger t where t.mission.code = ?1 and t.name = ?2")
	public DataDrivenOrderTrigger findByMissionCodeAndName(String missionCode, String name);

	/**
	 * Get all triggers within a mission with a workflow having the given product class as input
	 * 
	 * @param missionCode the mission code
	 * @param productClass the input product class of the trigger
	 * 
	 * @return the list of triggers for this mission using the given product class as input
	 */
	@Query("select t from DataDrivenOrderTrigger t where t.mission.code = ?1 and t.inputProductClass = ?2")
	public List<DataDrivenOrderTrigger> findByMissionCodeAndProductClass(String missionCode, ProductClass productclass);
}
