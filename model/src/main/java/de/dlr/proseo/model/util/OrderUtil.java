package de.dlr.proseo.model.util;

import java.time.DateTimeException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.rest.model.RestClassOutputParameter;
import de.dlr.proseo.model.rest.model.RestInputFilter;
import de.dlr.proseo.model.rest.model.RestOrbitQuery;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.ProductClass;

public class OrderUtil {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderUtil.class);
	
	/**
	 * Convert a prosEO model ProcessingOrder into a REST Order
	 * 
	 * @param processingOrder the prosEO model Order
	 * @return an equivalent REST Order or null, if no model Order was given
	 */

	public static RestOrder toRestOrder(ProcessingOrder processingOrder) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestOrder({})", (null == processingOrder ? "MISSING" : processingOrder.getId()));
		
		if (null == processingOrder)
			return null;
		
		RestOrder restOrder = new RestOrder();
		
		restOrder.setId(processingOrder.getId());
		restOrder.setVersion(Long.valueOf(processingOrder.getVersion()));
		
		if (null != processingOrder.getMission().getCode()) {
			restOrder.setMissionCode(processingOrder.getMission().getCode());

		}	
		if (null != processingOrder.getIdentifier()) {
			restOrder.setIdentifier(processingOrder.getIdentifier());
		}	
		if (null != processingOrder.getUuid()) {
			restOrder.setUuid(processingOrder.getUuid().toString());
		}	
		if (null != processingOrder.getOrderState()) {
			restOrder.setOrderState(processingOrder.getOrderState().toString());
		}
		if (null != processingOrder.getStartTime()) {
			restOrder.setStartTime(Date.from(processingOrder.getStartTime()));
		}
		if (null != processingOrder.getStopTime()) {
			restOrder.setStopTime(Date.from(processingOrder.getStopTime()));
		}
		if (null != processingOrder.getExecutionTime()) {
			restOrder.setExecutionTime(Date.from(processingOrder.getExecutionTime()));
		}
		if(null != processingOrder.getSlicingType()) {
			restOrder.setSlicingType(processingOrder.getSlicingType().name());
		}
		if(null != processingOrder.getSliceDuration()) {
			restOrder.setSliceDuration(processingOrder.getSliceDuration().getSeconds());
		}
		restOrder.setSliceOverlap(processingOrder.getSliceOverlap().getSeconds());

		if (null != processingOrder.getInputFilters()) {
			for (ProductClass sourceClass: processingOrder.getInputFilters().keySet()) {
				RestInputFilter restInputFilter = new RestInputFilter();
				restInputFilter.setProductClass(sourceClass.getProductType());
				Map<String, Parameter> filterConditions = processingOrder.getInputFilters().get(sourceClass).getFilterConditions();
				for (String paramKey : filterConditions.keySet()) {
					restInputFilter.getFilterConditions()
							.add(new RestParameter(paramKey,
									filterConditions.get(paramKey).getParameterType().toString(),
									filterConditions.get(paramKey).getParameterValue()));
				}
				restOrder.getInputFilters().add(restInputFilter);
			}
			
		}
		
		if(null != processingOrder.getClassOutputParameters()) {
			for (ProductClass targetClass: processingOrder.getClassOutputParameters().keySet()) {
				RestClassOutputParameter restClassOutputParameter = new RestClassOutputParameter();
				restClassOutputParameter.setProductClass(targetClass.getProductType());
				Map<String, Parameter> outputParameters = processingOrder.getClassOutputParameters().get(targetClass).getOutputParameters();
				for (String paramKey : outputParameters.keySet()) {
					restClassOutputParameter.getOutputParameters()
							.add(new RestParameter(paramKey,
									outputParameters.get(paramKey).getParameterType().toString(),
									outputParameters.get(paramKey).getParameterValue()));
				}
				restOrder.getClassOutputParameters().add(restClassOutputParameter);
			}
		}
		
		if(null != processingOrder.getOutputParameters()) {
			
			for (String paramKey: processingOrder.getOutputParameters().keySet()) {
				restOrder.getOutputParameters().add(
					new RestParameter(paramKey,
							processingOrder.getOutputParameters().get(paramKey).getParameterType().toString(),
							processingOrder.getOutputParameters().get(paramKey).getParameterValue()));
			}
		}
		if (null != processingOrder.getRequestedProductClasses()) {
			
			for (ProductClass productClass : processingOrder.getRequestedProductClasses()) {
				restOrder.getRequestedProductClasses().add(productClass.getProductType());
			}
			
		}
		if (null != processingOrder.getInputProductClasses()) {
			for (ProductClass productClass : processingOrder.getInputProductClasses()) {
				restOrder.getInputProductClasses().add(productClass.getProductType());
			}
			
		}
		
		if(null != processingOrder.getOutputFileClass()) {
			restOrder.setOutputFileClass(processingOrder.getOutputFileClass());
		}
		
		if(null != processingOrder.getProcessingMode()) {
			restOrder.setProcessingMode(processingOrder.getProcessingMode());
		}
		
		if (null != processingOrder.getProductionType()) {
			restOrder.setProductionType(processingOrder.getProductionType().toString());
		}
		if (null != processingOrder.hasFailedJobSteps()) {
			restOrder.setHasFailedJobSteps(processingOrder.hasFailedJobSteps());
		}
		
		if (null != processingOrder.getRequestedConfiguredProcessors()) {
			for (ConfiguredProcessor toAddProcessor: processingOrder.getRequestedConfiguredProcessors()) {
				restOrder.getConfiguredProcessors().add(toAddProcessor.getIdentifier());
			}
		}	
		
		// Create orbit ranges from orbits with contiguous orbit numbers
		if (null != processingOrder.getRequestedOrbits()) {
			
			List<RestOrbitQuery> orbitQueries = new ArrayList<RestOrbitQuery>();
			
			// Sort orbits by orbit number
			List<Orbit> sortedOrbits = new ArrayList<>();
			sortedOrbits.addAll(processingOrder.getRequestedOrbits());
			Collections.sort(sortedOrbits, (o1, o2) -> {
				return o1.getOrbitNumber().compareTo(o2.getOrbitNumber());
			});
			
			// Loop over all orbits, and create one RestOrbitQuery for each continuous range of orbit numbers
			Integer lastOrbitNumber = Integer.MIN_VALUE;
			RestOrbitQuery currentOrbitQuery = null;
			for (Orbit orbit: sortedOrbits) {
				if (orbit.getOrbitNumber() > lastOrbitNumber + 1) {
					currentOrbitQuery = new RestOrbitQuery();
					currentOrbitQuery.setSpacecraftCode(orbit.getSpacecraft().getCode());
					currentOrbitQuery.setOrbitNumberFrom(Long.valueOf(orbit.getOrbitNumber()));
					orbitQueries.add(currentOrbitQuery);
				}
				currentOrbitQuery.setOrbitNumberTo(Long.valueOf(orbit.getOrbitNumber()));
				lastOrbitNumber = orbit.getOrbitNumber();
			}

			restOrder.setOrbits(orbitQueries);
		}
		// Get the list of job step states
		// this is much faster than iterating over jobs and job steps
		List<String> jss = RepositoryService.getJobStepRepository().findDistinctJobStepStatesByOrderId(processingOrder.getId());
		if (jss == null) {
			jss = new ArrayList<String>();
		}
		restOrder.setJobStepStates(jss);
		
		// Calculate the percentage of finished job steps

		Long percentCompleted = (long) 0;
		Long percentFailed = (long) 0;
		Long percentRunning = (long) 0;
		Long jsCount = (long) RepositoryService.getJobStepRepository().countJobStepByOrderId(processingOrder.getId());
		if (jsCount != null && jsCount > 0) {
			Long jsCountCompleted = (long) RepositoryService.getJobStepRepository().countJobStepCompletedByOrderId(processingOrder.getId());
			percentCompleted = (jsCountCompleted * 100 / jsCount);
		}
		restOrder.setPercentCompleted(percentCompleted);
		if (jsCount != null && jsCount > 0) {
			Long jsCountFailed = (long) RepositoryService.getJobStepRepository().countJobStepFailedByOrderId(processingOrder.getId());
			percentFailed = (jsCountFailed * 100 / jsCount);
		}
		restOrder.setPercentFailed(percentFailed);
		if (jsCount != null && jsCount > 0) {
			Long jsCountRunning = (long) RepositoryService.getJobStepRepository().countJobStepRunningByOrderId(processingOrder.getId());
			percentRunning = (jsCountRunning * 100 / jsCount);
		}
		restOrder.setPercentRunning(percentRunning);
		return restOrder;
	}

	/**
	 * Convert a REST order into a prosEO model processingorder (scalar and embedded attributes only, no orbit references)
	 * 
	 * @param restOrder the REST order
	 * @return a (roughly) equivalent model order
	 * @throws IllegalArgumentException if the REST order violates syntax rules for date, enum or numeric values
	 */
	public static ProcessingOrder toModelOrder(RestOrder restOrder) throws IllegalArgumentException {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toModelOrder({})", (null == restOrder ? "MISSING" : restOrder.getId()));
		
		ProcessingOrder processingOrder = new ProcessingOrder();
		
		if (null != restOrder.getId() && 0 != restOrder.getId()) {
			processingOrder.setId(restOrder.getId());
			while (processingOrder.getVersion() < restOrder.getVersion()) {
				processingOrder.incrementVersion();
			} 
		}
		processingOrder.setIdentifier(restOrder.getIdentifier());
		if (null != restOrder.getUuid()) {
			processingOrder.setUuid(UUID.fromString(restOrder.getUuid()));
		}
		processingOrder.setOrderState(OrderState.valueOf(restOrder.getOrderState()));
		processingOrder.setProcessingMode(restOrder.getProcessingMode());

		if (null != restOrder.getStartTime()) {
			try {
				processingOrder.setStartTime(restOrder.getStartTime().toInstant());

			} catch (DateTimeException e) {
				throw new IllegalArgumentException(String.format("Invalid sensing start time '%s'", restOrder.getStartTime()));
			} 
		}
		if (null != restOrder.getStopTime()) {
			try {
				processingOrder.setStopTime(restOrder.getStopTime().toInstant());
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(String.format("Invalid sensing stop time '%s'", restOrder.getStartTime()));
			} 
		}
		if (null != restOrder.getExecutionTime()) {
			try {
				processingOrder.setExecutionTime(restOrder.getExecutionTime().toInstant());
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(String.format("Invalid sensing stop time '%s'", restOrder.getExecutionTime()));
			} 
		}
		if (null != restOrder.getOutputFileClass()) {
			processingOrder.setOutputFileClass(restOrder.getOutputFileClass());
		}
		if (null != restOrder.getSlicingType()) {
			processingOrder.setSlicingType(OrderSlicingType.valueOf(restOrder.getSlicingType()));	

		}
		if (null != restOrder.getSliceDuration()) {
			processingOrder.setSliceDuration(Duration.ofSeconds(restOrder.getSliceDuration()));

		}
		if (null != restOrder.getSliceOverlap()) {
			processingOrder.setSliceOverlap(Duration.ofSeconds(restOrder.getSliceOverlap()));

		}
		
		for (RestParameter restParam: restOrder.getOutputParameters()) {
			Parameter modelParam = new Parameter();
			modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
			processingOrder.getOutputParameters().put(restParam.getKey(), modelParam);
		}
		
		if (null != restOrder.getOutputFileClass()) {
			processingOrder.setOutputFileClass(restOrder.getOutputFileClass());
		}
		if (null != restOrder.getProcessingMode()) {
			processingOrder.setProcessingMode(restOrder.getProcessingMode());
		}
		if (null != restOrder.getProductionType()) {
			processingOrder.setProductionType(ProductionType.valueOf(restOrder.getProductionType()));
		}
		if (null != restOrder.getHasFailedJobSteps()) {
			processingOrder.setHasFailedJobSteps(restOrder.getHasFailedJobSteps());
		}
		
		return processingOrder;
	}
	
}