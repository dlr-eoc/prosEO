package de.dlr.proseo.ordermgr.rest.model;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.compare.CalendarComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.dlr.proseo.model.ConfigurationFile;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProcessingOrder.OrderSlicingType;
import de.dlr.proseo.model.ProcessingOrder.OrderState;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Parameter.ParameterType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.rest.model.Parameter;
import scala.sys.process.ProcessBuilderImpl.OrBuilder;
public class OrderUtil {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrbitUtil.class);
	
	
	
	/**
	 * Convert a prosEO model ProcessingOrder into a REST Order
	 * 
	 * @param processingOrder the prosEO model Order
	 * @return an equivalent REST Order or null, if no model Order was given
	 */

	@SuppressWarnings("unchecked")
	public static Order toRestOrder(ProcessingOrder processingOrder) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestOrder({})", (null == processingOrder ? "MISSING" : processingOrder.getId()));
		
		if (null == processingOrder)
			return null;
		
		Order restOrder = new Order();
		
		restOrder.setId(processingOrder.getId());
		restOrder.setVersion(Long.valueOf(processingOrder.getVersion()));
		
		if (null != processingOrder.getMission().getCode()) {
			restOrder.setMissionCode(processingOrder.getMission().getCode());
		}	
		if (null != processingOrder.getIdentifier()) {
			restOrder.setIdentifier(processingOrder.getIdentifier());
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
		
		//To be verified
		if(null != processingOrder.getSlicingType()) {
			restOrder.setSlicingType(processingOrder.getSlicingType().name());
		}
		//To be verified
		if(null != processingOrder.getSliceDuration()) {
			restOrder.setSliceDuration(processingOrder.getSliceDuration().getSeconds());
		}
		//to be added Slice Overlap
		if(null != processingOrder.getProcessingMode()) {
			restOrder.setProcessingMode(processingOrder.getProcessingMode());
		}

		if (null != processingOrder.getFilterConditions()) {
			
			for (String paramKey: processingOrder.getFilterConditions().keySet()) {
				restOrder.getFilterConditions().add(
					new de.dlr.proseo.ordermgr.rest.model.Parameter(paramKey,
							processingOrder.getFilterConditions().get(paramKey).getParameterType().toString(),
							processingOrder.getFilterConditions().get(paramKey).getParameterValue()));
			}
			
		}
		if(null != processingOrder.getOutputParameters()) {
			
			for (String paramKey: processingOrder.getOutputParameters().keySet()) {
				restOrder.getOutputParameters().add(
					new Parameter(paramKey,
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
		if (null != processingOrder.getRequestedConfiguredProcessors()) {

			for (ConfiguredProcessor toAddProcessor: processingOrder.getRequestedConfiguredProcessors()) {
				restOrder.getConfiguredProcessors().add(toAddProcessor.getIdentifier());
			}
		}	
		
		//The orbit range should be altered to accommodate more ranges than just everything in between min and max
		if (null != processingOrder.getRequestedOrbits()) {
			
			List<OrbitQuery> orbitQueries = new ArrayList<OrbitQuery>();
			
			//Get all the orbit numbers for the requested Orbits 
			List<Long> orbNumber = new ArrayList<Long>();
			for (de.dlr.proseo.model.Orbit orbit : processingOrder.getRequestedOrbits()) {
				orbNumber.add(orbit.getOrbitNumber().longValue());			
			}
			
			for (de.dlr.proseo.model.Orbit orbit : processingOrder.getRequestedOrbits()) {
				OrbitQuery orbitQuery = new OrbitQuery();
				orbitQuery.setSpacecraftCode(orbit.getSpacecraft().getCode());
				//Set the range for OrbitNumbers assuming the smallest as OrbitNumberFrom and the highest as OrbitNumberTo
				orbitQuery.setOrbitNumberFrom(Collections.min(orbNumber));
				orbitQuery.setOrbitNumberTo(Collections.max(orbNumber));

				orbitQueries.add(orbitQuery);
			}
			restOrder.setOrbits(orbitQueries);
		}
		
	
		return restOrder;
	}

	/**
	 * Convert a REST order into a prosEO model processingorder (scalar and embedded attributes only, no orbit references)
	 * 
	 * @param restOrder the REST order
	 * @return a (roughly) equivalent model order
	 * @throws IllegalArgumentException if the REST order violates syntax rules for date, enum or numeric values
	 */
	@SuppressWarnings("unchecked")
	public static ProcessingOrder toModelOrder(Order restOrder) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toModelOrder({})", (null == restOrder ? "MISSING" : restOrder.getId()));
		
		ProcessingOrder processingOrder = new ProcessingOrder();
		
		processingOrder.setId(restOrder.getId());
		while (processingOrder.getVersion() < restOrder.getVersion()) {
			processingOrder.incrementVersion();
		}
		processingOrder.setIdentifier(restOrder.getIdentifier());
		processingOrder.setOrderState(OrderState.valueOf(restOrder.getOrderState()));
		processingOrder.setProcessingMode(restOrder.getProcessingMode());
		try {
			processingOrder.setStartTime(restOrder.getStartTime().toInstant());
			
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid sensing start time '%s'", restOrder.getStartTime()));
		}
		try {
			processingOrder.setStopTime(restOrder.getStopTime().toInstant());
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid sensing stop time '%s'", restOrder.getStartTime()));
		}
		try {
			processingOrder.setExecutionTime(restOrder.getExecutionTime().toInstant());
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid sensing stop time '%s'", restOrder.getExecutionTime()));
		}	
		//To be verified
		processingOrder.setSlicingType(OrderSlicingType.valueOf(restOrder.getSlicingType()));
	
		//processingOrder.setSliceDuration(Duration.from(restOrder.getSliceDuration()));
		
		//The following section needs to be verified
		for (Parameter restParam : restOrder.getFilterConditions()) {
			de.dlr.proseo.model.Parameter modelParam = new de.dlr.proseo.model.Parameter();
			modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
			processingOrder.getFilterConditions().put(restParam.getKey(), modelParam);
		}
		
		for (Parameter restParam: restOrder.getOutputParameters()) {
			de.dlr.proseo.model.Parameter modelParam = new de.dlr.proseo.model.Parameter();
			modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
			processingOrder.getOutputParameters().put(restParam.getKey(), modelParam);
		}
		
		
		return processingOrder;
	}
	
}