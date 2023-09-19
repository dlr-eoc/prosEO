/**
 * WorkflowRepository.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Workflow;

/**
 * Data Access Object for the Workflow class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

	/**
	 * Get the workflow with the given UUID
	 * 
	 * @param uuid the UUID of the workflow
	 * @return the unique workflow identified by the given UUID
	 */
	public Workflow findByUuid(UUID uuid);

	/**
	 * Get all workflows for the given mission
	 * 
	 * @param missionCode the mission code
	 * @return the list of workflows for this mission
	 */
	@Query("select w from Workflow w where w.mission.code = ?1")
	public List<Workflow> findByMissionCode(String missionCode);

	/**
	 * Get all workflows within a mission with the given name
	 * @param missionCode the mission code
	 * @param name the name of the workflow
	 * 
	 * @return the list of workflows for this mission having the given name
	 */
	@Query("select w from Workflow w where w.mission.code = ?1 and w.name = ?2")
	public List<Workflow> findByMissionCodeAndName(String missionCode, String name);

	/**
	 * Get the workflow with the given mission, name and version
	 * 
	 * @param missionCode     the mission code
	 * @param name            the workflow name
	 * @param workflowVersion the workflow version
	 * @return the unique workflow identified by the search criteria
	 */
	@Query("select w from Workflow w where w.mission.code = ?1 and w.name = ?2 and w.workflowVersion = ?3")
	public Workflow findByMissionCodeAndNameAndVersion(String missionCode, String name, String workflowVersion);
}
