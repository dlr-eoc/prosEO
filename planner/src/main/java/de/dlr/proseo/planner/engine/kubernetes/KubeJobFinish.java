/**
 * KubeJobFinish.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.engine.kubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.EngineDispatcher;
import de.dlr.proseo.planner.engine.EngineJobFinish;
import de.dlr.proseo.planner.util.UtilService;

/**
 * Monitors the completion of a Kubernetes job.
 *
 * This class runs as a separate thread and waits for a Kubernetes job to finish. It periodically checks for the job's completion
 * status and retrieves the finish info. Once the job is finished, it performs additional actions based on the job's outcome. The
 * thread stops after a maximum number of cycles or when the job is found to be finished.
 *
 * Note: This class requires initialization with the planner, kube job, and job name.
 *
 * @author Ernst Melchinger
 */
public class KubeJobFinish extends EngineJobFinish {

	/** Logger of this class. */
	private static ProseoLogger logger = new ProseoLogger(KubeJobFinish.class);

	/**
	 * Creates a new thread instance to monitor a Kubernetes job until its completion.
	 *
	 * @param kubeJob The kube job.
	 * @param planner The production planner instance.
	 * @param jobName The Kubernetes job name.
	 */
	public KubeJobFinish(KubeJob kubeJob, ProductionPlanner planner, String jobName) {
		super(kubeJob, planner, jobName);
	}

}