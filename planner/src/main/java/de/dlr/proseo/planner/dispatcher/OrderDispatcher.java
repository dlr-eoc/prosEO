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
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
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
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.PlannerResultMessage;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.util.OrderPlanThread;
import de.dlr.proseo.planner.util.UtilService;

/**
 * Dispatcher to handle processing orders
 *
 * @author Ernst Melchinger
 */
@Service
public class OrderDispatcher {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(OrderDispatcher.class);

	/** The product query service */
	@Autowired
	private ProductQueryService productQueryService;

	/**
	 * Publish an order, create jobs and job steps needed to create all products
	 *
	 * @param orderId            The processing order id
	 * @param processingFacility The processing facility
	 * @param thread             The OrderPlanThread
	 * @return The PlannerResultMessage indicating the result of the operation. False if no order with the given id was found.
	 * @throws InterruptedException if the execution is interrupted
	 */
	public PlannerResultMessage prepareExpectedJobs(long orderId, ProcessingFacility processingFacility, OrderPlanThread thread)
			throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> prepareExpectedJobs({}, {}, {}", orderId,
				(null == processingFacility ? "null" : processingFacility.getName()), thread);

		// Initialize the result message (of the method) with a default value of FALSE
		PlannerResultMessage resultMessage = new PlannerResultMessage(GeneralMessage.FALSE);

		// Create a transaction template using the production planner's transaction manager
		TransactionTemplate transactionTemplate = new TransactionTemplate(ProductionPlanner.productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {

				// Execute the transaction within the transaction template
				resultMessage = (PlannerResultMessage) transactionTemplate.execute((status) -> {
					ProcessingOrder order = null;

					// Find the order with the given orderId
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
					if (orderOpt.isPresent()) {
						order = orderOpt.get();
					} else {
						PlannerResultMessage msg = new PlannerResultMessage(PlannerMessage.ORDER_NOT_EXIST);
						msg.setText(logger.log(msg.getMessage(), orderId));
						return msg;
					}

					if (logger.isTraceEnabled())
						logger.trace("... found order {}", order.getIdentifier());

					// Initialize the answer message (of the transaction template) with a default value of FALSE
					PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

					// Check if the thread executing this method has been interrupted
					if (thread.isInterrupted()) {
						PlannerResultMessage msg = new PlannerResultMessage(PlannerMessage.PLANNING_INTERRUPTED);
						msg.setText(logger.log(msg.getMessage(), orderId));
						return msg;
					}

					switch (order.getOrderState()) {
					case PLANNING:
					case APPROVED: 
						// Check if the order is valid
						answer = checkForValidOrder(order);
						if (!answer.getSuccess()) {
							break;
						}

						try {
							// Create jobs based on the order's slicing type
							switch (order.getSlicingType()) {
							case CALENDAR_DAY:
								answer = createJobsForDay(order, processingFacility, thread);
								break;
							case CALENDAR_MONTH:
								answer = createJobsForMonth(order, processingFacility, thread);
								break;
							case CALENDAR_YEAR:
								answer = createJobsForYear(order, processingFacility, thread);
								break;
							case ORBIT:
								answer = createJobsForOrbit(order, processingFacility, thread);
								break;
							case TIME_SLICE:
								answer = createJobsForTimeSlices(order, processingFacility, thread);
								break;
							case NONE:
								answer = createSingleJob(order, processingFacility);
								break;
							default:
								answer.setMessage(PlannerMessage.ORDER_SLICING_TYPE_NOT_SET);
								answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
								break;
							}
						} catch (InterruptedException e) {
							PlannerResultMessage msg = new PlannerResultMessage(PlannerMessage.PLANNING_INTERRUPTED);
							msg.setText(logger.log(msg.getMessage(), orderId));
							return msg;
						}

						if (order.getJobs().isEmpty()) {
							// Set the order states
							order.setOrderState(OrderState.PLANNED);
							order.setOrderState(OrderState.RELEASING);
							order.setOrderState(OrderState.RELEASED);
							order.setOrderState(OrderState.RUNNING);
							order.setOrderState(OrderState.COMPLETED);

							// Check for auto close and set times and state message
							UtilService.getOrderUtil().checkAutoClose(order);
							UtilService.getOrderUtil().setTimes(order);
							order.setStateMessage(ProductionPlanner.STATE_MESSAGE_COMPLETED);

							answer.setMessage(PlannerMessage.ORDER_ALREADY_COMPLETED);
						}
						break;

					case RELEASED: 
						answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASED);
						answer.setText(logger.log(answer.getMessage(), order.getIdentifier(), order.getOrderState()));
						break;

					default: 
						answer.setMessage(PlannerMessage.ORDER_WAIT_FOR_RELEASE); // TODO This message does not make sense here
						answer.setText(logger.log(answer.getMessage(), order.getIdentifier(), order.getOrderState()));
						break;

					}

					return answer;
				});
				break;
			} catch (CannotAcquireLockException e) {
				if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					ProseoUtil.dbWait(i + 1);
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			} catch (Exception e) {

				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

				throw e;
			}
		}

		// Check if the result message indicates a planning interruption
		if (resultMessage.getCode() == PlannerMessage.PLANNING_INTERRUPTED.getCode()) {
			throw new InterruptedException();
		}

		return resultMessage;
	}

	/**
	 * Checks if the given order is valid, i.e. has a mission and requested product classes
	 *
	 * @param order The processing order to check.
	 * @return A PlannerResultMessage indicating the validity of the order.
	 */
	public PlannerResultMessage checkForValidOrder(ProcessingOrder order) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkForValidOrder({})", (null == order ? "null" : order.getIdentifier()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.TRUE);

		// Check if the order has a mission set
		if (order.getMission() == null) {
			answer.setMessage(PlannerMessage.ORDER_MISSION_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}

		// Check if the order has requested product classes set
		if (order.getRequestedProductClasses().isEmpty()) {
			answer.setMessage(PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}

		return answer;
	}

	/**
	 * Create the necessary jobs for an order of order slicing type ORBIT.
	 *
	 * @param order              The processing order.
	 * @param processingFacility The processing facility.
	 * @param thread             The order plan thread to handle interrupts.
	 * @return The result message detailing if the jobs were created successfully or what type of error occurred.
	 * @throws InterruptedException if the execution is interrupted.
	 */
	public PlannerResultMessage createJobsForOrbit(ProcessingOrder order, ProcessingFacility processingFacility,
			OrderPlanThread thread) throws InterruptedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobsForOrbit({}, {})", (null == order ? "null" : order.getIdentifier()),
					(null == processingFacility ? "null" : processingFacility.getName()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

		if (order.getRequestedOrbits().isEmpty()) {
			// Orbits are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_ORBIT_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		} 

		if (order.getRequestedProductClasses().isEmpty()) {
			// Requested product classes are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		}
		
		// Retrieve the order's orbits
		try {
			// Create a job for each orbit
			for (Orbit orbit : order.getRequestedOrbits()) {
				if (thread.isInterrupted()) {
					answer.setMessage(PlannerMessage.PLANNING_INTERRUPTED);
					answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
					throw new InterruptedException();
				}

				answer = createJobForOrbitOrTime(order, orbit, null, null, processingFacility);

				if (!answer.getSuccess())
					break;
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());

			if (logger.isDebugEnabled()) {
				logger.debug("An exception occurred. Cause: ", e);
			}

			answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
			answer.setText(logger.log(answer.getMessage(), e.getMessage()));
		}

		return answer;
	}

	/**
	 * Create the necessary jobs for an order of order slicing type type CALENDER_DAY.
	 *
	 * @param order    The processing order.
	 * @param facility The processing facility.
	 * @param thread   The order plan thread to handle interrupts.
	 * @return The result message detailing if the jobs were created successfully or what type of error occurred.
	 * @throws InterruptedException if the execution is interrupted.
	 */
	public PlannerResultMessage createJobsForDay(ProcessingOrder order, ProcessingFacility facility, OrderPlanThread thread)
			throws InterruptedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobsForDay({}, {})", (null == order ? "null" : order.getIdentifier()),
					(null == facility ? "null" : facility.getName()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

		if (order.getStartTime() == null || order.getStopTime() == null) {
			// Start and stop time are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_DAY_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		}
		
		if (order.getRequestedProductClasses().isEmpty()) {
			// Requested product classes are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		
		try {
			// Set start and stop time to the exact beginning and end of the day
			Instant startTime = order.getStartTime().truncatedTo(ChronoUnit.DAYS);
			Instant stopTime = order.getStopTime();
			Instant sliceStopTime = startTime.plus(1, ChronoUnit.DAYS);

			// Create jobs until the last day is reached
			while (startTime.isBefore(stopTime)) {
				if (thread.isInterrupted()) {
					answer.setMessage(PlannerMessage.PLANNING_INTERRUPTED);
					answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
					throw new InterruptedException();
				}

				// Create jobs for each day
				answer = createJobForOrbitOrTime(order, null, startTime, sliceStopTime, facility);

				if (!answer.getSuccess())
					break;

				// Go to the next day
				startTime = sliceStopTime;
				sliceStopTime = startTime.plus(1, ChronoUnit.DAYS);
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
			answer.setText(logger.log(answer.getMessage(), e.getMessage()));
		}

		return answer;
	}

	/**
	 * Create the necessary jobs for an order of order slicing type type CALENDER_MONTH.
	 *
	 * @param order    The processing order.
	 * @param facility The processing facility.
	 * @param thread   The order plan thread to handle interrupts.
	 * @return The result message detailing if the jobs were created successfully or what type of error occurred.
	 * @throws InterruptedException if the execution is interrupted.
	 */
	public PlannerResultMessage createJobsForMonth(ProcessingOrder order, ProcessingFacility facility, OrderPlanThread thread)
			throws InterruptedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobsForMonth({}, {})", (null == order ? "null" : order.getIdentifier()),
					(null == facility ? "null" : facility.getName()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

		// Start and stop time are mandatory
		if (order.getStartTime() == null || order.getStopTime() == null) {
			answer.setMessage(PlannerMessage.ORDER_REQ_DAY_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		}

		// Requested stop times are mandatory
		Set<ProductClass> productClasses = order.getRequestedProductClasses();
		if (productClasses.isEmpty()) {
			answer.setMessage(PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		}
			
		try {
			// Set start and stop time to the exact beginning and end of the month
			ZonedDateTime zdt = ZonedDateTime.ofInstant(order.getStartTime(), ZoneId.of("UTC"));
			Calendar calStart = GregorianCalendar.from(zdt);
			calStart.set(Calendar.DAY_OF_MONTH, 1);
			calStart.set(Calendar.HOUR_OF_DAY, 0);
			calStart.set(Calendar.MINUTE, 0);
			calStart.set(Calendar.SECOND, 0);
			calStart.set(Calendar.MILLISECOND, 0);
			Calendar calStop = (Calendar) calStart.clone();
			calStop.add(Calendar.MONTH, 1);
			Instant stopTime = order.getStopTime();

			Instant startTime = calStart.toInstant();

			// Create jobs until the last month is reached
			while (startTime.isBefore(stopTime)) {
				if (thread.isInterrupted()) {
					answer.setMessage(PlannerMessage.PLANNING_INTERRUPTED);
					answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
					throw new InterruptedException();
				}

				// Create jobs for each month
				answer = createJobForOrbitOrTime(order, null, startTime, calStop.toInstant(), facility);
				if (!answer.getSuccess())
					break;

				// Go to the next month
				calStart = (Calendar) calStop.clone();
				calStop.add(Calendar.MONTH, 1);
				startTime = calStart.toInstant();
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
			answer.setText(logger.log(answer.getMessage(), e.getMessage()));
		}

		return answer;
	}

	/**
	 * Create the necessary jobs for an order of order slicing type type CALENDER_YEAR.
	 *
	 * @param order  The processing order.
	 * @param pf     The processing facility.
	 * @param thread The order plan thread to handle interrupts.
	 * @return The result message detailing if the jobs were created successfully or what type of error occurred.
	 * @throws InterruptedException if the execution is interrupted.
	 */
	public PlannerResultMessage createJobsForYear(ProcessingOrder order, ProcessingFacility pf, OrderPlanThread thread)
			throws InterruptedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobsForYear({}, {})", (null == order ? "null" : order.getIdentifier()),
					(null == pf ? "null" : pf.getName()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

		if (order.getStartTime() == null || order.getStopTime() == null) {
			// Start and stop time are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_DAY_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		}
		
		// Requested stop times are mandatory
		Set<ProductClass> productClasses = order.getRequestedProductClasses();
		if (productClasses.isEmpty()) {
			answer.setMessage(PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		
		try {
			// Set start and stop time to the exact beginning and end of the month
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
			
			Instant stopTime = order.getStopTime();
			Instant startTime = calStart.toInstant();

			// Create jobs until the last month is reached
			while (startTime.isBefore(stopTime)) {
				if (thread.isInterrupted()) {
					answer.setMessage(PlannerMessage.PLANNING_INTERRUPTED);
					answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
					throw new InterruptedException();
				}

				// Create jobs for each year
				answer = createJobForOrbitOrTime(order, null, startTime, calStop.toInstant(), pf);
				if (!answer.getSuccess())
					break;

				// Go to the next year
				calStart = (Calendar) calStop.clone();
				calStop.add(Calendar.YEAR, 1);
				startTime = calStart.toInstant();
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
			answer.setText(logger.log(answer.getMessage(), e.getMessage()));
		}

		return answer;
	}

	/**
	 * Create the necessary jobs for an order of order slicing type type TIME_SLICE.
	 *
	 * @param order    The processing order.
	 * @param facility The processing facility.
	 * @param thread   The order plan thread to handle interrupts.
	 * @return The result message detailing if the jobs were created successfully or what type of error occurred.
	 * @throws InterruptedException if the execution is interrupted.
	 */
	public PlannerResultMessage createJobsForTimeSlices(ProcessingOrder order, ProcessingFacility facility, OrderPlanThread thread)
			throws InterruptedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobsForTimeSlices({}, {})", (null == order ? "null" : order.getIdentifier()),
					(null == facility ? "null" : facility.getName()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

		if (order.getStartTime() == null || order.getStopTime() == null || order.getSliceDuration() == null) {
			// Start and stop time are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_TIMESLICE_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		} 		
		if (order.getRequestedProductClasses().isEmpty()) {
			// Requested product classes are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		}
		
		try {
			// Retrieve start and stop time of the time slice
			Instant startTime = order.getStartTime();
			Instant stopTime = order.getStopTime();
			Instant sliceStopTime = startTime.plus(order.getSliceDuration());

			// Create jobs until the last time slice is reached

			// Get slice overlap
			Duration delta = order.getSliceOverlap();
			if (delta == null) {
				delta = Duration.ofMillis(0);
			}
			delta = delta.dividedBy(2);

			if (startTime.equals(stopTime)) {
				answer = createJobForOrbitOrTime(order, null, startTime.minus(delta), stopTime.plus(delta), facility);
			} else {
				// Slice duration is mandatory
				if (Duration.ZERO.equals(order.getSliceDuration())) {
					answer.setMessage(PlannerMessage.ORDER_REQ_TIMESLICE_NOT_SET);
					answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
				}

				// Create jobs until the last time slice is reached
				while (startTime.isBefore(stopTime)) {
					if (thread.isInterrupted()) {
						answer.setMessage(PlannerMessage.PLANNING_INTERRUPTED);
						answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
						throw new InterruptedException();
					}

					// Create job for the current time slice
					answer = createJobForOrbitOrTime(order, null, startTime.minus(delta), sliceStopTime.plus(delta), facility);

					// Go to the next time slice
					startTime = sliceStopTime;
					sliceStopTime = startTime.plus(order.getSliceDuration());
				}
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
			answer.setText(logger.log(answer.getMessage(), e.getMessage()));
		}

		return answer;
	}

	/**
	 * Create the needed job for an order without slicing
	 *
	 * @param order    The processing order.
	 * @param facility The processing facility.
	 * @return The result message detailing if the jobs were created successfully or what type of error occurred.
	 */
	public PlannerResultMessage createSingleJob(ProcessingOrder order, ProcessingFacility facility) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createSingleJob({}, {})", (null == order ? "null" : order.getIdentifier()),
					(null == facility ? "null" : facility.getName()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

		if (order.getStartTime() == null || order.getStopTime() == null) {
			// Start and stop time are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_TIMESLICE_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		} 
		if (order.getRequestedProductClasses().isEmpty()) {
			// Requested product classes are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		} 

		try {
			// Create a job for the provided time frame
			answer = createJobForOrbitOrTime(order, null, order.getStartTime(), order.getStopTime(), facility);
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
			answer.setText(logger.log(answer.getMessage(), e.getMessage()));
		}

		return answer;
	}

	/**
	 * Create a job for an order based on the provided orbit or start/stop time of a slice.
	 *
	 * @param order     The processing order.
	 * @param orbit     The orbit (can be null if start/stop time is provided).
	 * @param startTime The start time of the slice (can be null if orbit is provided).
	 * @param stopTime  The stop time of the slice (can be null if orbit is provided).
	 * @param pf        The processing facility to run the job.
	 * @return The result message indicating the success (or cause for failure) of job creation.
	 */
	// Must be called within a transaction!
	//@Transactional(isolation = Isolation.REPEATABLE_READ)
	public PlannerResultMessage createJobForOrbitOrTime(ProcessingOrder order, Orbit orbit, Instant startTime, Instant stopTime,
			ProcessingFacility pf) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobForOrbitOrTime({}, {}, {}, {}, {})", (null == order ? "null" : order.getIdentifier()),
					(null == orbit ? "null" : orbit.getOrbitNumber()), startTime, stopTime, (null == pf ? "null" : pf.getName()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.TRUE);

		if (orbit == null && (startTime == null || stopTime == null)) {
			// Either orbit or start and stop time must be provided
			answer.setMessage(PlannerMessage.ORDER_REQ_ORBIT_OR_TIME_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		} 
		
		Set<ProductClass> requestedProductClasses = order.getRequestedProductClasses();
		if (requestedProductClasses.isEmpty()) {
			// Requested product classes are mandatory
			answer.setMessage(PlannerMessage.ORDER_REQ_PROD_CLASS_NOT_SET);
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
			return answer;
		}
			
		try {
			// Associate the job with an orbit
			if (null == orbit) {
				orbit = findOrbit(order, startTime, stopTime);
			} else {
				startTime = orbit.getStartTime();
				stopTime = orbit.getStopTime();
			}

			// Create and configure the job
			Job job = new Job();
			job.setOrbit(orbit);
			job.setJobState(JobState.INITIAL);

			job.setStartTime(startTime);
			job.setStopTime(stopTime);

			job.setPriority(order.getPriority());
			job.setProcessingOrder(order);
			job.setProcessingFacility(pf);

			// Check if the job already exists for the same order, start time, and stop time
			boolean exist = false;
			for (Job j : order.getJobs()) {
				if (j.getProcessingOrder().equals(job.getProcessingOrder()) && j.getStartTime().equals(job.getStartTime())
						&& j.getStopTime().equals(job.getStopTime())) {
					exist = true;
					break;
				}
			}

			if (exist) {
				logger.log(PlannerMessage.JOB_ALREADY_EXIST, order.getIdentifier());
			} else {
				// Save the job and add it to the order's jobs list
				job = RepositoryService.getJobRepository().save(job);
				order.getJobs().add(job);
			}
		} catch (IllegalArgumentException ex) {
			answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
			answer.setText(logger.log(answer.getMessage(), ex.getMessage()));
		} catch (NoSuchElementException ex) {
			answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
			answer.setText(logger.log(answer.getMessage(), ex.getMessage()));
		}

		return answer;
	}

	/**
	 * Create the job steps for each job of the processing order.
	 *
	 * @param orderId           The ID of the processing order.
	 * @param facility          The processing facility.
	 * @param productionPlanner The production planner instance.
	 * @param thread            The order plan thread to handle interrupts.
	 * @return The result message indicating the success (or the cause of failure) of creating job steps.
	 * @throws InterruptedException if the execution is interrupted.
	 */
	public PlannerResultMessage createJobSteps(long orderId, ProcessingFacility facility, ProductionPlanner productionPlanner,
			OrderPlanThread thread) throws InterruptedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobSteps({}, {}, ProductionPlanner, {})",
					orderId, (null == facility ? "null" : facility.getName()), thread);

		ProcessingOrder order = null;

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		final List<Job> jobList = new ArrayList<>();

		try {
			// If present, find the order with the given orderId, and retrieve its jobs
			transactionTemplate.setReadOnly(true);
			order = transactionTemplate.execute((status) -> {
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
				if (orderOpt.isPresent()) {
					// Copy the jobs into the local list
					for (Job j : (orderOpt.get()).getJobs()) {
						jobList.add(j);
					}
					return orderOpt.get();
				}
				return null;
			});
		} catch (Exception e) {
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			throw e;
		}

		if (logger.isTraceEnabled())
			logger.trace("... creating job steps for order {} in facility {})", (null == order ? "null" : order.getIdentifier()),
					(null == facility ? "null" : facility.getName()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.TRUE);

		// Calculate the planning packet size
		int packetSize = ProductionPlanner.config.getPlanningBatchSize();

		/*
		 * The following lists are used within the loop to control the iteration process and to ensure that the iteration continues
		 * until the desired packet size (batch size) is reached. By using lists instead of simple variables, the values can be
		 * modified and shared within the lambda expression passed to the transactionTemplate.execute() method, allowing them to
		 * maintain their state across iterations of the loop.
		 */
		final long jobCount = jobList.size();
		List<Integer> currentJobList = new ArrayList<>();
		currentJobList.add(0);
		List<Integer> currentJobStepList = new ArrayList<>();
		currentJobStepList.add(0);

		try {
			// Iterate over the jobs and create job steps for each job that is in the INITIAL state
			while (currentJobList.get(0) < jobCount) {
				
				// Prepare for transaction retry, if "org.springframework.dao.CannotAcquireLockException" is thrown
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.setReadOnly(false);
						PlannerResultMessage plannerResponse = transactionTemplate.execute((status) -> {
							currentJobStepList.set(0, 0);

							ProcessingOrder locOrder = RepositoryService.getOrderRepository().findById(orderId).orElse(null);

							while (currentJobList.get(0) < jobCount && currentJobStepList.get(0) < packetSize) {
								if (thread.isInterrupted()) {
									// If the thread is interrupted, return an interrupted message
									PlannerResultMessage msg = new PlannerResultMessage(PlannerMessage.PLANNING_INTERRUPTED);
									msg.setText(logger.log(msg.getMessage(), orderId));
									return msg;
								}

								// Retrieve the job from the repository
								if (jobList.get(currentJobList.get(0)).getJobState() == JobState.INITIAL) {
									Optional<Job> locJobOpt = RepositoryService.getJobRepository()
											.findById(jobList.get(currentJobList.get(0)).getId());
									Job locJob = null;
									if (locJobOpt.isPresent()) {
										locJob = locJobOpt.get();
									} else {
										return new PlannerResultMessage(GeneralMessage.FALSE);
									}
									// Create job steps for the job
									createJobStepsOfJob(locOrder, locJob, productionPlanner);
										
									// Update the count of job steps created
									currentJobStepList.set(0, currentJobStepList.get(0) + locJob.getJobSteps().size());
								}

								// Move to the next job
								currentJobList.set(0, currentJobList.get(0) + 1);
							}

							return new PlannerResultMessage(GeneralMessage.TRUE);
						});

						if (!plannerResponse.getSuccess()) {
							// If an interrupt message is received, set the answer, release the semaphore and return
							answer = plannerResponse;

							PlannerResultMessage msg = new PlannerResultMessage(PlannerMessage.PLANNING_INTERRUPTED);
							msg.setText(logger.log(msg.getMessage(), orderId));
							return msg;
						}
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait(i + 1);
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled())
				logger.debug("... exception in createJobSteps::doInTransaction(" + orderId + ", " 
						+ facility.getName() + ", ProductionPlanner): ", e);
			
			/*
			 * Exception above is recaught here (due to rethrow)
			 */
			throw e;
		}

		return answer;
	}

	/**
	 * Create the job steps for a specific job within a processing order.
	 *
	 * @param order             The processing order.
	 * @param job               The job for which to create the job steps.
	 * @param productionPlanner The production planner instance.
	 */
	private void createJobStepsOfJob(ProcessingOrder order, Job job, ProductionPlanner productionPlanner) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobStepsOfJob({}, {}, ProductionPlanner)", order.getId(), job.getId());

		if (order != null) {
			// Check if the job exists and is in the INITIAL state
			if (job != null && job.getJobState() == JobState.INITIAL) {
				List<JobStep> allJobSteps = new ArrayList<>();
				List<Product> allProducts = new ArrayList<>();

				// Retrieve the requested product classes for the order
				Set<ProductClass> requestedProductClasses = order.getRequestedProductClasses();

				// Iterate over the requested product classes
				for (ProductClass productClass : requestedProductClasses) {
					// Create the products and job steps for the product class
					createProductsAndJobStep(productClass, job, order, allJobSteps, allProducts, productionPlanner);
				}

				// Check if any job steps were created
				if (job.getJobSteps().isEmpty()) {
					// If no job steps were created, remove the job from the order and delete it
					order.getJobs().remove(job);
					RepositoryService.getJobRepository().delete(job);
				} else {
					// If job steps were created, update the job state to PLANNED and save the job
					job.setJobState(JobState.PLANNED);
					RepositoryService.getJobRepository().save(job);
				}
			}
		}

		if (logger.isTraceEnabled())
			logger.trace("<<< createJobStepsOfJob({}, {})", order.getId(), job.getId());
	}

	/**
	 * Find a suitable orbit for the given start and stop times. This is possible if exactly one spacecraft exists for the mission
	 * to which the order belongs, and the time interval between start and stop time spans at most two orbits.
	 *
	 * @param order     The processing order to extract the spacecraft from.
	 * @param startTime Start time of the time interval to check.
	 * @param stopTime  End time of the time interval to check.
	 * @return The orbit if a suitable orbit can be found, null otherwise.
	 */
	private Orbit findOrbit(ProcessingOrder order, Instant startTime, Instant stopTime) {
		// Check if start and stop times are provided
		if (startTime == null || stopTime == null) {
			return null;
		}

		// Get the spacecrafts associated with the mission of the order
		Set<Spacecraft> spacecrafts = order.getMission().getSpacecrafts();

		// Check if there is a unique spacecraft for the mission
		if (spacecrafts.size() == 1) {
			Spacecraft spacecraft = spacecrafts.iterator().next();

			// Find orbits that intersect with the given time interval and are associated with the mission and spacecraft
			List<Orbit> orbits = RepositoryService.getOrbitRepository()
				.findByMissionCodeAndSpacecraftCodeAndTimeIntersect(spacecraft.getMission().getCode(), spacecraft.getCode(),
						startTime, stopTime);

			// If one or two orbits are found, assign the first one (even if the product extends into the next orbit)
			if (orbits.size() == 1 || orbits.size() == 2) {
				return orbits.get(0);
			}

			// TODO Maybe log if more than two orbits (or more than one spacecraft) were found.
		}

		// Return null if no suitable orbit is found
		return null;
	}

	/**
	 * Check whether the given job step already has product queries for the source product classes of the given product class. If
	 * not, create them.
	 *
	 * @param jobStep      The job step to check.
	 * @param productClass The product class to check against.
	 * @throws IllegalArgumentException If the job step has a product query for a source product class, but it does not match the
	 *                                  selection rules of the given product class.
	 */
	private void findOrCreateProductQuery(JobStep jobStep, ProductClass productClass) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> findOrCreateProductQuery({}, {})", (null == jobStep ? "null" : jobStep.getId()),
					(null == productClass ? "null" : productClass.getProductType()));

		String mode = jobStep.getProcessingMode();

		// Search for selection rules with a specific mode or without a mode
		List<SimpleSelectionRule> selectedSelectionRules = new ArrayList<>();
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
		SELECTION_RULE: for (SimpleSelectionRule selectionRule : selectedSelectionRules) {

			// Skip selection rules not applicable for the selected configured processor
			if (!selectionRule.getConfiguredProcessors().isEmpty()) {
				Set<ConfiguredProcessor> requestedProcessors = jobStep.getJob()
					.getProcessingOrder()
					.getRequestedConfiguredProcessors();

				// Check if there are requested processors applicable for the product class
				if (!requestedProcessors.isEmpty()) {
					List<ConfiguredProcessor> applicableRequestedProcessors = new ArrayList<>();
					for (ConfiguredProcessor requestedProcessor : requestedProcessors) {
						if (requestedProcessor.getProcessor().getProcessorClass().getProductClasses().contains(productClass)) {
							applicableRequestedProcessors.add(requestedProcessor);
						}
					}

					// If applicable requested processors exist, the selection rule must be applicable for them
					if (!applicableRequestedProcessors.isEmpty()) {
						boolean processorFound = false;
						for (ConfiguredProcessor requestedProcessor : applicableRequestedProcessors) {
							if (selectionRule.getConfiguredProcessors().contains(requestedProcessor)) {
								processorFound = true;
								break;
							}
						}
						if (!processorFound) {
							if (logger.isDebugEnabled()) {
								logger.debug(
										"Skipping selection rule '{}', because it is not applicable for the requested processors",
										selectionRule);
							}
							continue SELECTION_RULE; // Skip to the next selection rule
						}
					}
				}
			}

			// The current selection rule is valid for the processing order

			// Check whether the job step already has a product query for the given product class
			boolean exist = false;
			for (ProductQuery productQuery : jobStep.getInputProductQueries()) {
				if (productQuery.getGeneratingRule().getSourceProductClass().equals(selectionRule.getSourceProductClass())) {
					// Make sure selection rules match (there may be multiple applicable selection rules/configured processors)
					if (!selectionRule.toString().equals(productQuery.getGeneratingRule().toString())) {
						if (logger.isDebugEnabled())
							logger.debug(String.format(
									"Selection rule %s from product query does not match selection rule %s from product class %s",
									productQuery.getGeneratingRule().toString(), selectionRule.toString(),
									productClass.getProductType()));
						continue SELECTION_RULE; // Skip to the next selection rule
					}
					exist = true; // Product query already exists
				}
			}

			if (!exist) {
				// Create a new product query based on the selection rule
				ProductQuery pq = ProductQuery.fromSimpleSelectionRule(selectionRule, jobStep,
						productQueryService.getProductColumnMapping(), ProductQueryService.FACILITY_QUERY_SQL,
						ProductQueryService.FACILITY_QUERY_SQL_SUBSELECT);
				pq = RepositoryService.getProductQueryRepository().save(pq);
				jobStep.getInputProductQueries().add(pq);

				if (logger.isDebugEnabled()) {
					logger.debug("Product query generated for rule '{}'", selectionRule);
				}
			}
		}

		if (jobStep.getInputProductQueries().isEmpty()) {
			logger.log(PlannerMessage.NO_INPUT_QUERIES, jobStep.getId());
		}
	}

	/**
	 * Recursively create output products and job steps for the given product class, order, and job.
	 *
	 * @param productClass      The requested output product class.
	 * @param job               The job to add the new job steps to.
	 * @param order             The processing order.
	 * @param allJobSteps       The result list of job steps.
	 * @param allProducts       The result list of output products.
	 * @param productionPlanner The production planner instance.
	 * @throws NoSuchElementException If a suitable configured processor could not be found.
	 */
	private void createProductsAndJobStep(ProductClass productClass, Job job, ProcessingOrder order, List<JobStep> allJobSteps,
			List<Product> allProducts, ProductionPlanner productionPlanner) throws NoSuchElementException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createProductsAndJobStep({}, {}, {}, List<JobStep>, List<Product>)",
					(null == productClass ? "null" : productClass.getProductType()), (null == job ? "null" : job.getId()),
					(null == order ? "null" : order.getIdentifier()));

		// Find the top-level product class with a processor class
		ProductClass topProductClass = getTopProductClassWithPC(productClass);
		if (logger.isDebugEnabled())
			logger.debug("Found top product class {}", topProductClass.getProductType());

		// Skip creation if the product class is already present in the order's input classes or if a job step for it has already
		// been generated
		if (order.getInputProductClasses().contains(productClass) || order.getInputProductClasses().contains(topProductClass)) {
			if (logger.isDebugEnabled())
				logger.debug("Skipping product class {} with top product class {} due to order input class stop list",
						productClass.getProductType(), topProductClass.getProductType());
			if (logger.isTraceEnabled())
				logger.trace("<<< createProductsAndJobStep");
			return;
		}

		// Skip creation if a job step for the product class has already been generated
		for (JobStep jobStep : allJobSteps) {
			if (jobStep.getOutputProduct() == null || jobStep.getOutputProduct().getProductClass().equals(topProductClass)) {
				if (logger.isDebugEnabled())
					logger.debug("Skipping product class {} because a job step for it has already been generated",
							productClass.getProductType());
				if (logger.isTraceEnabled())
					logger.trace("<<< createProductsAndJobStep");
				return;
			}
		}

		// Find a configured processor to use for the top product class
		ConfiguredProcessor configuredProcessor = searchConfiguredProcessorForProductClass(topProductClass,
				order.getRequestedConfiguredProcessors(), order.getProcessingMode());
		if (null == configuredProcessor) {
			// No configured processor found, handle the case based on whether the product class was requested or not
			if (order.getRequestedProductClasses().contains(productClass)
					|| order.getRequestedProductClasses().contains(topProductClass)) {
				// The product class was requested, so fail
				String message = logger.log(PlannerMessage.ORDERDISP_NO_CONF_PROC, topProductClass.getProductType());
				throw new NoSuchElementException(message);
			} else {
				// The product class was a preceding input class, so just wait for the product to appear
				if (logger.isDebugEnabled())
					logger.debug("Skipping product class {} because no configured processor can be found",
							productClass.getProductType());
				if (logger.isTraceEnabled())
					logger.trace("<<< createProductsAndJobStep");
				return;
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("Using configured processor {}", configuredProcessor.getIdentifier());

		// Create a job step for the product class
		JobStep jobStep = new JobStep();
		jobStep.setIsFailed(false);
		jobStep.setJobStepState(JobStepState.PLANNED);
		jobStep.setProcessingMode(order.getProcessingMode());
		jobStep.setPriority(job.getPriority());
		jobStep.setJob(job);
		jobStep.getOutputParameters().putAll(order.getOutputParameters(topProductClass));
		jobStep = RepositoryService.getJobStepRepository().save(jobStep);
		job.getJobSteps().add(jobStep);

		// Create products and related job steps for the top product class
		List<Product> products = new ArrayList<>();
		createProducts(topProductClass, null, configuredProcessor, job.getOrbit(), job, jobStep, order.getOutputFileClass(),
				job.getStartTime(), job.getStopTime(), products);

		// Create product queries for the job step
		if (products.isEmpty() || jobStep.getOutputProduct() == null) {
			// No products could be generated, remove the job step
			if (logger.isDebugEnabled())
				logger.debug("No products could be generated, skipping job step generation");
			job.getJobSteps().remove(jobStep);
			RepositoryService.getJobStepRepository().delete(jobStep);
			jobStep = null;
		} else {
			// Iterate over the generated products and create job steps for unsatisfied product queries
			for (Product product : products) {
				try {
					findOrCreateProductQuery(jobStep, product.getProductClass());
				} catch (Exception e) {
					logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage());
					
					if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

					job.getJobSteps().remove(jobStep);
					RepositoryService.getJobStepRepository().delete(jobStep);
					jobStep = null;
					if (logger.isTraceEnabled())
						logger.trace("<<< createProductsAndJobStep");
					throw e;
				} finally {
					// productionPlanner.releaseReleaseSemaphore();
				}
			}
			allProducts.addAll(products);

			// Check all product queries and recursively create job steps for unsatisfied queries
			List<JobStep> jobSteps = new ArrayList<>();
			jobSteps.add(jobStep);
			allJobSteps.add(jobStep);
			List<ProductQuery> productQueryList = new ArrayList<>();
			productQueryList.addAll(jobStep.getInputProductQueries());
			for (ProductQuery pq : productQueryList) {
				if (productQueryService.executeQuery(pq, true)) {
					// The product query is already satisfied, no processing of input product required
					if (logger.isDebugEnabled())
						logger.debug("Product query for rule '{}' already satisfied", pq.getGeneratingRule());
				} else {
					// Create job steps for the unsatisfied product query
					createProductsAndJobStep(pq.getRequestedProductClass(), job, order, allJobSteps, allProducts,
							productionPlanner);
				}
			}

			// Save the created job and job steps
			job = RepositoryService.getJobRepository().save(job);
			for (JobStep js : jobSteps) {
				js.setJob(job);
				JobStep jobS = RepositoryService.getJobStepRepository().save(js);
				if (js.getOutputProduct() != null) {
					js.getOutputProduct().setJobStep(jobS);
					Product product = RepositoryService.getProductRepository().save(js.getOutputProduct());
					jobS.setOutputProduct(product);
					jobS = RepositoryService.getJobStepRepository().save(jobS);
				} else {
					// Debug support
					@SuppressWarnings("unused")
					int bla = 1;
				}
			}
		}

		if (logger.isTraceEnabled())
			logger.trace("<<< createProductsAndJobStep");
	}

	/**
	 * Find the topmost product class.
	 *
	 * @param productClass The current product class.
	 * @return The topmost product class.
	 */
	public ProductClass getTopProductClassWithPC(ProductClass productClass) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getTopProductClassWithPC({})", (null == productClass ? "null" : productClass.getProductType()));

		ProductClass rootProductClass = productClass;
		// Iterate through the enclosing product classes until a product class with a processor class is found
		while (rootProductClass.getProcessorClass() == null && rootProductClass.getEnclosingClass() != null) {
			rootProductClass = rootProductClass.getEnclosingClass();
		}
		return rootProductClass;
	}

	/**
	 * Find all descendant product classes of the given product class.
	 *
	 * @param productClass The current product class.
	 * @return The list of descendant product classes of pc.
	 */
	public List<ProductClass> getAllComponentClasses(ProductClass productClass) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAllComponentClasses({})", (null == productClass ? "null" : productClass.getProductType()));

		List<ProductClass> productClasses = new ArrayList<>();
		// Add the component classes of the current product class
		productClasses.addAll(productClass.getComponentClasses());
		// Recursively add the component classes of the sub-product classes
		for (ProductClass subProductClass : productClass.getComponentClasses()) {
			productClasses.addAll(getAllComponentClasses(subProductClass));
		}
		return productClasses;
	}

	/**
	 * Helper function to create the products of a "product tree".
	 *
	 * @param productClass        The current product class.
	 * @param enclosingProduct    The enclosing product.
	 * @param configuredProcessor The configured processor.
	 * @param orbit               The orbit.
	 * @param job                 The job.
	 * @param jobStep             The job step.
	 * @param fileClass           The file class as string.
	 * @param startTime           The start time.
	 * @param stopTime            The stop time.
	 * @param products            List to collect all products created.
	 * @return The current created product.
	 */
	public Product createProducts(ProductClass productClass, Product enclosingProduct, ConfiguredProcessor configuredProcessor,
			Orbit orbit, Job job, JobStep jobStep, String fileClass, Instant startTime, Instant stopTime, List<Product> products) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createProducts({}, {}, {}, {}, {}, {}, {}, {}, {}, [...])",
					(null == productClass ? "null" : productClass.getProductType()),
					(null == enclosingProduct ? "null" : enclosingProduct.getId()),
					(null == configuredProcessor ? "null" : configuredProcessor.getIdentifier()),
					(null == orbit ? "null" : orbit.getOrbitNumber()), (null == job ? "null" : job.getId()),
					(null == jobStep ? "null" : jobStep), fileClass, startTime, stopTime);

		// Create the product using the given parameters
		Product product = createProduct(productClass, enclosingProduct, configuredProcessor, orbit, job, jobStep, fileClass,
				startTime, stopTime);

		if (product != null) {
			// Recursively create products for the component classes
			for (ProductClass pc : productClass.getComponentClasses()) {
				Product p = createProducts(pc, product, configuredProcessor, orbit, job, null, fileClass, startTime, stopTime,
						products);
				if (p != null && product != null) {
					product.getComponentProducts().add(p);
				}
			}

			// Only add the product to the list if it either doesn't have component classes or if component products have been
			// generated
			if (productClass.getComponentClasses().isEmpty() || !product.getComponentProducts().isEmpty()) {
				products.add(product);
			} else {
				logger.trace("... no component products generated, dropping generated product");
				RepositoryService.getProductRepository().delete(product);
				product = null;
				jobStep.setOutputProduct(null);
			}
		}
		return product;
	}

	/**
	 * Helper function to create a single product.
	 *
	 * @param productClass        The current product class.
	 * @param enclosingProduct    The enclosing product.
	 * @param configuredProcessor The configured processor.
	 * @param orbit               The orbit.
	 * @param job                 The job.
	 * @param jobStep             The job step.
	 * @param fileClass           The file class as string.
	 * @param startTime           The start time.
	 * @param stopTime            The stop time.
	 * @return The current created product.
	 */
	public Product createProduct(ProductClass productClass, Product enclosingProduct, ConfiguredProcessor configuredProcessor,
			Orbit orbit, Job job, JobStep jobStep, String fileClass, Instant startTime, Instant stopTime) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createProduct({}, {}, {}, {}, {}, {}, {}, {}, {})",
					(null == productClass ? "null" : productClass.getProductType()),
					(null == enclosingProduct ? "null" : enclosingProduct.getId()),
					(null == configuredProcessor ? "null" : configuredProcessor.getIdentifier()),
					(null == orbit ? "null" : orbit.getOrbitNumber()), (null == job ? "null" : job.getId()),
					(null == jobStep ? "null" : jobStep), fileClass, startTime, stopTime);

		Product product = new Product();
		// Do not set UUID before checking for existing products, otherwise Product::equals() will always fail!
		product.getParameters().clear();
		product.getParameters().putAll(job.getProcessingOrder().getOutputParameters(productClass));
		product.setProductClass(productClass);
		product.setConfiguredProcessor(configuredProcessor);
		product.setOrbit(orbit);
		product.setJobStep(jobStep);
		product.setFileClass(fileClass);
		product.setSensingStartTime(startTime);
		product.setSensingStopTime(stopTime);
		product.setProductionType(job.getProcessingOrder().getProductionType());
		product.setEvictionTime(null);
		if (null != jobStep) {
			product.setMode(jobStep.getProcessingMode());
		}
		product.setEnclosingProduct(enclosingProduct);

		// Check if the product already exists
		List<Product> foundProducts = RepositoryService.getProductRepository()
			.findByProductClassAndConfiguredProcessorAndSensingStartTimeAndSensingStopTime(productClass.getId(),
					configuredProcessor.getId(), startTime, stopTime);
		logger.trace("... found {} products with product class {}, configured processor {}, start time {} and stop time {}",
				foundProducts.size(), productClass.getProductType(), configuredProcessor.getIdentifier(), startTime, stopTime);
		for (Product foundProduct : foundProducts) {
			logger.trace("... testing product with ID {}", foundProduct.getId());
			if (foundProduct.equals(product)) {
				logger.trace("    ... fulfills 'equals'");
				if (!foundProduct.getProductFile().isEmpty()) {
					logger.trace("    ... has product files");
					for (ProductFile foundFile : foundProduct.getProductFile()) {
						logger.trace("        ... at facility {} (requested: {})", foundFile.getProcessingFacility().getName(),
								job.getProcessingFacility().getName());
						if (foundFile.getProcessingFacility().equals(job.getProcessingFacility())) {
							logger.trace("... skipping job step ('return null')");
							return null;
						}
					}
				}
			}
		}

		// New product, generate UUID and save it
		product.setUuid(UUID.randomUUID());
		product = RepositoryService.getProductRepository().save(product);
		if (jobStep != null) {
			jobStep.setOutputProduct(product);
		}

		if (logger.isDebugEnabled())
			logger.debug("Output product {} created", product.getUuid().toString());

		return product;
	}

	/**
	 * Search for the newest configured processor for a product class. The search is performed by comparing the processor and
	 * configuration versions.
	 *
	 * @param productClass                  The product class to search for the configured processor.
	 * @param requestedConfiguredProcessors The requested configured processors.
	 * @param processingMode                The processing mode.
	 * @return The found configured processor, or null if not found.
	 */
	private ConfiguredProcessor searchConfiguredProcessorForProductClass(ProductClass productClass,
			Set<ConfiguredProcessor> requestedConfiguredProcessors, String processingMode) {
		if (logger.isTraceEnabled())
			logger.trace(">>> searchConfiguredProcessorForProductClass({}, Set<ConfiguredProcessor, {})",
					(null == productClass ? "null" : productClass.getProductType()), processingMode);

		// Check if product class is null
		if (null == productClass) {
			if (logger.isDebugEnabled())
				logger.debug("searchConfiguredProcessorForProductClass called without product class");
			return null;
		}

		List<ConfiguredProcessor> foundConfiguredProcessors = new ArrayList<>();

		if (productClass.getProcessorClass() != null) {
			// Build a list of all configured processors
			List<ConfiguredProcessor> allConfiguredProcessors = new ArrayList<>();
			for (Processor processor : productClass.getProcessorClass().getProcessors()) {
				for (ConfiguredProcessor configuredProcessor : processor.getConfiguredProcessors()) {
					if (configuredProcessor.getEnabled()) {
						allConfiguredProcessors.add(configuredProcessor);
						if (logger.isDebugEnabled())
							logger.debug("Candidate configured processor {} found", configuredProcessor.getIdentifier());
					} else {
						if (logger.isDebugEnabled())
							logger.debug("Candidate configured processor {} is disabled", configuredProcessor.getIdentifier());
					}
				}
			}

			// Find the requested configured processors
			if (requestedConfiguredProcessors != null && !requestedConfiguredProcessors.isEmpty()) {
				for (ConfiguredProcessor configuredProcessor : allConfiguredProcessors) {
					if (requestedConfiguredProcessors.contains(configuredProcessor)) {
						foundConfiguredProcessors.add(configuredProcessor);
						if (logger.isDebugEnabled())
							logger.debug("Candidate configured processor {} in list of requested processors",
									configuredProcessor.getIdentifier());
					}
				}
			}

			// If no requested configured processors are found, add all possible ones to search for the newest
			if (foundConfiguredProcessors.isEmpty()) {
				foundConfiguredProcessors.addAll(allConfiguredProcessors);
			}
		}

		if (!foundConfiguredProcessors.isEmpty() && processingMode != null && !processingMode.isBlank()) {
			// Search for configured processors with the expected processing mode

			List<ConfiguredProcessor> cplistFoundWithMode = new ArrayList<>();

			for (ConfiguredProcessor configuredProcessor : foundConfiguredProcessors) {
				String configProcessingMode = configuredProcessor.getConfiguration().getMode();
				if (null != configProcessingMode && configProcessingMode.equals(processingMode)) {
					cplistFoundWithMode.add(configuredProcessor);
					if (logger.isDebugEnabled())
						logger.debug("Candidate configured processor {} intended for mode {}", configuredProcessor.getIdentifier(),
								processingMode);
				}
			}

			// If no configured processors are found with the expected mode, select ones without a mode
			if (cplistFoundWithMode.isEmpty()) {
				for (ConfiguredProcessor configuredProcessor : foundConfiguredProcessors) {
					if (null == configuredProcessor.getConfiguration().getMode()) {
						cplistFoundWithMode.add(configuredProcessor);
						if (logger.isDebugEnabled())
							logger.debug("Candidate configured processor {} suitable for mode {}",
									configuredProcessor.getIdentifier(), processingMode);
					}
				}
			}
			foundConfiguredProcessors.clear();
			foundConfiguredProcessors.addAll(cplistFoundWithMode);
		}

		// Search for the newest processor
		Processor foundProcessor = null;
		if (!foundConfiguredProcessors.isEmpty()) {
			for (ConfiguredProcessor configuredProcessor : foundConfiguredProcessors) {
				if (null == foundProcessor || configuredProcessor.getProcessor()
					.getProcessorVersion()
					.compareTo(foundProcessor.getProcessorVersion()) > 0) {
					foundProcessor = configuredProcessor.getProcessor();
				}
			}
			if (logger.isDebugEnabled())
				logger.debug("Newest applicable processor version is {}", foundProcessor.getProcessorVersion());
		}

		// Search for the configured processor with the newest configuration
		ConfiguredProcessor newestConfiguredProcessor = null;
		for (ConfiguredProcessor configuredProcessor : foundConfiguredProcessors) {
			if (configuredProcessor.getProcessor().equals(foundProcessor)) {
				if (null == newestConfiguredProcessor || configuredProcessor.getConfiguration()
					.getConfigurationVersion()
					.compareTo(newestConfiguredProcessor.getConfiguration().getConfigurationVersion()) > 0) {
					newestConfiguredProcessor = configuredProcessor;
				}
			}
		}

		return newestConfiguredProcessor;
	}

}