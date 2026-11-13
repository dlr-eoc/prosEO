package de.dlr.proseo.planner.engine.slurm;

import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.engine.EngineJob;
import de.dlr.proseo.planner.engine.EngineJobFinish;

public class SlurmJobFinish extends EngineJobFinish {

	/**
	 * Creates a new thread instance to monitor a slurm job until its completion.
	 *
	 * @param kubeJob The slurm job.
	 * @param planner The production planner instance.
	 * @param jobName The Kubernetes job name.
	 */
	public SlurmJobFinish(EngineJob engineJob, ProductionPlanner planner, String jobName) {
		super(engineJob, planner, jobName);
	}

}
