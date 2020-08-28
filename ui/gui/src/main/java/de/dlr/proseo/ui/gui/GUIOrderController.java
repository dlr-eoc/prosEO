package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.service.OrderComparator;
import de.dlr.proseo.ui.gui.service.OrderService;
import reactor.core.publisher.Mono;

import de.dlr.proseo.model.rest.model.RestProcessingFacility;
import de.dlr.proseo.model.rest.model.RestOrder;

@Controller
public class GUIOrderController extends GUIBaseController {
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

	@RequestMapping(value = "/order-close")
	public String closeOrder() {

		return "order-close";
	}
	@RequestMapping(value = "/order-create")
	public String createOrder() {

		return "order-create";
	}
	@RequestMapping(value = "/order-delete")
	public String deleteOrder() {

		return "order-delete";
	}
	@RequestMapping(value = "/order-plan")
	public String planOrderl() {

		return "order-plan";
	}
	@RequestMapping(value = "/order-release")
	public String releaseOrder() {

		return "order-release";
	}
	@RequestMapping(value = "/order-resume")
	public String resumeOrder() {

		return "order-resume";
	}
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
	@RequestMapping(value = "/order-suspend")
	public String suspendOrder() {

		return "order-suspend";
	}
	@RequestMapping(value = "/order-update")
	public String updateOrder() {

		return "order-update";
	}

    
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/order-show/get")
	public DeferredResult<String> getIdentifier(
			@RequestParam(required = false, value = "identifier") String identifier,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getIdentifier({}, {}, model)", identifier, identifier);
		Mono<ClientResponse> mono = orderService.get(identifier);
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
					orders.addAll(orderList);
					String key = "id";
					if (sortby != null) {
						if (sortby.contentEquals("identifier") || sortby.contentEquals("id") || sortby.contentEquals("orderState")) {
							key = sortby;
						}
					}
					Boolean dir = true;
					if (up != null) {
						dir = up;
					}
					OrderComparator oc = new OrderComparator(key, dir);
					orders.sort(oc);
					model.addAttribute("orders", orders);
					model.addAttribute("selcol", key);
					model.addAttribute("selorder", (up == true ? "select-up" : "select-down"));
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

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/order-state/post")
	public DeferredResult<String> setState(
			@RequestParam(required = true, value = "id") String id, 
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
		
        return facilities;
    }

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/order/get")
	public DeferredResult<String> getId(
			@RequestParam(required = true, value = "id") String id,
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

}


