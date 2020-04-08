/**
 * JobStepUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.dao.ProductRepository;
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
@Transactional
public class JobStepUtil {
	
	private static Logger logger = LoggerFactory.getLogger(JobStepUtil.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	@Autowired
	private ProductQueryService productQueryService;
	@Autowired
	private ProductionPlanner productionPlanner;
	
	@Transactional
	public void searchForJobStepsToRun(ProcessingFacility processingFacility) {
		// Search for all job steps on processingFacility with states INITIAL, WAITING_INPUT
		List<de.dlr.proseo.model.JobStep.JobStepState> jobStepStates = new ArrayList<>();
		jobStepStates.add(de.dlr.proseo.model.JobStep.JobStepState.INITIAL);
		jobStepStates.add(de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT);
		List<JobStep> jobSteps = null;
		if (processingFacility == null) {
			jobSteps = new ArrayList<>();
			for (ProcessingFacility pf : RepositoryService.getFacilityRepository().findAll()) {
				jobSteps.addAll(RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateIn(pf.getId(), jobStepStates));
			}
		} else {
			jobSteps = RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateIn(processingFacility.getId(), jobStepStates);
		}
		for (JobStep js : jobSteps) {
			checkJobStepQueries(js);
		}
		// check whether there are new satisfied product queries
		
		// Change state appropriate to result
	}

	@Transactional
	public Messages suspend(JobStep js, Boolean force) {
		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// INITIAL, WAITING_INPUT, READY, RUNNING, COMPLETED, FAILED
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
				answer = Messages.JOBSTEP_SUSPENDED;
				break;
			case READY:
			case WAITING_INPUT:
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.INITIAL);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
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
						}
					}
				}
				if (deleted) {
					js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.INITIAL);
					js.incrementVersion();
					answer = Messages.JOBSTEP_SUSPENDED;
					RepositoryService.getJobStepRepository().save(js);
				} else {
					answer = Messages.JOBSTEP_ALREADY_RUNNING;
				}
				break;
			case COMPLETED:
				answer = Messages.JOBSTEP_COMPLETED;
				break;
			case FAILED:
				answer = Messages.JOBSTEP_FAILED;
				break;
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Messages cancel(JobStep js) {
		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case READY:
			case WAITING_INPUT:
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.FAILED);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
				answer = Messages.JOBSTEP_CANCELED;
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
		}
		return answer;
	}

	@Transactional
	public Messages retry(JobStep js) {
		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case READY:
			case RUNNING:
			case COMPLETED:
				answer = Messages.JOBSTEP_COULD_NOT_RETRY;
				break;
			case WAITING_INPUT:
			case FAILED:
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.INITIAL);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
				answer = Messages.JOBSTEP_RETRIED;
				break;
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Boolean checkFinish(JobStep js) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case READY:
			case WAITING_INPUT:
				break;
			case RUNNING:
			case COMPLETED:
			case FAILED:
				em.merge(js);
				UtilService.getJobUtil().checkFinish(js.getJob());
				answer = true;
				break;
			default:
				break;
			}
		}
		return answer;
	}
	
	@Transactional
	public Boolean delete(JobStep js) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case READY:
			case WAITING_INPUT:
				if (js.getOutputProduct() != null) {
					deleteProduct(js.getOutputProduct());	
					js.setOutputProduct(null);
				};
				// fall through intended
			case COMPLETED:
			case FAILED:
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
				// RepositoryService.getJobStepRepository().delete(js);
				answer = true;
				break;
			case RUNNING:
				break;
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Boolean deleteForced(JobStep js) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case READY:
			case WAITING_INPUT:
				if (js.getOutputProduct() != null) {
					deleteProduct(js.getOutputProduct());	
					js.setOutputProduct(null);
				};
			case RUNNING:
			case COMPLETED:
			case FAILED:
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
				answer = true;
				break;
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Messages resume(JobStep js) {
		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case WAITING_INPUT:
				checkJobStepQueries(js);
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
		}
		return answer;
	}

	@Transactional
	public Boolean startJobStep(JobStep js) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		if (js != null) {
			switch (js.getJobStepState()) {
			case INITIAL:
			case WAITING_INPUT:
				break;
			case READY:
				UtilService.getJobUtil().startJob(js.getJob());
				js.setJobStepState(JobStepState.RUNNING);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
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

	@Transactional
	public void checkJobStepQueries(JobStep js) {
		Boolean hasUnsatisfiedInputQueries = false;
		if (   js.getJob() != null 
			&& (js.getJob().getJobState() == JobState.RELEASED || js.getJob().getJobState() == JobState.STARTED)) {
			logger.trace("Looking for product queries of job step: " + js.getId());
			for (ProductQuery pq : js.getInputProductQueries()) {
				if (!pq.isSatisfied()) {
					if (productQueryService.executeQuery(pq, false, false)) {
						js.getOutputProduct().getSatisfiedProductQueries().add(pq);
						RepositoryService.getProductQueryRepository().save(pq);
						RepositoryService.getProductRepository().save(js.getOutputProduct());
					} else {
						hasUnsatisfiedInputQueries = true;
					}
				}
			}
			if (hasUnsatisfiedInputQueries) {
				if (js.getJobStepState() != de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT) {
					js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.WAITING_INPUT);
					js.incrementVersion();
					RepositoryService.getJobStepRepository().save(js);
				}				
			} else {
				js.setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.READY);
				js.incrementVersion();
				RepositoryService.getJobStepRepository().save(js);
			}
		}
	}

	@Transactional
	private void deleteProduct(Product p) {
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

	@Transactional
    public void checkForJobStepsToRun() {
    	if (productionPlanner != null) {
    		Collection<KubeConfig> kcs = productionPlanner.getKubeConfigs();
    		if (kcs != null) {
    			for (KubeConfig kc : kcs) {
    				checkForJobStepsToRun(kc);
    			}
    		}
    	}
    }
	
	@Transactional
    public void checkForJobStepsToRun(KubeConfig kc) {
		if (productionPlanner != null) {
			if (kc != null) {
				List<JobStepState> states = new ArrayList<JobStepState>();
				states.add(JobStepState.READY);
				Optional<ProcessingFacility> pfo = RepositoryService.getFacilityRepository().findById(kc.getLongId());
				if (pfo.isPresent()) {
					this.searchForJobStepsToRun(pfo.get());
					List<JobStep> jobSteps = RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateIn(kc.getLongId(), states);
					for (JobStep js : jobSteps) {
						if (js.getJob().getJobState() == JobState.RELEASED || js.getJob().getJobState() == JobState.STARTED) {
							if (kc.couldJobRun()) {
								kc.createJob(String.valueOf(js.getId()), null, null);
							}
						}
					}
				}
			} else {
				checkForJobStepsToRun();
			}
		}
    }
}
