/**
 * OrderReleaseThread.java
 * 
 * @author Ernst Melchinger
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.planner.PlannerResultMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;

/**
 * The thread to release a processing order
 *
 */
public class OrderReleaseThread extends Thread {

	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(OrderReleaseThread.class);

	/**
	 * The job utility instance 
	 */
	private JobUtil jobUtil;

	/** The Production Planner instance */
	private ProductionPlanner productionPlanner;

	/**
	 * The processing order to plan.
	 */
	private ProcessingOrder order;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/**
	 * The result of the planning
	 */
	private PlannerResultMessage resultMessage;

	private String user;
	private String pw;

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @return the pw
	 */
	public String getPw() {
		return pw;
	}

	/**
	 * @return the resultMessage
	 */
	public PlannerResultMessage getResultMessage() {
		return resultMessage;
	}

	/**
	 * Create new thread
	 * 
	 * @param productionPlanner The production planner instance
	 * @param em Entity manager for native queries
	 * @param jobUtil The job utility instance
	 * @param order The processing order to plan
	 * @param name The thread name
	 * @param user the username for calling other prosEO services (e. g. AIP Client)
	 * @param pw the password for calling other prosEO services (e. g. AIP Client)
	 */
	public OrderReleaseThread(ProductionPlanner productionPlanner, EntityManager em, JobUtil jobUtil, ProcessingOrder order, 
			String name, String user, String pw) {
		super(name);
		this.productionPlanner = productionPlanner;
		this.em = em;
		this.jobUtil = jobUtil;
		this.order = order;
		this.user = user;
		this.pw = pw;
	}

	/**
	 * Start and initialize the release thread
	 */
	public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run() for thread {}", this.getName());

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (order != null && productionPlanner != null && jobUtil != null) {
			answer = new PlannerResultMessage(GeneralMessage.TRUE);
			final long orderId = order.getId();
			try {
				if (answer.getSuccess()) {
					answer = release(order.getId());
				}
				if (!answer.getSuccess()) {
					// nothing to do here, order state already set in release
				}
			}
			catch(InterruptedException e) {
				answer.setMessage(PlannerMessage.ORDER_RELEASING_INTERRUPTED);
				answer.setText(logger.log(answer.getMessage(), this.getName(), order.getIdentifier()));
			} 
			catch(IllegalStateException e) {
				transactionTemplate.setReadOnly(true);
				final ProcessingOrder order = transactionTemplate.execute((status) -> {
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
					if (orderOpt.isPresent()) {
						return orderOpt.get();
					}
					return null;
				});
				if (order.getOrderState().equals(OrderState.RUNNING) || order.getOrderState().equals(OrderState.RELEASED)) {
					// order already set to RELEASED or RUNNING, nothing to do					
				} else {
					answer.setMessage(PlannerMessage.ORDER_RELEASING_EXCEPTION);
					answer.setText(logger.log(PlannerMessage.ORDER_RELEASING_EXCEPTION, this.getName(), order.getIdentifier()));
					answer.setText(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage()));
					productionPlanner.acquireThreadSemaphore("runRelease1");
					transactionTemplate.setReadOnly(false);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {

							transactionTemplate.execute((status) -> {
								ProcessingOrder lambdaOrder = null;
								Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
								if (orderOpt.isPresent()) {
									lambdaOrder = orderOpt.get();
								}
								lambdaOrder.setOrderState(OrderState.PLANNED);
								UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_QUEUED);
								lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
								return null;
							});
							break;
						} catch (CannotAcquireLockException e1) {
							if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e1);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e1;
							}
						}
					}

					productionPlanner.releaseThreadSemaphore("runRelease1");
				}
			}
			catch(Exception e) {
				answer.setMessage(PlannerMessage.ORDER_RELEASING_EXCEPTION);
				answer.setText(logger.log(PlannerMessage.ORDER_RELEASING_EXCEPTION, this.getName(), order.getIdentifier()));
				answer.setText(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage()));
				productionPlanner.acquireThreadSemaphore("runRelease2");
				transactionTemplate.setReadOnly(false);
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.execute((status) -> {
							ProcessingOrder lambdaOrder = null;
							Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
							if (orderOpt.isPresent()) {
								lambdaOrder = orderOpt.get();
							}
							lambdaOrder.setOrderState(OrderState.PLANNED);
							UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_QUEUED);
							lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
							return null;
						});
						break;
					} catch (CannotAcquireLockException e1) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e1);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e1;
						}
					}
				}

				productionPlanner.releaseThreadSemaphore("runRelease2");
			}
		}
		this.resultMessage = answer;
		productionPlanner.getReleaseThreads().remove(this.getName());
		if (logger.isTraceEnabled()) logger.trace("<<< run() for thread {}", this.getName());
		productionPlanner.checkNextForRestart();				
	}


	/**
	 * Release the processing order 
	 * 
	 * @param orderId The id of the order
	 * @return The result message
	 * @throws InterruptedException
	 */
	public PlannerResultMessage release(long orderId) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> release({})", (null == order ? "null" : order.getId()));
		ProcessingOrder order = null;

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		final List<Job> jobList = new ArrayList<Job>();
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		PlannerResultMessage releaseAnswer = new PlannerResultMessage(GeneralMessage.FALSE);
		try {
			productionPlanner.acquireThreadSemaphore("release1");
			transactionTemplate.setReadOnly(true);
			order = transactionTemplate.execute((status) -> {
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
				if (orderOpt.isPresent()) {
					jobList.addAll(orderOpt.get().getJobs());

					jobList.sort(new Comparator<Job>() {
						@Override
						public int compare(Job o1, Job o2) {
							return o1.getStartTime().compareTo(o2.getStartTime());
						}});

					return orderOpt.get();
				}
				return null;
			});
			productionPlanner.releaseThreadSemaphore("release1");	
		} catch (Exception e) {
			answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
			answer.setText(logger.log(answer.getMessage(), e.getMessage()));

			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			productionPlanner.releaseThreadSemaphore("release1");
			return answer;
		}
		if (order != null && 
				(order.getOrderState() == OrderState.RELEASING || order.getOrderState() == OrderState.PLANNED)) {


			int packetSize = ProductionPlanner.config.getPlanningBatchSize();
			final Long jCount = (long) jobList.size();
			List<Integer> curJList = new ArrayList<Integer>();
			curJList.add(0);
			List<Integer> curJSList = new ArrayList<Integer>();
			curJSList.add(0);
			try {
				while (curJList.get(0) < jCount) {

					// Prepare for transaction retry, if "org.springframework.dao.CannotAcquireLockException" is thrown
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							productionPlanner.acquireThreadSemaphore("releaseOrder");
							if (logger.isTraceEnabled())
								logger.trace(">>> releaseJobBlock({})", curJList.get(0));

							transactionTemplate.setReadOnly(false);
							Object answer1 = transactionTemplate.execute((status) -> {
								curJSList.set(0, 0);
								PlannerResultMessage locAnswer = new PlannerResultMessage(GeneralMessage.TRUE);
								while (curJList.get(0) < jCount && curJSList.get(0) < packetSize) {
									if (this.isInterrupted()) {
										return PlannerMessage.ORDER_RELEASING_INTERRUPTED;
									}
									if (jobList.get(curJList.get(0)).getJobState() == JobState.PLANNED) {
										Job locJob = RepositoryService.getJobRepository()
												.getOne(jobList.get(curJList.get(0)).getId());
										try {
											locAnswer = jobUtil.resume(locJob);
										} catch (Exception e) {
											if (logger.isDebugEnabled())
												logger.debug("... exception in resume(" + locJob.getId() + "): ", e);
											throw e;
										}
										curJSList.set(0, curJSList.get(0) + locJob.getJobSteps().size());
									}
									curJList.set(0, curJList.get(0) + 1);
								}
								return locAnswer;
							});

							if (answer1 instanceof PlannerResultMessage) {
								answer = (PlannerResultMessage) answer1;
							}
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						} catch (Exception e) {
							if (logger.isDebugEnabled())
								logger.debug("... exception in release::doInTransaction1(" + orderId + "): ", e);

							throw e;
						} finally {
							productionPlanner.releaseThreadSemaphore("releaseOrder");
						} 
						if (answer == null || answer.getCode() == PlannerMessage.ORDER_RELEASING_INTERRUPTED.getCode()) {
							break;
						}
					}
					// Prepare for transaction retry, if "org.springframework.dao.CannotAcquireLockException" is thrown
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							productionPlanner.acquireThreadSemaphore("releaseOrder2");
							// This one requires special handling, because as a "side effect" the Kubernetes job is started
							// and must be cancelled, if the transaction fails
							transactionTemplate.setReadOnly(false);

							transactionTemplate.execute((status) -> {

								String nativeQuery = "SELECT j.start_time, js.id, pf.name "
										+ "FROM processing_order o "
										+ "JOIN job j ON o.id = j.processing_order_id "
										+ "JOIN job_step js ON j.id = js.job_id "
										+ "JOIN processing_facility pf ON j.processing_facility_id = pf.id "
										+ "WHERE o.id = :orderId "
										+ "AND js.job_step_state = :jsState "
										+ "ORDER BY js.priority desc, j.start_time, js.id";

								List<?> jobStepList = em.createNativeQuery(nativeQuery)
										.setParameter("orderId", orderId)
										.setParameter("jsState", JobStepState.READY.toString())
										.getResultList();

								for (Object jobStepObject: jobStepList) {
									if (jobStepObject instanceof Object[]) {

										Object[] jobStep = (Object[]) jobStepObject;

										if (logger.isTraceEnabled()) logger.trace("... found job step info {}", Arrays.asList(jobStep));

										// jobStep[0] is only used for ordering the result list
										Object jsIdObject = jobStep[1];
										Object pfNameObject = jobStep[2];

										Long jsId = jsIdObject instanceof BigInteger ? ((BigInteger) jsIdObject).longValue() : null;
										String pfName = pfNameObject instanceof String ? (String) pfNameObject : null;

										if (null == jsId || null == pfName) {
											logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, "Invalid query result: " + Arrays.asList(jobStep));

											throw new RuntimeException("Invalid query result");
										}

										try {
											KubeConfig kc = productionPlanner.getKubeConfig(pfName);
											if (!kc.couldJobRun(null)) {
												break;
											}
											UtilService.getJobStepUtil().checkJobStepToRun(kc, jsId);
										} catch (Exception e) {
											if (logger.isDebugEnabled())
												logger.debug("... exception in checkJobStepToRun(" + pfName + ", " + jsId + "): ", e);

											logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
											throw e;
										} 

									} else {
										logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, "Invalid query result: " + jobStepObject);
										throw new RuntimeException("Invalid query result");
									}
								}

								return null;					
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						} catch (Exception e) {
							if (logger.isDebugEnabled())
								logger.debug("... exception in release::doInTransaction2(" + orderId + "): ", e);
							throw e;
						} finally {
							productionPlanner.releaseThreadSemaphore("releaseOrder2");
						}
					}
				}
			} catch (Exception e) {	
				logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

				throw e;
			}
			if (this.isInterrupted()) {
				answer.setMessage(PlannerMessage.ORDER_RELEASING_INTERRUPTED);
				answer.setText(logger.log(answer.getMessage(), this.getName(), order.getIdentifier()));
				throw new InterruptedException();
			}
			final PlannerResultMessage finalAnswer = new PlannerResultMessage(releaseAnswer.getMessage());
			try {
				productionPlanner.acquireThreadSemaphore("releaseOrder3");	

				// TODO Add transaction retry here, but need to find the retry condition first
				transactionTemplate.setReadOnly(false);
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {

						answer = transactionTemplate.execute((status) -> {
							ProcessingOrder lambdaOrder = null;
							Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
							if (orderOpt.isPresent()) {
								lambdaOrder = orderOpt.get();
							}
							PlannerResultMessage lambdaAnswer = new PlannerResultMessage(GeneralMessage.FALSE);
							if (finalAnswer.getSuccess() || finalAnswer.getCode() == PlannerMessage.JOB_ALREADY_COMPLETED.getCode()) {
								if (lambdaOrder.getJobs().isEmpty()) {
									lambdaOrder.setOrderState(OrderState.COMPLETED);
									UtilService.getOrderUtil().checkAutoClose(lambdaOrder);
									UtilService.getOrderUtil().setTimes(lambdaOrder);
									UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_COMPLETED);
									lambdaAnswer.setMessage(PlannerMessage.ORDER_PRODUCT_EXIST);
								} else {
									// check whether order is already running
									Boolean running = false;
									for (Job j : lambdaOrder.getJobs()) {
										if (RepositoryService.getJobStepRepository().countJobStepRunningByJobId(j.getId()) > 0) {
											running = true;
											break;
										}
									}
									if (!lambdaOrder.getOrderState().equals(OrderState.RUNNING)) {
										lambdaOrder.setOrderState(OrderState.RELEASED);
										UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_RUNNING);
										if (running) {
											lambdaOrder.setOrderState(OrderState.RUNNING);
											UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_RUNNING);
										}
									}
									lambdaAnswer.setMessage(PlannerMessage.ORDER_RELEASED);
								}
								lambdaAnswer.setText(logger.log(lambdaAnswer.getMessage(), lambdaOrder.getIdentifier()));
								lambdaOrder.incrementVersion();
								try {
									lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
								} catch (Exception e) {
									if (logger.isDebugEnabled())
										logger.debug("... exception in getOrderRepository.save1(" + lambdaOrder.getIdentifier() + "): ", e);
									throw e;
								}
							} else {
								// the order is also released
								Boolean running = false;
								for (Job j : lambdaOrder.getJobs()) {
									if (RepositoryService.getJobStepRepository().countJobStepRunningByJobId(j.getId()) > 0) {
										running = true;
										break;
									}
								}
								if (!lambdaOrder.getOrderState().equals(OrderState.RUNNING)) {
									lambdaOrder.setOrderState(OrderState.RELEASED);
									UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_RUNNING);
									if (running) {
										lambdaOrder.setOrderState(OrderState.RUNNING);
										UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_RUNNING);
									}
								}
								lambdaAnswer.setMessage(PlannerMessage.ORDER_RELEASED);
								lambdaAnswer.setText(logger.log(lambdaAnswer.getMessage(), lambdaOrder.getIdentifier(), this.getName()));
								try {
									lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
								} catch (Exception e) {
									if (logger.isDebugEnabled())
										logger.debug("... exception in getOrderRepository.save2(" + lambdaOrder.getIdentifier() + "): ", e);
									throw e;
								}
							}
							return lambdaAnswer;
						});
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}

			} catch (Exception e) {	
				if (logger.isDebugEnabled())
					logger.debug("... exception in release::doInTransaction3(" + orderId + "): ", e);

				logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
				throw e;
			} finally {
				productionPlanner.releaseThreadSemaphore("releaseOrder3");
			}
		}

		return answer;
	}

}
