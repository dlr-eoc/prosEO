/**
 * KubeJobFinish.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.kubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.KubeDispatcher;
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
public class KubeJobFinish extends Thread {

	/** Logger of this class. */
	private static ProseoLogger logger = new ProseoLogger(KubeJobFinish.class);

	/** The production planner */
	private ProductionPlanner planner;

	/** The name of the kube job being monitored. */
	private String jobName;

	/** The kube job being monitored. */
	private KubeJob kubeJob;

	/**
	 * Creates a new thread instance to monitor a Kubernetes job until its completion.
	 *
	 * @param kubeJob The planner kube job.
	 * @param planner The production planner instance.
	 * @param jobName The Kubernetes job name.
	 */
	public KubeJobFinish(KubeJob kubeJob, ProductionPlanner planner, String jobName) {
		super(jobName);

		this.kubeJob = kubeJob;
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

		if (kubeJob == null || jobName == null || jobName.isEmpty())
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
					planner.acquireThreadSemaphore("KubeJobFinish.run");
					found = kubeJob.updateFinishInfoAndDelete(jobName);

					if (found) {
						final Long jobStepId = kubeJob.getJobId();

						// Start a transaction to fetch the job step details and retrieve associated product classes
						TransactionTemplate transactionTemplate = new TransactionTemplate(planner.getTxManager());
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
								UtilService.getJobStepUtil().checkForJobStepsToRun(kubeJob.getKubeConfig(), pcId, false, true);
							}
						}

						planner.releaseThreadSemaphore("KubeJobFinish.run");

						if (ProductionPlanner.config.getCheckForFurtherJobStepsToRun()) {
							// Start the KubeDispatcher to handle any further job steps that can be run
							KubeDispatcher kubeDispatcher = new KubeDispatcher(null, kubeJob.getKubeConfig(), true);
							kubeDispatcher.start();
						}
					} else {
						planner.releaseThreadSemaphore("KubeJobFinish.run");
					}
				} catch (Exception e) {
					planner.releaseThreadSemaphore("KubeJobFinish.run");
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
				}
			} catch (InterruptedException e) {
				// TODO Handle interruption exception
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
	@Transactional
	private List<ProductClass> getAllComponentClasses(ProductClass productClass) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> getAllComponentClasses({})", (null == productClass ? "null" : productClass.getProductType()));
		}

		TransactionTemplate transactionTemplate = new TransactionTemplate(ProductionPlanner.productionPlanner.getTxManager());
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