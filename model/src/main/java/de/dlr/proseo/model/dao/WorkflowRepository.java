/**
 * WorkflowRepository.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Workflow;

/**
 * Data Access Object for the Workflow class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow,Long> {

	/**
	 * Get the workflow with the given UUID
	 * 
	 * @param uuid the UUID of the workflow
	 * @return the unique workflow identified by the given UUID
	 */
	public Workflow findByUuid(UUID uuid);
	
	/**
	 * Get the workflow with the given name
	 * 
	 * @param uuid the name of the workflow
	 * @return the unique workflow identified by the given name
	 */
	public Workflow findByName(String name);
	
}
