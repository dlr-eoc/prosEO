/**
 * JobRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;

/**
 * Data Access Object for the Job class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Transactional
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

	@Query("select j from Job j where j.jobState = ?1")
	public List<Job> findAllByJobState(JobState jobState);	

	@Query("select j from Job j where j.processingOrder.id = ?1")
	public List<Job> findAllByProcessingOrder(long orderId);	

	@Deprecated
	@Query("select j from Job j where j.jobState = ?1 and j.processingOrder.id = ?2")
	public List<Job> findAllByJobStateAndProcessingOrder(JobState jobState, long orderId);	

	@Query("select count(*) from Job j where j.jobState = ?1 and j.processingOrder.id = ?2")
	public int countAllByJobStateAndProcessingOrder(JobState jobState, long orderId);	

	@Query("SELECT COUNT(*) FROM Job j WHERE j.processingOrder.id = ?1 AND  j.jobState NOT IN ('CLOSED', 'COMPLETED', 'FAILED')")
	public int countJobNotFinishedByProcessingOrderId(long id);

	@Query("SELECT COUNT(*) FROM Job j WHERE j.processingOrder.id = ?1 AND  j.jobState = 'FAILED'")
	public int countJobFailedByProcessingOrderId(long id);

	@Query("SELECT COUNT(*) FROM Job j WHERE j.processingOrder.id = ?1 AND  j.jobState = 'ON_HOLD'")
	public int countJobOnHoldByProcessingOrderId(long id);
}
