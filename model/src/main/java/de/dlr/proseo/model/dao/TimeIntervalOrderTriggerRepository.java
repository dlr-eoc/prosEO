/**
 * TimeIntervalOrderTriggerRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.TimeIntervalOrderTrigger;

/**
 * Data Access Object for the TimeIntervalOrderTrigger class
 * 
 * @author Ernst Melchinger
 *
 */
public interface TimeIntervalOrderTriggerRepository extends JpaRepository<TimeIntervalOrderTrigger, Long> {

	/**
	 * Get all triggers for the given mission
	 * 
	 * @param missionCode the mission code
	 * @return the list of triggers for this mission
	 */
	@Query("select t from TimeIntervalOrderTrigger t where t.mission.code = ?1")
	public List<TimeIntervalOrderTrigger> findByMissionCode(String missionCode);

	/**
	 * Get trigger within a mission with the given name
	 * @param missionCode the mission code
	 * @param name the name of the workflow
	 * 
	 * @return the trigger for this mission having the given name
	 */
	@Query("select t from TimeIntervalOrderTrigger t where t.mission.code = ?1 and t.name = ?2")
	public TimeIntervalOrderTrigger findByMissionCodeAndName(String missionCode, String name);
}
