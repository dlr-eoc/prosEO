/**
 * OrderDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ProseoMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.ProductQueryService;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.util.OrderPlanThread;
import de.dlr.proseo.planner.util.UtilService;

/**
 * Dispatcher to handle processing orders
 * 
 * @author Ernst Melchinger
 *
 */
@Service
public class OrderDispatcher {
	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(OrderDispatcher.class);

	@Autowired
	private ProductQueryService productQueryService;
		
	/**
	 * Publish an order, create jobs and job steps needed to create all products
	 * 
	 * @param order The processing order id
	 * @param pf The processing facility 
	 * @return true after success, else false
	 */
	public ProseoMessage prepareExpectedJobs(long orderId, ProcessingFacility pf, OrderPlanThread thread) throws InterruptedException {
		TransactionTemplate transactionTemplate = new TransactionTemplate(ProductionPlanner.productionPlanner.getTxManager());
		ProseoMessage msg = GeneralMessage.FALSE;
		try	{
			msg = (ProseoMessage) transactionTemplate.execute((status) -> {
				ProcessingOrder order = null;
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
				if (orderOpt.isPresent()) {
					order = orderOpt.get();
				}
				if (logger.isTraceEnabled()) logger.trace(">>> prepareExpectedJobs({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));

				ProseoMessage answer = GeneralMessage.FALSE;
				if (thread.isInterrupted()) {
					return PlannerMessage.PLANNING_INTERRUPTED;
				}
				if (order != null) {
					switch (order.getOrderState()) {
					case PLANNING: 
					case APPROVED: {
						// order is released, publish it
						answer = checkForValidOrder(order);
						if (!answer.getSuccess()) {
							break;
						}
						try {
							switch (order.getSlicingType()) {
							case CALENDAR_DAY:
								answer = createJobsForDay(order, pf, thread);
								break;
							case CALENDAR_MONTH:
								answer = createJobsForMonth(order, pf, thread);
								break;
							case CALENDAR_YEAR:
								answer = createJobsForYear(order, pf, thread);
								break;
							case ORBIT:
								answer = createJobsForOrbit(order, pf, thread);
								break;
							case TIME_SLICE:
								answer = createJobsForTimeSlices(order, pf, thread);
								break;
							case NONE:
								answer = createSingleJob(order, pf);
								break;
							default:
								answer = PlannerMessage.ORDER_SLICING_TYPE_NOT_SET;
								logger.log(answer, order.getIdentifier());
								break;

							}
						} catch (InterruptedException e) {
							return PlannerMessage.PLANNING_INTERRUPTED;
						}
						if (order.getJobs().isEmpty()) {
							order.setOrderState(OrderState.PLANNED);
							order.setOrderState(OrderState.RELEASING);
							order.setOrderState(OrderState.RELEASED);
							order.setOrderState(OrderState.RUNNING);
							order.setOrderState(OrderState.COMPLETED);
							UtilService.getOrderUtil().checkAutoClose(order);
						}
						break;
					}
					case RELEASED: {
						answer = PlannerMessage.ORDER_WAIT_FOR_RELEASE;
						logger.log(answer, order.getIdentifier(), order.getOrderState());
						break;
					}
					default: {
						answer = PlannerMessage.ORDER_WAIT_FOR_RELEASE;
						logger.log(answer, order.getIdentifier(), order.getOrderState());
						break;
					}

					}
				}	
				return answer;
			});
		} catch (Exception e) {
			throw e;
		}
		if (msg.getCode() == PlannerMessage.PLANNING_INTERRUPTED.getCode()) {
			throw new InterruptedException();
		}
		return msg;
	}

	/**
	 * Small test of order
	 * 
	 * @param order
	 * @return
	 */
	public ProseoMessage checkForValidOrder(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkForValidOrder({}, {})", (null == order ? "null": order.getIdentifier()));
		
		ProseoMessage answer = GeneralMessage.TRUE;
		// check for needed data
		if (order.getMission() == null) {
			answer = PlannerMessage.ORDER_MISSION_NOT_SET;
			logger.log(answer, order.getIdentifier());
		}
		if (order.getRequestedProductClasses().isEmpty()) {
			answer = PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET;
			logger.log(answer, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Create the needed job for an order of type orbit
	 * 
	 * @param order The processing order id
	 * @param pf The processing facility 
	 * @param thread The order plan thread to handle the interrupt
	 * @return The result message
	 * @throws InterruptedException
	 */
	public ProseoMessage createJobsForOrbit(ProcessingOrder order, ProcessingFacility pf, OrderPlanThread thread) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForOrbit({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		ProseoMessage answer = GeneralMessage.FALSE;
		// there has to be a list of orbits
		List<Orbit> orbits = order.getRequestedOrbits();
		try {
			if (orbits.isEmpty()) {
				answer = PlannerMessage.ORDER_REQ_ORBIT_NOT_SET;
				logger.log(answer, order.getIdentifier());
			} else {
				// create a job for each orbit
				// set order start time and stop time
				// we need			
				// product class
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					answer = PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET;
					logger.log(answer, order.getIdentifier());
				} else {
					// create jobs
					// for each orbit
					for (Orbit orbit : orbits) {
						if (thread.isInterrupted()) {
							answer = PlannerMessage.PLANNING_INTERRUPTED;
							logger.log(answer, order.getIdentifier());
							throw new InterruptedException();
						}
						// create job
						answer = createJobForOrbitOrTime(order, orbit, null, null, pf);
						if (!answer.getSuccess()) break;
					}
				}
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED;
				logger.log(answer, ex.getMessage());
		}

		return answer;
	}

	/**
	 * Create the needed job for an order of type day
	 * 
	 * @param order The processing order id
	 * @param pf The processing facility 
	 * @param thread The order plan thread to handle the interrupt
	 * @return The result message
	 * @throws InterruptedException
	 */
	public ProseoMessage createJobsForDay(ProcessingOrder order, ProcessingFacility pf, OrderPlanThread thread) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForDay({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		ProseoMessage answer = GeneralMessage.FALSE;

		Instant startT = null;
		Instant stopT = null;
		Instant sliceStopT = null;
		try {
			// get first day
			
			if (order.getStartTime() == null || order.getStopTime() == null) {
				answer = PlannerMessage.ORDER_REQ_DAY_NOT_SET;
				logger.log(answer, order.getIdentifier());
			} else {
				startT = order.getStartTime().truncatedTo(ChronoUnit.DAYS);
				stopT = order.getStopTime();
				sliceStopT = startT.plus(1, ChronoUnit.DAYS);
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					answer = PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET;
					logger.log(answer, order.getIdentifier());
				} else {
					// create jobs for each day
					while (startT.isBefore(stopT)) {
						if (thread.isInterrupted()) {
							answer = PlannerMessage.PLANNING_INTERRUPTED;
							logger.log(answer, order.getIdentifier());
							throw new InterruptedException();
						}
						// create job (without orbit association)
						answer = createJobForOrbitOrTime(order, null, startT, sliceStopT, pf);
						if (!answer.getSuccess()) break;
						startT = sliceStopT;
						sliceStopT = startT.plus(1, ChronoUnit.DAYS);
					} 
				}
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED;
			logger.log(answer, ex.getMessage());
		}

		return answer;
	}
	
	/**
	 * Create the needed job for an order of type month
	 * 
	 * @param order The processing order id
	 * @param pf The processing facility 
	 * @param thread The order plan thread to handle the interrupt
	 * @return The result message
	 * @throws InterruptedException
	 */
	public ProseoMessage createJobsForMonth(ProcessingOrder order, ProcessingFacility pf, OrderPlanThread thread) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForMonth({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		ProseoMessage answer = GeneralMessage.FALSE;

		Instant startT = null;
		Instant stopT = null;
		Instant sliceStopT = null;
		try {
			// get first day
			
			if (order.getStartTime() == null || order.getStopTime() == null) {
				answer = PlannerMessage.ORDER_REQ_DAY_NOT_SET;
				logger.log(answer, order.getIdentifier());
			} else {
				ZonedDateTime zdt = ZonedDateTime.ofInstant(order.getStartTime(), ZoneId.of("UTC"));
				Calendar calStart = GregorianCalendar.from(zdt);
				calStart.set(Calendar.DAY_OF_MONTH, 1);
				calStart.set(Calendar.HOUR_OF_DAY, 0);
				calStart.set(Calendar.MINUTE, 0);
				calStart.set(Calendar.SECOND, 0);
				calStart.set(Calendar.MILLISECOND, 0);
				Calendar calStop = (Calendar) calStart.clone();
				calStop.add(Calendar.MONTH, 1);				
				stopT = order.getStopTime();
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					answer = PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET;
					logger.log(answer, order.getIdentifier());
				} else {
					// create jobs for each day
					startT = calStart.toInstant();
					while (startT.isBefore(stopT)) {
						if (thread.isInterrupted()) {
							answer = PlannerMessage.PLANNING_INTERRUPTED;
							logger.log(answer, order.getIdentifier());
							throw new InterruptedException();
						}
						// create job (without orbit association)
						sliceStopT = calStop.toInstant();
						answer = createJobForOrbitOrTime(order, null, startT, sliceStopT, pf);
						if (!answer.getSuccess()) break;
						calStart = (Calendar) calStop.clone();
						calStop.add(Calendar.MONTH, 1);
						startT = calStart.toInstant();
					} 
				}
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED;
			logger.log(answer, ex.getMessage());
		}

		return answer;
	}

	/**
	 * Create the needed job for an order of type year
	 * 
	 * @param order The processing order id
	 * @param pf The processing facility 
	 * @param thread The order plan thread to handle the interrupt
	 * @return The result message
	 * @throws InterruptedException
	 */
	public ProseoMessage createJobsForYear(ProcessingOrder order, ProcessingFacility pf, OrderPlanThread thread) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForYear({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		ProseoMessage answer = GeneralMessage.FALSE;

		Instant startT = null;
		Instant stopT = null;
		Instant sliceStopT = null;
		try {
			// get first day
			
			if (order.getStartTime() == null || order.getStopTime() == null) {
				answer = PlannerMessage.ORDER_REQ_DAY_NOT_SET;
				logger.log(answer, order.getIdentifier());
			} else {
				ZonedDateTime zdt = ZonedDateTime.ofInstant(order.getStartTime(), ZoneId.of("UTC"));
				Calendar calStart = GregorianCalendar.from(zdt);
				calStart.set(Calendar.MONTH, 0);
				calStart.set(Calendar.DAY_OF_MONTH, 1);
				calStart.set(Calendar.HOUR_OF_DAY, 0);
				calStart.set(Calendar.MINUTE, 0);
				calStart.set(Calendar.SECOND, 0);
				calStart.set(Calendar.MILLISECOND, 0);
				Calendar calStop = (Calendar) calStart.clone();
				calStop.add(Calendar.YEAR, 1);				
				stopT = order.getStopTime();
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					answer = PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET;
					logger.log(answer, order.getIdentifier());
				} else {
					// create jobs for each day
					startT = calStart.toInstant();
					while (startT.isBefore(stopT)) {
						if (thread.isInterrupted()) {
							answer = PlannerMessage.PLANNING_INTERRUPTED;
							logger.log(answer, order.getIdentifier());
							throw new InterruptedException();
						}
						// create job (without orbit association)
						sliceStopT = calStop.toInstant();
						answer = createJobForOrbitOrTime(order, null, startT, sliceStopT, pf);
						if (!answer.getSuccess()) break;
						calStart = (Calendar) calStop.clone();
						calStop.add(Calendar.YEAR, 1);
						startT = calStart.toInstant();
					} 
				}
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED;
			logger.log(answer, ex.getMessage());
		}

		return answer;
	}

	/**
	 * Create the needed job for an order of type time slice
	 * 
	 * @param order The processing order id
	 * @param pf The processing facility 
	 * @param thread The order plan thread to handle the interrupt
	 * @return The result message
	 * @throws InterruptedException
	 */
	public ProseoMessage createJobsForTimeSlices(ProcessingOrder order, ProcessingFacility pf, OrderPlanThread thread) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForTimeSlices({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		ProseoMessage answer = GeneralMessage.FALSE;

		Instant startT = null;
		Instant stopT = null;
		Instant sliceStopT = null;
		try {
			// get first day
			
			if (order.getStartTime() == null || order.getStopTime() == null || order.getSliceDuration() == null) {
				answer = PlannerMessage.ORDER_REQ_TIMESLICE_NOT_SET;
				logger.log(answer, order.getIdentifier());
			} else {
				startT = order.getStartTime();
				stopT = order.getStopTime();
				sliceStopT = startT.plus(order.getSliceDuration());
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					answer = PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET;
					logger.log(answer, order.getIdentifier());
				} else {
					Duration delta = order.getSliceOverlap();
					if (delta == null) {
						delta = Duration.ofMillis(0);
					}
					delta = delta.dividedBy(2);
					if (startT.equals(stopT)) {
						answer = createJobForOrbitOrTime(order, null, startT.minus(delta), stopT.plus(delta), pf);
					} else {
						if (Duration.ZERO.equals(order.getSliceDuration())) {
							answer = PlannerMessage.ORDER_REQ_TIMESLICE_NOT_SET;
							logger.log(answer, order.getIdentifier());
						}
						// create jobs for each time slice
						while (startT.isBefore(stopT)) {
							if (thread.isInterrupted()) {
								answer = PlannerMessage.PLANNING_INTERRUPTED;
								logger.log(answer, order.getIdentifier());
								throw new InterruptedException();
							}
							// check orbit
							Orbit orbit = findOrbit(order, startT.minus(delta), sliceStopT.plus(delta));
							// create job
							answer = createJobForOrbitOrTime(order, orbit, startT.minus(delta), sliceStopT.plus(delta), pf);
							startT = sliceStopT;
							sliceStopT = startT.plus(order.getSliceDuration());
						} 
					}
				} 
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED;
			logger.log(answer, ex.getMessage());
		}

		return answer;
	}

	/**
	 * Create the needed job for an order without slicing
	 * 
	 * @param order The processing order
	 * @param pf The processing facility 
	 * @return The result message
	 */
	public ProseoMessage createSingleJob(ProcessingOrder order, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> createSingleJob({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		ProseoMessage answer = GeneralMessage.FALSE;

		try {
			if (order.getStartTime() == null || order.getStopTime() == null) {
				answer = PlannerMessage.ORDER_REQ_TIMESLICE_NOT_SET;
				logger.log(answer, order.getIdentifier());
			} else {
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					answer = PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET;
					logger.log(answer, order.getIdentifier());
				} else {
					answer = createJobForOrbitOrTime(order, null, order.getStartTime(), order.getStopTime(), pf);
				} 
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED;
			logger.log(answer, ex.getMessage());
		}

		return answer;
	}
	
	/**
	 * Create job for order of orbit or start/stop time of slice
	 * 
	 * @param order The processing order
	 * @param orbit The orbit 
	 * @param startT The start time
	 * @param stopT The stop time
	 * @param pf The facility to run the job
	 * @return true after success, else false
	 */
	@Transactional
	public ProseoMessage createJobForOrbitOrTime(ProcessingOrder order, Orbit orbit, Instant startT, Instant stopT, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobForOrbitOrTime({}, {}, {}, {}, {})",
				(null == order ? "null": order.getIdentifier()), (null == orbit ? "null" : orbit.getOrbitNumber()), startT, stopT,
				(null == pf ? "null" : pf.getName()));

		ProseoMessage answer = GeneralMessage.TRUE;
		// there has to be a list of orbits

		try {
			if (orbit == null && (startT == null || stopT == null)) {
				answer = PlannerMessage.ORDER_REQ_ORBIT_OR_TIME_NOT_SET;
				logger.log(answer, order.getIdentifier());
			} else {
				// create a job for each orbit
				// set order start time and stop time
				// we need			
				// product class
				Set<ProductClass> requestedProductClasses = order.getRequestedProductClasses();
				if (requestedProductClasses.isEmpty()) {
					answer = PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET;
					logger.log(answer, order.getIdentifier());
				} else {
					// create job (only keep it if at least one job step is created
					Job job = new Job();
					// Check whether job can be associated to an orbit
					if (null == orbit) {
						orbit = findOrbit(order, startT, stopT);
					}
					job.setOrbit(orbit);
					job.setJobState(JobState.INITIAL);
					if (startT == null) {
						startT = orbit.getStartTime();
					}
					job.setStartTime(startT);
					if (stopT == null) {
						stopT = orbit.getStopTime();
					}
					job.setStopTime(stopT);
					job.setProcessingOrder(order);
					job.setProcessingFacility(pf);
					Boolean exist = false;
					for (Job j : order.getJobs()) {
						if (j.getProcessingOrder().equals(job.getProcessingOrder())
								&& j.getStartTime().equals(job.getStartTime()) && j.getStopTime().equals(job.getStopTime())) {
							exist = true;
							break;
						}
					}
					if (exist) {
						logger.log(PlannerMessage.JOB_ALREADY_EXIST, order.getIdentifier());						
					} else {
						job = RepositoryService.getJobRepository().save(job);
						order.getJobs().add(job);
					}
				}
			}
		} catch (IllegalArgumentException ex) {
			answer = GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED;
			logger.log(answer, ex.getMessage());
		} catch (NoSuchElementException ex) {
			answer = GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED;
			logger.log(answer, ex.getMessage());
		}
		return answer;
	}

	/**
	 * The jobs were created, now create the job steps of each job.
	 * 
	 * @param order The processing order id
	 * @param pf The processing facility 
	 * @param productionPlanner The production planner instance
	 * @param thread The order plan thread to handle the interrupt
	 * @return The result message
	 * @throws InterruptedException
	 */
	public ProseoMessage createJobSteps(long orderId, ProcessingFacility pf, ProductionPlanner productionPlanner, OrderPlanThread thread) throws InterruptedException {
		ProcessingOrder order = null;

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		final List<Job> jobList = new ArrayList<Job>();
		try {
			productionPlanner.acquireThreadSemaphore("createJobSteps");
			order = (ProcessingOrder) transactionTemplate.execute((status) -> {
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
				if (orderOpt.isPresent()) {
					for(Job j : ((ProcessingOrder)(orderOpt.get())).getJobs()) {
						jobList.add(j);
					}
					return orderOpt.get();
				}
				return null;
			});
			productionPlanner.releaseThreadSemaphore("createJobSteps");	
		} catch (Exception e) {
			productionPlanner.releaseThreadSemaphore("createJobSteps");		
			throw e;
		}
		if (logger.isTraceEnabled()) logger.trace(">>> createJobSteps({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));

		ProseoMessage answer = GeneralMessage.TRUE;
		// calculate the planning packet size 
		
		int packetSize = ProductionPlanner.config.getPlanningBatchSize();
		final Long jCount = (long) jobList.size();
		List<Integer> curJList = new ArrayList<Integer>();
		curJList.add(0);
		List<Integer> curJSList = new ArrayList<Integer>();
		curJSList.add(0);
		try {
			while (curJList.get(0) < jCount) {
				productionPlanner.acquireThreadSemaphore("createJobSteps");
				ProseoMessage answer1 = transactionTemplate.execute((status) -> {
					curJSList.set(0, 0);
				    ProcessingOrder locOrder =  RepositoryService.getOrderRepository().getOne(orderId);
					while (curJList.get(0) < jCount && curJSList.get(0) < packetSize) {
						if (thread.isInterrupted()) {
							return PlannerMessage.PLANNING_INTERRUPTED;
						}
						if (jobList.get(curJList.get(0)).getJobState() == JobState.INITIAL) {
							Job locJob = RepositoryService.getJobRepository().getOne(jobList.get(curJList.get(0)).getId());
							createJobStepsOfJob(locOrder, locJob, productionPlanner);	
							curJSList.set(0, curJSList.get(0) + locJob.getJobSteps().size());
						}
						curJList.set(0, curJList.get(0) + 1);
					}
					return GeneralMessage.TRUE;
				});
				if (!answer1.getSuccess()) {
					answer = answer1;
					productionPlanner.releaseThreadSemaphore("createJobSteps");	
					return PlannerMessage.PLANNING_INTERRUPTED;
				}
				productionPlanner.releaseThreadSemaphore("createJobSteps");	
			}
		}
		catch (Exception e) {	
			productionPlanner.releaseThreadSemaphore("createJobSteps");
			throw e;
		}
		return answer;
	}

	/**
	 * Create the job steps of job with jobId.
	 * This is one complete transaction
	 * 
	 * @param order The processing order id
	 * @param jobId The job id 
	 * @param productionPlanner The production planner instance
	 */
	// @Transactional
	private void createJobStepsOfJob(ProcessingOrder order, Job job, ProductionPlanner productionPlanner) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobStepsOfJob({}, {})", order.getId(), job.getId());
			if (order != null ) {
				if (job != null && job.getJobState() == JobState.INITIAL) {
					List<JobStep> allJobSteps = new ArrayList<JobStep>();
					List<Product> allProducts = new ArrayList<Product>();
					// look for all products to create
					Set<ProductClass> requestedProductClasses = order.getRequestedProductClasses();
					for (ProductClass productClass : requestedProductClasses) {
						createProductsAndJobStep(productClass, job, order, allJobSteps, allProducts, productionPlanner);
					}
					if (job.getJobSteps().isEmpty()) {
						order.getJobs().remove(job);
						RepositoryService.getJobRepository().delete(job);
					} else {
						job.setJobState(JobState.PLANNED);
						RepositoryService.getJobRepository().save(job);
					}
				}
			}
		if (logger.isTraceEnabled()) logger.trace("<<< createJobStepsOfJob({}, {})", order.getId(), job.getId());
	}
	
	/**
	 * Find a suitable orbit for the given start and stop times. This is possible, if exactly one spacecraft exists for the
	 * mission, to which the order belongs, and the time interval between start and stop time spans at most two orbits
	 * (a product assigned to one orbit may extend into the next orbit).
	 * 
	 * @param order the processing order to extract the spacecraft from
	 * @param startT start time of the time interval to check
	 * @param stopT end time of the time interval to check
	 * @return the orbit, if a suitable orbit can be found, null otherwise
	 */
	private Orbit findOrbit(ProcessingOrder order, Instant startT, Instant stopT) {
		// Can we identify a unique spacecraft for the mission?
		if (startT == null || stopT == null) {
			return null;
		}
		
		Set<Spacecraft> spacecrafts = order.getMission().getSpacecrafts();
		if (1 == spacecrafts.size()) {
			Spacecraft spacecraft = spacecrafts.iterator().next();
			List<Orbit> orbits = RepositoryService.getOrbitRepository()
					.findByMissionCodeAndSpacecraftCodeAndTimeIntersect(
							spacecraft.getMission().getCode(), spacecraft.getCode(), startT, stopT);
			// If we find one or two orbits, then we assign the first one (even if the product extends into the next orbit)
			if (1 == orbits.size() || 2 == orbits.size()) {
				return orbits.get(0);
			}
		}
		return null;
	}

	/**
	 * Check whether the given job step already has product queries for the source product classes of the given product class,
	 * if not, create them
	 * 
	 * @param jobStep the job step to check
	 * @param productClass the product class to check against
	 * @throws IllegalArgumentException if the job step has a product query for a source product class, 
	 *         but it does not match the selection rules of the given product class
	 */
	private void findOrCreateProductQuery(JobStep jobStep, ProductClass productClass) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> findOrCreateProductQuery({}, {})",
				(null == jobStep ? "null": jobStep.getId()), (null == productClass ? "null" : productClass.getProductType()));
		
		String mode = jobStep.getProcessingMode();
		// search for selection rule with specific mode
		List<SimpleSelectionRule> selectedSelectionRules = new ArrayList<SimpleSelectionRule>();
		for (SimpleSelectionRule selectionRule : productClass.getRequiredSelectionRules()) {
			if (selectionRule.getMode() != null && selectionRule.getMode().equals(mode)) {
				selectedSelectionRules.add(selectionRule);
			}
		}
		if (selectedSelectionRules.isEmpty()) {
			for (SimpleSelectionRule selectionRule : productClass.getRequiredSelectionRules()) {
				if (selectionRule.getMode() == null) {
					selectedSelectionRules.add(selectionRule);
				}
			}
		}
		
		// Evaluate selection rules
		SELECTION_RULE:
		for (SimpleSelectionRule selectionRule : selectedSelectionRules) {
			
			// Skip selection rules not applicable for the selected configured processor
			if (!selectionRule.getConfiguredProcessors().isEmpty()) {
				Set<ConfiguredProcessor> requestedProcessors = jobStep.getJob().getProcessingOrder().getRequestedConfiguredProcessors();
				if (!requestedProcessors.isEmpty()) {
					// Check whether any of the requested processors in the order is applicable for the given product class
					List<ConfiguredProcessor> applicableRequestedProcessors = new ArrayList<>();
					for (ConfiguredProcessor requestedProcessor : requestedProcessors) {
						if (requestedProcessor.getProcessor().getProcessorClass().getProductClasses().contains(productClass)) {
							applicableRequestedProcessors.add(requestedProcessor);
						}
					}
					
					if (!applicableRequestedProcessors.isEmpty()) {
						// If there are requested processors, which are applicable for the product class,
						// the selection rule must be applicable for them
						Boolean processorFound = false;
						for (ConfiguredProcessor requestedProcessor : applicableRequestedProcessors) {
							if (selectionRule.getConfiguredProcessors().contains(requestedProcessor)) {
								// At least one requested processor exists, which is applicable for this selection rule
								processorFound = true;
								break;
							}
						}
						if (!processorFound) {
							// requestedProcessor is to be used, but selectionRule is not applicable for it, so skip it
							if (logger.isDebugEnabled())
								logger.debug(
										"Skipping selection rule '{}', because it is not applicable for the requested processors",
										selectionRule);
							continue SELECTION_RULE; // with next selection rule
						} 
					} 
				}
			}
			// OK - the current selection rule is valid for the processing order!
			
			// Check whether job step already has a product query for the given product class
			Boolean exist = false;
			for (ProductQuery productQuery: jobStep.getInputProductQueries()) {
				if (productQuery.getGeneratingRule().getSourceProductClass().equals(selectionRule.getSourceProductClass())) {
					// Make sure selection rules match (there may be multiple applicable selection rules/configured processors) 
					if (!selectionRule.toString().equals(productQuery.getGeneratingRule().toString())) {
						if (logger.isDebugEnabled()) logger.debug(
							String.format("Selection rule %s from product query does not match selection rule %s from product class %s",
								productQuery.getGeneratingRule().toString(), selectionRule.toString(), productClass.getProductType()));
						continue SELECTION_RULE;
					}
					// OK, product query already exists
					exist = true;
				}
			}
			if (!exist) {
				ProductQuery pq = ProductQuery.fromSimpleSelectionRule(selectionRule, jobStep,
						productQueryService.getProductColumnMapping(),
						ProductQueryService.FACILITY_QUERY_SQL,
						ProductQueryService.FACILITY_QUERY_SQL_SUBSELECT);
				pq = RepositoryService.getProductQueryRepository().save(pq);
				jobStep.getInputProductQueries().add(pq);
				if (logger.isDebugEnabled()) logger.debug("Product query generated for rule '{}'", selectionRule);
			}
		}
		if (jobStep.getInputProductQueries().isEmpty()) {
			logger.log(PlannerMessage.NO_INPUT_QUERIES, jobStep.getId());
		}
	}
	
	/**
	 * Recursively create output products and job steps for the given product class, order and job
	 * 
	 * @param productClass the requested output product class
	 * @param job the job to add the new job steps to
	 * @param order the processing order
	 * @param allJobSteps result list of job steps
	 * @param allProducts result list of output products
	 * @throws NoSuchElementException if a suitable configured processor could not be found
	 */
	private void createProductsAndJobStep(ProductClass productClass, Job job, ProcessingOrder order, List<JobStep> allJobSteps,
			List<Product> allProducts, ProductionPlanner productionPlanner) throws NoSuchElementException {
		if (logger.isTraceEnabled()) logger.trace(">>> createProductsAndJobStep({}, {}, {}, List<JobStep>, List<Product>)",
				(null == productClass ? "null" : productClass.getProductType()), (null == job ? "null": job.getId()),
				(null == order ? "null" : order.getIdentifier()));

		// Find product class with processor class
		ProductClass topProductClass = getTopProductClassWithPC(productClass);
		if (logger.isDebugEnabled()) logger.debug("Found top product class {}", topProductClass.getProductType());

		if (order.getInputProductClasses().contains(productClass) || order.getInputProductClasses().contains(topProductClass)) {
			// We don't need to create the product, it should be there
			if (logger.isDebugEnabled()) logger.debug("Skipping product class {} with top product class {} due to order input class stop list",
					productClass.getProductType(), topProductClass.getProductType());
			if (logger.isTraceEnabled()) logger.trace("<<< createProductsAndJobStep");
			return;
		}
		
		// Only one job step for one product
		for (JobStep i : allJobSteps) {
			if (i.getOutputProduct() == null || i.getOutputProduct().getProductClass().equals(topProductClass)) {
				if (logger.isDebugEnabled()) logger.debug("Skipping product class {} because a job step for it has already been generated",
						productClass.getProductType());
				if (logger.isTraceEnabled()) logger.trace("<<< createProductsAndJobStep");
				return;
			}
		}
		
		// Find configured processor to use
		ConfiguredProcessor configuredProcessor = searchConfiguredProcessorForProductClass(topProductClass, order.getRequestedConfiguredProcessors(), order.getProcessingMode());
		if (null == configuredProcessor) {
			// We cannot create the product, as there is no configured processor for it - now it depends ...
			if (order.getRequestedProductClasses().contains(productClass) || order.getRequestedProductClasses().contains(topProductClass)) {
				// This was one of the requested product classes, therefore fail
				String message = logger.log(PlannerMessage.ORDERDISP_NO_CONF_PROC, topProductClass.getProductType());
				throw new NoSuchElementException(message);
			} else {
				// This was a preceding (possibly auxiliary) input product class, so just wait for the product to appear
				if (logger.isDebugEnabled()) logger.debug("Skipping product class {} because no configured processor can be found",
						productClass.getProductType());
				if (logger.isTraceEnabled()) logger.trace("<<< createProductsAndJobStep");
				return;
			}
		}
		if (logger.isDebugEnabled()) logger.debug("Using configured processor {}", configuredProcessor.getIdentifier());

		// we have a configured processor!
		// now create products and job steps to generate output files 
		// create job step(s)
		JobStep jobStep = new JobStep();
		jobStep.setIsFailed(false);
		jobStep.setJobStepState(JobStepState.PLANNED);
		jobStep.setProcessingMode(order.getProcessingMode());
		jobStep.setJob(job);
		jobStep.getOutputParameters().putAll(order.getOutputParameters(topProductClass));
		jobStep = RepositoryService.getJobStepRepository().save(jobStep);
		job.getJobSteps().add(jobStep);

		// now we have the product class, create related products
		// also create job steps with queries related to product class
		// collect created products
		List<Product> products = new ArrayList<Product>();

		createProducts(topProductClass, 
				null, 
				configuredProcessor, 
				job.getOrbit(), 
				job,
				jobStep, 
				order.getOutputFileClass(), 
				job.getStartTime(), 
				job.getStopTime(), 
				products);
		
		// now we have to create the product queries for job step.

		if (products.isEmpty() || jobStep.getOutputProduct() == null) {
			if (logger.isDebugEnabled()) logger.debug("No products could be generated, skipping job step generation");
			job.getJobSteps().remove(jobStep);
			RepositoryService.getJobStepRepository().delete(jobStep);
			jobStep = null;
		} else {
			for (Product p : products) {
				try {
					// productionPlanner.acquireReleaseSemaphore();
					findOrCreateProductQuery(jobStep, p.getProductClass());
				} catch (Exception e) {
					logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage());
					job.getJobSteps().remove(jobStep);
					RepositoryService.getJobStepRepository().delete(jobStep);
					jobStep = null;
					if (logger.isTraceEnabled()) logger.trace("<<< createProductsAndJobStep");
					throw e;
				} finally {
					// productionPlanner.releaseReleaseSemaphore();					
				}
			}
			allProducts.addAll(products);
			
			// this means also to create new job steps for products which are not satisfied
			// check all queries for existing product definition (has not to be created!)
			List<JobStep> jobSteps = new ArrayList<JobStep>();
			jobSteps.add(jobStep);
			allJobSteps.add(jobStep);
			List<ProductQuery> pql = new ArrayList<ProductQuery>();
			pql.addAll(jobStep.getInputProductQueries());
			for (ProductQuery pq : pql) {
				if (productQueryService.executeQuery(pq, true)) {
					// Already satisfied, no processing of input product required							
					if (logger.isDebugEnabled()) logger.debug("Product query for rule '{}' already satisfied", pq.getGeneratingRule());
				} else {
					// otherwise create job step to build product.
					createProductsAndJobStep(pq.getRequestedProductClass(),
							job,
							order,
							allJobSteps,
							allProducts, 
							productionPlanner);
				} 
			}

			// save all created things
			job = RepositoryService.getJobRepository().save(job);
			for (JobStep js : jobSteps) {
				js.setJob(job);
				JobStep jobS = RepositoryService.getJobStepRepository().save(js);
				if (js.getOutputProduct() != null) {
					js.getOutputProduct().setJobStep(jobS);
					Product ps = RepositoryService.getProductRepository().save(js.getOutputProduct());
					jobS.setOutputProduct(ps);
					jobS = RepositoryService.getJobStepRepository().save(jobS);
				} else {
					@SuppressWarnings("unused")
					int bla = 1; // Debug support ;-)
				}
			}
		}
		if (logger.isTraceEnabled()) logger.trace("<<< createProductsAndJobStep");
	}
	
	/**
	 * Find the topmost product class.
	 * 
	 * @param pc The current product class
	 * @return The topmost product class
	 */
	public ProductClass getTopProductClassWithPC(ProductClass pc) {
		if (logger.isTraceEnabled()) logger.trace(">>> getTopProductClassWithPC({})", (null == pc ? "null" : pc.getProductType()));
		
		ProductClass rootProductClass = pc;
		while (rootProductClass.getProcessorClass() == null && rootProductClass.getEnclosingClass() != null) {
			rootProductClass = rootProductClass.getEnclosingClass();
		}		
		return rootProductClass;
	}
	
	/**
	 * Find all descendant product classes of pc
	 * 
	 * @param pc The current product class
	 * @return The list of descendant product classes of pc
	 */
	public List<ProductClass> getAllComponentClasses(ProductClass pc) {
		if (logger.isTraceEnabled()) logger.trace(">>> getAllComponentClasses({})", (null == pc ? "null" : pc.getProductType()));
		
		List<ProductClass> productClasses = new ArrayList<ProductClass>();
		productClasses.addAll(pc.getComponentClasses());
		for (ProductClass subPC : pc.getComponentClasses()) {
			productClasses.addAll(getAllComponentClasses(subPC));
		}		
		return productClasses;
	}
	

	/**
	 * Helper function to create the products of a "product tree"
	 * 
	 * @param productClass The current product class
	 * @param enclosingProduct The enclosing product
	 * @param cp The configured processor
	 * @param orbit The orbit
	 * @param job The job
	 * @param js The job step
	 * @param fileClass The file class as string
	 * @param startTime The start time 
	 * @param stopTime The stop time
	 * @param products List to collect all products created
	 * @return The current created product
	 */
	public Product createProducts(ProductClass productClass, Product enclosingProduct, ConfiguredProcessor cp, Orbit orbit, Job job, JobStep js,
				String fileClass, Instant startTime, Instant stopTime, List<Product> products) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProducts({}, {}, {}, {}, {}, {}, {}, {}, {}, [...])",
				(null == productClass ? "null" : productClass.getProductType()), (null == enclosingProduct ? "null" : enclosingProduct.getId()),
				(null == cp ? "null" : cp.getIdentifier()), (null == orbit ? "null" : orbit.getOrbitNumber()),
				(null == job ? "null" : job.getId()), (null == js ? "null" : js), fileClass, startTime, stopTime);
		
		Product product = createProduct(productClass, enclosingProduct, cp, orbit, job, js, fileClass, startTime, stopTime);

		if (product != null) {
			for (ProductClass pc : productClass.getComponentClasses()) {
				Product p = createProducts(pc, product, cp, orbit, job, null, fileClass, startTime, stopTime, products);
				if (p != null && product != null) {
					product.getComponentProducts().add(p);
				}
			}

			// We only add products, if they are not expected to have component products, or else if component products
			// have actually been generated
			if (productClass.getComponentClasses().isEmpty() || !product.getComponentProducts().isEmpty()) {
				products.add(product);
			} else {
				logger.trace("... no component products generated, dropping generated product");
				RepositoryService.getProductRepository().delete(product);
				product = null;
				js.setOutputProduct(null);
			}
		}
		return product;
	}

	/**
	 * Helper function to create a single product
	 * 
	 * @param productClass The current product class
	 * @param enclosingProduct The enclosing product
	 * @param cp The configured processor
	 * @param orbit The orbit
	 * @param job The job
	 * @param js The job step
	 * @param fileClass The file class as string
	 * @param startTime The start time 
	 * @param stopTime The stop time
	 * @return The current created product
	 */
	public Product createProduct(ProductClass productClass, Product enclosingProduct, ConfiguredProcessor cp, Orbit orbit, Job job,
				JobStep js, String fileClass, Instant startTime, Instant stopTime) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProduct({}, {}, {}, {}, {}, {}, {}, {}, {})",
				(null == productClass ? "null" : productClass.getProductType()), (null == enclosingProduct ? "null" : enclosingProduct.getId()),
				(null == cp ? "null" : cp.getIdentifier()), (null == orbit ? "null" : orbit.getOrbitNumber()),
				(null == job ? "null" : job.getId()), (null == js ? "null" : js), fileClass, startTime, stopTime);
		
		Product p = new Product();
		// Do not set UUID before checking for existing products, otherwise Product::equals() will always fail!
		p.getParameters().clear();
		p.getParameters().putAll(job.getProcessingOrder().getOutputParameters(productClass));
		p.setProductClass(productClass);
		p.setConfiguredProcessor(cp);
		p.setOrbit(orbit);
		p.setJobStep(js);
		p.setFileClass(fileClass);
		p.setSensingStartTime(startTime);
		p.setSensingStopTime(stopTime);
		p.setProductionType(job.getProcessingOrder().getProductionType());
		p.setEvictionTime(null);
		if (null != js) {
			p.setMode(js.getProcessingMode());
		}
		p.setEnclosingProduct(enclosingProduct);
		if ((enclosingProduct != null) && (p.getMode() == null || p.getMode().isEmpty())) {
			p.setMode(enclosingProduct.getMode());
		}
		// check if product exists
		// use configured processor, product class, sensing start and stop time
		List<Product> foundProducts = RepositoryService.getProductRepository()
				.findByProductClassAndConfiguredProcessorAndSensingStartTimeAndSensingStopTime(
						productClass.getId(),
						cp.getId(),
						startTime,
						stopTime);
		logger.trace("... found {} products with product class {}, configured processor {}, start time {} and stop time {}",
				productClass.getProductType(), cp.getIdentifier(), startTime, stopTime);
		for (Product foundProduct : foundProducts) {
			logger.trace("... testing product with ID {}", foundProduct.getId());
			if (foundProduct.equals(p)) {
				logger.trace("    ... fulfills 'equals'");
				if (!foundProduct.getProductFile().isEmpty()) {
					logger.trace("    ... has product files");
					for (ProductFile foundFile : foundProduct.getProductFile()) {
						logger.trace("        ... at facility {} (requested: {})", foundFile.getProcessingFacility().getName(), job.getProcessingFacility().getName());
						if (foundFile.getProcessingFacility().equals(job.getProcessingFacility())) {
							logger.trace("            ... skipping job step ('return null')");
							// it exists, nothing to do
							return null;
						}
					}
				}
			}
		}

		// New product, now we can safely set the UUID and store the product
		p.setUuid(UUID.randomUUID());
		p = RepositoryService.getProductRepository().save(p);
		if (js != null) {
			js.setOutputProduct(p);
		}
		
		if (logger.isDebugEnabled()) logger.debug("Output product {} created", p.getUuid().toString());
		
		return p;
	}
	
	/**
	 * Search newest configured processor for a product class
	 * Search first the newest processor by lexicographical comparison of processor version.
	 * Then search newest configuration by lexicographical comparison of configuration version and return corresponding 
	 * configured processor.
	 * 
	 * @param productClass To search for configured processor
	 * @return Configured processor found or null
	 */
	private ConfiguredProcessor searchConfiguredProcessorForProductClass(ProductClass productClass,
			Set<ConfiguredProcessor> requestedConfiguredProcessors, String processingMode) {
		if (logger.isTraceEnabled()) logger.trace(">>> searchConfiguredProcessorForProductClass({}, Set<ConfiguredProcessor, {})",
				(null == productClass ? "null" : productClass.getProductType()), processingMode);
		
		// Check arguments
		if (null == productClass) {
			if (logger.isDebugEnabled()) logger.debug("searchConfiguredProcessorForProductClass called without product class");
			return null;
		}
		
		List <ConfiguredProcessor> cplistFound = new ArrayList<ConfiguredProcessor>();
		
		if (productClass.getProcessorClass() != null) {
			// build list of all configured processors
			List <ConfiguredProcessor> cplist = new ArrayList<ConfiguredProcessor>();
			for (Processor p : productClass.getProcessorClass().getProcessors()) {
				for (ConfiguredProcessor cp : p.getConfiguredProcessors()) {
					if (cp.getEnabled()) {
						cplist.add(cp);
						if (logger.isDebugEnabled()) logger.debug("Candidate configured processor {} found", cp.getIdentifier());
					} else {
						if (logger.isDebugEnabled()) logger.debug("Candidate configured processor {} is disabled", cp.getIdentifier());
					}
				}
			}
			// now look whether one configured processor is in requested configured processors
			if (requestedConfiguredProcessors != null && !requestedConfiguredProcessors.isEmpty()) {
				for (ConfiguredProcessor cp : cplist) {
					if (requestedConfiguredProcessors.contains(cp)) {
						cplistFound.add(cp);
						if (logger.isDebugEnabled()) logger.debug("Candidate configured processor {} in list of requested processors", cp.getIdentifier());
					}
				}
			}
			// there is no requested configured processor, add all possible to look for the newest.
			if (cplistFound.isEmpty()) {
				cplistFound.addAll(cplist);
			}
		}

		if (!cplistFound.isEmpty() && processingMode != null && !processingMode.isBlank()) {
			// search the configured processors with expected processing mode
			List <ConfiguredProcessor> cplistFoundWithMode = new ArrayList<ConfiguredProcessor>();
			for (ConfiguredProcessor cp : cplistFound) {
				String configProcessingMode = cp.getConfiguration().getMode();
				if (null != configProcessingMode && configProcessingMode.equals(processingMode)) {
					cplistFoundWithMode.add(cp);
					if (logger.isDebugEnabled()) logger.debug("Candidate configured processor {} intended for mode {}", cp.getIdentifier(), processingMode);
				}
			}
			if (cplistFoundWithMode.isEmpty()) {
				for (ConfiguredProcessor cp : cplistFound) {
					if (null == cp.getConfiguration().getMode()) {
						cplistFoundWithMode.add(cp);
						if (logger.isDebugEnabled()) logger.debug("Candidate configured processor {} suitable for mode {}", cp.getIdentifier(), processingMode);
					}
				}
			}
			cplistFound.clear();
			cplistFound.addAll(cplistFoundWithMode);
		}

		// now search the newest processor
		Processor pFound = null;
		if (!cplistFound.isEmpty()) {
			for (ConfiguredProcessor cp : cplistFound) {
				if (null == pFound || cp.getProcessor().getProcessorVersion().compareTo(pFound.getProcessorVersion()) > 0) {
					pFound = cp.getProcessor();
				}
			}
			if (logger.isDebugEnabled()) logger.debug("Newest applicable processor version is {}", pFound.getProcessorVersion());
		}
		
		// search configured processor with newest configuration
		ConfiguredProcessor cpFound = null;
		for (ConfiguredProcessor cp : cplistFound) {
			if (cp.getProcessor().equals(pFound)) {
				if (null == cpFound || cp.getConfiguration().getConfigurationVersion().compareTo(cpFound.getConfiguration().getConfigurationVersion()) > 0) {
					cpFound = cp;
				}
			}
		}
		
		return cpFound;
	}
		
}
