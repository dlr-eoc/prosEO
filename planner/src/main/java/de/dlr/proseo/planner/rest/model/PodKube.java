/**
 * PodKube.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest.model;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import de.dlr.proseo.model.rest.model.PlannerPod;
import io.kubernetes.client.openapi.models.V1Job;

/**
 * Represents a Kubernetes pod/job and provides methods to handle its data. Extends PlannerPod to inherit common pod properties.
 * 
 * @author Ernst Melchinger
 */
public class PodKube extends PlannerPod {

	private static final long serialVersionUID = 287477937477814477L;
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu' 'HH:mm:ss")
		.withZone(ZoneId.of("UTC"));

	/**
	 * Constructs a PodKube object based on the provided Kubernetes job. Extracts relevant information from the job and sets
	 * instance variables.
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
					starttime = timeFormatter.format(job.getStatus().getStartTime().toInstant());
					type = "running";
				}

				if (job.getStatus().getCompletionTime() != null) {
					completiontime = timeFormatter.format(job.getStatus().getCompletionTime().toInstant());
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
	 * Checks if the job has been completed.
	 * 
	 * @return true if the job has been completed, otherwise false
	 */
	public boolean isCompleted() {
		return (this.hasStatus("complete") || type.equalsIgnoreCase("completed"));
	}

	/**
	 * Checks if the job has a specific status.
	 * 
	 * @param status the status to check for
	 * @return true if the job's state equals the specified status or if the job is completed and type is specified as completed,
	 *         otherwise false
	 */
	public boolean hasStatus(String status) {
		if (type.equalsIgnoreCase(status) || (status.equalsIgnoreCase("complete") && type.equalsIgnoreCase("completed"))) {
			return true;
		} else {
			return false;
		}
	}
}