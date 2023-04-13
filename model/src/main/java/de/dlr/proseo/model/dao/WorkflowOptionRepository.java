/**
 * WorkflowOptionRepository.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.WorkflowOption;

/**
 * Data Access Object for the WorkflowOption class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface WorkflowOptionRepository extends JpaRepository<WorkflowOption, Long> {

	/**
	 * Get the workflow option with the given name and workflow name
	 * 
	 * @param name         the workflow option name
	 * @param workflowName the workflow name
	 * @return the unique workflow option identified by the search criteria
	 */
	@Query("select w from WorkflowOption w where w.name = ?1 and w.workflow.name = ?2")
	public WorkflowOption findByNameAndWorkflowName(String name, String workflowName);
}
