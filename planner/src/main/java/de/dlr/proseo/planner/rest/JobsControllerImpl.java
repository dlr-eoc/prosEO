package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.rest.JobsController;
import de.dlr.proseo.planner.rest.model.PlannerJob;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;

/**
 * Spring MVC controller for the prosEO Production Planner; TODO
 *
 * @author NN
 *
 */
@Component
public class JobsControllerImpl implements JobsController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_HEADER_SUCCESS = "Success";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);

	/* (non-Javadoc)
	 * @see de.dlr.proseo.planner.rest.JobController#getPlannerJobsById(java.lang.String)
	 */
	@Override
	public ResponseEntity<List<PlannerJob>> getPlannerJobsById(String id) {
		// TODO Auto-generated method stub
		
		if (KubeConfig.isConnected()) {
			 
			V1JobList list = KubeConfig.getJobList();
			List<PlannerJob> jobList = new ArrayList<PlannerJob>();
			if (list != null) {
				for (V1Job item : list.getItems()) {
					jobList.add(new PlannerJob(item.getMetadata().getUid(), item.getMetadata().getName()));
				}
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "");
				return new ResponseEntity<>(jobList, responseHeaders, HttpStatus.FOUND);
			}
			
		} 
		String message = String.format(MSG_PREFIX + "GET for id %s not implemented (%d)", id, 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.planner.rest.JobController#createPlannerJob(de.dlr.proseo.ingestor.rest.model.PlannerJob)
	 */
	@Override
	public ResponseEntity<PlannerJob> updatePlannerJob(PlannerJob plannerJob) {
		// TODO Auto-generated method stub
		KubeJob aJob = KubeConfig.createJob();
		if (aJob != null) {
			PlannerJob aPlan = new PlannerJob();
			aPlan.setId(((Integer)aJob.getJobId()).toString());
			aPlan.setDescription(aJob.getJobName());
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_SUCCESS, "");
			return new ResponseEntity<>(aPlan, responseHeaders, HttpStatus.FOUND);
		}
		String message = String.format(MSG_PREFIX + "POST not implemented (%d)", 2001);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

}
