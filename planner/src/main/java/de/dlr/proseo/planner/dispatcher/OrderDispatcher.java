/**
 * OrderDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.SimpleSelectionRule;
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
		if (order.getRequestedConfiguredProcessors().isEmpty()) {
			// TODO: This is not an error. If a configured processor is set, it overrides the lookup of the most recent 
			//       configured processor for that processor class, but it is fine to not set such an override
			answer = false;
			Messages.ORDER_REQ_PROC_NOT_SET.log(logger, order.getIdentifier());
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

					// configured processor
					Set<ConfiguredProcessor> configuredProcessors = order.getRequestedConfiguredProcessors();
					if (configuredProcessors.isEmpty()) {
						Messages.ORDER_REQ_CON_PROC_NOT_SET.log(logger, order.getIdentifier());
						answer = false;
					} else {
						// create jobs
						// for each orbit
						for (Orbit orbit : orbits) {
							// create job
							createJobForOrbitOrTime(order, orbit, null, null, pf);
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

					// configured processor
					Set<ConfiguredProcessor> configuredProcessors = order.getRequestedConfiguredProcessors();
					if (configuredProcessors.isEmpty()) {
						Messages.ORDER_REQ_CON_PROC_NOT_SET.log(logger, order.getIdentifier());
						answer = false;
					} else {
						// create jobs
						// for each orbit
						while (startT.isBefore(stopT)) {
							// create job
							createJobForOrbitOrTime(order, null, startT, sliceStopT, pf);
							startT = sliceStopT;
							sliceStopT = startT.plus(1, ChronoUnit.DAYS);
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

					// configured processor
					Set<ConfiguredProcessor> configuredProcessors = order.getRequestedConfiguredProcessors();
					if (configuredProcessors.isEmpty()) {
						Messages.ORDER_REQ_CON_PROC_NOT_SET.log(logger, order.getIdentifier());
						answer = false;
					} else if (startT.equals(stopT)) {
						createJobForOrbitOrTime(order, null, startT, stopT, pf);
					} else {
						if (Duration.ZERO.equals(order.getSliceDuration())) {
							Messages.ORDER_REQ_TIMESLICE_NOT_SET.log(logger, order.getIdentifier()); // TODO more specific message
							answer = false;
						}
						// create jobs
						// for each orbit
						while (startT.isBefore(stopT)) {
							// create job
							createJobForOrbitOrTime(order, null, startT, sliceStopT, pf);
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
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					Messages.ORDER_REQ_PROD_CLASS_NOT_SET.log(logger, order.getIdentifier());
					answer = false;
				} else {

					// configured processor
					Set<ConfiguredProcessor> allConfiguredProcessors = order.getRequestedConfiguredProcessors();
					List<ConfiguredProcessor> configuredProcessors = new ArrayList<ConfiguredProcessor>();
					for (ConfiguredProcessor aCP : allConfiguredProcessors) {
						configuredProcessors.add(aCP);
					}
					if (configuredProcessors.isEmpty()) {
						Messages.ORDER_REQ_CON_PROC_NOT_SET.log(logger, order.getIdentifier());
						answer = false;
					} else {

						// create job
							// create job
							Job job = new Job();
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
							List <JobStep> allJobSteps = new ArrayList<JobStep>();
							// for each product class
							for (ProductClass productClass : productClasses) {
								// create output product
								// if product class has sub products or is sub product, create all related products to be created
								// TODO: This is not quite correct: Processor classes may be anywhere in the product class tree,
								//       but not necessarily on the root product class, approach would be to find the topmost class
								//       in the product tree, for which a processor class is defined.
								//       (Note that it may happen that for this processor class no configured processor exists, so we
								//       actually cannot create a job step at all)
								List<ProductClass> productClassesToCreate = new ArrayList<ProductClass>();
								ProductClass rootProductClass = getRootProductClass(productClass);
								productClassesToCreate.add(rootProductClass);
								productClassesToCreate.addAll(getAllComponentClasses(rootProductClass));
								// look for configured processor
								ConfiguredProcessor configuredProcessor = null;
								for (ConfiguredProcessor cp : configuredProcessors) {
									if (rootProductClass.getProcessorClass() == cp.getProcessor().getProcessorClass()) {
										configuredProcessor = cp;
									}
								}
								// Do not create output product or job step, if no configured processor was found
								if (null == configuredProcessor) {
									Messages.ORDERDISP_NO_CONF_PROC.log(logger, productClass.getProductType());
									continue;
								}

								// create job step(s)
								JobStep jobStep = new JobStep();
								jobStep.setJobStepState(JobStepState.INITIAL);
								jobStep.setProcessingMode(order.getProcessingMode());
								jobStep.setJob(job);
								jobStep.getOutputParameters().putAll(order.getOutputParameters(productClass));
								jobStep = RepositoryService.getJobStepRepository().save(jobStep);
								job.getJobSteps().add(jobStep);


								// now we have all product classes, create related products
								// also create job steps with queries related to product class
								// collect created products
								List <Product> products = new ArrayList<Product>();

								createProducts(rootProductClass, 
										null, 
										configuredProcessor, 
										orbit, 
										job,
										jobStep, 
										order.getOutputFileClass(), 
										job.getStartTime(), 
										job.getStopTime(), 
										products);
								// now we have to create the product queries for job step.

								if (products.isEmpty()) {
									job.getJobSteps().remove(jobStep);
									RepositoryService.getJobStepRepository().delete(jobStep);
									jobStep = null;
								} else {
									for (Product p : products) {
										for (SimpleSelectionRule selectionRule : p.getProductClass().getRequiredSelectionRules()) {
											ProductQuery pq = ProductQuery.fromSimpleSelectionRule(selectionRule, jobStep);
											pq = RepositoryService.getProductQueryRepository().save(pq);
											if (!jobStep.getInputProductQueries().contains(pq)) {
												jobStep.getInputProductQueries().add(pq);
											}
										}
									}

									// this means also to create new job steps for products which are not satisfied
									// check all queries for existing product definition (has not to be created!)
									List <JobStep> jobSteps = new ArrayList<JobStep>();
									jobSteps.add(jobStep);
									allJobSteps.add(jobStep);
									for (ProductQuery pq : jobStep.getInputProductQueries()) {
										if (productQueryService.executeQuery(pq, false, true)) {
											// jobStep.getOutputProduct().getSatisfiedProductQueries().add(pq);							
										} else {
											// otherwise create job step to build product.
											// todo how to find configured processor?
											createJobStepForProduct(job,
													pq.getRequestedProductClass(),
													configuredProcessors,
													jobSteps,
													allJobSteps,
													products,
													order.getInputProductClasses());
										} 
									}
									
									// save all created things
									if (answer) {
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
							}
							if (job.getJobSteps().isEmpty()) {
								order.getJobs().remove(job);							
								RepositoryService.getJobRepository().delete(job);	
								job = null;
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
	 * Create the job step(s) to produce product(s) of a product class. 
	 * This function creates all job steps for not existing (intermediate) products except the the products of classes contained in inputProducts. 
	 * 
	 * @param job The job for the job steps
	 * @param productClass The "target" product class
	 * @param configuredProcessors Configured processors used to create, if none is found in the list, the newest corresponding processor is used
	 * @param jobStepList The job steps created
	 * @param allJobStepList All job steps 
	 * @param allProducts All products created
	 * @param inputProducts The input products to use
	 */
	public void createJobStepForProduct(Job job, ProductClass productClass, List<ConfiguredProcessor> configuredProcessors, 
				List<JobStep> jobStepList, List<JobStep> allJobStepList, List<Product> allProducts, Set<ProductClass> inputProducts) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobStepForProduct({}, {}, [...], [...], [...], [...], [...])",
				(null == job ? "null": job.getId()), (null == productClass ? "null" : productClass.getProductType()));
		
		if (inputProducts.contains(productClass)) {
			return;
		}
		JobStep jobStep = new JobStep();
		jobStep.setJobStepState(JobStepState.INITIAL);
		jobStep.setJob(job);
		jobStep = RepositoryService.getJobStepRepository().save(jobStep);
		job.getJobSteps().add(jobStep);
		jobStep.getOutputParameters().putAll(job.getProcessingOrder().getOutputParameters(productClass));
		// create output product
		// if product class has sub products or is sub product, create all related products to be created
		List<ProductClass> productClassesToCreate = new ArrayList<ProductClass>();
		ProductClass rootProductClass = getRootProductClass(productClass);
		productClassesToCreate.add(rootProductClass);
		productClassesToCreate.addAll(getAllComponentClasses(rootProductClass));
		
		ConfiguredProcessor configuredProcessor = null;
		for (ConfiguredProcessor cp : configuredProcessors) {
			if (rootProductClass.getProcessorClass() == cp.getProcessor().getProcessorClass()) {
				configuredProcessor = cp;
			}
		}
		if (configuredProcessor == null) {
			// configured processor not provided by order, search for newest processor with corresponding newest configuration
			configuredProcessor = searchConfiguredProcessorForProductClass(rootProductClass);
			
		}
		if (configuredProcessor == null ) {
			Messages.ORDERDISP_NO_CONF_PROC.log(logger, rootProductClass.getProductType());
			job.getJobSteps().remove(jobStep);
			RepositoryService.getJobStepRepository().delete(jobStep);
			jobStep = null;
		} else {
			Boolean found = false;
			for (JobStep i : allJobStepList) {
				if (i.getOutputProduct().getProductClass().equals(rootProductClass)) {
					found = true;
					break;
				}
			}
			if (found) {
				job.getJobSteps().remove(jobStep);
				RepositoryService.getJobStepRepository().delete(jobStep);
				jobStep = null;
			} else {
				// now we have all product classes, create related products
				// also create job steps with queries related to product class
				// collect created products
				List <Product> products = new ArrayList<Product>();

				Product rootProduct = createProducts(rootProductClass, 
						null, 
						configuredProcessor, 
						job.getOrbit(), 
						job,
						jobStep, 
						job.getProcessingOrder().getOutputFileClass(), 
						job.getStartTime(), 
						job.getStopTime(), 
						products);
				// now we have to create the product queries for job step.
				if (jobStep.getOutputProduct() == null) {
					job.getJobSteps().remove(jobStep);
					RepositoryService.getJobStepRepository().delete(jobStep);
					jobStep = null;
				} else {
					jobStepList.add(jobStep);
					allJobStepList.add(jobStep);
					for (Product p : products) {
						for (SimpleSelectionRule selectionRule : p.getProductClass().getRequiredSelectionRules()) {
							ProductQuery pq = ProductQuery.fromSimpleSelectionRule(selectionRule, jobStep);
							pq = RepositoryService.getProductQueryRepository().save(pq);
							if (!jobStep.getInputProductQueries().contains(pq)) {
								jobStep.getInputProductQueries().add(pq);
							}
						}
					}
					allProducts.addAll(products);
					// this means also to create new job steps for products which are not satisfied
					// check all queries for existing product definition (has not to be created!)

					Boolean hasUnsatisfiedInputQueries = false;
					for (ProductQuery pq : jobStep.getInputProductQueries()) {
						if (productQueryService.executeQuery(pq, false, true)) {
							// jobStep.getOutputProduct().getSatisfiedProductQueries().add(pq);						
						} else {
							// create job step to build product.
							// todo how to find configured processor?

							createJobStepForProduct(job,
									pq.getRequestedProductClass(),
									configuredProcessors,
									jobStepList,
									allJobStepList,
									allProducts,
									inputProducts);
							hasUnsatisfiedInputQueries = true;
						}
					}
				}
			}
		}
	}

	public ProductClass getRootProductClass(ProductClass pc) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRootProductClass({})", (null == pc ? "null" : pc.getProductType()));
		
		ProductClass rootProductClass = pc;
		while (rootProductClass.getEnclosingClass() != null) {
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
	 * @param products List to collect all products created
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
	public ConfiguredProcessor searchConfiguredProcessorForProductClass(ProductClass productClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> searchConfiguredProcessorForProductClass({})", (null == productClass ? "null" : productClass.getProductType()));
		
		ConfiguredProcessor cpFound = null;
		Processor pFound = null;
		
		if (productClass != null) {
			if (productClass.getProcessorClass() != null) {
				// search newest processor
				for (Processor p : productClass.getProcessorClass().getProcessors()) {
					List <ConfiguredProcessor> cplist = new ArrayList<ConfiguredProcessor>();
					for (ConfiguredProcessor cp : p.getConfiguredProcessors()) {
						cplist.add(cp);
					}
					if (!cplist.isEmpty()) {
						if (pFound == null) {
							pFound = p;
						} else {
							if (p.getProcessorVersion().compareTo(pFound.getProcessorVersion()) > 0) {
								pFound = p;
							}
						}
					}
				}
				// search configured processor with newest configuration
				for (ConfiguredProcessor cp : pFound.getConfiguredProcessors()) {
					if (cpFound == null) {
						cpFound = cp;
					} else {
						if (cp.getConfiguration().getConfigurationVersion().compareTo(cpFound.getConfiguration().getConfigurationVersion()) > 0) {
							cpFound = cp;
						}
					}
				}
			}
		}
		
		return cpFound;
	}
}
