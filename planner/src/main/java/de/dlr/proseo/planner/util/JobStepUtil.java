/**
 * JobStepUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.time.Duration;
import java.time.Instant;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OdipMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.planner.PlannerResultMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
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
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.service.ProductQueryService;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.ProductionPlannerConfiguration;
import de.dlr.proseo.planner.ProductionPlannerSecurityConfig;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.service.ServiceConnection;

/**
 * Handle job steps 
 * 
 * @author Ernst Melchinger
 *
 */
@Component
// @Transactional
public class JobStepUtil {
	
	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(JobStepUtil.class);

	private final String URI_PATH_DOWNLOAD_ALLBYTIME = "/download/allbytime";

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	@Autowired
	private ProductQueryService productQueryService;
	@Autowired
	private ProductionPlanner productionPlanner;
	@Autowired
	private ServiceConnection serviceConnection;
	/** 
	 * Planner configuration 
	 */
	@Autowired
	ProductionPlannerConfiguration config;
	@Autowired
	ProductionPlannerSecurityConfig securityConfig;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	
	/**
	 * Find job steps of specific job step state. The result is ordered by processingCompletionTime descending and returns the first 'limit' entries.
	 *  
	 * @param state The job step state
	 * @param mission The mission code
	 * @param limit The length of result entry list
	 * @return The found job steps
	 */
	public List<JobStep> findOrderedByJobStepStateAndMission(JobStepState state, String mission, int limit) {
		if (logger.isTraceEnabled()) logger.trace(">>> findOrderedByJobStepStateAndMission({}, {}, {})", state, mission, limit);

		String query = "select js from JobStep js " + 
				" inner join Job j on js.job.id = j.id " + 
				" inner join ProcessingOrder o on j.processingOrder.id = o.id" + 
				" inner join Mission m on o.mission.id = m.id " + 
				" where js.processingCompletionTime is not null and js.jobStepState = '" + state + "' and m.code = '" + mission + 
				"' order by js.processingCompletionTime desc";
		// em.createNativeQ
		return em.createQuery(query,
			JobStep.class)
				.setMaxResults(limit)
				.getResultList();
	}
    
	/**
	 * Search for not satisfied product queries referencing product class on processing facility and check which are now satisfied. 
	 * Change state of job step to READY if all queries are now satisfied. 
	 * 
	 * @param processingFacility
	 * @param pc Product class
	 */
	// @Transactional
	public void searchForJobStepsToRun(long pfId, long pcId, boolean onlyWaiting) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		
		// TODO Replace findAllByProcessingFacilityAndJobStepStateInAndOrderBySensingStartTime() by native SQL query

		final ProcessingFacility processingFacility = transactionTemplate.execute((status) -> {
			Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(pfId);
			if (opt.isPresent()) {
				return opt.get();
			}
			return null;
		});
		final ProductClass pc = transactionTemplate.execute((status) -> {
			Optional<ProductClass> opt = RepositoryService.getProductClassRepository().findById(pcId);
			if (opt.isPresent()) {
				return opt.get();
			}
			return null;
		});
		if (logger.isTraceEnabled()) logger.trace(">>> searchForJobStepsToRun({}, {}, {})",
				(null == processingFacility ? "null" : processingFacility.getName()),
				(null == pc ? "null" : pc.getProductType()),
				onlyWaiting);

		// Search for all job steps on processingFacility with states PLANNED, WAITING_INPUT
		List<String> jobStepStates = new ArrayList<>();
		if (!onlyWaiting) {
			jobStepStates.add(JobStep.JobStepState.PLANNED.toString());
		}
		jobStepStates.add(JobStepState.WAITING_INPUT.toString());
		//List<JobStep> allJobSteps = null;

		List<Long> allJobSteps = transactionTemplate.execute((status) -> {
			List<Long> jobSteps = null;
			if (processingFacility == null) {
				jobSteps = new ArrayList<>();
				if (pc != null) {
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
				} else {
					for (ProcessingFacility pf : RepositoryService.getFacilityRepository().findAll()) {
						//jobSteps.addAll(RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateInAndOrderBySensingStartTime(pf.getId(), jobStepStates));
						jobSteps.addAll(findAllByProcessingFacilityAndJobStepStateInAndOrderBySensingStartTime(pf.getId(), jobStepStates));
					}
				}
			} else {
				if (pc != null) {
					jobSteps = new ArrayList<>();
					List<ProductQuery> productQueries = RepositoryService.getProductQueryRepository()
							.findUnsatisfiedByProductClass(pcId);
					for (ProductQuery pq : productQueries) {
						if (pq.getJobStep().getJob().getProcessingFacility().getId() == pfId) {
							if (pq.getJobStep().getJobStepState().equals(JobStepState.WAITING_INPUT)
									&& pq.getJobStep().getJob().getJobState() != JobState.ON_HOLD) {
								jobSteps.add(pq.getJobStep().getId());
							} else if (!onlyWaiting && pq.getJobStep().getJobStepState().equals(JobStepState.PLANNED)
									&& pq.getJobStep().getJob().getJobState() != JobState.ON_HOLD) {
								jobSteps.add(pq.getJobStep().getId());
							}
						}
					}
				} else {
					//jobSteps = RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateInAndOrderBySensingStartTime(pfId, jobStepStates);
					jobSteps = findAllByProcessingFacilityAndJobStepStateInAndOrderBySensingStartTime(pfId, jobStepStates);
				}
			}
			return jobSteps;
		});
		for (Long jsId : allJobSteps) {
			@SuppressWarnings("unused")
			final Object dummy = transactionTemplate.execute((status) -> {
				Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(jsId);
				if (opt.isPresent()) {
					checkJobStepQueries(opt.get(), false);
				}
				return null;
			});
		}
	}

	/**
	 * Find all job steps in the given states to be executed in the given processing facility
	 * 
	 * @param processingFacilityId the database ID of the processing facility
	 * @param jobStepStates a list of job step state names
	 * @return
	 */
	private List<Long> findAllByProcessingFacilityAndJobStepStateInAndOrderBySensingStartTime(long processingFacilityId,
			List<String> jobStepStates) {
		if (logger.isTraceEnabled()) logger.trace(">>> findAllByProcessingFacilityAndJobStepStateInAndOrderBySensingStartTime({}, {})",
				processingFacilityId, jobStepStates);

		String nativeQuery = "SELECT j.start_time, js.id "
				+ "FROM job j "
				+ "JOIN job_step js ON j.id = js.job_id "
				+ "WHERE j.processing_facility_id = :pfId "
				+ "AND js.job_step_state IN :jsStates "
				+ "ORDER BY js.priority desc, j.start_time, js.id";
		List<?> jobStepList = em.createNativeQuery(nativeQuery)
				.setParameter("pfId", processingFacilityId)
				.setParameter("jsStates", jobStepStates)
				.getResultList();

		List<Long> resultList = new ArrayList<>();
		
		for (Object jobStepObject: jobStepList) {
			if (jobStepObject instanceof Object[]) {

				Object[] jobStep = (Object[]) jobStepObject;

				if (logger.isTraceEnabled()) logger.trace("... found job step info {}", Arrays.asList(jobStep));

				// jobStep[0] is only used for ordering the result list
				Long jsId = jobStep[1] instanceof BigInteger ? ((BigInteger) jobStep[1]).longValue() : null;

				if (null == jsId) {
					throw new RuntimeException("Invalid query result: " + Arrays.asList(jobStep));
				}

				resultList.add(jsId);

			} else {
				throw new RuntimeException("Invalid query result: " + jobStepObject);
			}
		}
		
		return resultList;
	}

	/**
	 * Suspend job step, kill it if force is true otherwise wait until finish.
	 *  
	 * @param js Job step
	 * @param force Force 
	 * @return Result message
	 */
	@Transactional
	public PlannerResultMessage suspend(JobStep js, Boolean force) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspend({}, {})",
				(null == js ? "null" : js.getId()), force);

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		// check current state for possibility to be suspended
		// PLANNED, WAITING_INPUT, READY, RUNNING, COMPLETED, FAILED
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
				answer.setMessage(PlannerMessage.JOBSTEP_SUSPENDED);
				break;
			case READY:
			case WAITING_INPUT:
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.PLANNED);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
				em.merge(js);
				answer.setMessage(PlannerMessage.JOBSTEP_SUSPENDED);
				break;
			case RUNNING:
				Boolean deleted = false;
				if (force != null && force) {
					KubeConfig kc = productionPlanner.getKubeConfig(js.getJob().getProcessingFacility().getName());
					if (kc != null) {
						KubeJob kj = kc.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());
						if (kj != null) {
							deleted = kc.deleteJob(kj.getJobName());
						} else {
							kc.sync();
							kj = kc.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());
							if (kj != null) {
								deleted = kc.deleteJob(kj.getJobName());
							} else {
								// job not found, it was deleted anywhere else
								deleted = true;
							}
						}
					}
				}
				if (deleted) {
					em.merge(js);
					if (js.getJobStepState() == JobStepState.COMPLETED) {
						answer.setMessage(PlannerMessage.JOBSTEP_COMPLETED);
					} else if (js.getJobStepState() == JobStepState.FAILED) {
						answer.setMessage(PlannerMessage.JOBSTEP_COMPLETED);
					} else {
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
					KubeConfig kc = productionPlanner.getKubeConfig(js.getJob().getProcessingFacility().getName());
					if (kc != null) {
						kc.sync();
						KubeJob kj = kc.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());
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
	 * Cancel job step
	 * 
	 * @param js Job step
	 * @return Result message
	 */
	@Transactional
	public PlannerResultMessage cancel(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancel({})", (null == js ? "null" : js.getId()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.FAILED);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
				em.merge(js);
				answer.setMessage(PlannerMessage.JOBSTEP_CANCELED);
				break;
			case RUNNING:
				Boolean deleted = false;
					KubeConfig kc = productionPlanner.getKubeConfig(js.getJob().getProcessingFacility().getName());
					if (kc != null) {
						KubeJob kj = kc.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());
						if (kj != null) {
							deleted = kc.deleteJob(kj.getJobName());
						}
				}
				if (deleted) {
					js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.FAILED);
					js.incrementVersion();
					RepositoryService.getJobStepRepository().save(js);
					em.merge(js);
					answer.setMessage(PlannerMessage.JOBSTEP_CANCELED);
				} else {
					answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_RUNNING);
				}
				break;
			case COMPLETED:
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_COMPLETED);
				break;
			case FAILED:
				answer.setMessage(PlannerMessage.JOBSTEP_ALREADY_FAILED);
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
	 * Close job step
	 * 
	 * @param js Job step
	 * @return Result message
	 */
	public PlannerResultMessage close(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> close({})", (null == id ? "null" : id));

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		
		final JobStepState jobStepState = transactionTemplate.execute((status) -> {
			String sqlQuery = "select job_step_state from job_step where id = " + id + ";";
			Query query = em.createNativeQuery(sqlQuery);
			Object o = query.getSingleResult();
			return JobStepState.valueOf((String)o);			
		});
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		// check current state for possibility to be suspended
		if (id != null) {
			switch (jobStepState) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
			case RUNNING:
				answer.setMessage(PlannerMessage.JOBSTEP_COULD_NOT_CLOSE);
				break;
			case COMPLETED:
			case FAILED:
				UtilService.getJobStepUtil().deleteSatisfiedProductQueries(id);	
				transactionTemplate.execute((status) -> {
					String sqlQuery = "select version from job_step where id = " + id + ";";
					Query query = em.createNativeQuery(sqlQuery);
					Object o = query.getSingleResult();
					Integer version = (Integer)o;
					sqlQuery = "update job_step set job_step_state = 'CLOSED', version = " + (version + 1) + " where id = " + id + ";";
					query = em.createNativeQuery(sqlQuery);
					query.executeUpdate();
					return null;
				});		
				answer.setMessage(PlannerMessage.JOBSTEP_CLOSED);
				break;
			case CLOSED:
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
	 * Retry job step
	 * 
	 * @param js Job step
	 * @return Result message
	 */
	@Transactional
	public PlannerResultMessage retry(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> retry({})", (null == js ? "null" : js.getId()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case RUNNING:
			case COMPLETED:
			case CLOSED:
				answer.setMessage(PlannerMessage.JOBSTEP_COULD_NOT_RETRY);
				break;
			case WAITING_INPUT:
			case FAILED:
				Product jsp = js.getOutputProduct();
				if (jsp != null) {
					// collect output products
					List<Product> jspList = new ArrayList<Product>();
					collectProducts(jsp, jspList);
					js.setIsFailed(false);
					if (checkProducts(jspList, js.getJob().getProcessingFacility())) {
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
	 * Check job step whether job step has been finished
	 * 
	 * @param js Job step
	 * @return true if finished
	 */
	@Transactional
	public Boolean checkFinish(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkFinish({})", (null == js ? "null" : js.getId()));

		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
			case CLOSED:
				break;
			case RUNNING:
			case COMPLETED:
				em.merge(js);
				UtilService.getJobUtil().checkFinish(js.getJob().getId());
				answer = true;
				break;
			case FAILED:
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
	 * Delete job step
	 * 
	 * @param js Job step
	 * @return true  if deleted
	 */
	@Transactional
	public Boolean delete(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> delete({})", (null == js ? "null" : js.getId()));

		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
				if (js.getOutputProduct() != null) {
					deleteProduct(js.getOutputProduct());	
					js.setOutputProduct(null);
				};
				// fall through intended
			case COMPLETED:
			case FAILED:
			case CLOSED:
				deleteJOF(js);
				js.setJobOrderFilename(null);
				if (js.getOutputProduct() != null) {
					js.getOutputProduct().setJobStep(null);
				}
				for (ProductQuery pq : js.getInputProductQueries()) {
					for (Product p : pq.getSatisfyingProducts()) {						
						p.getSatisfiedProductQueries().remove(pq);
						RepositoryService.getProductRepository().save(p);
					}
					pq.getSatisfyingProducts().clear();
					RepositoryService.getProductQueryRepository().delete(pq);
				}
				js.getInputProductQueries().clear();
				logger.log(PlannerMessage.JOBSTEP_DELETED, String.valueOf(js.getId()));
				answer = true;
				break;
			case RUNNING:
				logger.log(PlannerMessage.JOBSTEP_ALREADY_RUNNING, String.valueOf(js.getId()));
				break;
			default:
				break;
			}
		}
		return answer;
	}

	/**
	 * Delete satisfied product queries of job step
	 * 
	 * @param js Job step
	 * @return true  if deleted
	 */
	public Boolean deleteSatisfiedProductQueries(Long jsId) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteSatisfiedProductQueries({})", (null == jsId ? "null" : jsId));

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		
		final JobStep js = transactionTemplate.execute((status) -> {
			Optional<JobStep> jsOpt = RepositoryService.getJobStepRepository().findById(jsId);
			if (jsOpt.isPresent()) {
				return jsOpt.get();
			}
			return null;
		});
		Boolean answer = false;
		if (js != null) {
			switch (js.getJobStepState()) {
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
							Long pqId = ((BigInteger)o).longValue();
							sqlQuery = "delete from product_query_satisfying_products where satisfied_product_queries_id = " + pqId + ";";
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
				logger.log(PlannerMessage.JOBSTEP_SPQ_DELETED, String.valueOf(js.getId()));
				answer = true;
				break;
			default:
				break;
			}
		}
		return answer;
	}


	/**
	 * Delete also not finished job step
	 * 
	 * @param js Job step
	 * @return true if deleted
	 */
	@Transactional
	public Boolean deleteForced(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteForced({})", (null == js ? "null" : js.getId()));

		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
				if (js.getOutputProduct() != null) {
					deleteProduct(js.getOutputProduct());	
					js.setOutputProduct(null);
				};
				// Fall through intended
			case RUNNING:
			case COMPLETED:	
			case CLOSED:
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
		if (logger.isTraceEnabled()) logger.trace(">>> resume({}, {})", js.getId(), force);
		
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case WAITING_INPUT:
				try {
					productionPlanner.acquireReleaseSemaphore("resume");
					checkJobStepQueries(js, force);
				} catch (Exception e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
				} finally {
					productionPlanner.releaseReleaseSemaphore("resume");				
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
	 * Start job step on Kubernetes cluster
	 * 
	 * @param js Job step
	 * @return Result message
	 */
	@Transactional
	public Boolean startJobStep(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> startJobStep({})", (null == js ? "null" : js.getId()));

		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case WAITING_INPUT:
				break;
			case READY:
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
	 * Check the queries of a job step which job is released or started.
	 * Check all if force is true
	 *  
	 * @param id Job step id
	 * @param force 
	 */
	@Transactional
	public void checkJobStepQueries(JobStep js, Boolean force) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> checkJobStepQueries({}, {}), jobStep state: {}", (null == js ? "null" : js.getId()), force, js.getJobStepState());

		if (js != null && (js.getJobStepState() == JobStepState.PLANNED || js.getJobStepState() == JobStepState.WAITING_INPUT)) {
			if (   js.getJob() != null 
					&& (force || js.getJob().getJobState() == JobState.RELEASED || js.getJob().getJobState() == JobState.STARTED)) {
				logger.trace("Looking for product queries of job step: " + js.getId());
				Boolean hasUnsatisfiedInputQueries = false;
				Map<String, Object> properties = new HashMap<>(); 
				properties.put("javax.persistence.lock.timeout", 10000L); 
				for (ProductQuery pq : js.getInputProductQueries()) {
					if (!pq.isSatisfied()) {
						if (productQueryService.executeQuery(pq, false)) {
							RepositoryService.getProductQueryRepository().save(pq);
							// we do not need to save the product in this transaction
							// only the satisfied product queries are updated but this relation is also saved by the product query
							// for (Product p: pq.getSatisfyingProducts()) {
							// 	 RepositoryService.getProductRepository().save(p);
							// }
							// The following removed - it is the *input* product queries that matter!
							//							jsx.getOutputProduct().getSatisfiedProductQueries().add(pq);
							//							RepositoryService.getProductRepository().save(jsx.getOutputProduct());
						} else {
							hasUnsatisfiedInputQueries = true;
							if (js.getJob().getProcessingOrder().getOrderSource() == OrderSource.ODIP) {
								// call aip client to download possible files
								if (!pq.getInDownload()) {
									pq.setInDownload(true);
									RepositoryService.getProductQueryRepository().save(pq);
									startAipDownload(pq);
								}
							}
						}
					}
				}
				if (hasUnsatisfiedInputQueries) {
					if (js.getJobStepState() != de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT) {
						js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT);
						js.incrementVersion();
						RepositoryService.getJobStepRepository().save(js);
						em.merge(js);
					}				
				} else {
					js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.READY);
					js.incrementVersion();
					RepositoryService.getJobStepRepository().save(js);
					em.merge(js);
				}
			}
			
		}
	}

	/**
	 * Delete product tree. Used during delete of job step.
	 * @param p
	 */
	@Transactional
	private void deleteProduct(Product p) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProduct({})", (null == p ? "null" : p.getId()));

		if (p!= null) {
			if (p.getEnclosingProduct() != null) {
				p.getEnclosingProduct().getComponentProducts().remove(p);
			}
			List<Product> cps = new ArrayList<Product>();
			cps.addAll(p.getComponentProducts());
			for (Product cp : cps) {
				deleteProduct(cp);
			}
			RepositoryService.getProductRepository().delete(p);
		}
	}

	/**
	 * Check all unsatisfied queries of all job steps on all facilities whether they can be started.
	 */
	// @Transactional
    public void checkForJobStepsToRun() {
		if (logger.isTraceEnabled()) logger.trace(">>> checkForJobStepsToRun()");

    	if (productionPlanner != null) {
    		Collection<KubeConfig> kcs = productionPlanner.getKubeConfigs();
    		if (kcs != null) {
    			for (KubeConfig kc : kcs) {
    				kc.sync();
    				checkForJobStepsToRun(kc, 0, false, true);
    			}
    		}
    	}
		if (logger.isTraceEnabled()) logger.trace("<<< checkForJobStepsToRun()");
    }

	/**
	 * If onlyRun is false, check unsatisfied queries of product class on processing facility (defined in Kube config).
	 * Start ready job steps on facility.
	 * 
	 * Method is synchronized to avoid different threads (background dispatching and event-triggered dispatching) to
	 * interfere with each other.
	 * 
	 * @param kc KubeConfig
	 * @param pc ProductClass
	 * @param onlyRun
	 */
    public void checkForJobStepsToRun(KubeConfig kc, long pcId, Boolean onlyRun, Boolean onlyWaiting) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkForJobStepsToRun({}, {}, {})",
				(null == kc ? "null" : kc.getId()),
				(pcId == 0 ? "null" : pcId),
				onlyRun);

		if (productionPlanner != null) {
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			if (kc != null) {
				try {
					productionPlanner.acquireReleaseSemaphore("checkForJobStepsToRun");

					final ProcessingFacility pfo = transactionTemplate.execute((status) -> {
						Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(kc.getLongId());
						if (opt.isPresent()) {
							return opt.get();
						}
						return null;
					});
					if (pfo != null) {
						if (!onlyRun) {
							this.searchForJobStepsToRun(kc.getLongId(), pcId, onlyWaiting);
						}
						transactionTemplate.execute((status) -> {

							String nativeQuery = "SELECT j.start_time, js.id "
									+ "FROM processing_order o "
									+ "JOIN job j ON o.id = j.processing_order_id "
									+ "JOIN job_step js ON j.id = js.job_id "
									+ "WHERE j.processing_facility_id = :pfId "
									+ "AND js.job_step_state = :jsStateReady "
									+ "AND o.order_state != :oStateSuspending "
									+ "AND o.order_state != :oStatePlanned "
									+ "AND ("
									+ "j.job_state = :jStateReleased OR j.job_state = :jStateStarted"
									+ ")"
									+ "ORDER BY js.priority desc, j.start_time, js.id";
							List<?> jobStepList = em.createNativeQuery(nativeQuery)
									.setParameter("pfId", kc.getLongId())
									.setParameter("jsStateReady", JobStepState.READY.toString())
									.setParameter("oStateSuspending", OrderState.SUSPENDING.toString())
									.setParameter("oStatePlanned", OrderState.PLANNED.toString())
									.setParameter("jStateReleased", JobState.RELEASED.toString())
									.setParameter("jStateStarted", JobState.STARTED.toString())
									.getResultList();

							for (Object jobStepObject: jobStepList) {
								if (jobStepObject instanceof Object[]) {

									Object[] jobStep = (Object[]) jobStepObject;

									if (logger.isTraceEnabled()) logger.trace("... found job step info {}", Arrays.asList(jobStep));

									// jobStep[0] is only used for ordering the result list
									Long jsId = jobStep[1] instanceof BigInteger ? ((BigInteger) jobStep[1]).longValue() : null;

									if (null == jsId) {
										throw new RuntimeException("Invalid query result: " + Arrays.asList(jobStep));
									}

									if (kc.couldJobRun()) {
										kc.createJob(String.valueOf(jsId), null, null);
									} else {
										// at the moment no further job could be started
										break;
									}

								} else {
									throw new RuntimeException("Invalid query result: " + jobStepObject);
								}
							}

							return null;
						});
					} 
					productionPlanner.releaseReleaseSemaphore("checkForJobStepsToRun");	
				} catch (Exception e) {
					productionPlanner.releaseReleaseSemaphore("checkForJobStepsToRun");	
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
				} 
			} else {
				transactionTemplate.execute((status) -> {
					checkForJobStepsToRun();
					return null;
				});
			}
		}
		if (logger.isTraceEnabled()) logger.trace("<<< checkForJobStepsToRun({}, {}, {})",
				(null == kc ? "null" : kc.getId()),
				(pcId == 0 ? "null" : pcId),
				onlyRun);
    }

	/**
	 * Check unsatisfied queries of job step on processing facility (defined in Kube config).
	 * Start ready job steps on facility.
	 * 
	 * Method is synchronized to avoid different threads (simultaneous event-triggered dispatching) to
	 * interfere with each other.
	 * 
	 * @param kc KubeConfig
	 * @param jsId JobStep id
	 */
	// @Transactional
    public Boolean checkJobStepToRun(KubeConfig kc, long jsId) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkForJobStepsToRun({}, {})",
				(null == kc ? "null" : kc.getId()),
				(0 == jsId ? "null" : jsId));

		Boolean answer = true;
		if (productionPlanner != null) {
			if (kc != null && jsId != 0) {
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				final ProcessingFacility pfo = transactionTemplate.execute((status) -> {
					Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(kc.getLongId());
					if (opt.isPresent()) {
						return opt.get();
					}
					return null;
				});
				if (pfo != null) {

					Boolean checkQueries = transactionTemplate.execute((status) -> {
						Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(jsId);
						if (opt.isPresent()) {
							if ((opt.get().getJobStepState().equals(JobStepState.PLANNED) 
									||  opt.get().getJobStepState().equals(JobStepState.WAITING_INPUT))
									&& opt.get().getJob().getJobState() != JobState.ON_HOLD) {
								return true;
							}
						}
						return false;
					});
					answer = transactionTemplate.execute((status) -> {
						Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(jsId);
						JobStep js = null;
						if (opt.isPresent()) {
							js =  opt.get();
						}
						if (js != null && checkQueries) {
							checkJobStepQueries(js, false);
						}
						if (js != null && js.getJobStepState() == JobStepState.READY) {
							if (js.getJob().getJobState() == JobState.PLANNED) {
								js.getJob().setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
								RepositoryService.getJobRepository().save(js.getJob());
							}
							if ((js.getJob().getJobState() == JobState.RELEASED || js.getJob().getJobState() == JobState.STARTED)
									&& js.getJob().getProcessingOrder().getOrderState() != OrderState.SUSPENDING
									&& js.getJob().getProcessingOrder().getOrderState() != OrderState.PLANNED) {
								if (kc.couldJobRun()) {
									kc.createJob(String.valueOf(js.getId()), null, null);
								} else {
									return false;
								}
							}
						}
						return true;
					});
				}
			}
		}
		return answer;
	}

	/**
	 * Check unsatisfied queries of job steps in job on processing facility (defined in Kube config).
	 * Start ready job steps on facility.
	 * 
	 * Method is synchronized to avoid different threads (simultaneous event-triggered dispatching) to
	 * interfere with each other.
	 * 
	 * @param kc KubeConfig
	 * @param job Job
	 * @throws InterruptedException 
	 */
    public void checkJobToRun(KubeConfig kc, long jobId) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> checkJobToRun({}, {})",
				(null == kc ? "null" : kc.getId()),
				(0 == jobId ? "null" : jobId));

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		if (productionPlanner != null) {
			if (kc != null && jobId != 0) {
				final ProcessingFacility pfo = transactionTemplate.execute((status) -> {
					Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(kc.getLongId());
					if (opt.isPresent()) {
						return opt.get();
					}
					return null;
				});
				if (pfo != null) {
					// wait until finish of concurrent createJob
					try {
						productionPlanner.acquireReleaseSemaphore("checkJobToRun");
						final List<Long> jobSteps = new ArrayList<Long>();

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
					} finally {
						productionPlanner.releaseReleaseSemaphore("checkJobToRun");					
					}
				}
			}
		}
	}

	/**
	 * Check unsatisfied queries of job steps in processing order on processing facility (defined in Kube config).
	 * Start ready job steps on facility.
	 * 
	 * Method is synchronized to avoid different threads (simultaneous event-triggered dispatching) to
	 * interfere with each other.
	 * 
	 * @param kc KubeConfig
	 * @param order ProcessingOrder
	 */
	// @Transactional
    public void checkOrderToRun(KubeConfig kc, long orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkOrderToRun({}, {})",
				(null == kc ? "null" : kc.getId()),
				(0 == orderId ? "null" : orderId));

		if (productionPlanner != null) {
			if (kc != null && orderId != 0) {
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				final ProcessingFacility pfo = transactionTemplate.execute((status) -> {
					Optional<ProcessingFacility> opt = RepositoryService.getFacilityRepository().findById(kc.getLongId());
					if (opt.isPresent()) {
						return opt.get();
					}
					return null;
				});
				if (pfo != null) {
					// wait until finish of concurrent createJob
					final List<Long> jobSteps = new ArrayList<Long>();
					try {
						productionPlanner.acquireReleaseSemaphore("checkOrderToRun");
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
									}});
								
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
					} finally {
						productionPlanner.releaseReleaseSemaphore("checkOrderToRun");					
					}
				}
			}
		}
	}

	/**
	 * Check whether the product of job step exists
	 * @param js job step
	 * @return true if all products are generated
	 */
	@Transactional
	public void checkCreatedProducts(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkCreatedProducts({})", (null == js ? "null" : js.getId()));

		if (js != null && js.getJobStepState() == JobStepState.COMPLETED && null != js.getOutputProduct()) {
			ProcessingFacility pf = js.getJob().getProcessingFacility();
			if (pf != null) {

				Optional<Product> op = RepositoryService.getProductRepository().findById(js.getOutputProduct().getId());
				Product p = null;
				p = op.isPresent() ? op.get() : null;
				if (p != null) {
					List<Product> ptr = new ArrayList<Product>();
					ptr = checkCreatedProduct(p, pf);
					for (Product cp : ptr) {
						if (p.getComponentProducts().contains(cp)) {
							p.getComponentProducts().remove(cp);
							RepositoryService.getProductRepository().delete(cp);
							RepositoryService.getProductRepository().save(p);
							em.merge(p);
						}
					}
					if (p.getComponentProducts().isEmpty() && p.getProductFile().isEmpty()) {
						RepositoryService.getProductRepository().delete(p);
						js.setOutputProduct(null);
						RepositoryService.getJobStepRepository().save(js);
						em.merge(js);
					}
				}
			}
		}
	}

	@Transactional
	private List<Product> checkCreatedProduct(Product p, ProcessingFacility pf) {
		List<Product> productsToRemove = new ArrayList<Product>();
		if (logger.isTraceEnabled()) logger.trace(">>> checkCreatedProduct({})",
				(null == p ? "null" : p.getId()),
				(null == pf ? "null" : pf.getName()));

		if (p != null && pf != null) {
			Set<Product> pList = p.getComponentProducts();
			if (pList.isEmpty()) {
				// check product file
				Boolean removeProduct = true;
				List<ProductFile> fToRemove = new ArrayList<ProductFile>();
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
				List<Product> ptr = new ArrayList<Product>();
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
	 * Recursively collect products of a product tree into list
	 * 
	 * @param p Root product
	 * @param list Product list
	 */
	@Transactional
	public void collectProducts(Product p, List<Product> list) {
		if (p != null) {
			list.add(p);
			for (Product cp : p.getComponentProducts()) {
				collectProducts(cp, list);
			}
		}
	}
	
	/**
	 * Check whether the products of list exists and are already generated
	 * @param list Product list
	 * @param pf ProcessingFacility
	 * @return true if all products are generated
	 */
	@Transactional
	public Boolean checkProducts(List<Product> list, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkCreatedProduct(Product[{}], {})",
				(null == list ? "null" : list.size()),
				(null == pf ? "null" : pf.getName()));

		Boolean answer = true;
		for (Product p : list) {
			if (p.getProductFile().isEmpty()) {
				answer = false;
				break;
			} else {
				Boolean onPF = false;
				for (ProductFile f : p.getProductFile()) {
					if (f.getProcessingFacility().equals(pf)) {
						onPF = true;
						break;
					}
				}
				if (!onPF) {
					answer = false;
					break;
				}
			}
		}
		return answer;
	}
	
	/**
	 * Delete the Job Order file for the given job step from the Storage Manager
	 * 
	 * @param js the job step to delete the JOF from
	 * @return true on success, false otherwise
	 */
	private Boolean deleteJOF(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteJOF({})", (null == js ? "null" : js.getId()));

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
	
	private void startAipDownload(ProductQuery pq) {
		if (logger.isTraceEnabled()) logger.trace(">>> ProductQuery({})", pq);
		// calculate start and stop time
		// analyze sql query for times
		Instant startTime = pq.getJobStep().getJob().getStartTime();
		Instant stopTime = pq.getJobStep().getJob().getStopTime();
		List<SimplePolicy> simplePolicies = pq.getGeneratingRule().getSimplePolicies();
		for (SimplePolicy simplePolicy : simplePolicies) {
			startTime = startTime.isAfter(startTime.minusMillis(simplePolicy.getDeltaTimeT0().toMilliseconds())) ?
					startTime.minusMillis(simplePolicy.getDeltaTimeT0().toMilliseconds()) : startTime;
			stopTime = stopTime.isBefore(startTime.plusMillis(simplePolicy.getDeltaTimeT0().toMilliseconds())) ?
					stopTime.plusMillis(simplePolicy.getDeltaTimeT0().toMilliseconds()) : stopTime;
		}
		String user = "";
		String pw = "";
		if (Thread.currentThread() instanceof OrderReleaseThread) {
			user = ((OrderReleaseThread) Thread.currentThread()).getUser();
			pw = ((OrderReleaseThread) Thread.currentThread()).getPw();
		}
		try {
			if (logger.isTraceEnabled()) logger.trace("  {}{})", config.getAipUrl(),
					URI_PATH_DOWNLOAD_ALLBYTIME + "?productType=" + pq.getRequestedProductClass().getProductType() + "&startTime=" + OrbitTimeFormatter.format(startTime)
					+ "&stopTime=" + OrbitTimeFormatter.format(stopTime) + "&facility=" 
					+ pq.getJobStep().getJob().getProcessingFacility().getName());
			@SuppressWarnings("unchecked")
			List<RestProduct> restProducts = (List<RestProduct>) serviceConnection.getFromService(
					config.getAipUrl(),
					URI_PATH_DOWNLOAD_ALLBYTIME + "?productType=" + pq.getRequestedProductClass().getProductType() + "&startTime=" + OrbitTimeFormatter.format(startTime)
							+ "&stopTime=" + OrbitTimeFormatter.format(stopTime) + "&facility=" 
							+ pq.getJobStep().getJob().getProcessingFacility().getName(),
					RestProduct.class,
					user, 
					pw);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
				// nothing to do, perhaps it is optional
			} else {
				String message = logger.log(OdipMessage.MSG_EXCEPTION, e.getMessage(), e);
				// throw new Exception(message);
			}
		} catch (Exception e) {
			String message = logger.log(OdipMessage.MSG_EXCEPTION, e.getMessage(), e);
			// throw new Exception(message);
		}
	}
	

}
