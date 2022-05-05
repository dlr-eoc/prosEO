/**
 * JobStepUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.service.ProductQueryService;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;

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
	private static Logger logger = LoggerFactory.getLogger(JobStepUtil.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	@Autowired
	private ProductQueryService productQueryService;
	@Autowired
	private ProductionPlanner productionPlanner;
	
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
		List<de.dlr.proseo.model.JobStep.JobStepState> jobStepStates = new ArrayList<>();
		if (!onlyWaiting) {
			jobStepStates.add(de.dlr.proseo.model.JobStep.JobStepState.PLANNED);
		}
		jobStepStates.add(de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT);
		List<JobStep> allJobSteps = null;

		allJobSteps = transactionTemplate.execute((status) -> {
			List<JobStep> jobSteps = null;
			if (processingFacility == null) {
				jobSteps = new ArrayList<>();
				if (pc != null) {
					List<ProductQuery> productQueries = RepositoryService.getProductQueryRepository()
							.findUnsatisfiedByProductClass(pcId);
					for (ProductQuery pq : productQueries) {
						if (pq.getJobStep().getJobStepState().equals(JobStepState.WAITING_INPUT)
								&& pq.getJobStep().getJob().getJobState() != JobState.ON_HOLD) {
							jobSteps.add(pq.getJobStep());
						} else if (!onlyWaiting && pq.getJobStep().getJobStepState().equals(JobStepState.PLANNED)
								&& pq.getJobStep().getJob().getJobState() != JobState.ON_HOLD) {
							jobSteps.add(pq.getJobStep());
						}
					}
				} else {
					for (ProcessingFacility pf : RepositoryService.getFacilityRepository().findAll()) {
						jobSteps.addAll(RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateIn(pf.getId(), jobStepStates));
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
								jobSteps.add(pq.getJobStep());
							} else if (!onlyWaiting && pq.getJobStep().getJobStepState().equals(JobStepState.PLANNED)
									&& pq.getJobStep().getJob().getJobState() != JobState.ON_HOLD) {
								jobSteps.add(pq.getJobStep());
							}
						}
					}
				} else {
					jobSteps = RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateIn(pfId, jobStepStates);
				}
			}
			return jobSteps;
		});
		for (JobStep js : allJobSteps) {
			checkJobStepQueries(js.getId(), false);
		}
	}

	/**
	 * Suspend job step, kill it if force is true otherwise wait until finish.
	 *  
	 * @param js Job step
	 * @param force Force 
	 * @return Result message
	 */
	@Transactional
	public Messages suspend(JobStep js, Boolean force) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspend({}, {})",
				(null == js ? "null" : js.getId()), force);

		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// PLANNED, WAITING_INPUT, READY, RUNNING, COMPLETED, FAILED
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
				answer = Messages.JOBSTEP_SUSPENDED;
				break;
			case READY:
			case WAITING_INPUT:
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.PLANNED);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
				em.merge(js);
				answer = Messages.JOBSTEP_SUSPENDED;
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
						answer = Messages.JOBSTEP_COMPLETED;
					} else if (js.getJobStepState() == JobStepState.FAILED) {
						answer = Messages.JOBSTEP_COMPLETED;
					} else {
						js.setProcessingStartTime(null);
						js.setProcessingCompletionTime(null);
						js.setProcessingStdOut(null);
						js.setProcessingStdErr(null);
						js.setJobOrderFilename(null);
						js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.PLANNED);
						js.incrementVersion();
						answer = Messages.JOBSTEP_SUSPENDED;
						RepositoryService.getJobStepRepository().save(js);
						em.merge(js);
					}
				} else {
					KubeConfig kc = productionPlanner.getKubeConfig(js.getJob().getProcessingFacility().getName());
					if (kc != null) {
						kc.sync();
						KubeJob kj = kc.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());
						if (kj != null) {
							answer = Messages.JOBSTEP_ALREADY_RUNNING;
						}
					} else {
						answer = Messages.JOBSTEP_SUSPENDED;
					}
				}
				break;
			case COMPLETED:
				answer = Messages.JOBSTEP_COMPLETED;
				break;
			case FAILED:
				answer = Messages.JOBSTEP_FAILED;
				break;
			case CLOSED:
				answer = Messages.JOBSTEP_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(js.getId()));
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
	public Messages cancel(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancel({})", (null == js ? "null" : js.getId()));

		Messages answer = Messages.FALSE;
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
				answer = Messages.JOBSTEP_CANCELED;
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
					answer = Messages.JOBSTEP_CANCELED;
				} else {
					answer = Messages.JOBSTEP_ALREADY_RUNNING;
				}
				break;
			case COMPLETED:
				answer = Messages.JOBSTEP_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.JOBSTEP_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.JOBSTEP_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(js.getId()));
		}
		return answer;
	}

	/**
	 * Close job step
	 * 
	 * @param js Job step
	 * @return Result message
	 */
	@Transactional
	public Messages close(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancel({})", (null == js ? "null" : js.getId()));

		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case WAITING_INPUT:
			case RUNNING:
				answer = Messages.JOBSTEP_COULD_NOT_CLOSE;
				break;
			case COMPLETED:
			case FAILED:
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.CLOSED);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
				em.merge(js);
				answer = Messages.JOBSTEP_CLOSED;
				break;
			case CLOSED:
				answer = Messages.JOBSTEP_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(js.getId()));
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
	public Messages retry(JobStep js) {
		if (logger.isTraceEnabled()) logger.trace(">>> retry({})", (null == js ? "null" : js.getId()));

		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case READY:
			case RUNNING:
			case COMPLETED:
			case CLOSED:
				answer = Messages.JOBSTEP_COULD_NOT_RETRY;
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
						answer = Messages.JOBSTEP_RETRIED_COMPLETED;
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
				answer = Messages.JOBSTEP_RETRIED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(js.getId()));
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
					}
					pq.getSatisfyingProducts().clear();
					RepositoryService.getProductQueryRepository().delete(pq);
				}
				js.getInputProductQueries().clear();
				Messages.JOBSTEP_DELETED.log(logger, String.valueOf(js.getId()));
				answer = true;
				break;
			case RUNNING:
				Messages.JOBSTEP_ALREADY_RUNNING.log(logger, String.valueOf(js.getId()));
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
					}
					pq.getSatisfyingProducts().clear();
					RepositoryService.getProductQueryRepository().delete(pq);
				}
				js.getInputProductQueries().clear();
				// RepositoryService.getJobStepRepository().delete(js);
				Messages.JOBSTEP_DELETED.log(logger, String.valueOf(js.getId()));
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
	public Messages resume(long jsId, Boolean force) {
		if (logger.isTraceEnabled()) logger.trace(">>> resume({}, {})", jsId, force);
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());

		final JobStep js = transactionTemplate.execute((status) -> {
			Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(jsId);
			if (opt.isPresent()) {
				return opt.get();
			}
			return null;
		});

		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case PLANNED:
			case WAITING_INPUT:
				try {
					productionPlanner.acquireReleaseSemaphore("resume");
					checkJobStepQueries(js.getId(), force);
				} catch (Exception e) {
					Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
				} finally {
					productionPlanner.releaseReleaseSemaphore("resume");					
				}
				if (js.getJobStepState() == JobStepState.WAITING_INPUT) {
					answer = Messages.JOBSTEP_WAITING;
				} else {
					answer = Messages.JOBSTEP_READY;
				}				
				break;
			case READY:
				answer = Messages.JOBSTEP_READY;
				break;
			case RUNNING:
				answer = Messages.JOBSTEP_ALREADY_RUNNING;
				break;
			case COMPLETED:
				answer = Messages.JOBSTEP_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.JOBSTEP_ALREADY_FAILED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(js.getId()));
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
				Messages.JOBSTEP_STARTED.log(logger, String.valueOf(js.getId()));
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
	public void checkJobStepQueries(long id, Boolean force) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());

		final JobStep js = transactionTemplate.execute((status) -> {
			Optional<JobStep> jsOpt = RepositoryService.getJobStepRepository().findById(id);
			if (jsOpt.isPresent()) {
				return jsOpt.get();
			}
			return null;
		});
		
		if (logger.isTraceEnabled()) logger.trace(">>> checkJobStepQueries({}, {}), jobStep state: {}", (null == js ? "null" : js.getId()), force, js.getJobStepState());

		if (js != null && (js.getJobStepState() == JobStepState.PLANNED || js.getJobStepState() == JobStepState.WAITING_INPUT)) {
			if (   js.getJob() != null 
					&& (force || js.getJob().getJobState() == JobState.RELEASED || js.getJob().getJobState() == JobState.STARTED)) {
				logger.trace("Looking for product queries of job step: " + js.getId());
				@SuppressWarnings("unused")
				final JobStep dummy = transactionTemplate.execute((status) -> {
					Boolean hasUnsatisfiedInputQueries = false;
					JobStep jsx = null;
					Optional<JobStep> jsOpt = RepositoryService.getJobStepRepository().findById(id);
					if (jsOpt.isPresent()) {
						jsx = jsOpt.get();
					}
					Map<String, Object> properties = new HashMap<>(); 
					properties.put("javax.persistence.lock.timeout", 10000L); 
					for (ProductQuery pq : jsx.getInputProductQueries()) {
						if (!pq.isSatisfied()) {
							if (productQueryService.executeQuery(pq, false)) {
								RepositoryService.getProductQueryRepository().save(pq);
								for (Product p: pq.getSatisfyingProducts()) {
									RepositoryService.getProductRepository().save(p);
								}
								// The following removed - it is the *input* product queries that matter!
								//							jsx.getOutputProduct().getSatisfiedProductQueries().add(pq);
								//							RepositoryService.getProductRepository().save(jsx.getOutputProduct());
							} else {
								hasUnsatisfiedInputQueries = true;
							}
						}
					}
					if (hasUnsatisfiedInputQueries) {
						if (jsx.getJobStepState() != de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT) {
							jsx.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT);
							jsx.incrementVersion();
							RepositoryService.getJobStepRepository().save(jsx);
							em.merge(jsx);
						}				
					} else {
						jsx.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.READY);
						jsx.incrementVersion();
						RepositoryService.getJobStepRepository().save(jsx);
						em.merge(jsx);
					}
					return null;
				});
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
			if (kc != null) {
				try {
					productionPlanner.acquireReleaseSemaphore("checkForJobStepsToRun");
					List<JobStepState> states = new ArrayList<JobStepState>();
					states.add(JobStepState.READY);

					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());

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
						@SuppressWarnings("unused")
						String dummy = transactionTemplate.execute((status) -> {
							List<JobStep> jobSteps = RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateIn(kc.getLongId(), states);
							for (JobStep js : jobSteps) {
								if ((js.getJob().getJobState() == JobState.RELEASED || js.getJob().getJobState() == JobState.STARTED)
										&& js.getJob().getProcessingOrder().getOrderState() != OrderState.SUSPENDING
										&& js.getJob().getProcessingOrder().getOrderState() != OrderState.PLANNED) {
									if (kc.couldJobRun()) {
										kc.createJob(String.valueOf(js.getId()), null, null);
									} else {
										// at the moment no further job could be started
										break;
									}
								}
							}
							return null;
						});
					} 
					productionPlanner.releaseReleaseSemaphore("checkForJobStepsToRun");	
				} catch (Exception e) {
					productionPlanner.releaseReleaseSemaphore("checkForJobStepsToRun");	
					Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
				} 
			} else {
				checkForJobStepsToRun();
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
					if (checkQueries) {
						checkJobStepQueries(jsId, false);
					}
					answer = transactionTemplate.execute((status) -> {
						Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(jsId);
						JobStep js = null;
						if (opt.isPresent()) {
							js =  opt.get();
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
				productionPlanner.acquireThreadSemaphore("checkJobToRun");
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
						Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
					} finally {
						productionPlanner.releaseReleaseSemaphore("checkJobToRun");					
					}
				}
				productionPlanner.releaseThreadSemaphore("release");	
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
								for (Job job : order.getJobs()) {
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
						Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
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

		if (js != null && js.getJobStepState() == JobStepState.COMPLETED) {
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
				Messages.JOF_DELETED.log(logger, js.getJobOrderFilename());
				return true;
			} catch (RestClientException e) {
				Messages.JOF_DELETING_ERROR.log(logger, js.getJobOrderFilename(), facility.getName(), e.getMessage());
				return false;
			} 
		} else {
			return false;
		}
	}
}
