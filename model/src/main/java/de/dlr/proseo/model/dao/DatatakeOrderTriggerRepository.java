/**
 * DatatakeOrderTriggerRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.DatatakeOrderTrigger;

/**
 * Data Access Object for the DatatakeOrderTrigger class
 * 
 * @author Ernst Melchinger
 *
 */
public interface DatatakeOrderTriggerRepository extends JpaRepository<DatatakeOrderTrigger, Long> {

	/**
	 * Get all triggers for the given mission
	 * 
	 * @param missionCode the mission code
	 * @return the list of triggers for this mission
	 */
	@Query("select t from DatatakeOrderTrigger t where t.mission.code = ?1")
	public List<DatatakeOrderTrigger> findByMissionCode(String missionCode);

	/**
	 * Get trigger within a mission with the given name
	 * @param missionCode the mission code
	 * @param name the name of the workflow
	 * 
	 * @return the trigger for this mission having the given name
	 */
	@Query("select t from DatatakeOrderTrigger t where t.mission.code = ?1 and t.name = ?2")
	public DatatakeOrderTrigger findByMissionCodeAndName(String missionCode, String name);
}
