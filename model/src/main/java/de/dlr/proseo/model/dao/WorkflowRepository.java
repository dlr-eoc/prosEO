/**
 * WorkflowRepository.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

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
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

}
