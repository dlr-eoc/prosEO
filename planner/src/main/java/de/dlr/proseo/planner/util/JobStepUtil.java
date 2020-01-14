package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.service.ProductQueryService;
import de.dlr.proseo.model.service.RepositoryService;


@Service
public class JobStepUtil {
	
	private static Logger logger = LoggerFactory.getLogger(JobStepUtil.class);

	@Autowired
	private ProductQueryService productQueryService;
	 
	@Transactional
	public void searchForJobStepsToRun(ProcessingFacility processingFacility) {
		// Search for all job steps on processingFacility with states INITIAL, WAITING_INPUT
		List<de.dlr.proseo.model.JobStep.JobStepState> jobStepStates = new ArrayList<>();
		jobStepStates.add(de.dlr.proseo.model.JobStep.JobStepState.INITIAL);
		jobStepStates.add(de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT);
		List<JobStep> jobSteps = null;
		if (processingFacility == null) {
			jobSteps = new ArrayList<>();
			for (ProcessingFacility pf : RepositoryService.getFacilityRepository().findAll()) {
				jobSteps.addAll(RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateIn(pf.getId(), jobStepStates));
			}
		} else {
			jobSteps = RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateIn(processingFacility.getId(), jobStepStates);
		}
		for (JobStep js : jobSteps) {
			checkJobStepQueries(js);
		}
		// check whether there are new satisfied product queries
		
		// Change state appropriate to result
	}

	@Transactional
	public Boolean suspend(JobStep js) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		// INITIAL, WAITING_INPUT, READY, RUNNING, COMPLETED, FAILED
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case READY:
			case WAITING_INPUT:
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.INITIAL);
				RepositoryService.getJobStepRepository().save(js);
				answer = true;
				break;
			case RUNNING:
			case COMPLETED:
			case FAILED:
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Boolean cancel(JobStep js) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case READY:
			case WAITING_INPUT:
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.FAILED);
				RepositoryService.getJobStepRepository().save(js);
				answer = true;
				break;
			case RUNNING:
			case COMPLETED:
			case FAILED:
			default:
				break;
			}
		}
		return answer;
	}
	
	@Transactional
	public Boolean delete(JobStep js) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case READY:
				RepositoryService.getJobStepRepository().delete(js);
				answer = true;
			case WAITING_INPUT:
				break;
			case RUNNING:
			case COMPLETED:
			case FAILED:
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Boolean resume(JobStep js) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case WAITING_INPUT:
				checkJobStepQueries(js);
				answer = true;
				break;
			case READY:
			case RUNNING:
			case COMPLETED:
			case FAILED:
			default:
				break;
			}
		}
		return answer;
	}

	public void checkJobStepQueries(JobStep js) {
		Boolean hasUnsatisfiedInputQueries = false;
		logger.trace("Looking for product queries of job step: " + js.getId());
		for (ProductQuery pq : js.getInputProductQueries()) {
			if (!pq.isSatisfied()) {
				if (productQueryService.executeQuery(pq, false, false)) {
					js.getOutputProduct().getSatisfiedProductQueries().add(pq);
					RepositoryService.getProductQueryRepository().save(pq);
					RepositoryService.getProductRepository().save(js.getOutputProduct());
				} else {
					hasUnsatisfiedInputQueries = true;
				}
			}
		}
		if (hasUnsatisfiedInputQueries) {
			if (js.getJobStepState() != de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT) {
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT);
				RepositoryService.getJobStepRepository().save(js);
			}				
		} else {
			js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.READY);
			RepositoryService.getJobStepRepository().save(js);
		}
	}

}
