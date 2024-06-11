/**
 * JobStepUtil.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.SimplePolicy;
import de.dlr.proseo.model.enums.OrderSource;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.rest.model.RestProduct;
import de.dlr.proseo.model.service.ProductQueryService;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.PlannerResultMessage;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.ProductionPlannerConfiguration;
import de.dlr.proseo.planner.ProductionPlannerSecurityConfig;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.service.ServiceConnection;

/**
 * Utility class for managing job steps.
 *
 * @author Ernst Melchinger
 */
@Component
// @Transactional(isolation = Isolation.REPEATABLE_READ)
public class JobStepUtil {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(JobStepUtil.class);

	/** Allbytime download path */
	private final String URI_PATH_DOWNLOAD_ALLBYTIME = "/download/allbytime";

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** The product query service */
	@Autowired
	private ProductQueryService productQueryService;

	/** The production planner instance */
	@Autowired
	private ProductionPlanner productionPlanner;

	/** The service connection */
	@Autowired
	private ServiceConnection serviceConnection;

	/** The planner configuration */
	@Autowired
	ProductionPlannerConfiguration config;

	/** The planner security configuration */
	@Autowired
	ProductionPlannerSecurityConfig securityConfig;

	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/**
	 * Retrieves job steps with a specific job step state, associated with a given mission code. Results are ordered by processing
	 * completion time in descending order, limited to a specified number.
	 * 
	 * @param state   The job step state
	 * @param mission The mission code
	 * @param limit   The maximum number of entries to return
	 * @return A list of job steps meeting the criteria
	 */
	public List<JobStep> findOrderedByJobStepStateAndMission(JobStepState state, String mission, int limit) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findOrderedByJobStepStateAndMission({}, {}, {})", state, mission, limit);

		String query = "select js from JobStep js " + " inner join Job j on js.job.id = j.id "
				+ " inner join ProcessingOrder o on j.processingOrder.id = o.id" + " inner join Mission m on o.mission.id = m.id "
				+ " where js.processingCompletionTime is not null and js.jobStepState = '" + state + "' and m.code = '" + mission
				+ "' order by js.processingCompletionTime desc";

		// em.createNativeQ

		return em.createQuery(query, JobStep.class).setMaxResults(limit).getResultList();
	}

	/**
	 * Searches for job steps with unsatisfied product queries associated with a given processing facility and product class. If any
	 * queries are now satisfied, changes the state of the corresponding job steps to READY.
	 *
	 * @param pfId        The ID of the processing facility
	 * @param pcId        The ID of the product class
	 * @param onlyWaiting Flag indicating whether to include only job steps in the WAITING_INPUT state
	 */
	// @Transactional(isolation = Isolation.REPEATABLE_READ)
	public void searchForJobStepsToRun(long pfId, long pcId, boolean onlyWaiting) {

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);

		final ProcessingFacility processingFacility = transactionTemplate.execute((status) -> {
			Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(pfId);
			if (opt.isPresent()) {
				return opt.get();
			}
			return null;
		});

		final ProductClass productClass = transactionTemplate.execute((status) -> {
			Optional<ProductClass> opt = RepositoryService.getProductClassRepository().findById(pcId);
			if (opt.isPresent()) {
				return opt.get();
			}
			return null;
		});

		if (logger.isTraceEnabled())
			logger.trace(">>> searchForJobStepsToRun({}, {}, {})",
					(null == processingFacility ? "null" : processingFacility.getName()),
					(null == productClass ? "null" : productClass.getProductType()), onlyWaiting);

		// Determine job step states to consider
		List<String> jobStepStates = new ArrayList<>();
		if (!onlyWaiting) {
			jobStepStates.add(JobStep.JobStepState.PLANNED.toString());
		}
		jobStepStates.add(JobStepState.WAITING_INPUT.toString());

		// Fetch job steps based on criteria
		List<Long> allJobSteps = transactionTemplate.execute((status) -> {
			List<Long> jobSteps = null;

			if (processingFacility == null) {
				jobSteps = new ArrayList<>();

				if (productClass != null) {
					// Find job steps for product queries without a processing facility constraint
					List<ProductQuery> productQueries = RepositoryService.getProductQueryRepository()
						.findUnsatisfiedByProductClass(pcId);
					for (ProductQuery pq : productQueries) {
						if (pq.getJobStep().getJobStepState().equals(JobStepState.WAITING_INPUT)
								&& pq.getJobStep().getJob().getJobState() != JobState.ON_HOLD) {
							jobSteps.add(pq.getJobStep().getId());
						} else if (!onlyWaiting && pq.getJobStep().getJobStepState().equals(JobStepState.PLANNED)
								&& pq.getJobStep().getJob().getJobState() != JobState.ON_HOLD) {
							jobSteps.add(pq.getJobStep().getId());
						}
					}

					jobSteps.sort(null);
				} else {
					// Find all job steps if no product class constraint is provided
					for (ProcessingFacility pf : RepositoryService.getFacilityRepository().findAll()) {
						jobSteps.addAll(findAllByProcessingFacilityAndJobStepStateInAndOrderBy(pf.getId(), jobStepStates));
					}
				}
			} else {
				if (productClass != null) {
					// Find job steps for product queries with a specific processing facility constraint
					jobSteps = new ArrayList<>();

					List<ProductQuery> productQueries = RepositoryService.getProductQueryRepository()
						.findUnsatisfiedByProductClass(pcId);

					for (ProductQuery productQuery : productQueries) {
						if (productQuery.getJobStep().getJob().getProcessingFacility().getId() == pfId) {
							if (productQuery.getJobStep().getJobStepState().equals(JobStepState.WAITING_INPUT)
									&& productQuery.getJobStep().getJob().getJobState() != JobState.ON_HOLD) {
								jobSteps.add(productQuery.getJobStep().getId());
							} else if (!onlyWaiting && productQuery.getJobStep().getJobStepState().equals(JobStepState.PLANNED)
									&& productQuery.getJobStep().getJob().getJobState() != JobState.ON_HOLD) {
								jobSteps.add(productQuery.getJobStep().getId());
							}
						}
					}

					jobSteps.sort(null);
				} else {
					// Find all job steps associated with the given processing facility
					jobSteps = findAllByProcessingFacilityAndJobStepStateInAndOrderBy(pfId, jobStepStates);
				}
			}

			return jobSteps;
		});

		// Process each job step
		for (Long jobStepId : allJobSteps) {
			for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
				try {
					transactionTemplate.setReadOnly(false);
					transactionTemplate.execute((status) -> {
						Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(jobStepId);
						if (opt.isPresent()) {
							checkJobStepQueries(opt.get(), false);
						}
						return null;
					});

					break;
				} catch (CannotAcquireLockException e) {
					if (logger.isDebugEnabled())
						logger.debug("... database concurrency issue detected: ", e);

					if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
						ProseoUtil.dbWait();
					} else {
						if (logger.isDebugEnabled())
							logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
						throw e;
					}
				}
			}
		}
	}

	/**
	 * Finds all job steps in the given states to be executed in the specified processing facility.
	 *
	 * @param processingFacilityId The database ID of the processing facility
	 * @param jobStepStates        A list of job step state names to filter by
	 * @return A list of IDs of job steps meeting the criteria
	 */
	private List<Long> findAllByProcessingFacilityAndJobStepStateInAndOrderBy(long processingFacilityId,
			List<String> jobStepStates) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findAllByProcessingFacilityAndJobStepStateInAndOrderBy({}, {}), sort order: {}", processingFacilityId,
					jobStepStates, config.getJobStepSort());

		// Determine the native query based on the configured sort order
		String nativeQuery;
		switch (config.getJobStepSort()) {
		case SUBMISSION_TIME:
			nativeQuery = "SELECT po.submission_time, js.id " + "FROM job j " + "JOIN job_step js ON j.id = js.job_id "
					+ "JOIN processing_order po ON po.id = j.processing_order_id " + "WHERE j.processing_facility_id = :pfId "
					+ "AND js.job_step_state IN :jsStates " + "ORDER BY js.priority desc, po.submission_time, js.id";
			break;
		case SENSING_TIME:
			nativeQuery = "SELECT j.start_time, js.id " + "FROM job j " + "JOIN job_step js ON j.id = js.job_id "
					+ "WHERE j.processing_facility_id = :pfId " + "AND js.job_step_state IN :jsStates "
					+ "ORDER BY js.priority desc, j.start_time, js.id";
			break;
		default:
			nativeQuery = "SELECT j.start_time, js.id " + "FROM job j " + "JOIN job_step js ON j.id = js.job_id "
					+ "WHERE j.processing_facility_id = :pfId " + "AND js.job_step_state IN :jsStates "
					+ "ORDER BY js.priority desc, j.start_time, js.id";
			break;

		}

		// Execute the native query to fetch job steps
		List<?> jobStepList = em.createNativeQuery(nativeQuery)
			.setParameter("pfId", processingFacilityId)
			.setParameter("jsStates", jobStepStates)
			.getResultList();

		// Process the query result
		List<Long> resultList = new ArrayList<>();
		for (Object jobStepObject : jobStepList) {
			if (jobStepObject instanceof Object[]) {

				Object[] jobStep = (Object[]) jobStepObject;

				if (logger.isTraceEnabled())
					logger.trace("... found job step info {}", Arrays.asList(jobStep));

				// Extract the job step ID for ordering the result list
				Long jobStepId = jobStep[1] instanceof BigInteger ? ((BigInteger) jobStep[1]).longValue() : null;

				if (null == jobStepId) {
					throw new RuntimeException("Invalid query result: " + Arrays.asList(jobStep));
				}

				resultList.add(jobStepId);

			} else {
				throw new RuntimeException("Invalid query result: " + jobStepObject);
			}
		}

		return resultList;
	}

	/**
	 * Suspends a job step, either forcefully terminating it or waiting until completion.
	 * 
	 * @param js    The job step to suspend
	 * @param force True to forcibly terminate the job step, false to wait until completion
	 * @return A PlannerResultMessage indicating the outcome of the suspension attempt
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public PlannerResultMessage suspend(JobStep js, Boolean force) {
		if (logger.isTraceEnabled())
			logger.trace(">>> suspend({}, {})", (null == js ? "null" : js.getId()), force);

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

		// Ensure current state allows for suspension
		// PLANNED, WAITING_INPUT, READY, RUNNING, COMPLETED, FAILED
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
				answer.setMessage(PlannerMessage.JOBSTEP_SUSPENDED);
				break;

			case READY:
			case WAITING_INPUT:
				// If the job step is ready or waiting for input, change its state to PLANNED
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.PLANNED);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
				em.merge(js);
				answer.setMessage(PlannerMessage.JOBSTEP_SUSPENDED);
				break;

			case RUNNING:
				Boolean deleted = false;
				// If force is true, attempt to forcibly terminate the job step
				if (force != null && force) {
					KubeConfig kubeConfig = productionPlanner.getKubeConfig(js.getJob().getProcessingFacility().getName());

					if (kubeConfig != null) {
						KubeJob kubeJob = kubeConfig.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());

						if (kubeJob != null) {
							deleted = kubeConfig.deleteJob(kubeJob.getJobName());
						} else {
							kubeConfig.sync();
							kubeJob = kubeConfig.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());

							if (kubeJob != null) {
								deleted = kubeConfig.deleteJob(kubeJob.getJobName());
							} else {
								// Job was not found, it was deleted anywhere else
								deleted = true;
							}
						}
					}
				}
				// Process the outcome of the termination attempt
				if (deleted) {
					em.merge(js);
					if (js.getJobStepState() == JobStepState.COMPLETED) {
						answer.setMessage(PlannerMessage.JOBSTEP_COMPLETED);
					} else if (js.getJobStepState() == JobStepState.FAILED) {
						answer.setMessage(PlannerMessage.JOBSTEP_COMPLETED);
					} else {
						// Reset job step attributes if it's neither completed nor failed
						js.setProcessingStartTime(null);
						js.setProcessingCompletionTime(null);
						js.setProcessingStdOut(null);
						js.setProcessingStdErr(null);
						js.setJobOrderFilename(null);
						js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.PLANNED);
						js.incrementVersion();

						answer.setMessage(PlannerMessage.JOBSTEP_SUSPENDED);
						RepositoryService.getJobStepRepository().save(js);
						em.merge(js);
					}
				} else {
					// Check if the job step is still running
					KubeConfig kubeConfig = productionPlanner.getKubeConfig(js.getJob().getProcessingFacility().getName());

					if (kubeConfig != null) {
						kubeConfig.sync();
						KubeJob kj = kubeConfig.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());

						if (kj != null) {
							answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_RUNNING);
						}
					} else {
						answer.setMessage(PlannerMessage.JOBSTEP_SUSPENDED);
					}
				}
				break;

			case COMPLETED:
				answer.setMessage(PlannerMessage.JOBSTEP_COMPLETED);
				break;

			case FAILED:
				answer.setMessage(PlannerMessage.JOBSTEP_FAILED);
				break;

			case CLOSED:
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_CLOSED);
				break;

			default:
				break;
			}

			answer.setText(logger.log(answer.getMessage(), js.getId()));
		}

		return answer;
	}

	/**
	 * Cancels a job step.
	 * 
	 * @param js The job step to cancel
	 * @return A PlannerResultMessage indicating the outcome of the cancellation attempt
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public PlannerResultMessage cancel(JobStep js) {
		if (logger.isTraceEnabled())
			logger.trace(">>> cancel({})", (null == js ? "null" : js.getId()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

		// Ensure the current job step state allows for cancellation
		if (js != null) {
			switch (js.getJobStepState()) {

			case PLANNED:
				// If the job step is in PLANNED state, mark it as FAILED
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.FAILED);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
				em.merge(js);
				answer.setMessage(PlannerMessage.JOBSTEP_CANCELED);
				break;

			case READY:
			case WAITING_INPUT:
			case RUNNING:
				// If the job step is in a running state, it cannot be canceled
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_RUNNING);
				break;

			case COMPLETED:
				// If the job step is already completed, it cannot be canceled
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_COMPLETED);
				break;

			case FAILED:
				// If the job step is already failed, it cannot be canceled
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_FAILED);
				break;

			case CLOSED:
				// If the job step is closed, it cannot be canceled
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_CLOSED);
				break;

			default:
				break;
			}

			answer.setText(logger.log(answer.getMessage(), js.getId()));
		}

		return answer;
	}

	/**
	 * Closes a job step.
	 * 
	 * @param id The ID of the job step to close
	 * @return A PlannerResultMessage indicating the outcome of the closure attempt
	 */
	public PlannerResultMessage close(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> close({})", (null == id ? "null" : id));

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		final JobStepState jobStepState = transactionTemplate.execute((status) -> {
			String sqlQuery = "select job_step_state from job_step where id = " + id + ";";
			Query query = em.createNativeQuery(sqlQuery);
			Object o = query.getSingleResult();
			return JobStepState.valueOf((String) o);
		});

		// Ensure current job step state allows for closing
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (id != null) {
			switch (jobStepState) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
			case RUNNING:
				// Closing not allowed
				answer.setMessage(PlannerMessage.JOBSTEP_COULD_NOT_CLOSE);
				break;

			case COMPLETED:
			case FAILED:
				// Delete satisfied product queries and update job step state to CLOSED
				UtilService.getJobStepUtil().deleteSatisfiedProductQueries(id);

				transactionTemplate.execute((status) -> {
					String sqlQuery = "select version from job_step where id = " + id + ";";
					Query query = em.createNativeQuery(sqlQuery);
					Object o = query.getSingleResult();
					Integer version = (Integer) o;
					sqlQuery = "update job_step set job_step_state = 'CLOSED', version = " + (version + 1) + " where id = " + id
							+ ";";
					query = em.createNativeQuery(sqlQuery);
					query.executeUpdate();
					return null;
				});

				answer.setMessage(PlannerMessage.JOBSTEP_CLOSED);
				break;

			case CLOSED:
				// Job step is already closed
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_CLOSED);
				break;

			default:
				break;
			}

			answer.setText(logger.log(answer.getMessage(), id));
		}

		return answer;
	}

	/**
	 * Retries a job step, attempting to re-execute it if it previously failed.
	 * 
	 * @param js The job step to retry
	 * @return A PlannerResultMessage indicating the outcome of the retry attempt
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public PlannerResultMessage retry(JobStep js) {
		if (logger.isTraceEnabled())
			logger.trace(">>> retry({})", (null == js ? "null" : js.getId()));

		// Ensure current job step state allows for retrying
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case WAITING_INPUT:
			case READY:
			case RUNNING:
			case COMPLETED:
			case CLOSED:
				// Retry not allowed
				answer.setMessage(PlannerMessage.JOBSTEP_COULD_NOT_RETRY);
				break;

			case FAILED:
				Product outputProduct = js.getOutputProduct();

				if (outputProduct != null) {
					// Collect output products and retry the job step
					List<Product> outputProductList = new ArrayList<>();
					collectProducts(outputProduct, outputProductList);
					js.setIsFailed(false);

					if (checkProducts(outputProductList, js.getJob().getProcessingFacility())) {
						// Product was created, due to some communication problems the wrapper process finished with errors.
						// Discard this problem and set job step to completed
						if (js.getJobStepState() == de.dlr.proseo.model.JobStep.JobStepState.FAILED) {
							js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.PLANNED);
						}

						js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.READY);
						js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.RUNNING);
						js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.COMPLETED);
						UtilService.getJobStepUtil().checkCreatedProducts(js);
						js.incrementVersion();

						RepositoryService.getJobStepRepository().save(js);
						em.merge(js);
						answer.setMessage(PlannerMessage.JOBSTEP_RETRIED_COMPLETED);

						break;
					}
				}

				// Reset the job step and mark it as PLANNED
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.PLANNED);
				js.setProcessingStartTime(null);
				js.setProcessingCompletionTime(null);
				js.setProcessingStdOut(null);
				js.setProcessingStdErr(null);
				js.setJobOrderFilename(null);
				js.incrementVersion();

				RepositoryService.getJobStepRepository().save(js);
				em.merge(js);
				answer.setMessage(PlannerMessage.JOBSTEP_RETRIED);
				break;

			default:
				break;
			}

			answer.setText(logger.log(answer.getMessage(), js.getId()));
		}

		return answer;
	}

	/**
	 * Checks whether a job step has been finished.
	 * 
	 * @param js The job step to check
	 * @return true if the job step has finished, false otherwise
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public Boolean checkFinish(JobStep js) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkFinish({})", (null == js ? "null" : js.getId()));

		boolean answer = false;

		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
			case CLOSED:
				// Job step not finished yet
				break;

			case RUNNING:
			case COMPLETED:
				// If the job step is running or completed, mark it as finished
				em.merge(js);
				UtilService.getJobUtil().checkFinish(js.getJob().getId());
				answer = true;
				break;

			case FAILED:
				// If the job step failed, mark it as failed and check job finish
				js.setIsFailed(true);
				RepositoryService.getJobStepRepository().save(js);
				em.merge(js);
				UtilService.getJobUtil().setHasFailedJobSteps(js.getJob(), true);
				UtilService.getJobUtil().checkFinish(js.getJob().getId());
				answer = true;
				break;

			default:
				break;
			}
		}
		return answer;
	}

	/**
	 * Deletes a job step if it is in a deletable state.
	 * 
	 * @param js The job step to delete
	 * @return true if the job step is deleted, false otherwise
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public Boolean delete(JobStep js) {
		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", (null == js ? "null" : js.getId()));

		boolean answer = false;
		// Ensure current job step state allows for deletion
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
				// Delete the output product if exists and proceed to delete the job step
				if (js.getOutputProduct() != null) {
					deleteProduct(js.getOutputProduct());
					js.setOutputProduct(null);
				}
				// Fall through to continue deletion process
			case COMPLETED:
			case FAILED:
			case CLOSED:
				// Delete the job order file and related product queries
				deleteJOF(js);
				js.setJobOrderFilename(null);

				if (js.getOutputProduct() != null) {
					js.getOutputProduct().setJobStep(null);
				}

				for (ProductQuery productQuery : js.getInputProductQueries()) {
					for (Product product : productQuery.getSatisfyingProducts()) {
						product.getSatisfiedProductQueries().remove(productQuery);
						RepositoryService.getProductRepository().save(product);
					}
					productQuery.getSatisfyingProducts().clear();
					RepositoryService.getProductQueryRepository().delete(productQuery);
				}

				js.getInputProductQueries().clear();
				logger.log(PlannerMessage.JOBSTEP_DELETED, String.valueOf(js.getId()));
				answer = true;
				break;

			case RUNNING:
				// Cannot delete a job step while it's running
				logger.log(PlannerMessage.JOBSTEP_ALREADY_RUNNING, String.valueOf(js.getId()));
				break;

			default:
				break;
			}
		}

		return answer;
	}

	/**
	 * Deletes the satisfied product queries associated with a job step if it's in a deletable state.
	 *
	 * @param jsId The ID of the job step
	 * @return true if the satisfied product queries are deleted, false otherwise
	 */
	public Boolean deleteSatisfiedProductQueries(Long jsId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteSatisfiedProductQueries({})", (null == jsId ? "null" : jsId));

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		final JobStep jobStep = transactionTemplate.execute((status) -> {
			Optional<JobStep> jsOpt = RepositoryService.getJobStepRepository().findById(jsId);
			if (jsOpt.isPresent()) {
				return jsOpt.get();
			}
			return null;
		});

		boolean answer = false;
		if (jobStep != null) {
			switch (jobStep.getJobStepState()) {
			case COMPLETED:
			case FAILED:
			case CLOSED:
				// select id from product_query where job_step_id = jsId;

				// delete from product_query_satisfying_products where satisfied_product_queries_id = pqid
				// delete from product_query_filter_conditions where product_query_id = pqid

				transactionTemplate.execute((status) -> {
					String sqlQuery = "select id from product_query where job_step_id = " + jsId + ";";
					Query query = em.createNativeQuery(sqlQuery);
					List<?> pqIds = query.getResultList();

					for (Object o : pqIds) {
						if (o instanceof BigInteger) {
							Long pqId = ((BigInteger) o).longValue();
							sqlQuery = "delete from product_query_satisfying_products where satisfied_product_queries_id = " + pqId
									+ ";";
							query = em.createNativeQuery(sqlQuery);
							query.executeUpdate();
							sqlQuery = " delete from product_query_filter_conditions where product_query_id = " + pqId + ";";
							query = em.createNativeQuery(sqlQuery);
							query.executeUpdate();
							sqlQuery = " delete from product_query where id = " + pqId + ";";
							query = em.createNativeQuery(sqlQuery);
							query.executeUpdate();
						}
					}
					return null;
				});
//
//				for (ProductQuery pq : js.getInputProductQueries()) {
//					for (Product p : pq.getSatisfyingProducts()) {
//
//						p.getSatisfiedProductQueries().remove(pq);
//						RepositoryService.getProductRepository().save(p);
//					}
//					pq.getSatisfyingProducts().clear();
//					RepositoryService.getProductQueryRepository().delete(pq);
//				}
//				js.getInputProductQueries().clear();

				logger.log(PlannerMessage.JOBSTEP_SPQ_DELETED, String.valueOf(jobStep.getId()));
				answer = true;
				break;

			default:
				break;
			}
		}
		return answer;
	}

	/**
	 * Deletes a job step forcefully, including those that are not finished.
	 * 
	 * @param js The job step to delete
	 * @return true if the job step is deleted, false otherwise
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public Boolean deleteForced(JobStep js) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteForced({})", (null == js ? "null" : js.getId()));

		boolean answer = false;

		// Ensure current job step state allows for deletion
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
				// Delete the output product if exists and proceed to delete the job step
				if (js.getOutputProduct() != null) {
					deleteProduct(js.getOutputProduct());
					js.setOutputProduct(null);
				}
				// Fall through to continue deletion process
			case RUNNING:
			case COMPLETED:
			case CLOSED:
				// Delete the job order file and related product queries
				deleteJOF(js);
				js.setJobOrderFilename(null);

				if (js.getOutputProduct() != null) {
					js.getOutputProduct().setJobStep(null);
				}

				for (ProductQuery pq : js.getInputProductQueries()) {
					for (Product p : pq.getSatisfyingProducts()) {
						p.getSatisfiedProductQueries().clear();
						RepositoryService.getProductRepository().save(p);
					}
					pq.getSatisfyingProducts().clear();
					RepositoryService.getProductQueryRepository().delete(pq);
				}

				js.getInputProductQueries().clear();
				// RepositoryService.getJobStepRepository().delete(js);
				logger.log(PlannerMessage.JOBSTEP_DELETED, String.valueOf(js.getId()));
				answer = true;
				break;

			default:
				break;
			}
		}

		return answer;
	}

	/**
	 * Resume job step
	 *
	 * @param js Job step
	 * @return Result message
	 */
	public PlannerResultMessage resume(JobStep js, Boolean force) {
		if (logger.isTraceEnabled())
			logger.trace(">>> resume({}, {})", js.getId(), force);

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		// Ensure current job step state allows for resumption
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case WAITING_INPUT:
				try {
					checkJobStepQueries(js, force);
				} catch (Exception e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				}
				if (js.getJobStepState() == JobStepState.WAITING_INPUT) {
					answer.setMessage(PlannerMessage.JOBSTEP_WAITING);
				} else {
					answer.setMessage(PlannerMessage.JOBSTEP_READY);
				}
				break;

			case READY:
				answer.setMessage(PlannerMessage.JOBSTEP_READY);
				break;

			case RUNNING:
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_RUNNING);
				break;

			case COMPLETED:
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_COMPLETED);
				break;

			case FAILED:
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_FAILED);
				break;

			default:
				break;
			}

			answer.setText(logger.log(answer.getMessage(), js.getId()));
		}
		return answer;
	}

	/**
	 * Initiates the execution of a job step on a Kubernetes cluster.
	 * 
	 * @param js The job step to start
	 * @return True if the job step was successfully started, false otherwise
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public Boolean startJobStep(JobStep js) {
		if (logger.isTraceEnabled())
			logger.trace(">>> startJobStep({})", (null == js ? "null" : js.getId()));

		boolean answer = false;
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case WAITING_INPUT:
				// If the job step is in PLANNED or WAITING_INPUT state, do nothing
				break;

			case READY:
				// If the job step is in READY state, initiate execution
				UtilService.getJobUtil().startJob(js.getJob());
				js.setJobStepState(JobStepState.RUNNING);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
				em.merge(js);
				UtilService.getOrderUtil().logOrderState(js.getJob().getProcessingOrder());
				logger.log(PlannerMessage.JOBSTEP_STARTED, String.valueOf(js.getId()));
				answer = true;
				break;

			case RUNNING:
				answer = true;
				break;

			case COMPLETED:
			case FAILED:
			default:
				break;
			}
		}

		return answer;
	}

	/**
	 * Checks the input product queries of a job step when the job is released or started. If force is true, checks all queries.
	 *
	 * @param js    The job step to check
	 * @param force If true, forces the check on all queries
	 */
	// @Transactional(isolation = Isolation.REPEATABLE_READ)
	public void checkJobStepQueries(JobStep js, Boolean force) {

		if (logger.isTraceEnabled())
			logger.trace(">>> checkJobStepQueries({}, {}), jobStep state: {}", (null == js ? "null" : js.getId()), force,
					js.getJobStepState());

		// Proceed only if the job step is not null and is in PLANNED or WAITING_INPUT state
		if (js != null && (js.getJobStepState() == JobStepState.PLANNED || js.getJobStepState() == JobStepState.WAITING_INPUT)) {

			// Check if the associated job is not null and either force is true or the job is released or started
			if (js.getJob() != null
					&& (force || js.getJob().getJobState() == JobState.RELEASED || js.getJob().getJobState() == JobState.STARTED)) {
				logger.trace("Looking for product queries of job step: " + js.getId());

				boolean hasUnsatisfiedInputQueries = false;

				// Set lock timeout for JPA operations
				Map<String, Object> properties = new HashMap<>();
				properties.put("javax.persistence.lock.timeout", 10000L);

				// Iterate over the input product queries of the job step
				for (ProductQuery productQuery : js.getInputProductQueries()) {
					if (!productQuery.isSatisfied()) {
						// Execute the product query
						if (productQueryService.executeQuery(productQuery, false)) {
							// If the query is successfully executed, update its state and save
							if (js.getJob().getProcessingOrder().getOrderSource() == OrderSource.ODIP) {
								if (!productQuery.getInDownload() && productQuery.getSatisfyingProducts().isEmpty()) {
									// An optional query will be satisfied, even if there are no input products (locally)
									// Try to fetch more input products from some external archive
									productQuery.setInDownload(true);
									startAipDownload(productQuery);
									// Prevent immediate start of job step without completion of download
									// TODO The download state must be annotated with the product, the current implementation is not
									// safe
									productQuery.setIsSatisfied(false);
									hasUnsatisfiedInputQueries = true;
								}
							}

							RepositoryService.getProductQueryRepository().save(productQuery);
							// we do not need to save the product in this transaction
							// only the satisfied product queries are updated but this relation is also saved by the product query
							// for (Product p: pq.getSatisfyingProducts()) {
							// RepositoryService.getProductRepository().save(p);
							// }
							// The following removed - it is the *input* product queries that matter!
							// jsx.getOutputProduct().getSatisfiedProductQueries().add(pq);
							// RepositoryService.getProductRepository().save(jsx.getOutputProduct());
						} else {
							// If query execution fails, set the flag and start download if necessary
							hasUnsatisfiedInputQueries = true;
							if (js.getJob().getProcessingOrder().getOrderSource() == OrderSource.ODIP) {
								// call aip client to download possible files
								if (!productQuery.getInDownload()) {
									productQuery.setInDownload(true);
									RepositoryService.getProductQueryRepository().save(productQuery);
									startAipDownload(productQuery);
								}
							}
						}
					}
				}

				// Update the state of the job step based on the query results
				if (hasUnsatisfiedInputQueries) {
					// If there are unsatisfied input queries, set the job step state to WAITING_INPUT
					if (js.getJobStepState() != de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT) {
						js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT);
						js.incrementVersion();
						RepositoryService.getJobStepRepository().save(js);
						// em.merge(js);
					}
				} else {
					// If all input queries are satisfied, set the job step state to READY
					js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.READY);
					js.incrementVersion();
					RepositoryService.getJobStepRepository().save(js);
					// em.merge(js);
				}
			}

		}
	}

	/**
	 * Deletes the product tree recursively. This method is used during the deletion of a job step.
	 * 
	 * @param p The root product of the tree to delete
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	private void deleteProduct(Product p) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteProduct({})", (null == p ? "null" : p.getId()));

		if (p != null) {
			if (p.getEnclosingProduct() != null) {
				p.getEnclosingProduct().getComponentProducts().remove(p);
			}

			List<Product> componentProducts = new ArrayList<>();
			componentProducts.addAll(p.getComponentProducts());

			for (Product componentProduct : componentProducts) {
				deleteProduct(componentProduct);
			}

			RepositoryService.getProductRepository().delete(p);
		}
	}

	/**
	 * Checks all unsatisfied queries of job steps across all facilities to determine if they can be started.
	 */
	// @Transactional(isolation = Isolation.REPEATABLE_READ)
	public void checkForJobStepsToRun() {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkForJobStepsToRun()");

		if (productionPlanner != null) {
			Collection<KubeConfig> kubeConfigurations = productionPlanner.getKubeConfigs();

			if (kubeConfigurations != null) {
				for (KubeConfig kubeConfig : kubeConfigurations) {
					kubeConfig.sync();
					checkForJobStepsToRun(kubeConfig, 0, false, true);
				}
			}
		}

		if (logger.isTraceEnabled())
			logger.trace("<<< checkForJobStepsToRun()");
	}

	/**
	 * Checks for job steps of a specific product class that can be started on the specified Kubernetes configuration. If onlyRun is
	 * false, also checks unsatisfied queries on the processing facility defined in the Kubernetes configuration.
	 *
	 * @param kc          The Kubernetes configuration to check
	 * @param pcId        The ID of the product class to check for
	 * @param onlyRun     Indicates whether to only check for job steps to run without checking unsatisfied queries
	 * @param onlyWaiting Indicates whether to only check for job steps in waiting state
	 */
	public void checkForJobStepsToRun(KubeConfig kc, long pcId, Boolean onlyRun, Boolean onlyWaiting) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkForJobStepsToRun({}, {}, {})", (null == kc ? "null" : kc.getId()), (pcId == 0 ? "null" : pcId),
					onlyRun);

		if (productionPlanner != null) {
			if (kc == null) {
				// Transaction handling in recursive calling of this method
				checkForJobStepsToRun();
			} else {
				try {
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					transactionTemplate.setReadOnly(true);

					final ProcessingFacility processingFacility = transactionTemplate.execute((status) -> {
						Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(kc.getLongId());

						if (opt.isPresent()) {
							return opt.get();
						}

						return null;
					});

					if (processingFacility != null) {
						if (!onlyRun) {
							this.searchForJobStepsToRun(kc.getLongId(), pcId, onlyWaiting);
						}

						transactionTemplate.setReadOnly(true);

						List<?> jobStepList = transactionTemplate.execute((status) -> {

							// Construct the native query based on the configured job step sorting option
							String nativeQuery;
							switch (config.getJobStepSort()) {
							case SUBMISSION_TIME:
								nativeQuery = "SELECT po.submission_time, js.id " + "FROM processing_order o "
										+ "JOIN job j ON o.id = j.processing_order_id " + "JOIN job_step js ON j.id = js.job_id "
										+ "JOIN processing_order po ON po.id = j.processing_order_id "
										+ "WHERE j.processing_facility_id = :pfId " + "AND js.job_step_state = :jsStateReady "
										+ "AND o.order_state != :oStateSuspending " + "AND o.order_state != :oStatePlanned "
										+ "AND (" + "j.job_state = :jStateReleased OR j.job_state = :jStateStarted" + ")"
										+ "ORDER BY js.priority desc, po.submission_time, js.id";
								break;
							case SENSING_TIME:
								nativeQuery = "SELECT j.start_time, js.id " + "FROM processing_order o "
										+ "JOIN job j ON o.id = j.processing_order_id " + "JOIN job_step js ON j.id = js.job_id "
										+ "WHERE j.processing_facility_id = :pfId " + "AND js.job_step_state = :jsStateReady "
										+ "AND o.order_state != :oStateSuspending " + "AND o.order_state != :oStatePlanned "
										+ "AND (" + "j.job_state = :jStateReleased OR j.job_state = :jStateStarted" + ")"
										+ "ORDER BY js.priority desc, j.start_time, js.id";
								break;
							default:
								nativeQuery = "SELECT j.start_time, js.id " + "FROM processing_order o "
										+ "JOIN job j ON o.id = j.processing_order_id " + "JOIN job_step js ON j.id = js.job_id "
										+ "WHERE j.processing_facility_id = :pfId " + "AND js.job_step_state = :jsStateReady "
										+ "AND o.order_state != :oStateSuspending " + "AND o.order_state != :oStatePlanned "
										+ "AND (" + "j.job_state = :jStateReleased OR j.job_state = :jStateStarted" + ")"
										+ "ORDER BY js.priority desc, j.start_time, js.id";
								break;

							}

							return em.createNativeQuery(nativeQuery)
								.setParameter("pfId", kc.getLongId())
								.setParameter("jsStateReady", JobStepState.READY.toString())
								.setParameter("oStateSuspending", OrderState.SUSPENDING.toString())
								.setParameter("oStatePlanned", OrderState.PLANNED.toString())
								.setParameter("jStateReleased", JobState.RELEASED.toString())
								.setParameter("jStateStarted", JobState.STARTED.toString())
								.getResultList();

						});

						// For each job step, check if it can be run
						for (Object jobStepObject : jobStepList) {
							if (jobStepObject instanceof Object[]) {

								Object[] jobStep = (Object[]) jobStepObject;

								if (logger.isTraceEnabled())
									logger.trace("... found job step info {}", Arrays.asList(jobStep));

								// jobStep[0] is only used for ordering the result list
								Long jsId = jobStep[1] instanceof BigInteger ? ((BigInteger) jobStep[1]).longValue() : null;

								if (null == jsId) {
									throw new RuntimeException("Invalid query result: " + Arrays.asList(jobStep));
								}

								if (kc.couldJobRun(jsId)) {
									// Job creation is transactional in KubeJob, therefore removed from transaction above
									// TODO Add retrying for concurrent updates, taking into account side effect of
									// Kubernetes job creation
									try {
										kc.getJobCreatingList().put(jsId, jsId);
										kc.createJob(String.valueOf(jsId), null, null);
									} catch (Exception e) {
										throw e;
									} finally {
										kc.getJobCreatingList().remove(jsId);
									}
								} else {
									// at the moment no further job could be started
									break;
								}

							} else {
								throw new RuntimeException("Invalid query result: " + jobStepObject);
							}
						}

					}
				} catch (Exception e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				}
			}
		}

		if (logger.isTraceEnabled())
			logger.trace("<<< checkForJobStepsToRun({}, {}, {})", (null == kc ? "null" : kc.getId()), (pcId == 0 ? "null" : pcId),
					onlyRun);
	}

	/**
	 * Checks if a job step can be run on the specified processing facility defined in the Kubernetes configuration. This method is
	 * synchronized to prevent interference between different threads (simultaneous event-triggered dispatching).
	 *
	 * @param kc   The Kubernetes configuration specifying the processing facility
	 * @param jsId The ID of the job step to check for running
	 * @return true if the job step can be run, false otherwise
	 */
	// @Transactional(isolation = Isolation.REPEATABLE_READ)
	public Boolean checkJobStepToRun(KubeConfig kc, long jsId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkForJobStepsToRun({}, {})", (null == kc ? "null" : kc.getId()), (0 == jsId ? "null" : jsId));

		Boolean answer = true;
		if (productionPlanner != null) {
			if (kc != null && jsId != 0) {
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
				transactionTemplate.setReadOnly(true);

				final ProcessingFacility processingFacility = transactionTemplate.execute((status) -> {
					Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(kc.getLongId());
					if (opt.isPresent()) {
						return opt.get();
					}
					return null;
				});

				if (processingFacility != null) {

					transactionTemplate.setReadOnly(true);
					Boolean checkQueries = transactionTemplate.execute((status) -> {
						Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(jsId);
						if (opt.isPresent()) {
							if ((opt.get().getJobStepState().equals(JobStepState.PLANNED)
									|| opt.get().getJobStepState().equals(JobStepState.WAITING_INPUT))
									&& opt.get().getJob().getJobState() != JobState.ON_HOLD) {
								return true;
							}
						}
						return false;
					});

					transactionTemplate.setReadOnly(false);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							answer = transactionTemplate.execute((status) -> {
								Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(jsId);
								JobStep jobStep = null;

								if (opt.isPresent()) {
									jobStep = opt.get();
								}

								if (jobStep != null && checkQueries) {
									checkJobStepQueries(jobStep, false);
								}

								if (jobStep != null && jobStep.getJobStepState() == JobStepState.READY) {
									if (jobStep.getJob().getJobState() == JobState.PLANNED) {
										jobStep.getJob().setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
										RepositoryService.getJobRepository().save(jobStep.getJob());
									}

									if ((jobStep.getJob().getJobState() == JobState.RELEASED
											|| jobStep.getJob().getJobState() == JobState.STARTED)
											&& jobStep.getJob().getProcessingOrder().getOrderState() != OrderState.SUSPENDING
											&& jobStep.getJob().getProcessingOrder().getOrderState() != OrderState.PLANNED) {
										if (kc.couldJobRun(jobStep.getId())) {
											try {
												kc.getJobCreatingList().put(jobStep.getId(), jobStep.getId());
												kc.createJob(String.valueOf(jobStep.getId()), null, null);
											} catch (Exception e) {
												throw e;
											} finally {
												kc.getJobCreatingList().remove(jobStep.getId());
											}
										} else {
											return false;
										}
									}
								}
								return true;
							});

							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				}
			}
		}

		return answer;
	}

	/**
	 * Checks unsatisfied queries of job steps in a job assigned to a processing facility defined in the Kubernetes config and
	 * starts ready job steps. This method is synchronized to prevent interference between different threads (simultaneous
	 * event-triggered dispatching).
	 *
	 * @param kc    KubeConfig
	 * @param jobId ID of the job to be checked
	 * @throws InterruptedException If the thread is interrupted during execution
	 */
	public void checkJobToRun(KubeConfig kc, long jobId) throws InterruptedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkJobToRun({}, {})", (null == kc ? "null" : kc.getId()), (0 == jobId ? "null" : jobId));

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		if (productionPlanner != null) {
			if (kc != null && jobId != 0) {
				final ProcessingFacility processingFacility = transactionTemplate.execute((status) -> {
					Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(kc.getLongId());
					if (opt.isPresent()) {
						return opt.get();
					}
					return null;
				});

				if (processingFacility != null) {
					// Wait until finish of concurrent createJob
					try {
						final List<Long> jobSteps = new ArrayList<>();

						@SuppressWarnings("unused")
						String dummy = transactionTemplate.execute((status) -> {
							Optional<Job> opt = RepositoryService.getJobRepository().findById(jobId);
							if (opt.isPresent()) {
								Job job = opt.get();
								for (JobStep js : job.getJobSteps()) {
									jobSteps.add(js.getId());
								}
							}
							return null;
						});

						for (Long jsId : jobSteps) {
							checkJobStepToRun(kc, jsId);
						}
					} catch (Exception e) {
						logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

						if (logger.isDebugEnabled())
							logger.debug("... exception stack trace: ", e);
					}
				}
			}
		}
	}

	/**
	 * Check unsatisfied queries of job steps in processing order on processing facility (defined in Kube config). Start ready job
	 * steps on facility.
	 *
	 * Method is synchronized to avoid different threads (simultaneous event-triggered dispatching) to interfere with each other.
	 *
	 * @param kc      KubeConfig
	 * @param orderId The ID of the ProcessingOrder to check
	 */
	// @Transactional(isolation = Isolation.REPEATABLE_READ)
	public void checkOrderToRun(KubeConfig kc, long orderId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkOrderToRun({}, {})", (null == kc ? "null" : kc.getId()), (0 == orderId ? "null" : orderId));

		if (productionPlanner != null) {
			if (kc != null && orderId != 0) {
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

				final ProcessingFacility processingFacility = transactionTemplate.execute((status) -> {
					Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(kc.getLongId());
					if (opt.isPresent()) {
						return opt.get();
					}
					return null;
				});

				if (processingFacility != null) {
					// wait until finish of concurrent createJob
					final List<Long> jobSteps = new ArrayList<>();
					try {
						@SuppressWarnings("unused")
						String dummy = transactionTemplate.execute((status) -> {
							ProcessingOrder order = null;
							Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(orderId);

							if (opt.isPresent()) {
								order = opt.get();
							}

							if (order != null) {
								List<Job> jobList = new ArrayList<Job>();
								jobList.addAll(order.getJobs());
								jobList.sort(new Comparator<Job>() {
									@Override
									public int compare(Job o1, Job o2) {
										return o1.getStartTime().compareTo(o2.getStartTime());
									}
								});

								for (Job job : jobList) {
									for (JobStep js : job.getJobSteps()) {
										jobSteps.add(js.getId());
									}

								}
							}
							return null;
						});

						for (Long jsId : jobSteps) {
							if (!checkJobStepToRun(kc, jsId)) {
								break;
							}
						}
					} catch (Exception e) {
						logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

						if (logger.isDebugEnabled())
							logger.debug("... exception stack trace: ", e);
					}
				}
			}
		}
	}

	/**
	 * Checks whether all products associated with a completed job step exist. If not, removes any missing products and cleans up
	 * dependencies.
	 * 
	 * @param js The job step to be checked
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void checkCreatedProducts(JobStep js) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkCreatedProducts({})", (null == js ? "null" : js.getId()));

		// Ensure job step exists and is completed with output products
		if (js != null && js.getJobStepState() == JobStepState.COMPLETED && null != js.getOutputProduct()) {
			ProcessingFacility processingFacility = js.getJob().getProcessingFacility();
			if (processingFacility != null) {

				Optional<Product> opt = RepositoryService.getProductRepository().findById(js.getOutputProduct().getId());
				Product product = null;
				product = opt.isPresent() ? opt.get() : null;

				if (product != null) {
					List<Product> createdProductList = new ArrayList<>();
					createdProductList = checkCreatedProduct(product, processingFacility);

					// Remove missing products and update dependencies
					for (Product createdProduct : createdProductList) {
						if (product.getComponentProducts().contains(createdProduct)) {
							product.getComponentProducts().remove(createdProduct);
							RepositoryService.getProductRepository().delete(createdProduct);
							RepositoryService.getProductRepository().save(product);
							em.merge(product);
						}
					}

					// If all components and files are removed, delete the product
					if (product.getComponentProducts().isEmpty() && product.getProductFile().isEmpty()) {
						RepositoryService.getProductRepository().delete(product);
						js.setOutputProduct(null);
						RepositoryService.getJobStepRepository().save(js);
						em.merge(js);
					}
				}
			}
		}
	}

	/**
	 * Checks the existence of the product and its dependencies on a specified processing facility. Removes any missing products and
	 * cleans up associated dependencies.
	 *
	 * @param p  The product to be checked
	 * @param pf The processing facility to check against
	 * @return A list of products to be removed due to missing dependencies or files
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	private List<Product> checkCreatedProduct(Product p, ProcessingFacility pf) {
		List<Product> productsToRemove = new ArrayList<>();
		if (logger.isTraceEnabled())
			logger.trace(">>> checkCreatedProduct({})", (null == p ? "null" : p.getId()), (null == pf ? "null" : pf.getName()));

		if (p != null && pf != null) {
			Set<Product> pList = p.getComponentProducts();
			if (pList.isEmpty()) {
				// check product file
				boolean removeProduct = true;
				List<ProductFile> fToRemove = new ArrayList<>();
				// search a product file on facility
				for (ProductFile f : p.getProductFile()) {
					if (f.getProcessingFacility().equals(pf)) {
						if (f.getProductFileName() != null && !f.getProductFileName().isEmpty()) {
							// product file found, all is okay
							removeProduct = false;
						} else {
							// object product file found, but no file name.
							// remove it later
							fToRemove.add(f);
						}
					}
				}
				if (!fToRemove.isEmpty()) {
					// remove product files without file name
					for (ProductFile f : fToRemove) {
						p.getProductFile().remove(f);
						RepositoryService.getProductFileRepository().delete(f);
						RepositoryService.getProductRepository().save(p);
						em.merge(p);
					}
				}
				if (removeProduct) {
					// the product doesn't a file on facility
					// search all satisfied queries which contain this product on facility
					for (ProductQuery pq : p.getSatisfiedProductQueries()) {
						if (pq.getJobStep().getJob().getProcessingFacility().equals(pf)) {
							// remove the product from the query and set satisified to false
							pq.getSatisfyingProducts().remove(p);
							pq.setIsSatisfied(false);
							RepositoryService.getProductQueryRepository().save(pq);
							em.merge(pq);
						}
					}
					if (p.getProductFile().isEmpty()) {
						// there are no product files for the product, remove it
						productsToRemove.add(p);
					}
				}
			} else {
				// check components
				List<Product> ptr = new ArrayList<>();
				for (Product cp : pList) {
					ptr.addAll(checkCreatedProduct(cp, pf));
				}
				// remove the component products without file and components
				for (Product cp : ptr) {
					p.getComponentProducts().remove(cp);
					RepositoryService.getProductRepository().delete(cp);
					RepositoryService.getProductRepository().save(p);
					em.merge(p);
				}
				if (p.getComponentProducts().isEmpty() && p.getProductFile().isEmpty()) {
					// there are no product files or component for the product, remove it
					productsToRemove.add(p);
				}
			}
		}
		return productsToRemove;
	}

	/**
	 * Recursively collects products from a product tree into a list, starting from the root product.
	 *
	 * @param p    The root product of the tree
	 * @param list The list to collect the products into
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void collectProducts(Product p, List<Product> list) {
		if (p != null) {
			list.add(p);
			for (Product cp : p.getComponentProducts()) {
				collectProducts(cp, list);
			}
		}
	}

	/**
	 * Checks whether all products in the list exist and have been generated on the specified processing facility.
	 * 
	 * @param list The list of products to be checked
	 * @param pf   The processing facility to check against
	 * @return True if all products in the list are generated on the specified processing facility, false otherwise
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public Boolean checkProducts(List<Product> list, ProcessingFacility pf) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkCreatedProduct(Product[{}], {})", (null == list ? "null" : list.size()),
					(null == pf ? "null" : pf.getName()));

		boolean allGenerated = true;
		for (Product product : list) {
			// Check if the product has any associated product files
			if (product.getProductFile().isEmpty()) {
				allGenerated = false;
				break;
			} else {
				boolean onProcessingFacility = false;

				for (ProductFile f : product.getProductFile()) {
					if (f.getProcessingFacility().equals(pf)) {
						onProcessingFacility = true;
						break;
					}
				}

				// If no product file belongs to the specified processing facility, set allGenerated to false and exit the loop
				if (!onProcessingFacility) {
					allGenerated = false;
					break;
				}
			}
		}

		return allGenerated;
	}

	/**
	 * Deletes the Job Order file associated with the given job step from the Storage Manager.
	 *
	 * @param js The job step for which the Job Order file is to be deleted
	 * @return True if the deletion is successful, false otherwise
	 */
	private Boolean deleteJOF(JobStep js) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteJOF({})", (null == js ? "null" : js.getId()));

		if (js != null && js.getJobOrderFilename() != null) {
			ProcessingFacility facility = js.getJob().getProcessingFacility();
			String storageManagerUrl = facility.getStorageManagerUrl()
					+ String.format("/products?pathInfo=%s", js.getJobOrderFilename());

			RestTemplate restTemplate = rtb
				.basicAuthentication(facility.getStorageManagerUser(), facility.getStorageManagerPassword())
				.build();

			try {
				restTemplate.delete(storageManagerUrl);
				logger.log(PlannerMessage.JOF_DELETED, js.getJobOrderFilename());
				return true;
			} catch (RestClientException e) {
				logger.log(PlannerMessage.JOF_DELETING_ERROR, js.getJobOrderFilename(), facility.getName(), e.getMessage());
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Initiates the download process for the input products associated with the given product query.
	 *
	 * @param pq The product query for which input products are to be downloaded
	 */
	private void startAipDownload(ProductQuery pq) {
		if (logger.isTraceEnabled())
			logger.trace(">>> ProductQuery({})", pq);

		// Calculate start and stop time considering any delta time from policies
		Instant startTime = pq.getJobStep().getJob().getStartTime();
		Instant stopTime = pq.getJobStep().getJob().getStopTime();

		List<SimplePolicy> simplePolicies = pq.getGeneratingRule().getSimplePolicies();
		for (SimplePolicy simplePolicy : simplePolicies) {
			startTime = startTime.isAfter(startTime.minusMillis(simplePolicy.getDeltaTimeT0().toMilliseconds()))
					? startTime.minusMillis(simplePolicy.getDeltaTimeT0().toMilliseconds())
					: startTime;
			stopTime = stopTime.isBefore(stopTime.plusMillis(simplePolicy.getDeltaTimeT0().toMilliseconds()))
					? stopTime.plusMillis(simplePolicy.getDeltaTimeT0().toMilliseconds())
					: stopTime;
		}

		// Prepare authentication credentials for AIP service
		String user = "";
		String pw = "";
		user = pq.getJobStep().getJob().getProcessingOrder().getMission().getCode() + "-" + config.getAipUser();
		pw = config.getAipPassword();
		if (user == null) {
			user = "";
			pw = "";
		}

		// Determine if retry is needed based on simple policies
		boolean retry = false;
		for (SimplePolicy simplePolicy : pq.getGeneratingRule().getSimplePolicies()) {
			switch (simplePolicy.getPolicyType()) {
			case LatestValidity:
			case LatestStartValidity:
				retry = true;
				break;
			case LatestStopValidity:
				retry = true;
				break;
			case ClosestStartValidity:
			case LatestValidityClosest:
				retry = true;
				break;
			case ClosestStopValidity:
				retry = true;
				break;
			case LatestValCover:
				break;
			case ValIntersect:
			case ValIntersectWithoutDuplicates:
				break;
			case LatestValIntersect:
				break;
			case LastCreated:
				break;
			default:
				break;
			}
		}

		// Set retry count based on retry requirement
		int retryCount = 1;
		if (retry) {
			retryCount = 10;
		}

		// Retry downloading products with the adjusted time range if necessary
		while (retryCount > 0) {
			try {
				if (logger.isTraceEnabled())
					logger.trace("  {}{})", config.getAipUrl(),
							URI_PATH_DOWNLOAD_ALLBYTIME + "?productType=" + pq.getRequestedProductClass().getProductType()
									+ "&startTime=" + OrbitTimeFormatter.format(startTime) + "&stopTime="
									+ OrbitTimeFormatter.format(stopTime) + "&facility="
									+ pq.getJobStep().getJob().getProcessingFacility().getName());

				// Attempt to download products from the AIP service
				serviceConnection.getFromService(config.getAipUrl(),
						URI_PATH_DOWNLOAD_ALLBYTIME + "?productType=" + pq.getRequestedProductClass().getProductType()
								+ "&startTime=" + OrbitTimeFormatter.format(startTime) + "&stopTime="
								+ OrbitTimeFormatter.format(stopTime) + "&facility="
								+ pq.getJobStep().getJob().getProcessingFacility().getName(),
						RestProduct[].class, user, pw);

				// If products are found, exit retry loop
				break;
			} catch (HttpClientErrorException e) {
				if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
					if (retry) {
						// Expand the time range to find input product(s) if retry is enabled
						// TODO: Discuss
						startTime = startTime.minus(1, ChronoUnit.DAYS);
						stopTime = stopTime.plus(1, ChronoUnit.DAYS);
					}
				} else {
					String message = logger.log(PlannerMessage.MSG_EXCEPTION, e.getMessage(), e);
					// throw new Exception(message);
				}
			} catch (Exception e) {
				String message = logger.log(PlannerMessage.MSG_EXCEPTION, e.getMessage(), e);

				if (logger.isDebugEnabled())
					logger.debug("... exception stack trace: ", e);

				// throw new Exception(message);
			}
			retryCount--;
		}
	}

}
