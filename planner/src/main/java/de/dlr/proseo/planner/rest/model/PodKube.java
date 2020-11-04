/**
 * PodKube.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest.model;

import de.dlr.proseo.model.rest.model.PlannerPod;
import io.kubernetes.client.openapi.models.V1Job;
/**
 * Handle a Kubernetes pod/job 
 * 
 * @author Ernst Melchinger
 *
 */
public class PodKube extends PlannerPod {

	private static final long serialVersionUID = 287477937477814477L;

	/**
	 * Set instance variables and convert data to text
	 * 
	 * @param job Kubernetes job
	 */
	public PodKube(V1Job job) {
		super();
		if (job != null) {
			this.id = job.getMetadata().getUid();
			this.name = job.getMetadata().getName();
			this.starttime = "";
			this.completiontime = "";
			this.succeded = "";
			this.type = "";
			this.status = "";
			if (job.getStatus() != null) {
				if (job.getStatus().getStartTime() != null) { 
					starttime = job.getStatus().getStartTime().toString("dd.MM.YYYY HH:mm:ss", null);
					type = "running";
				}
				if (job.getStatus().getCompletionTime() != null) { 
					completiontime = job.getStatus().getCompletionTime().toString("dd.MM.YYYY HH:mm:ss", null);
				}
				if (job.getStatus() != null) {
					if (job.getStatus().getSucceeded() != null) {
						succeded = job.getStatus().getSucceeded().toString();
					}
					if (job.getStatus().getConditions() != null && job.getStatus().getConditions().size() > 0) {
						type = job.getStatus().getConditions().get(0).getType();
						status = job.getStatus().getConditions().get(0).getStatus();
					}
				}
			}
		}
	}
	
	/**
	 * @return true if job has been completed
	 */
	public boolean isCompleted() {
		return (this.hasStatus("complete") || type.equalsIgnoreCase("completed"));
	}
	
	/**
	 * @param status to look for
	 * @return true if job state equals status or Kubernetes job state and type is completed
	 */
	public boolean hasStatus(String status) {
		if (type.equalsIgnoreCase(status) || (status.equalsIgnoreCase("complete") && type.equalsIgnoreCase("completed"))) {
			return true;
		} else {
			return false;
		}
	}
}
