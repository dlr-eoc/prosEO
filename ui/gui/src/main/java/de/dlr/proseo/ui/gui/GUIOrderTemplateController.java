/**
 * GUIOrderTemplateController.java
 *
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import reactor.netty.http.client.HttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * A controller for retrieving and handling order data
 *
 * @author David Mazo
 */
@Controller
public class GUIOrderTemplateController extends GUIBaseController {
	private static final String MAPKEY_ID = "id";

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIOrderTemplateController.class);

	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** Date formatter for input type date */
	// private static final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

	/** HTTP Warning header */
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

	/**
	 * Show the order view
	 *
	 * @return the name of the order view template
	 */
	@GetMapping(value = "/order-template-show")
	public String showOrderTemplate() {
		return "order-template-show";
	}

	/**
	 * Retrieve the order list filtered by the specified parameters.
	 *
	 * @param name The order name (name) as a pattern.
	 * @param states     The order states (separated by ':').
	 * @param from       The start date of the time period.
	 * @param to         The end date of the time period.
	 * @param products   The product class list (separated by ':').
	 * @param recordFrom the first record to retrieve
	 * @param recordTo   the last record to retrieve
	 * @param sortby     The sort column.
	 * @param up         The sort direction (true for 'up').
	 * @param model      The model to hold the data.
	 * @return The deferred result containing the result.
	 */
	@GetMapping("/order-template-show/get")
	public DeferredResult<String> getIdentifier(@RequestParam(required = false, value = "name") String name,
			@RequestParam(required = false, value = "products") String products,
			@RequestParam(required = false, value = "recordFrom") Long recordFrom,
			@RequestParam(required = false, value = "recordTo") Long recordTo,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getIdentifier({}, {}, model)", name, name);

		checkClearCache();

		Long from = null;
		Long to = null;

		if (recordFrom != null && recordFrom >= 0) {
			from = recordFrom;
		} else {
			from = (long) 0;
		}

		Long count = countOrderTemplates(name, products, null, null, sortby, up);

		if (recordFrom != null && from != null && recordTo > from) {
			to = recordTo;
		} else if (from != null) {
			to = count;
		}

		Long pageSize = to - from;
		long deltaPage = (count % pageSize) == 0 ? 0 : 1;
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;

		// Perform the HTTP request to retrieve orders
		ResponseSpec responseSpec = get(name, products, from, to, sortby, up);
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> ordertemplates = new ArrayList<>();

		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				if (e instanceof WebClientResponseException.NotFound) {
					model.addAttribute("ordertemplates", ordertemplates);

					modelAddAttributes(model, count, pageSize, pages, page);

					if (logger.isTraceEnabled())
						logger.trace(model.toString() + "MODEL TO STRING");

					deferredResult.setResult("order-template-show :: #ordertemplatescontent");
				} else {
					model.addAttribute("errormsg", e.getMessage());
					deferredResult.setResult("order-template-show :: #errormsg");
				}
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);

				if (entityList.getStatusCode().is2xxSuccessful() 
						|| entityList.getStatusCode().value() ==  HttpStatus.NOT_FOUND.value()) {
					// orders.addAll(selectOrderTemplates(orderList, name, states, from, to, products));
					ordertemplates.addAll(entityList.getBody());

					String key = MAPKEY_ID;
					if (sortby != null) {
						if (sortby.contentEquals("name") || sortby.contentEquals(MAPKEY_ID)
								|| sortby.contentEquals("orderState")) {
							key = sortby;
						}
					}
					Boolean isUp = (null == up ? true : up);

					// Sort the orders based on the selected key and sort direction
					// MapComparator oc = new MapComparator(key, isUp);
					// orders.sort(oc);

					model.addAttribute("ordertemplates", ordertemplates);
					model.addAttribute("selcol", key);
					model.addAttribute("selorder", (isUp ? "select-up" : "select-down"));
					
					modelAddAttributes(model, count, pageSize, pages, page);

					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + ordertemplates.toString());
																	  
					deferredResult.setResult("order-template-show :: #ordertemplatescontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("order-template-show :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("order-template-show :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + ordertemplates.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEFERRED STRING: {}", deferredResult);

		return deferredResult;
	}

	/**
	 * Count the orders specified by following parameters
	 *
	 * @param name Identifier pattern
	 * @param states     The states (divided by ':')
	 * @param products   The product classes (divided by ':')
	 * @param from       The earliest start time
	 * @param to         The latest start time
	 * @param recordFrom the first record to retrieve
	 * @param recordTo   the last record to retrieve
	 * @param sortCol    The sort criteria
	 * @param up         Ascending if true, otherwise descending
	 * @return The number of orders found
	 */
	public Long countOrderTemplates(String name, String products, Long recordFrom,
			Long recordTo, String sortCol, Boolean up) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uriString = "/ordertemplates/count";

		String divider = "?";
		if (mission != null && !mission.isEmpty()) {
			uriString += divider + "mission=" + mission;
			divider = "&";
		}
		if (name != null && !name.isEmpty()) {
			String nameQueryParam = name.replaceAll("[*]", "%").trim();
			uriString += divider + "name=" + nameQueryParam;
			divider = "&";
		}
		if (products != null && !products.isEmpty()) {
			String[] pcs = products.split(":");
			for (String pc : pcs) {
				uriString += divider + "productClasses=" + pc;
				divider = "&";
			}
		}
		if (recordFrom != null) {
			uriString += divider + "recordFrom=" + recordFrom;
			divider = "&";
		}
		if (recordTo != null) {
			uriString += divider + "recordTo=" + recordTo;
			divider = "&";
		}
		if (sortCol != null && !sortCol.isEmpty()) {
			uriString += divider + "orderBy=" + sortCol;
			if (up != null && !up) {
				try {
					uriString += URLEncoder.encode(" DESC", "UTF-8");
				} catch (UnsupportedEncodingException e) {
					uriString += "%20DESC";
					logger.log(UIMessage.EXCEPTION, e.getMessage());
				}
			} else {
				try {
					uriString += URLEncoder.encode(" ASC", "UTF-8");
				} catch (UnsupportedEncodingException e) {
					uriString += "%20ASC";
					logger.log(UIMessage.EXCEPTION, e.getMessage());
				}
			}
			divider = "&";
		}

		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(), uri.toString(), String.class,
					auth.getProseoName(), auth.getPassword());

			if (resStr != null && resStr.length() > 0) {
				result = Long.valueOf(resStr);
			}
		} catch (RestClientResponseException e) {

			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return result;
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return result;
		}
		return result;

	}
	/**
	 * Retrieves a list of orders based on various search parameters
	 *
	 * @param identifier    the order identifier
	 * @param products      the product
	 * @param recordFrom    the first result to return
	 * @param recordTo      the last result to return
	 * @param sortCol       the column on which to base the sorting
	 * @param up            true if the sorting should be ascending, false if it should be descending
	 * @return a ResponseSpec; providing access to the response status and headers, and as well as methods to consume the response
	 *         body
	 */
	public ResponseSpec get(String name, String products, 
			Long recordFrom, Long recordTo, String sortCol, Boolean up) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build the request URI
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(config.getOrderManager()).path("/ordertemplates");

		if (mission != null && !mission.isBlank()) {
			uriBuilder.queryParam("mission", mission.trim());
		}
		if (name != null && !name.isBlank()) {
			String identifierQueryParam = name.replaceAll("[*]", "%");
			uriBuilder.queryParam("name", identifierQueryParam.trim());
		}
		if (products != null && !products.isBlank()) {
			String[] productsQueryParam = products.split(":");
			uriBuilder.queryParam("productClasses", (Object[]) productsQueryParam);
		}
		if (recordFrom != null) {
			uriBuilder.queryParam("recordFrom", recordFrom);
		}
		if (recordTo != null) {
			uriBuilder.queryParam("recordTo", recordTo);
		}
		if (sortCol != null && !sortCol.isBlank()) {
			String orderByQueryParam = sortCol + (up != null && up ? " ASC" : " DESC");
			uriBuilder.queryParam("orderBy", orderByQueryParam);
		}

		URI uri = uriBuilder.build().toUri();
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));

		// The returned ResponseSpec can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

}