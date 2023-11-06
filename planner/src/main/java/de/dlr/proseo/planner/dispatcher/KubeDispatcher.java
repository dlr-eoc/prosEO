/**
 * KubeDispatcher.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.util.UtilService;

/**
 * Dispatcher to look for runnable job steps
 *
 * @author Ernst Melchinger
 */
public class KubeDispatcher extends Thread {

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(KubeDispatcher.class);

	/** The planner instance */
	private ProductionPlanner productionPlanner;

	/** Flag to decide to run once or indefinitely */
	private boolean runOnce;

	/** If true, search only for runnable job steps, else evaluate not satisfied queries too. */
	private boolean onlyRun;

	/** The kube configuration of facility */
	private KubeConfig kubeConfig;

	/**
	 * Create new KubeDispatcher for planner
	 *
	 * @param planner           The planner
	 * @param kubeConfiguration The facility's kube configuration
	 * @param onlyRun           set to true to evaluate only runnable job steps or to false to check all job steps
	 */
	public KubeDispatcher(ProductionPlanner planner, KubeConfig kubeConfiguration, Boolean onlyRun) {
		// Set the thread name based on the runOnce flag
		super((kubeConfiguration != null && planner == null) ? "KubeDispatcherRunOnce" : "KubeDispatcher");

		// Set the thread as a daemon thread
		this.setDaemon(true);

		// Assign the provided planner and kubeConfiguration to the corresponding class variables
		productionPlanner = planner;
		kubeConfig = kubeConfiguration;

		// Set the onlyRun flag to determine whether to evaluate only runnable job steps
		this.onlyRun = onlyRun;

		// Set the runOnce flag based on the provided kubeConfiguration and planner
		runOnce = (kubeConfiguration != null && planner == null);
	}

	/**
	 * Checks for job steps, which are ready to run; depending on its creation parameter "runOnce" this is a one-time process or it
	 * is running cyclically until it is terminated externally
	 *
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		if (logger.isTraceEnabled())
			logger.trace(">>> run()");

		// Get the wait time from the configuration
		int wait = ProductionPlanner.config.getProductionPlannerDispatcherWaitTime();

		if (runOnce) {
			// Execute the run-once scenario
			logger.log(PlannerMessage.KUBEDISPATCHER_RUN_ONCE);

			if (kubeConfig != null) {
				try {
					// Acquire the thread semaphore for the kubeConfig's production planner
					kubeConfig.getProductionPlanner().acquireThreadSemaphore("run");

					// Check for job steps to run
					UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig, 0, onlyRun, true);

					// Release the thread semaphore for the kubeConfig's production planner
					kubeConfig.getProductionPlanner().releaseThreadSemaphore("run");
				} catch (Exception e) {
					if (logger.isDebugEnabled())
						logger.debug("... exception in checkForJobStepsToRun(" + kubeConfig.getId() + ", " + 0 + ", " + onlyRun + ", true): ", e);

					// Release the thread semaphore in case of an exception
					kubeConfig.getProductionPlanner().releaseThreadSemaphore("run");
					logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
				}
			} else {
				logger.log(PlannerMessage.KUBEDISPATCHER_CONFIG_NOT_SET);
			}

		} else {
			// Execute the cyclic scenario

			if (productionPlanner != null) {
				if (wait <= 0) {
					logger.log(PlannerMessage.KUBEDISPATCHER_RUN_ONCE);

					try {
						// Acquire the thread semaphore for the production planner
						productionPlanner.acquireThreadSemaphore("run");

						// Check for job steps to run using the JobStepUtil
						UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig, 0, onlyRun, true);

						// Release the thread semaphore for the production planner
						productionPlanner.releaseThreadSemaphore("run");
					} catch (Exception e) {
						// Release the thread semaphore in case of an exception
						productionPlanner.releaseThreadSemaphore("run");
						logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
						
						if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
					}
				} else {
					while (!this.isInterrupted()) {
						// Look for job steps to run
						logger.log(PlannerMessage.KUBEDISPATCHER_CYCLE);

						// Only check for job steps if there are no released threads in the production planner
						if (productionPlanner.getReleaseThreads().size() == 0) {
							try {
								// Acquire the thread semaphore for the production planner
								productionPlanner.acquireThreadSemaphore("run");

								// Check for job steps to run using the JobStepUtil
								UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig, 0, onlyRun, true);

								// Release the thread semaphore for the production planner
								productionPlanner.releaseThreadSemaphore("run");
							} catch (Exception e) {
								if (logger.isDebugEnabled())
									logger.debug("... exception in checkForJobStepsToRun(" + kubeConfig.getId() + ", " + 0 + ", " + onlyRun + ", true): ", e);

								// Release the thread semaphore in case of an exception
								productionPlanner.releaseThreadSemaphore("run");
								logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
							}
						}

						try {
							// Sleep for the specified wait time
							logger.log(PlannerMessage.KUBEDISPATCHER_SLEEP, wait);
							sleep(wait);
						} catch (InterruptedException e) {
							// Log the interruption message
							logger.log(PlannerMessage.KUBEDISPATCHER_INTERRUPT);
							this.interrupt();
						}
					}
				}
			} else {
				logger.log(PlannerMessage.KUBEDISPATCHER_PLANNER_NOT_SET);
			}
		}
	}

}