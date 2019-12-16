package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.JobStepDispatcher;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.rest.model.RestOrbitQuery;
import de.dlr.proseo.planner.rest.model.RestOrder;
import de.dlr.proseo.planner.rest.model.RestParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderControllerImpl implements OrderController {
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_HEADER_SUCCESS = "Success";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(OrderControllerImpl.class);
	
	
    @Autowired
    private ProductionPlanner productionPlanner;

    @Autowired
    private OrderDispatcher orderDispatcher;

    @Autowired
    private JobStepDispatcher jobStepDispatcher;


	/**
	 * Get all processing orders
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<List<RestOrder>> getOrders() {
		
		Iterable<ProcessingOrder> orders = RepositoryService.getOrderRepository().findAll();
		List<RestOrder> list = new ArrayList<RestOrder>();

		for (ProcessingOrder po : orders) {
			RestOrder ro = createAndCopyToRestOrder(po);
			list.add(ro);			
		}

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_SUCCESS, "");
		return new ResponseEntity<>(list, responseHeaders, HttpStatus.OK);
	}

	/**
	 * Plan processing order of id on processing facility
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> planOrder(String releaseId, String facility) {
		if (releaseId != null) {
			ProcessingOrder order = findOrder(releaseId);
			if (order != null) {
				ProcessingFacility pf = null;
				if (facility != null) {
					KubeConfig kc = productionPlanner.getKubeConfig(facility);
					if (kc != null) {
						pf = kc.getProcessingFacility();
					}
				}
				if (pf == null) {
					productionPlanner.getKubeConfig("Lerchenhof").getProcessingFacility();
				}
				if (orderDispatcher.publishOrder(order, pf)) {
					jobStepDispatcher.searchForJobStepsToRun(pf);
					RestOrder ro = createAndCopyToRestOrder(order);
					String message = String.format(MSG_PREFIX + "CREATE jobs for order '%s' created (%d)", order.getIdentifier(), 2000);
					logger.error(message);
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.set(HTTP_HEADER_WARNING, message);
					return new ResponseEntity<>(ro, responseHeaders, HttpStatus.CREATED);
				} else {
					String message = String.format(MSG_PREFIX + "CREATE jobs for order '%s' not created (%d)", order.getIdentifier(), 2000);
					logger.error(message);
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.set(HTTP_HEADER_WARNING, message);
					return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
				}
			}
		} else {
			String message = String.format(MSG_PREFIX + "CREATE order '%s' not found (%d)", releaseId, 2000);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
    	String message = String.format(MSG_PREFIX + "CREATE parameter name missing (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Release prcessing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> releaseOrder(String resumeId) {
    	String message = String.format(MSG_PREFIX + "Resume not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Cancel prcessing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> cancelOrder(String cancelId) {
    	String message = String.format(MSG_PREFIX + "Cancel not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Suspend prcessing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> suspendOrder(String suspendId) {
    	String message = String.format(MSG_PREFIX + "Suspend not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Transactional
	private ProcessingOrder findOrder(String orderId) {
		Optional<ProcessingOrder> orderOpt = null;
		ProcessingOrder order = null;
		try {
			Long id = Long.valueOf(orderId);
			orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				order = orderOpt.get();
			}
		} catch (NumberFormatException nfe) {
			// use name as identifier
		}
		if (order == null) {
			order = RepositoryService.getOrderRepository().findByIdentifier(orderId);
		}
		return order;
	}

	private RestOrder createAndCopyToRestOrder(ProcessingOrder po) {
		RestOrder ro = new RestOrder();
		if (po != null) {
			ro.setId(po.getId());
			ro.setIdentifier(po.getIdentifier());
			if (po.getExecutionTime() != null) {
				ro.setExecutionTime(Date.from(po.getExecutionTime()));
			}
			for (ConfiguredProcessor cp : po.getRequestedConfiguredProcessors()) {
				ro.getConfiguredProcessors().add(cp.getIdentifier());
			}
			for (Entry<String, Parameter> entry : po.getFilterConditions().entrySet()) {
				ro.getFilterConditions().add(new RestParameter(entry.getKey(), entry.getValue().getParameterType().toString(), entry.getValue().getStringValue()));
			}
			for (ProductClass pc : po.getInputProductClasses()) {
				ro.getInputProductClasses().add(pc.getProductType());
			}
			ro.setMissionCode(po.getMission().getCode());
			// orbits
			for (Orbit o : po.getRequestedOrbits()) {
				ro.getOrbits().add(new RestOrbitQuery(o.getSpacecraft().getCode(), (long) o.getOrbitNumber(), (long) o.getOrbitNumber()));
			}
			ro.setOrderState(po.getOrderState().toString());
			ro.setOutputFileClass(po.getOutputFileClass());
			for (Entry<String, Parameter> entry : po.getOutputParameters().entrySet()) {
				ro.getOutputParameters().add(new RestParameter(entry.getKey(), entry.getValue().getParameterType().toString(), entry.getValue().getStringValue()));
			}
			ro.setProcessingMode(po.getProcessingMode());
			for (ProductClass pc : po.getRequestedProductClasses()) {
				ro.getRequestedProductClasses().add(pc.getProductType());
			}
			if (po.getSliceDuration() != null) {
				ro.setSliceDuration(po.getSliceDuration().getSeconds());
			}
			if (po.getSliceOverlap() != null) {
				ro.setSliceOverlap(po.getSliceOverlap().getSeconds());
			}
			ro.setSlicingType(po.getSlicingType().toString());
			if (po.getStartTime() != null) {
				ro.setStartTime(Date.from(po.getStartTime()));
			}
			if (po.getStopTime() != null) {
				ro.setStopTime(Date.from(po.getStopTime()));
			}
			ro.setVersion((long) po.getVersion());
		}			
		return ro;
	}
	
}
