/**
 * DataDrivenOrderTriggerRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.DataDrivenOrderTrigger;

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
	 * @param missionCode the mission code
	 * @param name the name of the workflow
	 * 
	 * @return the trigger for this mission having the given name
	 */
	@Query("select t from DataDrivenOrderTrigger t where t.mission.code = ?1 and t.name = ?2")
	public DataDrivenOrderTrigger findByMissionCodeAndName(String missionCode, String name);
}
