package de.dlr.proseo.planner.dispatcher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.ProductQueryService;
import de.dlr.proseo.model.service.RepositoryService;

@Transactional
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
			case INITIAL: {
				// order is released, publish it
				if (!checkForValidOrder(order)) {
					break;
				}
				switch (order.getSlicingType()) {
				case CALENDAR_DAY:
					break;
				case ORBIT:
					answer = createJobsForOrbit(order, pf);
					break;
				case TIME_SLICE:
					break;
				default:
					logger.info("Order " + order.getId() + " slicing type not set");
					break;

				}
				break;
			}
			case RELEASED: {
				logger.info("Order " + order.getId() + " has state " + order.getOrderState().toString(), ", wait for release");
				break;
			}
			default: {
				logger.info("Order " + order.getId() + " has state " + order.getOrderState().toString(), ", nothing to publish");
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
			logger.info("Order " + order.getId() + ": mission not set");
		}
		if (order.getRequestedConfiguredProcessors().isEmpty()) {
			answer = false;
			logger.info("Order " + order.getId() + ": requested processor(s) not set");
		}
		if (order.getRequestedProductClasses().isEmpty()) {
			answer = false;
			logger.info("Order " + order.getId() + ": requested product class(es) not set");
		}
		return answer;
	}

	public boolean createJobsForOrbit(ProcessingOrder order, ProcessingFacility pf) {
		boolean answer = true;
		// there has to be a list of orbits
		List<Orbit> orbits = order.getRequestedOrbits();
		try {
			if (orbits.isEmpty()) {
				logger.info("Order " + order.getId() + ": requested orbit(s) not set");
				answer = false;
			} else {
				// create a job for each orbit
				// set order start time and stop time
				// we need			
				// product class
				Set<ProductClass> productClasses = order.getRequestedProductClasses();
				if (productClasses.isEmpty()) {
					logger.info("Order " + order.getId() + ": requested product class(es) not set");
					answer = false;
				} else {

					// configured processor
					Set<ConfiguredProcessor> configuredProcessors = order.getRequestedConfiguredProcessors();
					if (configuredProcessors.isEmpty()) {
						logger.info("Order " + order.getId() + ": requested configured processor(s) not set");
						answer = false;
					} else {

						// create jobs
						// for each product class
						for (ProductClass productClass : productClasses) {
							// for each orbit
							for (Orbit orbit : orbits) {
									// create job
									Job job = new Job();
									job.setOrbit(orbit);
									job.setJobState(JobState.INITIAL);
									Instant startT;
									Instant stopT;
									if (order.getStartTime() != null) {
										startT = order.getStartTime().isBefore(orbit.getStartTime())? orbit.getStartTime() : order.getStartTime();
									} else {
										startT = orbit.getStartTime();
									}
									job.setStartTime(startT);
									if (order.getStopTime() != null) {
										stopT = order.getStopTime().isAfter(orbit.getStopTime())? orbit.getStopTime() : order.getStopTime();
									} else {
										stopT = orbit.getStopTime();
									}
									job.setStopTime(stopT);
									job.setProcessingOrder(order);
									job.setProcessingFacility(pf);
									// create job step(s)
									JobStep jobStep = new JobStep();
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
									List <Product> products = new ArrayList<Product>();

									Product rootProduct = createProducts(rootProductClass, 
											null, 
											configuredProcessor, 
											orbit, 
											jobStep, 
											order.getOutputFileClass(), 
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

									// this means also to create new job steps for products which are not satisfied
									// check all queries for existing product definition (has not to be created!)
									List <JobStep> jobSteps = new ArrayList<JobStep>();
									jobSteps.add(jobStep);
									for (ProductQuery pq : jobStep.getInputProductQueries()) {
										if (!productQueryService.executeQuery(pq)) {
											// create job step to build product.
											// todo how to find configured processor?

											createJobStepForProduct(job,
													pq.getRequestedProductClass(),
													configuredProcessors,
													jobSteps,
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
												jobS.setOutputProduct(js.getOutputProduct());
											}
//											for (ProductQuery pq : js.getInputProductQueries()) {
//												// pq.setJobStep(jobS);
//												RepositoryService.getProductQueryRepository().save(pq);
//											}
										}
										for (Product p : products) {
											if (p.getEnclosingProduct() == null) {
												RepositoryService.getProductRepository().save(p);
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

	public void createJobStepForProduct(Job job, ProductClass productClass, Set<ConfiguredProcessor> configuredProcessors, List<JobStep> jobStepList, List<Product> allProducts) {


		JobStep jobStep = new JobStep();
		jobStep.setJob(job);
		jobStepList.add(jobStep);
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
		// now we have all product classes, create related products
		// also create job steps with queries related to product class
		// collect created products
		List <Product> products = new ArrayList<Product>();

		Product rootProduct = createProducts(rootProductClass, 
											 null, 
											 configuredProcessor, 
											 job.getOrbit(), 
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

		for (ProductQuery pq : jobStep.getInputProductQueries()) {
			if (!productQueryService.executeQuery(pq)) {
				// create job step to build product.
				// todo how to find configured processor?

				createJobStepForProduct(job,
										pq.getRequestedProductClass(),
										configuredProcessors,
										jobStepList,
										allProducts);
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
	

	public Product createProducts(ProductClass productClass, Product enclosingProduct, ConfiguredProcessor cp, Orbit orbit, JobStep js, String fileClass, Instant startTime, Instant stopTime, List<Product> products) {
		Product product = createProduct(productClass, enclosingProduct, cp, orbit, js, fileClass, startTime, stopTime);
		products.add(product);
		for (ProductClass pc : productClass.getComponentClasses()) {
			Product p = createProducts(pc, product, cp, orbit, null, fileClass, startTime, stopTime, products);
			product.getComponentProducts().add(p);			
		}
		return product;
	}

	public Product createProduct(ProductClass productClass, Product enclosingProduct, ConfiguredProcessor cp, Orbit orbit, JobStep js, String fileClass, Instant startTime, Instant stopTime) {
		Product p = new Product();
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
}
