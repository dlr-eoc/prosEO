/**
 * OrderDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
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
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.SimpleSelectionRule;
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
	private static Logger logger = LoggerFactory.getLogger(OrderDispatcher.class);

	@Autowired
	private ProductQueryService productQueryService;
		
	@Transactional
	public boolean publishOrder(ProcessingOrder order, ProcessingFacility pf) {
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

	public boolean checkForValidOrder(ProcessingOrder order) {
		boolean answer = true;
		// check for needed data
		if (order.getMission() == null) {
			answer = false;
			Messages.ORDER_MISSION_NOT_SET.log(logger, order.getIdentifier());
		}
		if (order.getRequestedConfiguredProcessors().isEmpty()) {
			answer = false;
			Messages.ORDER_REQ_PROC_NOT_SET.log(logger, order.getIdentifier());
		}
		if (order.getRequestedProductClasses().isEmpty()) {
			answer = false;
			Messages.ORDER_REQ_PROD_CLASS_NOT_SET.log(logger, order.getIdentifier());
		}
		return answer;
	}

	public boolean createJobsForOrbit(ProcessingOrder order, ProcessingFacility pf) {
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
	
	public boolean createJobsForDay(ProcessingOrder order, ProcessingFacility pf) {
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

	public boolean createJobsForTimeSlices(ProcessingOrder order, ProcessingFacility pf) {
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
					} else {
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
	
	@Transactional
	public boolean createJobForOrbitOrTime(ProcessingOrder order, Orbit orbit, Instant startT, Instant stopT, ProcessingFacility pf) {
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
					Set<ConfiguredProcessor> configuredProcessors = order.getRequestedConfiguredProcessors();
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
							List <JobStep> allJobSteps = new ArrayList<JobStep>();
							// for each product class
							for (ProductClass productClass : productClasses) {
								// create job step(s)
								JobStep jobStep = new JobStep();
								jobStep.setJobStepState(JobStepState.INITIAL);
								jobStep.setJob(job);

								// create output product
								// if product class has sub products or is sub product, create all related products to be created
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
								// now we have all product classes, create related products
								// also create job steps with queries related to product class
								// collect created products
								List <Product> productsToCreate = new ArrayList<Product>();

								Product rootProduct = createProducts(rootProductClass, 
										null, 
										configuredProcessor, 
										orbit, 
										job,
										jobStep, 
										order.getOutputFileClass(), 
										job.getStartTime(), 
										job.getStopTime(), 
										productsToCreate);
								// now we have to create the product queries for job step.

								List <Product> products = new ArrayList<Product>();
								for (Product p : productsToCreate) {
									// check if product exists
									// use configured processor, product class, sensing start and stop time, orbit (if set)
									if (RepositoryService.getProductRepository()
										   .findByProductClassAndConfiguredProcessorAndSensingStartTimeAndSensingStopTime(
												p.getProductClass().getId(),
												p.getConfiguredProcessor().getId(),
												p.getSensingStartTime(),
												p.getSensingStopTime()).isEmpty()) {
										products.add(p);
									}
								}
								if (!products.isEmpty()) {
									for (Product p : products) {
										for (SimpleSelectionRule selectionRule : p.getProductClass().getRequiredSelectionRules()) {
											ProductQuery pq = ProductQuery.fromSimpleSelectionRule(selectionRule, jobStep);
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
													products);
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
												int bla = 1;
											}
										}
									}
								}
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

	public void createJobStepForProduct(Job job, ProductClass productClass, Set<ConfiguredProcessor> configuredProcessors, List<JobStep> jobStepList, List<JobStep> allJobStepList, List<Product> allProducts) {


		JobStep jobStep = new JobStep();
		jobStep.setJobStepState(JobStepState.INITIAL);
		jobStep.setJob(job);
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
				jobStep = null;
			} else {
				jobStepList.add(jobStep);
				allJobStepList.add(jobStep);
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

				for (Product p : products) {
					for (SimpleSelectionRule selectionRule : p.getProductClass().getRequiredSelectionRules()) {
						ProductQuery pq = ProductQuery.fromSimpleSelectionRule(selectionRule, jobStep);
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
								allProducts);
						hasUnsatisfiedInputQueries = true;
					}
				}
				//			if (hasUnsatisfiedInputQueries) {
				//				jobStep.setJobStepState(JobStepState.WAITING_INPUT);
				//			}
			}
		}
	}

	public ProductClass getRootProductClass(ProductClass pc) {
		ProductClass rootProductClass = pc;
		while (rootProductClass.getEnclosingClass() != null) {
			rootProductClass = rootProductClass.getEnclosingClass();
		}		
		return rootProductClass;
	}
	
	public List<ProductClass> getAllComponentClasses(ProductClass pc) {
		List<ProductClass> productClasses = new ArrayList<ProductClass>();
		productClasses.addAll(pc.getComponentClasses());
		for (ProductClass subPC : pc.getComponentClasses()) {
			productClasses.addAll(getAllComponentClasses(subPC));
		}		
		return productClasses;
	}
	

	public Product createProducts(ProductClass productClass, Product enclosingProduct, ConfiguredProcessor cp, Orbit orbit, Job job, JobStep js, String fileClass, Instant startTime, Instant stopTime, List<Product> products) {
		Product product = createProduct(productClass, enclosingProduct, cp, orbit, job, js, fileClass, startTime, stopTime);
		products.add(product);
		for (ProductClass pc : productClass.getComponentClasses()) {
			Product p = createProducts(pc, product, cp, orbit, job, null, fileClass, startTime, stopTime, products);
			product.getComponentProducts().add(p);			
		}
		return product;
	}

	public Product createProduct(ProductClass productClass, Product enclosingProduct, ConfiguredProcessor cp, Orbit orbit, Job job, JobStep js, String fileClass, Instant startTime, Instant stopTime) {
		Product p = new Product();
		p.getParameters().clear();
		p.setUuid(UUID.randomUUID());
		p.getParameters().putAll(job.getProcessingOrder().getOutputParameters());
		p.setProductClass(productClass);
		p.setConfiguredProcessor(cp);
		p.setOrbit(orbit);
		p.setJobStep(js);
		if (js != null) {
			js.setOutputProduct(p);
		}
		p.setFileClass(fileClass);
		p.setSensingStartTime(startTime);
		p.setSensingStopTime(stopTime);
		p.setEnclosingProduct(enclosingProduct);
		
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
		ConfiguredProcessor cpFound = null;
		Processor pFound = null;
		
		if (productClass != null) {
			if (productClass.getProcessorClass() != null) {
				// search newest processor
				for (Processor p : productClass.getProcessorClass().getProcessors()) {
					if (!p.getConfiguredProcessors().isEmpty()) {
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
