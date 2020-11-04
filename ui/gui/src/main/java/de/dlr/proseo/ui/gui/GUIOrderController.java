package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_PRODUCTCLASSES_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.service.MapComparator;
import de.dlr.proseo.ui.gui.service.OrderService;
import reactor.core.publisher.Mono;

import de.dlr.proseo.model.rest.model.RestProcessingFacility;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestProductClass;

@Controller
public class GUIOrderController extends GUIBaseController {
	private static final String MAPKEY_ID = "id";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIOrderController.class);
	
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/** WebClient-Service-Builder */
	@Autowired
	private OrderService orderService;
	
	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
	
	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	
	/**
	 * Date formatter for input type date
	 */
	private static final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);	
	
	@GetMapping(value ="/order-show")
	public String showOrder() {
		ModelAndView modandview = new ModelAndView("order-show");
		modandview.addObject("message", "TEST");
		return "order-show";
	}
	@GetMapping(value ="/order")
	public String order() {
		ModelAndView modandview = new ModelAndView("order");
		modandview.addObject("message", "TEST");
		return "order";
	}
    
	/**
	 * Retrieve the order list filtered by these parameters
	 * 
	 * @param identifier The order identifier (name) as pattern
	 * @param states The order states (seperated by ':')
	 * @param from The from date of start time
	 * @param to The to date of start time
	 * @param products The product class list (seperated by ':')
	 * @param sortby The sort column
	 * @param up The sort direction (true for 'up')
	 * @param model The model to hold the data
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/order-show/get")
	public DeferredResult<String> getIdentifier(
			@RequestParam(required = false, value = "identifier") String identifier,
			@RequestParam(required = false, value = "states") String states,
			@RequestParam(required = false, value = "from") String from,
			@RequestParam(required = false, value = "to") String to,
			@RequestParam(required = false, value = "products") String products,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getIdentifier({}, {}, model)", identifier, identifier);
		String myIdent = null;
		if (identifier != null && identifier.indexOf("*") < 0) {
			myIdent = identifier;
		}
		Mono<ClientResponse> mono = orderService.get(myIdent);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> orders = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("order-show :: #orderscontent");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("order-show :: #orderscontent");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(orderList -> {
					orders.addAll(selectOrders(orderList, identifier, states, from, to, products));
					String key = MAPKEY_ID;
					if (sortby != null) {
						if (sortby.contentEquals("identifier") || sortby.contentEquals(MAPKEY_ID) || sortby.contentEquals("orderState")) {
							key = sortby;
						}
					}
					Boolean isUp = (null == up ? true : up);
					MapComparator oc = new MapComparator(key, isUp);
					orders.sort(oc);
					model.addAttribute("orders", orders);
					model.addAttribute("selcol", key);
					model.addAttribute("selorder", (isUp ? "select-up" : "select-down"));
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + orders.toString());
					deferredResult.setResult("order-show :: #orderscontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + orders.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	/**
	 * Set the new state of an order
	 * 
	 * @param id The order id
	 * @param state The new state
	 * @param facility The facility (for plan)
	 * @param model The model to hold the data
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/order-state/post")
	public DeferredResult<String> setState(
			@RequestParam(required = true, value = MAPKEY_ID) String id, 
			@RequestParam(required = true, value = "state") String state,
			@RequestParam(required = false, value = "facility") String facility,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> setState({}, {}, model)", id, state, facility);
		Mono<ClientResponse> mono = orderService.setState(id, state, facility);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		mono.timeout(Duration.ofMillis(config.getTimeout())).subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("order-show :: #content");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("order-show :: #content");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				if (clientResponse.statusCode().compareTo(HttpStatus.NO_CONTENT) == 0) {
					deferredResult.setResult("no content");
				} else {
					clientResponse.bodyToMono(HashMap.class).timeout(Duration.ofMillis(config.getTimeout())).subscribe(orderList -> {
						model.addAttribute("ord", orderList);
						logger.trace(model.toString() + "MODEL TO STRING");
						logger.trace(">>>>MONO" + orderList.toString());
						deferredResult.setResult("order-show :: #content");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				}
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}
	
    /**
     * Retrieve the processing facilities
     * 
     * @return Facility list
     */
    @ModelAttribute("facilities")
    public List<String> facilities() {
    	List<String> facilities = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		
		try {
			resultList = serviceConnection.getFromService(config.getProductionPlanner(),
					"/processingfacilities", List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return facilities;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return facilities;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestProcessingFacility restFacility = mapper.convertValue(object, RestProcessingFacility.class);
				facilities.add(restFacility.getName());
			}
		}

		Comparator<String> c = Comparator.comparing((String x) -> x);
		facilities.sort(c);
        return facilities;
    }
    
    /**
     * Retrieve the product classes of mission
     * 
     * @return Product class list
     */
    @ModelAttribute("productclasses")
    public List<String> productclasses() {
    	List<String> productclasses = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
					"/productclasses?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PRODUCTCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return productclasses;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return productclasses;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestProductClass restProductClass = mapper.convertValue(object, RestProductClass.class);
				productclasses.add(restProductClass.getProductType());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		productclasses.sort(c);
        return productclasses;
    }

	/**
	 * Retrieve a single order
	 * 
	 * @param id The order id.
	 * @param model The model to hold the data
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/order/get")
	public DeferredResult<String> getId(
			@RequestParam(required = true, value = MAPKEY_ID) String id,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getId({}, model)", id);
		Mono<ClientResponse> mono = orderService.getId(id);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> orders = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("order :: #ordercontent");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("order :: #ordercontent");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(HashMap.class).subscribe(order -> {
					orders.add(order);
					model.addAttribute("orders", orders);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + orders.toString());
					deferredResult.setResult("order :: #ordercontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + orders.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	/**
	 * Retrieve all jobs of an order
	 * 
	 * @param id The order id
	 * @param model The model to hold the data
	 * 
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/jobs/get")
	public DeferredResult<String> getJobsOfOrder(
			@RequestParam(required = true, value = "orderid") String id,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getId({}, model)", id);
		Mono<ClientResponse> mono = orderService.getJobsOfOrder(id);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> jobs = new ArrayList<>();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("order :: #jobscontent");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("order :: #jobscontent");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(jobList -> {
					jobs.addAll(jobList);
					/*
					 * for (Object o : jobs) { if (o instanceof HashMap) { HashMap<String, Object> h
					 * = (HashMap<String, Object>) o; String jobId = h.get("id").toString();
					 * HashMap<String, Object> result = orderService.getGraphOfJob(jobId, auth);
					 * h.put("graph", result); } }
					 */		
					model.addAttribute("jobs", jobs);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + jobs.toString());
					deferredResult.setResult("order :: #jobscontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + jobs.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/jobs/graph")
	public DeferredResult<String> getGraphOfJob(
			@RequestParam(required = true, value = "jobid") String id,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getId({}, model)", id);
		Mono<ClientResponse> mono = orderService.getGraphOfJob(id);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> jobs = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("order :: #jobscontent");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("order :: #jobscontent");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(HashMap.class).subscribe(jobgraph -> {
					jobs.add(jobgraph);
					model.addAttribute("jobgraph", jobs);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + jobs.toString());
					deferredResult.setResult("order :: #jobgraph");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + jobs.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	/**
	 * Helper function to select orders 
	 * 
	 * @param identifier The order identifier (name) as pattern
	 * @param orderList The complete list
	 * @param states The order states (seperated by ':')
	 * @param from The from date of start time
	 * @param to The to date of start time
	 * @param products The product class list (seperated by ':')
	 * @return List of selected orders
	 */
	private List<Object> selectOrders(List<Object> orderList, String identifier, String states, String from, String to, String products) {
		List<Object> result = new ArrayList<Object>();
		if (orderList != null && (identifier != null || states != null || from != null || to != null || products != null)) {
			String myident = null;
			if (identifier != null) {
				// at the moment only simple pattern are supported, replace '*'
				myident = identifier.replaceAll("[*]", ".*");
			}
			ArrayList<String> stateArray = null;
			if (states != null) {
				// build array of selected states
				stateArray = new ArrayList<String>();
				String[] x = states.split(":");				
				for (int i = 0; i < x.length; i++) {
					stateArray.add(x[i]);
				}
			}
			ArrayList<String> productArray = null;
			if (products != null) {
				productArray = new ArrayList<String>();
				String[] x = products.split(":");		
				for (int i = 0; i < x.length; i++) {
					productArray.add(x[i]);
				}
			}
			Date fromTime = null;
			if (from != null) {			
				try {
					fromTime = simpleDateFormatter.parse(from);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Date toTime = null;
			if (to != null) {		
				try {
					toTime = simpleDateFormatter.parse(to);
					Instant t = toTime.toInstant().plus(Duration.ofDays(1));
					toTime = Date.from(t);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			ObjectMapper mapper = new ObjectMapper();
			Boolean found = true;
			for (Object o : orderList) {
				found = true;
				try {
					RestOrder order =  mapper.convertValue(o, RestOrder.class);
					if (order != null) {
						if (found && identifier != null) {
							if (!order.getIdentifier().matches(myident)) found = false;					
						}
						if (stateArray != null) {
							if (!stateArray.contains(order.getOrderState())) found = false;
						}
						if (fromTime != null && order.getStartTime() != null) {
							if (fromTime.compareTo(order.getStartTime()) > 0) found = false;
						}
						if (toTime != null && order.getStopTime() != null) {
							if (toTime.compareTo(order.getStopTime()) < 0) found = false;
						}
						if (productArray != null && order.getRequestedProductClasses() != null) {
							Boolean lfound = false;
							for (String pc : order.getRequestedProductClasses()) {
								if (productArray.contains(pc)) {
									lfound = true;
									break;
								}
							}
							if (!lfound) found = false;
						}
					}
					if (found) {
						result.add(o);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		} else {
			return orderList;
		}
		return result;
	}
	
}


