/**
 * JobStepRepository.java
 * 
 * @ 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;

/**
 * Data Access Object for the JobStep class
 * 
 * @author melchinger
 *
 */
@Transactional
@Repository
public interface JobStepRepository extends JpaRepository<JobStep, Long> {

	public List<JobStep> findAllByJobStepState(JobStepState jobStepState);	

	@Query("select js from JobStep js where js.job.processingFacility.id = ?1 and js.jobStepState in ?2")
	public List<JobStep> findAllByProcessingFacilityAndJobStepStateIn(long id, List<JobStepState> jobStepStates);

	@Query("select js from JobStep js where js.job.processingFacility.id = ?1 and js.jobStepState in ?2 order by js.job.startTime, js.id")
	public List<JobStep> findAllByProcessingFacilityAndJobStepStateInAndOrderBySensingStartTime(long id, List<JobStepState> jobStepStates);

	@Query("select js from JobStep js where js.jobStepState = ?1 and js.job.processingOrder.mission.code = ?2 order by js.processingCompletionTime desc")
	public List<JobStep> findAllByJobStepStateAndMissionOrderByDate(JobStepState jobStepState, String mission);

	@Query("select js from JobStep js where js.jobStepState = ?1 and js.job.processingOrder.id = ?2 order by js.job.startTime, js.id")
	public List<JobStep> findAllByJobStepStateAndOrderIdByDate(JobStepState jobStepState, long orderId);

	@Query("SELECT COUNT(*) FROM JobStep js WHERE js.job.id = ?1 AND  js.jobStepState NOT IN ('COMPLETED', 'FAILED')")
	public int countJobStepNotFinishedByJobId(long id);

	@Query("SELECT COUNT(*) FROM JobStep js WHERE js.job.processingOrder.id = ?1 AND js.jobStepState NOT IN ('COMPLETED', 'FAILED')")
	public int countJobStepNotFinishedByOrderId(long id);

	@Query("SELECT COUNT(*) FROM JobStep js WHERE js.job.processingOrder.id = ?1")
	public int countJobStepByOrderId(long id);

	@Query("SELECT COUNT(*) FROM JobStep js WHERE js.job.id = ?1 AND  js.jobStepState = 'COMPLETED'")
	public int countJobStepCompletedByJobId(long id);
	
	@Query("SELECT COUNT(*) FROM JobStep js WHERE js.job.id = ?1 AND  js.jobStepState = 'FAILED'")
	public int countJobStepFailedByJobId(long id);
	
	@Query("SELECT COUNT(*) FROM JobStep js WHERE js.job.id = ?1 AND  js.jobStepState = 'RUNNING'")
	public int countJobStepRunningByJobId(long id);

	@Query("SELECT COUNT(*) FROM JobStep js WHERE js.job.processingOrder.id = ?1 AND  js.jobStepState = 'COMPLETED'")
	public int countJobStepCompletedByOrderId(long id);
	
	@Query("SELECT COUNT(*) FROM JobStep js WHERE js.job.processingOrder.id = ?1 AND  js.jobStepState = 'FAILED'")
	public int countJobStepFailedByOrderId(long id);
	
	@Query("SELECT COUNT(*) FROM JobStep js WHERE js.job.processingOrder.id = ?1 AND  js.jobStepState = 'RUNNING'")
	public int countJobStepRunningByOrderId(long id);
	
	@Query("select js.jobStepState, count(*) "
				+ "from ProcessingOrder o "
				+ "join o.jobs j "
				+ "join j.jobSteps js "
				+ "where o.id = ?1 "
				+ "group by js.jobStepState")
	public List<Object[]> countJobStepStatesByOrderId(long id);

	@Query("SELECT DISTINCT(js.jobStepState) FROM JobStep js WHERE js.job.processingOrder.id = ?1")
	public List<String> findDistinctJobStepStatesByOrderId(long id);
}
