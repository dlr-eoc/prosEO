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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
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
import de.dlr.proseo.planner.Messages;

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
	private static Logger logger = LoggerFactory.getLogger(OrderDispatcher.class);

	@Autowired
	private ProductQueryService productQueryService;
		
	/**
	 * Publish an order, create jobs and job steps needed to create all products
	 * 
	 * @param order The processing order
	 * @param pf The processing facility 
	 * @return true after success, else false
	 */
	@Transactional
	public boolean publishOrder(ProcessingOrder order, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> publishOrder({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		boolean answer = false;
		if (order != null) {
			switch (order.getOrderState()) {
			case APPROVED: {
				// order is released, publish it
				if (!checkForValidOrder(order)) {
					break;
				}
				switch (order.getSlicingType()) {
				case CALENDAR_DAY:
					answer = createJobsForDay(order, pf);
					break;
				case CALENDAR_MONTH:
					answer = createJobsForMonth(order, pf);
					break;
				case CALENDAR_YEAR:
					answer = createJobsForYear(order, pf);
					break;
				case ORBIT:
					answer = createJobsForOrbit(order, pf);
					break;
				case TIME_SLICE:
					answer = createJobsForTimeSlices(order, pf);
					break;
				default:
					Messages.ORDER_SLICING_TYPE_NOT_SET.log(logger, order.getIdentifier());
					break;

				}
				if (order.getJobs().isEmpty()) {
					order.setOrderState(OrderState.COMPLETED);
				}
				break;
			}
			case RELEASED: {
				Messages.ORDER_WAIT_FOR_RELEASE.log(logger, order.getIdentifier(), order.getOrderState().toString());
				break;
			}
			default: {
				Messages.ORDER_WAIT_FOR_RELEASE.log(logger, order.getIdentifier(), order.getOrderState().toString());
				break;
			}
				
			}
		}		
		if (!answer) {
			// TODO Improve handling by returning a proper error message to the caller
			throw new RuntimeException("publishOrder rollback");
		}
		return answer;
	}

	/**
	 * Small test of order
	 * 
	 * @param order
	 * @return
	 */
	public boolean checkForValidOrder(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkForValidOrder({}, {})", (null == order ? "null": order.getIdentifier()));
		
		boolean answer = true;
		// check for needed data
		if (order.getMission() == null) {
			answer = false;
			Messages.ORDER_MISSION_NOT_SET.log(logger, order.getIdentifier());
		}
		if (order.getRequestedProductClasses().isEmpty()) {
			answer = false;
			Messages.ORDER_REQ_PROD_CLASS_NOT_SET.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Create the needed job for an order of type orbit
	 * 
	 * @param order The processing order
	 * @param pf The processing facility 
	 * @return true after success, else false
	 */
	public boolean createJobsForOrbit(ProcessingOrder order, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForOrbit({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		boolean answer = true;
		// there has to be a list of orbits
		List<Orbit> orbits = order.getRequestedOrbits();
		try {
			if (orbits.isEmpty()) {
				Messages.ORDER_REQ_ORBIT_NOT_SET.log(logger, order.getIdentifier());
				answer = false;
			} else {
				// create a job for each orbit
				// set order start time and stop time
				// we need			
				// product class
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					Messages.ORDER_REQ_PROD_CLASS_NOT_SET.log(logger, order.getIdentifier());
					answer = false;
				} else {
					// create jobs
					// for each orbit
					for (Orbit orbit : orbits) {
						// create job
						answer = createJobForOrbitOrTime(order, orbit, null, null, pf);
						if (!answer) break;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = false;
		}

		return answer;
	}

	/**
	 * Create the needed job for an order of type day
	 * 
	 * @param order The processing order
	 * @param pf The processing facility 
	 * @return true after success, else false
	 */
	public boolean createJobsForDay(ProcessingOrder order, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForDay({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		boolean answer = true;

		Instant startT = null;
		Instant stopT = null;
		Instant sliceStopT = null;
		try {
			// get first day
			
			if (order.getStartTime() == null || order.getStopTime() == null) {
				Messages.ORDER_REQ_DAY_NOT_SET.log(logger, order.getIdentifier());
				answer = false;
			} else {
				startT = order.getStartTime().truncatedTo(ChronoUnit.DAYS);
				stopT = order.getStopTime();
				sliceStopT = startT.plus(1, ChronoUnit.DAYS);
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					Messages.ORDER_REQ_PROD_CLASS_NOT_SET.log(logger, order.getIdentifier());
					answer = false;
				} else {
					// create jobs for each day
					while (startT.isBefore(stopT)) {
						// create job (without orbit association)
						answer = createJobForOrbitOrTime(order, null, startT, sliceStopT, pf);
						if (!answer) break;
						startT = sliceStopT;
						sliceStopT = startT.plus(1, ChronoUnit.DAYS);
					} 
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = false;
		}

		return answer;
	}

	/**
	 * Create the needed job for an order of type month
	 * 
	 * @param order The processing order
	 * @param pf The processing facility 
	 * @return true after success, else false
	 */
	public boolean createJobsForMonth(ProcessingOrder order, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForMonth({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		boolean answer = true;

		Instant startT = null;
		Instant stopT = null;
		Instant sliceStopT = null;
		try {
			// get first day
			
			if (order.getStartTime() == null || order.getStopTime() == null) {
				Messages.ORDER_REQ_DAY_NOT_SET.log(logger, order.getIdentifier());
				answer = false;
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
					Messages.ORDER_REQ_PROD_CLASS_NOT_SET.log(logger, order.getIdentifier());
					answer = false;
				} else {
					// create jobs for each day
					startT = calStart.toInstant();
					while (startT.isBefore(stopT)) {
						// create job (without orbit association)
						sliceStopT = calStop.toInstant();
						answer = createJobForOrbitOrTime(order, null, startT, sliceStopT, pf);
						if (!answer) break;
						calStart = (Calendar) calStop.clone();
						calStop.add(Calendar.MONTH, 1);
						startT = calStart.toInstant();
					} 
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = false;
		}

		return answer;
	}

	/**
	 * Create the needed job for an order of type year
	 * 
	 * @param order The processing order
	 * @param pf The processing facility 
	 * @return true after success, else false
	 */
	public boolean createJobsForYear(ProcessingOrder order, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForYear({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		boolean answer = true;

		Instant startT = null;
		Instant stopT = null;
		Instant sliceStopT = null;
		try {
			// get first day
			
			if (order.getStartTime() == null || order.getStopTime() == null) {
				Messages.ORDER_REQ_DAY_NOT_SET.log(logger, order.getIdentifier());
				answer = false;
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
					Messages.ORDER_REQ_PROD_CLASS_NOT_SET.log(logger, order.getIdentifier());
					answer = false;
				} else {
					// create jobs for each day
					startT = calStart.toInstant();
					while (startT.isBefore(stopT)) {
						// create job (without orbit association)
						sliceStopT = calStop.toInstant();
						answer = createJobForOrbitOrTime(order, null, startT, sliceStopT, pf);
						if (!answer) break;
						calStart = (Calendar) calStop.clone();
						calStop.add(Calendar.YEAR, 1);
						startT = calStart.toInstant();
					} 
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = false;
		}

		return answer;
	}

	/**
	 * Create the needed job for an order of type time slice
	 * 
	 * @param order The processing order
	 * @param pf The processing facility 
	 * @return true after success, else false
	 */
	public boolean createJobsForTimeSlices(ProcessingOrder order, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsForTimeSlices({}, {})", (null == order ? "null": order.getIdentifier()), (null == pf ? "null" : pf.getName()));
		
		boolean answer = true;

		Instant startT = null;
		Instant stopT = null;
		Instant sliceStopT = null;
		try {
			// get first day
			
			if (order.getStartTime() == null || order.getStopTime() == null || order.getSliceDuration() == null) {
				Messages.ORDER_REQ_TIMESLICE_NOT_SET.log(logger, order.getIdentifier());
				answer = false;
			} else {
				startT = order.getStartTime();
				stopT = order.getStopTime();
				sliceStopT = startT.plus(order.getSliceDuration());
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					Messages.ORDER_REQ_PROD_CLASS_NOT_SET.log(logger, order.getIdentifier());
					answer = false;
				} else {

					if (startT.equals(stopT)) {
						answer = createJobForOrbitOrTime(order, null, startT, stopT, pf);
					} else {
						if (Duration.ZERO.equals(order.getSliceDuration())) {
							Messages.ORDER_REQ_TIMESLICE_NOT_SET.log(logger, order.getIdentifier()); // TODO more specific message
							answer = false;
						}
						// create jobs for each time slice
						while (startT.isBefore(stopT)) {
							// check orbit
							Orbit orbit = findOrbit(order, startT, sliceStopT);
							// create job
							answer = createJobForOrbitOrTime(order, orbit, startT, sliceStopT, pf);
							startT = sliceStopT;
							sliceStopT = startT.plus(order.getSliceDuration());
						} 
					}
				} 
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = false;
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
	 * @param pf The facilty to run the job
	 * @return true after success, else false
	 */
	@Transactional
	public boolean createJobForOrbitOrTime(ProcessingOrder order, Orbit orbit, Instant startT, Instant stopT, ProcessingFacility pf) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobForOrbitOrTime({}, {}, {}, {}, {})",
				(null == order ? "null": order.getIdentifier()), (null == orbit ? "null" : orbit.getOrbitNumber()), startT, stopT,
				(null == pf ? "null" : pf.getName()));

		boolean answer = true;
		// there has to be a list of orbits

		try {
			if (orbit == null && (startT == null || stopT == null)) {
				Messages.ORDER_REQ_ORBIT_OR_TIME_NOT_SET.log(logger, order.getIdentifier());
				answer = false;
			} else {
				// create a job for each orbit
				// set order start time and stop time
				// we need			
				// product class
				Set<ProductClass> requestedProductClasses = order.getRequestedProductClasses();
				if (requestedProductClasses.isEmpty()) {
					Messages.ORDER_REQ_PROD_CLASS_NOT_SET.log(logger, order.getIdentifier());
					answer = false;
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
					job = RepositoryService.getJobRepository().save(job);
					order.getJobs().add(job);
					List<JobStep> allJobSteps = new ArrayList<JobStep>();
					List<Product> allProducts = new ArrayList<Product>();
					if (logger.isTraceEnabled()) logger.trace("Job with time {} - {}", startT, stopT);
					// look for all products to create

					for (ProductClass productClass : requestedProductClasses) {
						createProductsAndJobStep(productClass, job, order, allJobSteps, allProducts);
					}
				}
			}
		} catch (IllegalArgumentException | NoSuchElementException ex) {
			answer = false;
		} catch (Exception ex) {
			ex.printStackTrace();
			answer = false;
		}

		return answer;
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
			List<Orbit> orbits = RepositoryService.getOrbitRepository()
					.findBySpacecraftCodeAndTimeIntersect(spacecrafts.iterator().next().getCode(), startT, stopT);
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
		if (logger.isTraceEnabled()) logger.trace(">>> findOrCreateSelectionRule({}, {})",
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
		for (SimpleSelectionRule selectionRule : selectedSelectionRules) {
			// Check whether job step already has a product query for the given product class
			Boolean exist = false;
			for (ProductQuery productQuery: jobStep.getInputProductQueries()) {
				if (productQuery.getGeneratingRule().getSourceProductClass().equals(selectionRule.getSourceProductClass())) {
					// Make sure selection rules match
					if (!selectionRule.toString().equals(productQuery.getGeneratingRule().toString())) {
						throw new IllegalArgumentException(
							String.format("Selection rule %s from product query does not match selection rule %s from product class %s",
									productQuery.getGeneratingRule().toString(), selectionRule.toString(), productClass.getProductType()));
					}
					// OK, product query already exists
					exist = true;
				}
			}
			if (!exist) {
				ProductQuery pq = ProductQuery.fromSimpleSelectionRule(selectionRule, jobStep);
				pq = RepositoryService.getProductQueryRepository().save(pq);
				jobStep.getInputProductQueries().add(pq);
				if (logger.isDebugEnabled()) logger.debug("Product query generated for rule '{}'", selectionRule);
			}
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
			List<Product> allProducts) throws NoSuchElementException {
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
			return;
		}
		
		// Only one job step for one product
		for (JobStep i : allJobSteps) {
			if (i.getOutputProduct().getProductClass().equals(topProductClass)) {
				if (logger.isDebugEnabled()) logger.debug("Skipping product class {} because a job step for it has already been generated",
						productClass.getProductType());
				return;
			}
		}
		
		// Find configured processor to use
		ConfiguredProcessor configuredProcessor = searchConfiguredProcessorForProductClass(topProductClass, order.getRequestedConfiguredProcessors(), order.getProcessingMode());
		if (null == configuredProcessor) {
			// We cannot create the product, as there is no configured processor for it - now it depends ...
			if (order.getRequestedProductClasses().contains(productClass) || order.getRequestedProductClasses().contains(topProductClass)) {
				// This was one of the requested product classes, therefore fail
				Messages.ORDERDISP_NO_CONF_PROC.log(logger, topProductClass.getProductType());
				throw new NoSuchElementException(Messages.ORDERDISP_NO_CONF_PROC.format(topProductClass.getProductType()));
			} else {
				// This was a preceding (possibly auxiliary) input product class, so just wait for the product to appear
				if (logger.isDebugEnabled()) logger.debug("Skipping product class {} because no configured processor can be found",
						productClass.getProductType());
				return;
			}
		}
		if (logger.isDebugEnabled()) logger.debug("Using configured processor {}", configuredProcessor.getIdentifier());

		// we have a configured processor!
		// now create products and job steps to generate output files 
		// create job step(s)
		JobStep jobStep = new JobStep();
		jobStep.setJobStepState(JobStepState.INITIAL);
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

		if (products.isEmpty()) {
			if (logger.isDebugEnabled()) logger.debug("No products could be generated, skipping job step generation");
			job.getJobSteps().remove(jobStep);
			RepositoryService.getJobStepRepository().delete(jobStep);
			jobStep = null;
		} else {
			for (Product p : products) {
				try {
					findOrCreateProductQuery(jobStep, p.getProductClass());
				} catch (IllegalArgumentException e) {
					logger.error(e.getMessage());
					job.getJobSteps().remove(jobStep);
					RepositoryService.getJobStepRepository().delete(jobStep);
					jobStep = null;
					throw e;
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
							allProducts);
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
	}
	
	public ProductClass getTopProductClassWithPC(ProductClass pc) {
		if (logger.isTraceEnabled()) logger.trace(">>> getTopProductClassWithPC({})", (null == pc ? "null" : pc.getProductType()));
		
		ProductClass rootProductClass = pc;
		while (rootProductClass.getProcessorClass() == null && rootProductClass.getEnclosingClass() != null) {
			rootProductClass = rootProductClass.getEnclosingClass();
		}		
		return rootProductClass;
	}
	
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
			products.add(product);
		}
		for (ProductClass pc : productClass.getComponentClasses()) {
			Product p = createProducts(pc, product, cp, orbit, job, null, fileClass, startTime, stopTime, products);
			if (p != null) {
				product.getComponentProducts().add(p);
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
		p.getParameters().clear();
		p.setUuid(UUID.randomUUID());
		p.getParameters().putAll(job.getProcessingOrder().getOutputParameters(productClass));
		p.setProductClass(productClass);
		p.setConfiguredProcessor(cp);
		p.setOrbit(orbit);
		p.setJobStep(js);
		p.setFileClass(fileClass);
		p.setSensingStartTime(startTime);
		p.setSensingStopTime(stopTime);
		p.setProductionType(job.getProcessingOrder().getProductionType());
		if (null != js) {
			p.setMode(js.getProcessingMode());
		}
		p.setEnclosingProduct(enclosingProduct);		
		// check if product exists
		// use configured processor, product class, sensing start and stop time
		for (Product foundProduct : RepositoryService.getProductRepository()
				.findByProductClassAndConfiguredProcessorAndSensingStartTimeAndSensingStopTime(
						productClass.getId(),
						cp.getId(),
						startTime,
						stopTime)) {
			if (foundProduct.equals(p)) {
				if (!foundProduct.getProductFile().isEmpty()) {
					for (ProductFile foundFile : foundProduct.getProductFile()) {
						if (foundFile.getProcessingFacility().equals(job.getProcessingFacility())) {
							// it exists, nothing to do
							return null;
						}
					}
				}
			}
		}

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
			if (logger.isDebugEnabled()) logger.warn("searchConfiguredProcessorForProductClass called without product class");
			return null;
		}
		
		List <ConfiguredProcessor> cplistFound = new ArrayList<ConfiguredProcessor>();
		
		if (productClass.getProcessorClass() != null) {
			// build list of all configured processors
			List <ConfiguredProcessor> cplist = new ArrayList<ConfiguredProcessor>();
			for (Processor p : productClass.getProcessorClass().getProcessors()) {
				for (ConfiguredProcessor cp : p.getConfiguredProcessors()) {
					cplist.add(cp);
					if (logger.isDebugEnabled()) logger.debug("Candidate configured processor {} found", cp.getIdentifier());
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
