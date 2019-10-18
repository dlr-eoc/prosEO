/**
 * JobStepRepository.java
 * 
 * @ 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;

/**
 * Data Access Object for the JobStep class
 * 
 * @author melchinger
 *
 */
@Repository
public interface JobStepRepository extends JpaRepository<JobStep, Long> {
	
	public List<JobStep> findAllByJobStepState(JobStepState jobStepState);
}
