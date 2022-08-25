package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_MODIFIED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_ORDER_DATA_INVALID;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_ORDER_NOT_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;

import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.rest.model.JobStepState;
import de.dlr.proseo.model.rest.model.RestClassOutputParameter;
import de.dlr.proseo.model.rest.model.RestInputFilter;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.service.OrderService;
import reactor.core.publisher.Mono;

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
//	private static final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);	

	private static final String HTTP_HEADER_WARNING = "Warning";

	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message.replaceAll("\n", " "));
		responseHeaders.set("error", message.replaceAll("\n", " "));
		return responseHeaders;
	}
	
	@GetMapping(value ="/order-show")
	public String showOrder() {
		return "order-show";
	}
	@GetMapping(value ="/order-edit")
	public String editOrder() {
		return "order-edit";
	}
	@GetMapping(value ="/order")
	public String order() {
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
			@RequestParam(required = false, value = "recordFrom") Long recordFrom,
			@RequestParam(required = false, value = "recordTo") Long recordTo,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getIdentifier({}, {}, model)", identifier, identifier);

		checkClearCache();

		Long fromi = null;
		Long toi = null;
		if (recordFrom != null && recordFrom >= 0) {
			fromi = recordFrom;
		} else {
			fromi = (long) 0;
		}
		Long count = countOrdersL(identifier, states, products, from, to, null, null,
				sortby, up);
		if (recordFrom != null && fromi != null && recordTo > fromi) {
			toi = recordTo;
		} else if (from != null) {
			toi = count;
		}
		Long pageSize = toi - fromi;
		Long deltaPage = (long) ((count % pageSize)==0?0:1);
		Long pages = (count / pageSize) + deltaPage;
		Long page = (fromi / pageSize) + 1;
		Mono<ClientResponse> mono = orderService.get(identifier, states, products, from, to, 
				fromi, toi, sortby, up);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> orders = new ArrayList<>();
		mono.doOnError(e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order-show :: #errormsg");
		})
	 	.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().compareTo(HttpStatus.NOT_FOUND) == 0) {
				// this is no error cause only already planned orders have job steps
				model.addAttribute("count", 0);
				model.addAttribute("pageSize", 0);
				model.addAttribute("pageCount", 0);
				model.addAttribute("page", 0);
				deferredResult.setResult("order :: #jobscontent");
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(orderList -> {
					// orders.addAll(selectOrders(orderList, identifier, states, from, to, products));
					orders.addAll(orderList);
					String key = MAPKEY_ID;
					if (sortby != null) {
						if (sortby.contentEquals("identifier") || sortby.contentEquals(MAPKEY_ID) || sortby.contentEquals("orderState")) {
							key = sortby;
						}
					}
					Boolean isUp = (null == up ? true : up);
//					MapComparator oc = new MapComparator(key, isUp);
//					orders.sort(oc);
					model.addAttribute("orders", orders);
					model.addAttribute("selcol", key);
					model.addAttribute("selorder", (isUp ? "select-up" : "select-down"));
					model.addAttribute("count", count);
					model.addAttribute("pageSize", pageSize);
					model.addAttribute("pageCount", pages);
					model.addAttribute("page", page);
					List<Long> showPages = new ArrayList<Long>();
					Long start = Math.max(page - 4, 1);
					Long end = Math.min(page + 4, pages);
					if (page < 5) {
						end = Math.min(end + (5 - page), pages);
					}
					if (pages - page < 5) {
						start = Math.max(start - (4 - (pages - page)), 1);
					}
					for (Long i = start; i <= end; i++) {
						showPages.add(i);
					}
					model.addAttribute("showPages", showPages);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + orders.toString());
					deferredResult.setResult("order-show :: #orderscontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			} else {
				handleHTTPError(clientResponse, model);
				deferredResult.setResult("order-show :: #errormsg");
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order-show :: #errormsg");
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
	@RequestMapping(value = "/order-state/post")
	public DeferredResult<String> setState(
			@RequestParam(required = true, value = MAPKEY_ID) String id, 
			@RequestParam(required = true, value = "state") String state,
			@RequestParam(required = false, value = "facility") String facility,
			Model model,
			HttpServletResponse httpResponse) {
		if (logger.isTraceEnabled())
			logger.trace(">>> setState({}, {}, model)", id, state, facility);
		Mono<ClientResponse> mono = orderService.setState(id, state, facility);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		mono.timeout(Duration.ofMillis(config.getTimeout())).doOnError(e -> {
			model.addAttribute("warnmsg", e.getMessage());
			// deferredResult.setResult("order-show :: #warnmsg");
			deferredResult.setErrorResult(model.asMap().get("warnmsg"));
		}).subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is2xxSuccessful()) {
				if (clientResponse.statusCode().compareTo(HttpStatus.NO_CONTENT) == 0) {
					deferredResult.setResult("order-show :: #warnmsg");
					httpResponse.setHeader("warnstatus", "nocontent");
				} else {
					clientResponse.bodyToMono(HashMap.class).timeout(Duration.ofMillis(config.getTimeout())).subscribe(orderList -> {
						model.addAttribute("ord", orderList);
						logger.trace(model.toString() + "MODEL TO STRING");
						logger.trace(">>>>MONO" + orderList.toString());
						deferredResult.setResult("order-show :: #ordercontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				}
			} else {
				handleHTTPWarning(clientResponse, model, httpResponse);
				deferredResult.setResult("order-show :: #warnmsg");
				clientResponse.bodyToMono(String.class). subscribe(body -> {
					httpResponse.setHeader("warndesc", body);
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("warnmsg", e.getMessage());
			// deferredResult.setResult("order-show :: #warnmsg");
			deferredResult.setErrorResult(model.asMap().get("warnmsg"));
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	/**
	 * Set the new state of a job
	 * 
	 * @param id The job id
	 * @param state The new state
	 * @param model The model to hold the data
	 * @return The result
	 */
	@RequestMapping(value = "/job-state/post")
	public DeferredResult<String> setJobState(
			@RequestParam(required = true, value = MAPKEY_ID) String id, 
			@RequestParam(required = true, value = "state") String state,
			Model model,
			HttpServletResponse httpResponse) {
		if (logger.isTraceEnabled())
			logger.trace(">>> setState({}, {}, model)", id, state);
		Mono<ClientResponse> mono = orderService.setJobState(id, state);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		mono.timeout(Duration.ofMillis(config.getTimeout())).doOnError(e -> {
			model.addAttribute("warnmsg", e.getMessage());
			// deferredResult.setResult("order-show :: #warnmsg");
			deferredResult.setErrorResult(model.asMap().get("warnmsg"));
		}).subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is2xxSuccessful()) {
				if (clientResponse.statusCode().compareTo(HttpStatus.NO_CONTENT) == 0) {
					deferredResult.setResult("order-show :: #warnmsg");
					httpResponse.setHeader("warnstatus", "nocontent");
				} else {
					clientResponse.bodyToMono(HashMap.class).timeout(Duration.ofMillis(config.getTimeout())).subscribe(orderList -> {
						model.addAttribute("ord", orderList);
						logger.trace(model.toString() + "MODEL TO STRING");
						logger.trace(">>>>MONO" + orderList.toString());
						deferredResult.setResult("order-show :: #null");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				}
			} else {
				handleHTTPWarning(clientResponse, model, httpResponse);
				deferredResult.setResult("order-show :: #warnmsg");
				clientResponse.bodyToMono(String.class). subscribe(body -> {
					httpResponse.setHeader("warndesc", body);
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("warnmsg", e.getMessage());
			// deferredResult.setResult("order-show :: #warnmsg");
			deferredResult.setErrorResult(model.asMap().get("warnmsg"));
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}
	
	/**
	 * Set the new state of a job step
	 * 
	 * @param id The job step id
	 * @param state The new state
	 * @param model The model to hold the data
	 * @return The result
	 */
	@RequestMapping(value = "/jobstep-state/post")
	public DeferredResult<String> setJobStepState(
			@RequestParam(required = true, value = MAPKEY_ID) String id, 
			@RequestParam(required = true, value = "state") String state,
			Model model,
			HttpServletResponse httpResponse) {
		if (logger.isTraceEnabled())
			logger.trace(">>> setState({}, {}, model)", id, state);
		Mono<ClientResponse> mono = orderService.setJobStepState(id, state);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		mono.timeout(Duration.ofMillis(config.getTimeout())).doOnError(e -> {
			model.addAttribute("warnmsg", e.getMessage());
			// deferredResult.setResult("order-show :: #warnmsg");
			deferredResult.setErrorResult(model.asMap().get("warnmsg"));
		}).subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is2xxSuccessful()) {
				if (clientResponse.statusCode().compareTo(HttpStatus.NO_CONTENT) == 0) {
					deferredResult.setResult("order-show :: #warnmsg");
					httpResponse.setHeader("warnstatus", "nocontent");
				} else {
					clientResponse.bodyToMono(HashMap.class).timeout(Duration.ofMillis(config.getTimeout())).subscribe(orderList -> {
						model.addAttribute("job", orderList);
						logger.trace(model.toString() + "MODEL TO STRING");
						logger.trace(">>>>MONO" + orderList.toString());
						deferredResult.setResult("order-show :: #null");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				}
			} else {
				handleHTTPWarning(clientResponse, model, httpResponse);
				deferredResult.setResult("order-show :: #warnmsg");
				clientResponse.bodyToMono(String.class). subscribe(body -> {
					httpResponse.setHeader("warndesc", body);
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("warnmsg", e.getMessage());
			// deferredResult.setResult("order-show :: #warnmsg");
			deferredResult.setErrorResult(model.asMap().get("warnmsg"));
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}
    
	/**
	 * Retrieve a single order
	 * 
	 * @param id The order id.
	 * @param model The model to hold the data
	 * @return The result
	 */
	@RequestMapping(value = "/order/get")
	public DeferredResult<String> getId(
			@RequestParam(required = true, value = MAPKEY_ID) String id,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getId({}, model)", id);
    	checkClearCache();
		Mono<ClientResponse> mono = orderService.getId(id);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> orders = new ArrayList<>();
		mono.doOnError(e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order :: #errormsg");
		})
	 	.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(HashMap.class).subscribe(order -> {
					orders.add(order);
					model.addAttribute("orders", orders);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + orders.toString());
					deferredResult.setResult("order :: #ordercontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			} else {
				handleHTTPError(clientResponse, model);
				deferredResult.setResult("order :: #errormsg");
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order :: #errormsg");
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + orders.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}
	
	/**
	 * Retrieve a single order
	 * 
	 * @param id The order id.
	 * @param model The model to hold the data
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/order-edit/get")
	public DeferredResult<String> getIdForEdit(
			@RequestParam(required = true, value = MAPKEY_ID) String id,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getId({}, model)", id);
    	checkClearCache();
		Mono<ClientResponse> mono = orderService.getId(id);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> orders = new ArrayList<>();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		mono.doOnError(e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order-edit :: #errormsg");
		})
	 	.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(HashMap.class).subscribe(order -> {
					order.put("missionCode", mission);
					orders.add(order);
					model.addAttribute("orders", orders);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + orders.toString());
					deferredResult.setResult("order-edit :: #orderedit");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			} else {
				handleHTTPError(clientResponse, model);
				deferredResult.setResult("order-edit :: #errormsg");
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order-edit :: #errormsg");
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + orders.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}
	/**
	 * Retrieve a single order
	 * 
	 * @param id The order id.
	 * @param model The model to hold the data
	 * @return The result
	 */
	@RequestMapping(value = "/order-submit", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<OrderInfo> submitOrder(@RequestBody RestOrder updateOrder) {
		if (logger.isTraceEnabled())
			logger.trace(">>> order-submit({}, model)", updateOrder);
    	checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		// some checks on updateOrder
		if (updateOrder.getInputFilters() != null) {
			List<RestInputFilter> newList = new ArrayList<RestInputFilter>();
			for (RestInputFilter ele : updateOrder.getInputFilters()) {
				if (ele != null) {
					ele.setFilterConditions(stripNullInParameterList(ele.getFilterConditions()));
					newList.add(ele);
				}
			}
			updateOrder.setInputFilters(newList);
		}
		if (updateOrder.getClassOutputParameters() != null) {
			List<RestClassOutputParameter> newList = new ArrayList<RestClassOutputParameter>();
			for (RestClassOutputParameter ele : updateOrder.getClassOutputParameters()) {
				if (ele != null) {
					ele.setOutputParameters(stripNullInParameterList(ele.getOutputParameters()));
					newList.add(ele);
				}
			}
			updateOrder.setClassOutputParameters(newList);
		}
		if (updateOrder.getOutputParameters() != null) {
			updateOrder.setOutputParameters(stripNullInParameterList(updateOrder.getOutputParameters()));
		}
		if (updateOrder.getUuid() != null && updateOrder.getUuid().isEmpty()) {
			updateOrder.setUuid(null);
		}
		if (updateOrder.getProductRetentionPeriod() != null) {
			updateOrder.setProductRetentionPeriod(updateOrder.getProductRetentionPeriod() * 86400);
		}
		updateOrder.setStartTime(normStartEnd(updateOrder.getStartTime(), updateOrder.getSlicingType()));
		updateOrder.setStopTime(normStartEnd(updateOrder.getStopTime(), updateOrder.getSlicingType()));
		RestOrder origOrder = null;
		if (updateOrder.getId() != null && updateOrder.getId() > 0) {
			origOrder = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(), "/orders/" + updateOrder.getId(),
					RestOrder.class, auth.getProseoName(), auth.getPassword());
			if (origOrder != null) {
				if (origOrder.getOrderState().equals(OrderState.INITIAL.toString())) {
					try {
						origOrder = serviceConnection.patchToService(serviceConfig.getOrderManagerUrl(), "/orders/" + updateOrder.getId(),
								updateOrder, RestOrder.class, auth.getProseoName(), auth.getPassword());
						return new ResponseEntity<OrderInfo>(new OrderInfo(HttpStatus.OK, origOrder.getId().toString(), ""), HttpStatus.OK);
					} catch (RestClientResponseException e) {
						if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
						String message = null;
						switch (e.getRawStatusCode()) {
						case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
							return new ResponseEntity<OrderInfo>(new OrderInfo(HttpStatus.NOT_MODIFIED, "0", uiMsg(MSG_ID_NOT_MODIFIED)), HttpStatus.OK);
						case org.apache.http.HttpStatus.SC_NOT_FOUND:
							message = uiMsg(MSG_ID_ORDER_NOT_FOUND, updateOrder.getIdentifier());
							break;
						case org.apache.http.HttpStatus.SC_BAD_REQUEST:
							message = uiMsg(MSG_ID_ORDER_DATA_INVALID,  e.getMessage());
							break;
						case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
						case org.apache.http.HttpStatus.SC_FORBIDDEN:
							message = uiMsg(MSG_ID_NOT_AUTHORIZED, auth.getProseoName(), "orders", auth.getMission());
							break;
						default:
							message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
						}
						return new ResponseEntity<OrderInfo>(new OrderInfo(HttpStatus.INTERNAL_SERVER_ERROR, "0", message), errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
					} catch (RuntimeException e) {
						return new ResponseEntity<OrderInfo>(new OrderInfo(HttpStatus.INTERNAL_SERVER_ERROR, "0", uiMsg(MSG_ID_EXCEPTION, e.getMessage())), errorHeaders(uiMsg(MSG_ID_EXCEPTION, e.getMessage())), HttpStatus.INTERNAL_SERVER_ERROR);
					}
				} else {
					// order not in initial state, can't update, throw error
				}
			} else {
				// why does the order not exist any more??
				// TODO create new one?
			}
			
		} else {
			// this is a new order, create it
			// first check whether the identifier exists already (should be done by GUI, but anyway...)
			try {
				origOrder = serviceConnection.postToService(serviceConfig.getOrderManagerUrl(), "/orders",
						updateOrder, RestOrder.class, auth.getProseoName(), auth.getPassword());
				return new ResponseEntity<OrderInfo>(new OrderInfo(HttpStatus.CREATED, origOrder.getId().toString(), ""), HttpStatus.CREATED);
			} catch (RestClientResponseException e) {
				if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
				String message = null;
				switch (e.getRawStatusCode()) {
				case org.apache.http.HttpStatus.SC_BAD_REQUEST:
					message = uiMsg(MSG_ID_ORDER_DATA_INVALID,  e.getMessage());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				case org.apache.http.HttpStatus.SC_FORBIDDEN:
					message = uiMsg(MSG_ID_NOT_AUTHORIZED, auth.getProseoName(), "orders", auth.getMission());
					break;
				default:
					message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
				}
				System.err.println(message);
				return new ResponseEntity<OrderInfo>(new OrderInfo(HttpStatus.INTERNAL_SERVER_ERROR, "0", message), errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (RuntimeException e) {
				return new ResponseEntity<OrderInfo>(new OrderInfo(HttpStatus.INTERNAL_SERVER_ERROR, "0", uiMsg(MSG_ID_EXCEPTION, e.getMessage())), errorHeaders(uiMsg(MSG_ID_EXCEPTION, e.getMessage())), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}	
		
		return new ResponseEntity<OrderInfo>(new OrderInfo(HttpStatus.OK, "0", ""), HttpStatus.OK);
	}

	/**
	 * Retrieve the jobs of an order filtered by following criteria
	 * If fromIndex is not set the job or job step are used to calculate it.
	 * 
	 * @param id The order id
	 * @param fromIndex The from index (first row)
	 * @param toIndex The to index (last row)
	 * @param jobId The job id 
	 * @param jobStepId
	 * @param model
	 * @return The jobs found
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/jobs/get")
	public DeferredResult<String> getJobsOfOrder(
			@RequestParam(required = true, value = "orderid") String id,
			@RequestParam(required = false, value = "recordFrom") Long fromIndex,
			@RequestParam(required = false, value = "recordTo") Long toIndex,
			@RequestParam(required = false, value = "job") String jobId,
			@RequestParam(required = false, value = "jobStep") String jobStepId,
			@RequestParam(required = false, value = "jobstates") String states,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getId({}, model)", id);
    	checkClearCache();
		Long from = null;
		Long to = null;
		Boolean calcPage = false;
		if (fromIndex != null && fromIndex >= 0) {
			from = fromIndex;
		} else {
			from = (long) 0;
			if (jobId != null || jobStepId != null) {
				calcPage = true;
			}
		}
		Long count = countJobs(id, states);
		if (toIndex != null && from != null && toIndex > from) {
			to = toIndex;
		} else if (from != null) {
			to = count;
		}
		Long pageSize = to - from;
		if (calcPage) {
			Long page = pageOfJob(id, jobId, jobStepId, pageSize);
			from = page * pageSize;
			to = from + pageSize;
		}
		
		Long deltaPage = pageSize == 0L ? 0L : ((long) ((count % pageSize)==0?0:1));
		Long pages = pageSize == 0L ? 0L : ((count / pageSize) + deltaPage);
		Long page = pageSize == 0L ? 0L : ((from / pageSize) + 1);
		// TODO use jobId to find page of job 
		Mono<ClientResponse> mono = orderService.getJobsOfOrder(id, from, to, states);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> jobs = new ArrayList<>();
		mono.doOnError(e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order :: #errormsg");
		})
	 	.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().compareTo(HttpStatus.NOT_FOUND) == 0) {
				// this is no error cause only already planned orders have job steps
				model.addAttribute("jobs", jobs);
				model.addAttribute("count", 0);
				model.addAttribute("pageSize", 0);
				model.addAttribute("pageCount", 0);
				model.addAttribute("page", 0);
				deferredResult.setResult("order :: #jobscontent");
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
					model.addAttribute("count", count);
					model.addAttribute("pageSize", pageSize);
					model.addAttribute("pageCount", pages);
					model.addAttribute("page", page);
					List<Long> showPages = new ArrayList<Long>();
					Long start = Math.max(page - 4, 1);
					Long end = Math.min(page + 4, pages);
					if (page < 5) {
						end = Math.min(end + (5 - page), pages);
					}
					if (pages - page < 5) {
						start = Math.max(start - (4 - (pages - page)), 1);
					}
					for (Long i = start; i <= end; i++) {
						showPages.add(i);
					}
					model.addAttribute("showPages", showPages);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + jobs.toString());
					deferredResult.setResult("order :: #jobscontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			} else {
				handleHTTPError(clientResponse, model);
				deferredResult.setResult("order :: #errormsg");
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order :: #errormsg");
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + jobs.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	@RequestMapping(value = "/jobs/graph")
	public DeferredResult<String> getGraphOfJob(
			@RequestParam(required = true, value = "jobid") String id,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getId({}, model)", id);
    	checkClearCache();
		Mono<ClientResponse> mono = orderService.getGraphOfJob(id);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> jobs = new ArrayList<>();
		mono.doOnError(e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order :: #errormsg");
		})
	 	.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(HashMap.class).subscribe(jobgraph -> {
					jobs.add(jobgraph);
					model.addAttribute("jobgraph", jobs);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + jobs.toString());
					deferredResult.setResult("order :: #jobgraph");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			} else {
				handleHTTPError(clientResponse, model);
				deferredResult.setResult("order :: #errormsg");
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("order :: #errormsg");
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + jobs.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}
	
	@GetMapping("/hasorbits")
	public ResponseEntity<?> hasorbits(@RequestParam(required = true, value = "spacecraft") String spacecraft,
			@RequestParam(required = true, value = "from") Long from,
			@RequestParam(required = true, value = "to") Long to){
		Boolean result = countOrbits(spacecraft, from, to) > 0;
	    return new ResponseEntity<>(result.toString(), HttpStatus.OK);
	}

	@GetMapping("/hasorder")
	public ResponseEntity<?> hasorder(@RequestParam(required = true, value = "identifier") String identifier,
			@RequestParam(required = true, value = "nid") String id){
		Boolean result = countOrders(identifier, id) > 0;
	    return new ResponseEntity<>(result.toString(), HttpStatus.OK);
	}
	
	@GetMapping("/jobsteplog.txt")
	public ResponseEntity<String> jobsteplog(@RequestParam(required = true, value = "id") String id){

		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String result = "";
		RestJobStep js;

		try {
			js = serviceConnection.getFromService(config.getOrderManager(),
					"/orderjobsteps/" + id, RestJobStep.class, auth.getProseoName(), auth.getPassword());
			if (js !=  null) {
				if (js.getJobStepState() == JobStepState.RUNNING) {
					// update log file before, this does the planner 
					js = serviceConnection.getFromService(config.getProductionPlanner(),
							"/jobsteps/" + id, RestJobStep.class, auth.getProseoName(), auth.getPassword());
				}
			}
			if (js !=  null) {
				result = js.getProcessingStdOut();
			}
			if (result == null) {
				result = "";
			}
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
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
		}
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("Content-Type", "text/plain");
		map.add("Content-Length", String.valueOf(result.length()));
	    return new ResponseEntity<>(result, map, HttpStatus.OK);
	}

	private List<RestParameter> stripNullInParameterList(List<RestParameter> list) {
		List<RestParameter> newList = new ArrayList<RestParameter>();
		if (list!= null) {
			for (RestParameter p : list) {
				if (p != null) {
					newList.add(p);
				}
			}
		}
		return newList;
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
//	private List<Object> selectOrders(List<Object> orderList, String identifier, String states, String from, String to, String products) {
//		List<Object> result = new ArrayList<Object>();
//		if (orderList != null && (identifier != null || states != null || from != null || to != null || products != null)) {
//			String myident = null;
//			if (identifier != null) {
//				// at the moment only simple pattern are supported, replace '*'
//				myident = identifier.replaceAll("[*]", ".*");
//			}
//			ArrayList<String> stateArray = null;
//			if (states != null) {
//				// build array of selected states
//				stateArray = new ArrayList<String>();
//				String[] x = states.split(":");				
//				for (int i = 0; i < x.length; i++) {
//					stateArray.add(x[i]);
//				}
//			}
//			ArrayList<String> productArray = null;
//			if (products != null) {
//				productArray = new ArrayList<String>();
//				String[] x = products.split(":");		
//				for (int i = 0; i < x.length; i++) {
//					productArray.add(x[i]);
//				}
//			}
//			Date fromTime = null;
//			if (from != null) {			
//				try {
//					fromTime = simpleDateFormatter.parse(from);
//				} catch (ParseException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			Date toTime = null;
//			if (to != null) {		
//				try {
//					toTime = simpleDateFormatter.parse(to);
//					Instant t = toTime.toInstant().plus(Duration.ofDays(1));
//					toTime = Date.from(t);
//				} catch (ParseException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			ObjectMapper mapper = new ObjectMapper();
//			Boolean found = true;
//			for (Object o : orderList) {
//				found = true;
//				try {
//					RestOrder order =  mapper.convertValue(o, RestOrder.class);
//					if (order != null) {
//						if (found && identifier != null) {
//							if (!order.getIdentifier().matches(myident)) found = false;					
//						}
//						if (stateArray != null) {
//							if (!stateArray.contains(order.getOrderState())) found = false;
//						}
//						if (fromTime != null && order.getStartTime() != null) {
//							if (fromTime.compareTo(Date.from(Instant.from(OrbitTimeFormatter.parse(order.getStartTime())))) > 0) found = false;
//						}
//						if (toTime != null && order.getStopTime() != null) {
//							if (toTime.compareTo(Date.from(Instant.from(OrbitTimeFormatter.parse(order.getStopTime())))) < 0) found = false;
//						}
//						if (productArray != null && order.getRequestedProductClasses() != null) {
//							Boolean lfound = false;
//							for (String pc : order.getRequestedProductClasses()) {
//								if (productArray.contains(pc)) {
//									lfound = true;
//									break;
//								}
//							}
//							if (!lfound) found = false;
//						}
//					}
//					if (found) {
//						result.add(o);
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			
//		} else {
//			return orderList;
//		}
//		return result;
//	}

    /**
     * Count the number of jobs in an order.
     * 
     * @param id The order id
     * @return The number of jobs
     */
    private Long countJobs(String id, String states)  {	    	
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String uri = "/orderjobs/count";
		String divider = "?";
		if (id != null) {
			uri += divider + "orderid=" + id;
			divider ="&";
		}
		if (states != null && !states.isEmpty()) {
			String [] pcs = states.split(":");
			for (String pc : pcs) {
				uri += divider + "state=" + pc;
				divider ="&";
			}
		}
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					uri, String.class, auth.getProseoName(), auth.getPassword());

			if (resStr != null && resStr.length() > 0) {
				result = Long.valueOf(resStr);
			}
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
			return result;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return result;
		}
		
        return result;
    }
    
    /**
     * Get the display page of a job in an order
     * 
     * @param orderId The order id
     * @param jobId The job id
     * @param jobStepId The job step id
     * @param pageSize The page six
     * @return The page number
     */
    private Long pageOfJob(String orderId, String jobId, String jobStepId, Long pageSize) {    	
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String uri = "/orderjobs/index";
		String divider = "?";
		if (orderId != null) {
			uri += divider + "orderid=" + orderId;
			divider ="&";
		}
		if (jobId != null) {
			uri += divider + "jobid=" + jobId;
			divider ="&";
		}
		if (jobStepId != null) {
			uri += divider + "jobstepid=" + jobStepId;
			divider ="&";
		}
		divider ="&";
		try {
			uri += divider + "orderBy=startTime"  + URLEncoder.encode(" ASC", "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// should not be, but in case of
			e1.printStackTrace();
		}
		Long result = (long) 0;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					uri, String.class, auth.getProseoName(), auth.getPassword());

			if (resStr != null && resStr.length() > 0) {
				result = Long.valueOf(resStr);
				// this is the row index in complete list, now calculate the page
				result = (result) / pageSize;
			}
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
			return result;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return result;
		}
		
        return result;
    }

    /**
     * Count the existing orbits of a spacecraft between from and to.
     * 
     * @param spacecraft The spacecraft code
     * @param from Orbit number from
     * @param to Orbit number to
     * @return The orbit count
     */
    private Long countOrbits(String spacecraft, Long from, Long to)  {	    	
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String uri = "/orbits/count";
		String divider = "?";
		if (spacecraft != null) {
			uri += divider + "spacecraftCode=" + spacecraft;
			divider ="&";
		}
		if (from != null) {
			uri += divider + "orbitNumberFrom=" + from;
			divider ="&";
		}
		if (to != null) {
			uri += divider + "orbitNumberTo=" + to;
			divider ="&";
		}
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					uri, String.class, auth.getProseoName(), auth.getPassword());

			if (resStr != null && resStr.length() > 0) {
				result = Long.valueOf(resStr);
			}
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
			return result;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return result;
		}
		
        return result;
    }

    /**
     * Count the number of orders
     * 
     * @param orderName
     * @param nid
     * @return The number of orbits
     */
    public Long countOrders(String orderName, String nid) {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
    	Long result = (long) -1;
    	String mission = auth.getMission();
    	String uri = "/orders/count";
    	String divider = "?";
    	if(null != mission) {
    		uri += divider + "mission=" + mission;
    		divider = "&";
    	}
    	if (null != orderName && !orderName.trim().isEmpty()) {
    		uri += divider + "identifier=" + orderName.trim();
    	}
    	if (null != nid && !nid.trim().isEmpty()) {
    		uri += divider + "nid=" + nid.trim();
    	}

    	try {
    		String resStr = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
    				uri, String.class, auth.getProseoName(), auth.getPassword());

    		if (resStr != null && resStr.length() > 0) {
    			result = Long.valueOf(resStr);
    		}
    	} catch (RestClientResponseException e) {
    		String message = null;
    		switch (e.getRawStatusCode()) {
    		case org.apache.http.HttpStatus.SC_NOT_FOUND:
    			break;
    		case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
    		case org.apache.http.HttpStatus.SC_FORBIDDEN:
    			message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
    			break;
    		default:
    			message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
    		}
    		System.err.println(message);
    		return result;
    	} catch (RuntimeException e) {
    		System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
    		return result;
    	}

    	return result;
    }

	/** Count the orders specified by following parameters
	 * 
	 * @param identifier Identifier pattern
	 * @param states The states (divided by ':')
	 * @param products The product classes (divided by ':')
	 * @param from The earliest start time
	 * @param to The latest start time
	 * @param recordFrom 
	 * @param recordTo
	 * @param sortCol The sort criteria
	 * @param up Ascending if true, otherwise descending
	 * @return The number of orders found
	 */
	public Long countOrdersL(String identifier, String states, String products, String from, String to, 
			Long recordFrom, Long recordTo, String sortCol, Boolean up) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = "/orders/countselect";
		
		String divider = "?";
		if (mission != null && !mission.isEmpty()) {
			uri += divider + "mission=" + mission;
			divider ="&";
		}
		if (identifier != null && !identifier.isEmpty()) {
			try {
				uri += divider + "identifier=" + URLEncoder.encode(identifier.replaceAll("[*]", "%"), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				logger.error(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			}
			divider ="&";
		}
		if (states != null && !states.isEmpty()) {
			String [] pcs = states.split(":");
			for (String pc : pcs) {
				uri += divider + "state=" + pc;
				divider ="&";
			}
		}

		if (states != null && !states.isEmpty()) {
			String [] pcs = states.split(":");
			for (String pc : pcs) {
				uri += divider + "state=" + pc;
				divider ="&";
			}
		}
		if (products != null && !products.isEmpty()) {
			String [] pcs = products.split(":");
			for (String pc : pcs) {
				uri += divider + "productClass=" + pc;
				divider ="&";
			}
		}
		if (from != null && !from.isEmpty()) {
			uri += divider + "startTime=" + from;
			divider ="&";
		}
		if (to != null && !to.isEmpty()) {
			uri += divider + "stopTime=" + to;
			divider ="&";
		}
		if (recordFrom != null) {
			uri += divider + "recordFrom=" + recordFrom;
			divider ="&";
		}
		if (recordTo != null) {
			uri += divider + "recordTo=" + recordTo;
			divider ="&";
		}
		if (sortCol != null && !sortCol.isEmpty()) {
			uri += divider + "orderBy=" + sortCol;
			if (up != null && !up) {
				try {
					uri += URLEncoder.encode(" DESC", "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					uri += "%20DESC";
					logger.error(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				}
			} else {
				try {
					uri += URLEncoder.encode(" ASC", "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					uri += "%20ASC";
					logger.error(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				}
			}
			divider ="&";
		}

		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					uri, String.class, auth.getProseoName(), auth.getPassword());

			if (resStr != null && resStr.length() > 0) {
				result = Long.valueOf(resStr);
			}
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
			return result;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return result;
		}
		return result;
		
	}
	
    /**
     * Normalize the date string
     * 
     * @param se The input date string
     * @param type The type of it
     * @return The normalized date string
     */
    private String normStartEnd(String se, String type) {
    	String val = se;
    	if (se != null) {
    		if (type.equalsIgnoreCase(OrderSlicingType.CALENDAR_DAY.toString())) {
    			val = val + "T00:00:00.000000";
    		} else if (type.equalsIgnoreCase(OrderSlicingType.CALENDAR_MONTH.toString())) {
    			val = val + "-01T00:00:00.000000";
    		} else if (type.equalsIgnoreCase(OrderSlicingType.CALENDAR_YEAR.toString())) {
    			val = val + "-01-01T00:00:00.000000";
    		}
    	}
        return val;
    }
}

