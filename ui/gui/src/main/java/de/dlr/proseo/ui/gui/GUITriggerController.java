/**
 * GUITriggerController.java
 *
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.netty.http.client.HttpClient;

/**
 * A controller for retrieving and handling triggers
 *
 * @author Ernst Melchinger
 */
@Controller
public class GUITriggerController extends GUIBaseController {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUITriggerController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Show the trigger view
	 *
	 * @return the name of the trigger view template
	 */
	@GetMapping("/trigger-show")
	public String showTrigger() {
		return "trigger-show";
	}


	/**
	 * Fetch and return triggers from the processor manager
	 *
	 * @param id                the workflow id
	 * @param name              the workflow name
	 * @param workflowVersion   the workflow version
	 * @param inputProductClass the input product class
	 * @param recordFrom        the first record to retrieve
	 * @param recordTo          the last record to retrieve
	 * @param currentPage       the current page (needed for paging logic)
	 * @param pageSize          the page size (needed for paging logic)
	 * @param model             the attributes to return
	 * @return the result
	 */
	@GetMapping("/trigger/get")
	public DeferredResult<String> getTriggers(
			@RequestParam(required = false, value = "type") String type,
			@RequestParam(required = false, value = "name") String name,
			@RequestParam(required = false, value = "workflow") String workflow,
			@RequestParam(required = false, value = "inputProductClass") String inputProductClass,
			@RequestParam(required = false, value = "outputProductClass") String outputProductClass,
			@RequestParam(required = false, value = "recordFrom") Long recordFrom,
			@RequestParam(required = false, value = "recordTo") Long recordTo,
			@RequestParam(required = false, value = "currentPage") Long currentPage,
			@RequestParam(required = false, value = "pageSize") Long pageSize,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {

		logger.trace(">>> getTriggers({}, {}, {}, {}, {}, model)", type, name, workflow, inputProductClass, outputProductClass, recordFrom, recordTo);

		DeferredResult<String> deferredResult = new DeferredResult<>();

		// Count triggers
		final Long count = countTriggers(type, name, workflow, inputProductClass, outputProductClass);

		Long from = null;
		Long to = null;
		if (recordFrom != null && recordFrom >= 0) {
			from = recordFrom;
		} else {
			from = (long) 0;
		}
		if (recordTo != null && from != null && recordTo > from) {
			to = recordTo;
		} else if (from != null) {
			to = count;
		}
		Long aPageSize = to - from;
		long deltaPage = (count % pageSize) == 0 ? 0 : 1;
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;

		makeGetRequest(type, name, workflow, inputProductClass, outputProductClass, recordFrom, recordTo, sortby, up).toEntityList(Object.class)
			.subscribe(

					// In case of success, handle HTTP response
					clientResponse -> {

						logger.trace("Now in Consumer::accept({})", clientResponse);

						if (clientResponse.getStatusCode().is2xxSuccessful()) {

							if (clientResponse.getBody() instanceof Collection) {
								
								// If no ID was provided, several triggers may be retrieved
								// Fill model with attributes to return
								model.addAttribute("triggers", clientResponse.getBody());

								modelAddAttributes(model, count, aPageSize, pages, page);

								if (logger.isTraceEnabled())
									logger.trace(model.toString() + "MODEL TO STRING");
								if (logger.isTraceEnabled())
									logger.trace(">>>>MONO" + clientResponse.getBody().toString());
								deferredResult.setResult("trigger-show :: #triggercontent");

								logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
								
							} else {
								
								// If an ID was provided, only one trigger is retrieved

								List<Object> triggers = new ArrayList<>();
								triggers.add(clientResponse.getBody());

								model.addAttribute("triggers", triggers);
								modelAddAttributes(model, count, aPageSize, pages, page);

								if (logger.isTraceEnabled())
									logger.trace(model.toString() + "MODEL TO STRING");
								if (logger.isTraceEnabled())
									logger.trace(">>>>MONO" + triggers.toString());

								deferredResult.setResult("trigger-show :: #triggercontent");
								logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());

							}

						} else {
							// Handle and display HTTP error
							ClientResponse errorResponse = ClientResponse.create(clientResponse.getStatusCode())
								.headers(headers -> headers.addAll(clientResponse.getHeaders()))
								.build();
							handleHTTPError(errorResponse, model);

							deferredResult.setResult("trigger-show :: #errormsg");
						}

						logger.trace(">>>>MODEL" + model.toString());

					},

					// In case of errors, display an error message
					error -> {
						if (error instanceof WebClientResponseException.NotFound) {
							model.addAttribute("triggers", new ArrayList());

							modelAddAttributes(model, count, pageSize, 1L, 1L);

							if (logger.isTraceEnabled())
								logger.trace(model.toString() + "MODEL TO STRING");

							deferredResult.setResult("trigger-show :: #triggercontent");
						} else {
							model.addAttribute("errormsg", error.getMessage());
							deferredResult.setResult("trigger-show :: #errormsg");
						}
					}

			);

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		return deferredResult;
	}

	/**
	 * Retrieve the number of triggers matching a given name, trigger version, and input product class.
	 *
	 * @param name              the trigger name
	 * @param triggerVersion   the trigger version
	 * @param inputProductClass the input product class
	 * @return the number of triggers with the respective name, version, and input product class
	 */
	private Long countTriggers(String type, String name, String workflow, String inputProductClass, String outputProductClass) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build request URI
		String divider = "?";
		String uriString = "/triggers/count";

		if (mission != null && !mission.isEmpty()) {
			uriString += divider + "mission=" + mission;
			divider = "&";
		}
		if (type != null && !type.isEmpty()) {
			String typeParam = type;
			uriString += divider + "type=" + typeParam;
			divider = "&";
		}
		if (name != null && !name.isEmpty()) {
			String nameParam = name.replaceAll("[*]", "%");
			uriString += divider + "name=" + nameParam.toUpperCase();
			divider = "&";
		}
		if (workflow != null && !workflow.isEmpty()) {
			String workflowParam = workflow.replaceAll("[*]", "%");
			uriString += divider + "workflow=" + workflowParam;
			divider = "&";
		}
		if (inputProductClass != null && !inputProductClass.isEmpty()) {
			uriString += divider + "inputProductClass=" + inputProductClass;
			divider = "&";
		}
		if (outputProductClass != null && !outputProductClass.isEmpty()) {
			uriString += divider + "outputProductClass=" + outputProductClass;
			divider = "&";
		}

		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
		// Initialize the result with -1 to indicate errors if no valid response is
		// given
		Long result = -1l;

		try {
			// Fetch trigger count from processor manager
			String response = serviceConnection.getFromService(serviceConfig.getOrderGenUrl(), uri.toString(), String.class,
					auth.getProseoName(), auth.getPassword());

			if (response != null && response.length() > 0) {
				result = Long.valueOf(response);
			}

			// Return the number of triggers with the respective name, version, and input
			// product class
			return result;
		} catch (RestClientResponseException e) {
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, auth.getProseoName(), "triggers", mission);
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			// Return a result of -1 to indicate that an error occurred
			return result;
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());

			// Return a result of -1 to indicate that an error occurred
			return result;
		}
	}

	/**
	 * Makes an HTTP GET request to retrieve triggers based on the provided parameters.
	 *
	 * @param id                the ID of the workflow (optional)
	 * @param name              the name of the workflow (optional)
	 * @param workflowVersion   the version of the workflow (optional)
	 * @param inputProductClass the input product class of the workflow (optional)
	 * @param recordFrom        the first record to retrieve
	 * @param recordTo          the last record to retrieve
	 * @return a Mono containing the HTTP response
	 */
	private ResponseSpec makeGetRequest(String type, String name, String workflow, String inputProductClass, String outputProductClass, 
			Long recordFrom, Long recordTo, String sortby, Boolean up) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build request URI
		String uriString = serviceConfig.getOrderGenUrl() + "/triggers";

			// Else build a request URI with all other parameters
			String divider = "?";

			if (mission != null && !mission.isEmpty()) {
				uriString += divider + "mission=" + mission;
				divider = "&";
			}
			if (type != null && !type.isEmpty()) {
				String typeParam = type;
				uriString += divider + "type=" + typeParam;
				divider = "&";
			}
			if (name != null && !name.isEmpty()) {
				String nameParam = name.replaceAll("[*]", "%");
				uriString += divider + "name=" + nameParam.toUpperCase();
				divider = "&";
			}
			if (workflow != null && !workflow.isEmpty()) {
				String workflowParam = workflow.replaceAll("[*]", "%");
				uriString += divider + "workflow=" + workflowParam;
				divider = "&";
			}
			if (inputProductClass != null && !inputProductClass.isEmpty()) {
				uriString += divider + "inputProductClass=" + inputProductClass;
				divider = "&";
			}
			if (outputProductClass != null && !outputProductClass.isEmpty()) {
				uriString += divider + "outputProductClass=" + outputProductClass;
				divider = "&";
			}
			if (recordFrom != null) {
				uriString += divider + "recordFrom=" + recordFrom;
				divider = "&";
			}
			if (recordTo != null) {
				uriString += divider + "recordTo=" + recordTo;
				divider = "&";
			}

			String sortString = "orderBy=type ASC,name ASC";
			String direction = "ASC";
			if (up != null && !up) {
				direction = "DESC";
			}
			if (sortby != null) {
				if (sortby.equals("name")) {
					sortString = "orderBy=name " + direction + ",type " + direction;
				} else if (sortby.equals("type")) {
					sortString = "orderBy=type " + direction + ",name " + direction;
				} else if (sortby.equals("workflow")) {
					sortString = "orderBy=workflow.name " + direction + ",type " + direction + ",name " + direction;
				}
			}
			uriString += divider + sortString;

		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((request, response) -> {
				// Follow redirects if the HTTP status is FOUND

				logger.trace("response:{}", response.status());
				return HttpResponseStatus.FOUND.equals(response.status());
			})));

		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));

		/*
		 * The returned ResponseSpec can be subscribed to in order to retrieve the actual response and perform additional operations
		 * on it, such as extracting the response body or handling any errors that may occur during the request.
		 */
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();

	}
}

