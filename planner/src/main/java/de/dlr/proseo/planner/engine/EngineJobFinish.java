package de.dlr.proseo.planner.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.EngineDispatcher;
import de.dlr.proseo.planner.util.UtilService;

/**
 * Monitors the completion of an Engine job.
 *
 * This class runs as a separate thread and waits for an EngineJob to finish. It periodically checks for the job's completion
 * status and retrieves the finish info. Once the job is finished, it performs additional actions based on the job's outcome. The
 * thread stops after a maximum number of cycles or when the job is found to be finished.
 *
 * Note: This class requires initialization with the planner, engine job, and job name.
 *
 * @author Ernst Melchinger
 */
public abstract class EngineJobFinish extends Thread {

	/** Logger of this class. */
	private static ProseoLogger logger = new ProseoLogger(EngineJobFinish.class);

	/** The production planner */
	protected ProductionPlanner planner;

	/** The name of the kube job being monitored. */
	protected String jobName;

	/** The kube job being monitored. */
	protected EngineJob engineJob;

	/**
	 * Creates a new thread instance to monitor a Kubernetes job until its completion.
	 *
	 * @param kubeJob The planner kube job.
	 * @param planner The production planner instance.
	 * @param jobName The Kubernetes job name.
	 */
	public EngineJobFinish(EngineJob engineJob, ProductionPlanner planner, String jobName) {
		super(jobName);

		this.engineJob = engineJob;
		this.jobName = jobName;
		this.planner = planner;
	}

	/**
	 * Starts the thread to monitor the Kubernetes job until it finishes and the finish info is retrieved. This check sleeps for a
	 * defined time between cycles and stops after a maximum number of cycles (parameters are defined in the configuration).
	 *
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> run()");
		}

		if (engineJob == null || jobName == null || jobName.isEmpty())
			return;

		boolean found = false;
		int cycleNumber = 0;
		int waitTime = ProductionPlanner.config.getProductionPlannerCycleWaitTime();
		int maxCycles = ProductionPlanner.config.getProductionPlannerMaxCycles();

		while (!found && cycleNumber < maxCycles) {
			try {
				cycleNumber++;
				sleep(waitTime);

				try {
					// Update the finish info of the Kubernetes job and check if it is finished
					found = engineJob.updateFinishInfoAndDelete(jobName);

					if (found) {
						final Long jobStepId = engineJob.getJobId();

						// Start a transaction to fetch the job step details and retrieve associated product classes
						TransactionTemplate transactionTemplate = new TransactionTemplate(planner.getTxManager());
						transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
						final List<Long> productClassIds = transactionTemplate.execute((status) -> {
							Optional<JobStep> jobStep = RepositoryService.getJobStepRepository().findById(jobStepId);
							List<Long> componentProductClassIds = new ArrayList<>();

							if (jobStep.isPresent()) {
								if (jobStep.get().getOutputProduct() != null) {
									// Get all component classes of the output product class
									List<ProductClass> productClasses = getAllComponentClasses(
											jobStep.get().getOutputProduct().getProductClass());
									productClasses.add(jobStep.get().getOutputProduct().getProductClass());
									for (ProductClass pc : productClasses) {
										componentProductClassIds.add(pc.getId());
									}
								}
							}
							// TODO Else maybe log or throw exception?

							return componentProductClassIds;
						});

						if (ProductionPlanner.config.getCheckForFurtherJobStepsToRun()) {
							// Check for any job steps that can be run based on the finished job and associated product classes
							for (Long pcId : productClassIds) {
								UtilService.getJobStepUtil().checkForJobStepsToRun(engineJob.getEngineConfig(), pcId, false, false);
							}
						}

						if (ProductionPlanner.config.getCheckForFurtherJobStepsToRun()) {
							// Start the EngineDispatcher to handle any further job steps that can be run
							EngineDispatcher engineDispatcher = new EngineDispatcher(null, engineJob.getEngineConfig(), true);
							engineDispatcher.start();
						}
					}
				} catch (Exception e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
					
					if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
				}
			} catch (InterruptedException e) {
				logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
								
				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			}
		}

		// Remove the finished job from the list of monitored jobs
		planner.getFinishThreads().remove(this.jobName);
	}

	/**
	 * Recursively collects all component classes of a product class into a list.
	 *
	 * @param productClass The product class.
	 * @return The collected component classes.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	protected List<ProductClass> getAllComponentClasses(ProductClass productClass) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> getAllComponentClasses({})", (null == productClass ? "null" : productClass.getProductType()));
		}

		TransactionTemplate transactionTemplate = new TransactionTemplate(ProductionPlanner.productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		final List<ProductClass> componentProducts = new ArrayList<>();

		// Get the component product information from the product class repository
		transactionTemplate.execute((status) -> {
			Optional<ProductClass> retrievedProductClass = RepositoryService.getProductClassRepository()
				.findById(productClass.getId());
			if (retrievedProductClass.isPresent()) {
				componentProducts.addAll(retrievedProductClass.get().getComponentClasses());
				for (ProductClass subPC : retrievedProductClass.get().getComponentClasses()) {
					componentProducts.addAll(getAllComponentClasses(subPC));
				}
			}

			// TODO Else maybe log or throw exception?
			return componentProducts;
		});

		return componentProducts;
	}



}
