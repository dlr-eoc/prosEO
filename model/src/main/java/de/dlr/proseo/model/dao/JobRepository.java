/**
 * JobRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Job;

/**
 * Data Access Object for the Job class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

}
