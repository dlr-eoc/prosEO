/**
 * CalendarOrderTriggerRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.CalendarOrderTrigger;

/**
 * Data Access Object for the CalendarOrderTrigger class
 * 
 * @author Ernst Melchinger
 *
 */
public interface CalendarOrderTriggerRepository extends JpaRepository<CalendarOrderTrigger, Long> {

	/**
	 * Get all triggers for the given mission
	 * 
	 * @param missionCode the mission code
	 * @return the list of triggers for this mission
	 */
	@Query("select t from CalendarOrderTrigger t where t.mission.code = ?1")
	public List<CalendarOrderTrigger> findByMissionCode(String missionCode);

	/**
	 * Get trigger within a mission with the given name
	 * @param missionCode the mission code
	 * @param name the name of the workflow
	 * 
	 * @return the trigger for this mission having the given name
	 */
	@Query("select t from CalendarOrderTrigger t where t.mission.code = ?1 and t.name = ?2")
	public CalendarOrderTrigger findByMissionCodeAndName(String missionCode, String name);
}
