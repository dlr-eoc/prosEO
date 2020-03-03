/**
 * PodKube.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest.model;

import io.kubernetes.client.models.V1Job;
/**
 * Handle a Kubernetes pod/job
 * 
 * @author Ernst Melchinger
 *
 */
public class PodKube extends PlannerPod {

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
	public boolean isCompleted() {
		return (this.hasStatus("complete") || type.equalsIgnoreCase("completed"));
	}
	public boolean hasStatus(String status) {
		if (type.equalsIgnoreCase(status) || (status.equalsIgnoreCase("complete") &&type.equalsIgnoreCase("completed"))) {
			return true;
		} else {
			return false;
		}
	}
}
