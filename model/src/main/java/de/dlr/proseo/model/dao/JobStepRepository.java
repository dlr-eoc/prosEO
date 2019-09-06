/**
 * JobStepRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.JobStep;

/**
 * Data Access Object for the JobStep class
 * 
 * @author melchinger
 *
 */
@Repository
public interface JobStepRepository extends CrudRepository<JobStep, Long> {

}
