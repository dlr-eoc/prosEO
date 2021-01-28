package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_MODIFIED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_PRODUCTCLASSES_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_ORDER_DATA_INVALID;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_ORDER_NOT_FOUND;
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
import org.springframework.boot.actuate.trace.http.HttpTrace.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.service.MapComparator;
import de.dlr.proseo.ui.gui.service.OrderService;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import de.dlr.proseo.model.rest.model.RestProcessingFacility;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.rest.model.RestClassOutputParameter;
import de.dlr.proseo.model.rest.model.RestConfiguredProcessor;
import de.dlr.proseo.model.rest.model.RestInputFilter;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.rest.model.RestProductClass;
import de.dlr.proseo.model.rest.model.RestSpacecraft;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

@Controller
public class GUIOrderController extends GUIBaseController {
	private static final String MAPKEY_ID = "id";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIOrderController.class);
	
	/**
	 * List with cached data
	 */
	private List<String> facilities = null;
	/**
	 * List with cached data
	 */
	private List<String> productclasses = null;
	/**
	 * List with cached data
	 */
	private List<String> configuredProcessors = null;
	/**
	 * List with cached data
	 */
	private List<String> fileClasses = null;
	/**
	 * List with cached data
	 */
	private List<String> processingModes = null;
	/**
	 * List with cached data
	 */
	private List<String> spaceCrafts = null;
	/**
	 * List with cached data
	 */
	private List<String> productiontypes = null;
	/**
	 * List with cached data
	 */
	private List<String> slicingtypes = null;
	/**
	 * List with cached data
	 */
	private List<String> parametertypes = null;
			
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
		ModelAndView modandview = new ModelAndView("order-show");
		modandview.addObject("message", "TEST");
		return "order-show";
	}
	@GetMapping(value ="/order-edit")
	public String editOrder() {
		ModelAndView modandview = new ModelAndView("order-edit");
		return "order-edit";
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
		// clearCache();
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
     * @return String list
     */
    @ModelAttribute("facilities")
    public List<String> facilities() {
    	if (facilities != null && !facilities.isEmpty()) return facilities;
    	
    	facilities = new ArrayList<String>();   
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
     * @return String list
     */
    @ModelAttribute("productclasses")
    public List<String> productclasses() {
    	if (productclasses != null && !productclasses.isEmpty()) return productclasses;
    	
    	productclasses = new ArrayList<String>();   
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
     * Retrieve the configured processors of mission
     * 
     * @return String list
     */
    @ModelAttribute("configuredprocessors")
    public List<String> configuredProcessors() {
    	if (configuredProcessors != null && !configuredProcessors.isEmpty()) return configuredProcessors;
    	
    	configuredProcessors = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					"/configuredprocessors?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
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
			return configuredProcessors;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return configuredProcessors;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestConfiguredProcessor restConfiguredProcessorClass = mapper.convertValue(object, RestConfiguredProcessor.class);
				configuredProcessors.add(restConfiguredProcessorClass.getIdentifier());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		configuredProcessors.sort(c);
        return configuredProcessors;
    }
    
    /**
     * Retrieve the file classes of mission
     * 
     * @return String list
     */
    @ModelAttribute("fileclasses")
    public List<String> fileClasses() {
    	if (fileClasses != null && !fileClasses.isEmpty()) return fileClasses;
    	
    	fileClasses = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
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
			return fileClasses;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return fileClasses;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission =  mapper.convertValue(resultList.get(0), RestMission.class);
				fileClasses.addAll(restMission.getFileClasses());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		fileClasses.sort(c);
        return fileClasses;
    }
    
    /**
     * Retrieve the processing modes of mission
     * 
     * @return String list
     */
    @ModelAttribute("processingmodes")
    public List<String> processingModes() {
    	if (processingModes != null && !processingModes.isEmpty()) return processingModes;
    	
    	processingModes = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
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
			return processingModes;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return processingModes;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission =  mapper.convertValue(resultList.get(0), RestMission.class);
				processingModes.addAll(restMission.getProcessingModes());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		processingModes.sort(c);
        return processingModes;
    }

    /**
     * Retrieve the space crafts of mission
     * 
     * @return String list
     */
    @ModelAttribute("spacecrafts")
    public List<String> spaceCrafts() {
    	if (spaceCrafts != null && !spaceCrafts.isEmpty()) return spaceCrafts;
    	
    	spaceCrafts = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
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
			return spaceCrafts;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return spaceCrafts;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission =  mapper.convertValue(resultList.get(0), RestMission.class);
				for (RestSpacecraft spaceCraft : restMission.getSpacecrafts()) {
					spaceCrafts.add(spaceCraft.getCode());
				}
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		spaceCrafts.sort(c);
        return spaceCrafts;
    }

    /**
     * Retrieve the production type enum
     * 
     * @return String list
     */
    @ModelAttribute("productiontypes")
    public List<String> productiontypes() {
    	if (productiontypes != null && !productiontypes.isEmpty()) return productiontypes;
    	
    	productiontypes = new ArrayList<String>(); 
    	for (ProductionType value: ProductionType.values()) {
    		productiontypes.add(value.toString());
    	}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		productiontypes.sort(c);
        return productiontypes;
    }

    /**
     * Retrieve the slicing type enum
     * 
     * @return String list
     */
    @ModelAttribute("slicingtypes")
    public List<String> slicingtypes() {
    	if (slicingtypes != null && !slicingtypes.isEmpty()) return slicingtypes;
    	
    	slicingtypes = new ArrayList<String>(); 
    	for (OrderSlicingType value: OrderSlicingType.values()) {
    		slicingtypes.add(value.toString());
    	}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		slicingtypes.sort(c);
        return slicingtypes;
    }

    /**
     * Retrieve the parameter type enum
     * 
     * @return String list
     */
    @ModelAttribute("parametertypes")
    public List<String> parametertypes() {
    	if (parametertypes != null && !parametertypes.isEmpty()) return parametertypes;
    	
    	parametertypes = new ArrayList<String>(); 
    	for (ParameterType value: ParameterType.values()) {
    		parametertypes.add(value.toString());
    	}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		parametertypes.sort(c);
        return parametertypes;
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
		// clearCache();
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
		// clearCache();
		Mono<ClientResponse> mono = orderService.getId(id);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> orders = new ArrayList<>();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("order-edit :: #orderedit");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("order-edit :: #orderedit");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(HashMap.class).subscribe(order -> {
					order.put("missionCode", mission);
					orders.add(order);
					model.addAttribute("orders", orders);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + orders.toString());
					deferredResult.setResult("order-edit :: #orderedit");
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
	 * Retrieve a single order
	 * 
	 * @param id The order id.
	 * @param model The model to hold the data
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/order-submit", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<OrderInfo> submitOrder(@RequestBody RestOrder updateOrder) {
		if (logger.isTraceEnabled())
			logger.trace(">>> order-submit({}, model)", updateOrder);
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
			@RequestParam(required = false, value = "recordFrom") Long fromIndex,
			@RequestParam(required = false, value = "recordTo") Long toIndex,
			@RequestParam(required = false, value = "job") String jobId,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getId({}, model)", id);
		Long from = null;
		Long to = null;
		if (fromIndex != null && fromIndex >= 0) {
			from = fromIndex;
		} else {
			from = (long) 0;
		}
		Long count = countJobs(id);
		if (toIndex != null && from != null && toIndex > from) {
			to = toIndex;
		} else if (from != null) {
			to = count;
		}
		Long pageSize = to - from;
		Long deltaPage = (long) ((count % pageSize)==0?0:1);
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;
		// TODO use jobId to find page of job 
		Mono<ClientResponse> mono = orderService.getJobsOfOrder(id, from, to);
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
							if (fromTime.compareTo(Date.from(Instant.from(OrbitTimeFormatter.parse(order.getStartTime())))) > 0) found = false;
						}
						if (toTime != null && order.getStopTime() != null) {
							if (toTime.compareTo(Date.from(Instant.from(OrbitTimeFormatter.parse(order.getStopTime())))) < 0) found = false;
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

    private Long countJobs(String id)  {	    	
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = "/jobs/count";
		String divider = "?";
		if (id != null) {
			uri += divider + "orderid=" + id;
			divider ="&";
		}
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getProductionPlannerUrl(),
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
    

    private Long countOrbits(String spacecraft, Long from, Long to)  {	    	
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
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

	private void clearCache() {
		configuredProcessors = null;
		facilities = null;
		productclasses = null;
		spaceCrafts = null;
		fileClasses = null;
		processingModes = null;
	}
}

