package de.dlr.proseo.model.util;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.OrderMgrMessage;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.InputProductReference;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.NotificationEndpoint;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.OrderSource;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.rest.model.RestClassOutputParameter;
import de.dlr.proseo.model.rest.model.RestInputFilter;
import de.dlr.proseo.model.rest.model.RestInputReference;
import de.dlr.proseo.model.rest.model.RestNotificationEndpoint;
import de.dlr.proseo.model.rest.model.RestOrbitQuery;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.ProductClass;

public class OrderUtil {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderUtil.class);
	
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
		Comparator<String> c = Comparator.comparing((String x) -> x);
		
		restOrder.setId(processingOrder.getId());
		restOrder.setVersion(Long.valueOf(processingOrder.getVersion()));
		
		if (null != processingOrder.getMission()) {
			restOrder.setMissionCode(processingOrder.getMission().getCode());

		}	
		if (null != processingOrder.getIdentifier()) {
			restOrder.setIdentifier(processingOrder.getIdentifier());
		}	
		if (null != processingOrder.getUuid()) {
			restOrder.setUuid(processingOrder.getUuid().toString());
		}
		if (null != processingOrder.getPriority()) {
			restOrder.setPriority(processingOrder.getPriority());
		}	
		if (null != processingOrder.getOrderState()) {
			restOrder.setOrderState(processingOrder.getOrderState().toString());
		}
		if (null != processingOrder.getStateMessage()) {
			restOrder.setStateMessage(processingOrder.getStateMessage());
		}
		if (null != processingOrder.getSubmissionTime()) {
			restOrder.setSubmissionTime(Date.from(processingOrder.getSubmissionTime()));
		}
		if (null != processingOrder.getReleaseTime()) {
			restOrder.setReleaseTime(Date.from(processingOrder.getReleaseTime()));
		}
		if (null != processingOrder.getStartTime()) {
			restOrder.setStartTime(OrbitTimeFormatter.format(processingOrder.getStartTime()));
		}
		if (null != processingOrder.getStopTime()) {
			restOrder.setStopTime(OrbitTimeFormatter.format(processingOrder.getStopTime()));
		}
		if (null != processingOrder.getExecutionTime()) {
			restOrder.setExecutionTime(Date.from(processingOrder.getExecutionTime()));
		}
		if (null != processingOrder.getEstimatedCompletionTime()) {
			restOrder.setEstimatedCompletionTime(Date.from(processingOrder.getEstimatedCompletionTime()));
		}
		if (null != processingOrder.getActualCompletionTime()) {
			restOrder.setActualCompletionTime(Date.from(processingOrder.getActualCompletionTime()));
		}
		if (null != processingOrder.getEvictionTime()) {
			restOrder.setEvictionTime(Date.from(processingOrder.getEvictionTime()));
		}
		if(null != processingOrder.getSlicingType()) {
			restOrder.setSlicingType(processingOrder.getSlicingType().name());
		}
		if(null != processingOrder.getSliceDuration()) {
			restOrder.setSliceDuration(processingOrder.getSliceDuration().getSeconds());
		}
		restOrder.setSliceOverlap(processingOrder.getSliceOverlap().getSeconds());

		if (null != processingOrder.getInputFilters()) {
			for (ProductClass sourceClass : processingOrder.getInputFilters().keySet()) {
				RestInputFilter restInputFilter = new RestInputFilter();
				restInputFilter.setProductClass(sourceClass.getProductType());
				Map<String, Parameter> filterConditions = processingOrder.getInputFilters().get(sourceClass)
						.getFilterConditions();
				for (String paramKey : filterConditions.keySet()) {
					restInputFilter.getFilterConditions()
							.add(new RestParameter(paramKey,
									filterConditions.get(paramKey).getParameterType().toString(),
									filterConditions.get(paramKey).getParameterValue()));
				}
				Collections.sort(restInputFilter.getFilterConditions(), (o1, o2) -> {
					return o1.getKey().compareTo(o2.getKey());
				});
				restOrder.getInputFilters().add(restInputFilter);
			}
			Collections.sort(restOrder.getInputFilters(), (o1, o2) -> {
				return o1.getProductClass().compareTo(o2.getProductClass());
			});
		}

		if (null != processingOrder.getClassOutputParameters()) {
			for (ProductClass targetClass : processingOrder.getClassOutputParameters().keySet()) {
				RestClassOutputParameter restClassOutputParameter = new RestClassOutputParameter();
				restClassOutputParameter.setProductClass(targetClass.getProductType());
				Map<String, Parameter> outputParameters = processingOrder.getClassOutputParameters().get(targetClass)
						.getOutputParameters();
				for (String paramKey : outputParameters.keySet()) {
					restClassOutputParameter.getOutputParameters()
							.add(new RestParameter(paramKey,
									outputParameters.get(paramKey).getParameterType().toString(),
									outputParameters.get(paramKey).getParameterValue()));
				}
				Collections.sort(restClassOutputParameter.getOutputParameters(), (o1, o2) -> {
					return o1.getKey().compareTo(o2.getKey());
				});
				restOrder.getClassOutputParameters().add(restClassOutputParameter);
			}
			Collections.sort(restOrder.getClassOutputParameters(), (o1, o2) -> {
				return o1.getProductClass().compareTo(o2.getProductClass());
			});
		}
		
		if(null != processingOrder.getOutputParameters()) {
			
			for (String paramKey: processingOrder.getOutputParameters().keySet()) {
				restOrder.getOutputParameters().add(
					new RestParameter(paramKey,
							processingOrder.getOutputParameters().get(paramKey).getParameterType().toString(),
							processingOrder.getOutputParameters().get(paramKey).getParameterValue()));
			}
			Collections.sort(restOrder.getOutputParameters(), (o1, o2) -> {
				return o1.getKey().compareTo(o2.getKey());
			});
		}
		if (null != processingOrder.getRequestedProductClasses()) {
			
			for (ProductClass productClass : processingOrder.getRequestedProductClasses()) {
				restOrder.getRequestedProductClasses().add(productClass.getProductType());
			}
			restOrder.getRequestedProductClasses().sort(c);
			
		}
		if (null != processingOrder.getInputProductClasses()) {
			for (ProductClass productClass : processingOrder.getInputProductClasses()) {
				restOrder.getInputProductClasses().add(productClass.getProductType());
			}
			restOrder.getInputProductClasses().sort(c);
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
		if (null != processingOrder.getProductRetentionPeriod()) {
			restOrder.setProductRetentionPeriod(processingOrder.getProductRetentionPeriod().getSeconds());
		}
		if (null != processingOrder.hasFailedJobSteps()) {
			restOrder.setHasFailedJobSteps(processingOrder.hasFailedJobSteps());
		}
		if (null != processingOrder.getOrderSource()) {
			restOrder.setOrderSource(processingOrder.getOrderSource().toString());
		}
		
		if (null != processingOrder.getRequestedConfiguredProcessors()) {
			for (ConfiguredProcessor toAddProcessor: processingOrder.getRequestedConfiguredProcessors()) {
				restOrder.getConfiguredProcessors().add(toAddProcessor.getIdentifier());
			}
			restOrder.getConfiguredProcessors().sort(c);
		}	
		
		if (null != processingOrder.getDynamicProcessingParameters()
				& !processingOrder.getDynamicProcessingParameters().isEmpty()) {
			List<RestParameter> dynamicProcessingParameters = new ArrayList<>();
			for (String key : processingOrder.getDynamicProcessingParameters().keySet()) {
				Parameter param = processingOrder.getDynamicProcessingParameters().get(key);
				RestParameter restParam = new RestParameter(key, param.getParameterType().name(),
						param.getStringValue());
				dynamicProcessingParameters.add(restParam);
			}
			Collections.sort(dynamicProcessingParameters, (o1, o2) -> {
				return o1.getKey().compareTo(o2.getKey());
			});
			restOrder.setDynamicProcessingParameters(dynamicProcessingParameters);
		}
		
		if (null != processingOrder.getNotificationEndpoint()) {
			NotificationEndpoint notificationEndpoint = processingOrder.getNotificationEndpoint();
			RestNotificationEndpoint restNotificationEndpoint = new RestNotificationEndpoint();
			restNotificationEndpoint.setPassword(notificationEndpoint.getPassword());
			restNotificationEndpoint.setUri(notificationEndpoint.getUri());
			restNotificationEndpoint.setUsername(notificationEndpoint.getUsername());
			restOrder.setNotificationEndpoint(restNotificationEndpoint);
		}
		
		if (null != processingOrder.getInputProductReference()) {
			InputProductReference inputProductReference = processingOrder.getInputProductReference();
			RestInputReference restInputReference = new RestInputReference();
			if (null != inputProductReference.getInputFileName()) {
				restInputReference.setInputFileName(inputProductReference.getInputFileName());
			}
			if (null != inputProductReference.getSensingStartTime()
					&& null != inputProductReference.getSensingStopTime()) {
				if (inputProductReference.getSensingStartTime().isAfter(inputProductReference.getSensingStopTime()))
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_INPUT_REFERENCE));
				restInputReference.setSensingStartTime(inputProductReference.getSensingStartTime().toString());
				restInputReference.setSensingStopTime(inputProductReference.getSensingStopTime().toString());
			}
			restOrder.setInputProductReference(restInputReference);
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

		if (null != processingOrder.getWorkflow()) {
			restOrder.setWorkflowName(processingOrder.getWorkflow().getName());
			restOrder.setWorkflowUuid(processingOrder.getWorkflow().getUuid().toString());
		}
		// Get the list of job step states
		// this is much faster than iterating over jobs and job steps
		List<String> jss = RepositoryService.getJobStepRepository().findDistinctJobStepStatesByOrderId(processingOrder.getId());
		if (jss == null) {
			jss = new ArrayList<String>();
		}
		restOrder.setJobStepStates(jss);
		
		// Calculate the percentage of finished job steps

		// Get job and job step statistics for display
		List<String> jobStepStates = new ArrayList<>();
		Long jsCountRunning = 0L;
		Long jsCountFailed = 0L;
		Long jsCountCompleted = 0L;
		Long jsCountTotal = 0L;
		
		if (logger.isTraceEnabled()) logger.trace("... checking job step states for order with DBID = {}", processingOrder.getId());
		List<Object[]> resultRecords = RepositoryService.getJobStepRepository().countJobStepStatesByOrderId(processingOrder.getId());
		if (logger.isTraceEnabled()) logger.trace("... found {} different job step states", resultRecords.size());
		
		for (Object[] resultRecord: resultRecords) {
			if (logger.isTraceEnabled()) logger.trace("... checking job step state {} (class {}) with count {} (class {})", 
					resultRecord[0], resultRecord[0].getClass().getCanonicalName(), resultRecord[1], resultRecord[1].getClass().getCanonicalName());
			if (resultRecord[0] instanceof JobStepState && resultRecord[1] instanceof Long) {
				String jsState = ((JobStepState) resultRecord[0]).toString();
				jobStepStates.add(jsState);

				switch(jsState) {
				case "RUNNING":
					jsCountRunning += (Long) resultRecord[1];
					break;
				case "COMPLETED":
					jsCountCompleted += (Long) resultRecord[1];
					break;
				case "FAILED":
					jsCountFailed += (Long) resultRecord[1];
					break;
				}
				jsCountTotal += (Long) resultRecord[1];
			}
		}
		if (logger.isTraceEnabled()) logger.trace("... found {} total job steps, of which {} running, {} failed, {} completed", 
				jsCountTotal, jsCountRunning, jsCountFailed, jsCountCompleted);
		
		// Create job step statistics
		restOrder.setHasFailedJobSteps(jsCountFailed > 0);
		restOrder.setJobStepStates(jobStepStates);
		if (0 < jsCountTotal) {
			restOrder.setPercentCompleted(jsCountCompleted * 100 / jsCountTotal);
			restOrder.setPercentFailed(jsCountFailed * 100 / jsCountTotal);
			restOrder.setPercentRunning(jsCountRunning * 100 / jsCountTotal);
		} else {
			restOrder.setPercentCompleted(0L);
			restOrder.setPercentFailed(0L);
			restOrder.setPercentRunning(0L);
		}
		// Create job statistics
		int expectedCount = processingOrder.getJobs().size();
		if (logger.isTraceEnabled()) logger.trace("... found {} total jobs for order in state {}", expectedCount, processingOrder.getOrderState());
		int plannedCount = RepositoryService.getJobRepository().countAllByJobStateAndProcessingOrder(JobState.PLANNED, processingOrder.getId());
		if (logger.isTraceEnabled()) logger.trace("... found {} planned jobs", plannedCount);
		int createdCount = 0;
		if (processingOrder.getOrderState() == OrderState.PLANNING) {
			createdCount = plannedCount;
		} else if (processingOrder.getOrderState() == OrderState.RELEASING) {
			createdCount = expectedCount - plannedCount;
		}
		if (logger.isTraceEnabled()) logger.trace("... calculated {} created jobs", createdCount);
		restOrder.setExpectedJobs(Long.valueOf(expectedCount));
		restOrder.setCreatedJobs(Long.valueOf(createdCount));
		
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
		if (null != restOrder.getMissionCode()) {
			processingOrder.setMission(RepositoryService.getMissionRepository().findByCode(restOrder.getMissionCode()));
		}		
		processingOrder.setIdentifier(restOrder.getIdentifier());
		if (null != restOrder.getUuid()) {
			processingOrder.setUuid(UUID.fromString(restOrder.getUuid()));
		}
		processingOrder.setPriority(restOrder.getPriority());
		processingOrder.setOrderState(OrderState.valueOf(restOrder.getOrderState()));
		processingOrder.setStateMessage(restOrder.getStateMessage());
		processingOrder.setProcessingMode(restOrder.getProcessingMode());
		OrderSource orderSource = OrderSource.OTHER;
		try {
			orderSource = OrderSource.valueOf(restOrder.getOrderSource());
		} catch (Exception ex) {
			if (logger.isTraceEnabled()) logger.trace("    orderSource unknown: {}", (null == restOrder.getOrderSource() ? "null" : restOrder.getOrderSource()));
			orderSource = OrderSource.OTHER;
		}
		processingOrder.setOrderSource(orderSource);

		if (null != restOrder.getSubmissionTime()) {
			try {
				processingOrder.setSubmissionTime(restOrder.getSubmissionTime().toInstant());
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						String.format("Invalid submission time '%s'", restOrder.getReleaseTime()));
			}
		}
		if (null != restOrder.getReleaseTime()) {
			try {
				processingOrder.setReleaseTime(restOrder.getReleaseTime().toInstant());
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						String.format("Invalid release time '%s'", restOrder.getReleaseTime()));
			}
		}
		if (null != restOrder.getStartTime()) {
			try {
				processingOrder.setStartTime(Instant.from(OrbitTimeFormatter.parse(restOrder.getStartTime())));
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(String.format("Invalid sensing start time '%s'", restOrder.getStartTime()));
			} 
		}
		if (null != restOrder.getStopTime()) {
			try {
				processingOrder.setStopTime(Instant.from(OrbitTimeFormatter.parse(restOrder.getStopTime())));
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(String.format("Invalid sensing stop time '%s'", restOrder.getStartTime()));
			} 
		}
		if (null != restOrder.getExecutionTime()) {
			try {
				processingOrder.setExecutionTime(restOrder.getExecutionTime().toInstant());
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(String.format("Invalid execution time '%s'", restOrder.getExecutionTime()));
			} 
		}
		if (null != restOrder.getEstimatedCompletionTime()) {
			try {
				processingOrder.setEstimatedCompletionTime(restOrder.getEstimatedCompletionTime().toInstant());
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						String.format("Invalid estimated completion time '%s'", restOrder.getReleaseTime()));
			}
		}		
		if (null != restOrder.getActualCompletionTime()) {
			try {
				processingOrder.setActualCompletionTime(restOrder.getActualCompletionTime().toInstant());
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						String.format("Invalid actual completion time '%s'", restOrder.getReleaseTime()));
			}
		}
		if (null != restOrder.getEvictionTime()) {
			try {
				processingOrder.setEvictionTime(restOrder.getEvictionTime().toInstant());
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(String.format("Invalid eviction time '%s'", restOrder.getEvictionTime()));
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
		if (null != restOrder.getProductRetentionPeriod()) {
			processingOrder.setProductRetentionPeriod(Duration.ofSeconds(restOrder.getProductRetentionPeriod()));
		}
		if (null != restOrder.getHasFailedJobSteps()) {
			processingOrder.setHasFailedJobSteps(restOrder.getHasFailedJobSteps());
		}

		if (null != restOrder.getDynamicProcessingParameters()
				& !restOrder.getDynamicProcessingParameters().isEmpty()) {
			for (RestParameter restParam : restOrder.getDynamicProcessingParameters()) {
				Parameter modelParam = new Parameter();
				modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
				processingOrder.getDynamicProcessingParameters().put(restParam.getKey(), modelParam);
			}
		}

		if (null != restOrder.getNotificationEndpoint()) {
			RestNotificationEndpoint restNotificationEndpoint = restOrder.getNotificationEndpoint();
			NotificationEndpoint notificationEndpoint = new NotificationEndpoint();
			notificationEndpoint.setPassword(restNotificationEndpoint.getPassword());
			notificationEndpoint.setUri(restNotificationEndpoint.getUri());
			notificationEndpoint.setUsername(restNotificationEndpoint.getUsername());
			processingOrder.setNotificationEndpoint(notificationEndpoint);
		}
		
		// Set only if either file name or start and stop time are not null, ensure that stop is after start time 
		RestInputReference restInputReference = restOrder.getInputProductReference();
		InputProductReference inputProductReference = new InputProductReference();
		if (null != restInputReference) {
			if (null != restInputReference.getInputFileName()) {
				inputProductReference.setInputFileName(restInputReference.getInputFileName());
			} 
			if (null != restInputReference.getSensingStartTime() && null != restInputReference.getSensingStopTime()) {
				if (Instant.from(OrbitTimeFormatter.parse(restInputReference.getSensingStartTime()))
						.isAfter(Instant.from(OrbitTimeFormatter.parse(restInputReference.getSensingStopTime()))))
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_INPUT_REFERENCE));
				inputProductReference.setSensingStartTime(Instant.from(OrbitTimeFormatter.parse(restInputReference.getSensingStartTime())));
				inputProductReference.setSensingStopTime(Instant.from(OrbitTimeFormatter.parse(restInputReference.getSensingStopTime())));
			}
			processingOrder.setInputProductReference(inputProductReference);
		}
		
		return processingOrder;
	}
	
}