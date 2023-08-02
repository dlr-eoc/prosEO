/**
 * GUIWorkflowController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * A controller for retrieving and handling workflows
 *
 * @author Katharina Bassler
 */
@Controller
public class GUIWorkflowController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIWorkflowController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Show the workflow view
	 *
	 * @return the name of the workflow view template
	 */
	@RequestMapping(value = "/workflow-show")
	public String showWorkflow() {
		return "workflow-show";
	}

	/**
	 * Fetch and return workflows from the processor manager
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
	@RequestMapping(value = "/workflow/get")
	public DeferredResult<String> getWorkflows(@RequestParam(required = false, value = "id") Long id,
			@RequestParam(required = false, value = "name") String name,
			@RequestParam(required = false, value = "workflowVersion") String workflowVersion,
			@RequestParam(required = false, value = "inputProductClass") String inputProductClass,
			@RequestParam(required = false, value = "recordFrom") Long recordFrom,
			@RequestParam(required = false, value = "recordTo") Long recordTo,
			@RequestParam(required = false, value = "currentPage") Long currentPage,
			@RequestParam(required = false, value = "pageSize") Long pageSize, Model model) {

		logger.trace(">>> getProducs({}, {}, {}, {}, {}, model)", name, workflowVersion, inputProductClass, recordFrom, recordTo);

		DeferredResult<String> deferredResult = new DeferredResult<>();

		// Count workflows
		final Long count;
		if (id != null && id > 0) {
			count = -1l;
		} else {
			count = countWorkflows(name, workflowVersion, inputProductClass);
		}

		makeGetRequest(id, name, workflowVersion, inputProductClass, recordFrom, recordTo).subscribe(

				// In case of success, handle HTTP response
				clientResponse -> {

					logger.trace("Now in Consumer::accept({})", clientResponse);

					if (clientResponse.statusCode().is2xxSuccessful()) {

						if (id != null && id > 0) {

							// If an ID was provided, only one workflow is retrieved

							clientResponse.bodyToMono(HashMap.class).subscribe(workflow -> {

								List<Object> workflows = new ArrayList<>();
								workflows.add(workflow);

								model.addAttribute("workflows", workflows);
								model.addAttribute("numberOfPages", 1);
								model.addAttribute("currentPage", 1);

								// Helper list for the buttons with page numbers
								List<Long> showPages = new ArrayList<>();
								showPages.add(1l);
								model.addAttribute("showPages", showPages);

								if (logger.isTraceEnabled())
									logger.trace(model.toString() + "MODEL TO STRING");
								if (logger.isTraceEnabled())
									logger.trace(">>>>MONO" + workflows.toString());

								deferredResult.setResult("workflow-show :: #workflowcontent");
								logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
							});

						} else {

							// If no ID was provided, several workflows may be retrieved

							clientResponse.bodyToMono(List.class).subscribe(workflows -> {

								// Determine number of pages
								Long numberOfPages = count % pageSize == 0 ? count / pageSize : count / pageSize + 1;

								// Determine which page buttons to show (maximum of nine buttons)
								List<Long> showPages = new ArrayList<>();
								Long start = Math.max(currentPage - 4, 1);
								Long end = Math.min(currentPage + 4, numberOfPages);
								if (currentPage < 5) {
									end = Math.min(end - currentPage + 5, numberOfPages);
								}
								if (numberOfPages - currentPage < 5) {
									start = Math.max(start - 4 + numberOfPages - currentPage, 1);
								}
								for (Long i = start; i <= end; i++) {
									showPages.add(i);
								}

								// Fill model with attributes to return
								model.addAttribute("workflows", workflows);
								model.addAttribute("numberOfPages", numberOfPages);
								model.addAttribute("currentPage", currentPage);
								model.addAttribute("showPages", showPages);

								if (logger.isTraceEnabled())
									logger.trace(model.toString() + "MODEL TO STRING");
								if (logger.isTraceEnabled())
									logger.trace(">>>>MONO" + workflows.toString());
								deferredResult.setResult("workflow-show :: #workflowcontent");

								logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
							});
						}

					} else {
						// Handle and display HTTP error
						handleHTTPError(clientResponse, model);
						deferredResult.setResult("workflow-show :: #errormsg");
					}

					logger.trace(">>>>MODEL" + model.toString());

				},

				// In case of errors, display an error message
				error -> {
					model.addAttribute("errormsg", error.getMessage());
					deferredResult.setResult("workflow-show :: #errormsg");
				}

		);

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		return deferredResult;
	}

	/**
	 * Retrieve the number of workflows matching a given name, workflow version, and input product class.
	 *
	 * @param name              the workflow name
	 * @param workflowVersion   the workflow version
	 * @param inputProductClass the input product class
	 * @return the number of workflows with the respective name, version, and input product class
	 */
	private Long countWorkflows(String name, String workflowVersion, String inputProductClass) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build request URI
		String divider = "?";
		String uri = "/workflows/count";

		if (mission != null && !mission.isEmpty()) {
			uri += divider + "mission=" + mission;
			divider = "&";
		}
		if (name != null && !name.isEmpty()) {
			uri += divider + "name=" + name;
			divider = "&";
		}
		if (workflowVersion != null && !workflowVersion.isEmpty()) {
			uri += divider + "workflowVersion=" + workflowVersion;
			divider = "&";
		}
		if (inputProductClass != null && !inputProductClass.isEmpty()) {
			uri += divider + "inputProductClass=" + inputProductClass;
			divider = "&";
		}

		// Initialize the result with -1 to indicate errors if no valid response is
		// given
		Long result = -1l;

		try {
			// Fetch workflow count from processor manager
			String response = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(), uri, String.class,
					auth.getProseoName(), auth.getPassword());

			if (response != null && response.length() > 0) {
				result = Long.valueOf(response);
			}

			// Return the number of workflows with the respective name, version, and input
			// product class
			return result;
		} catch (RestClientResponseException e) {
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, auth.getProseoName(), "workflows", mission);
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
	 * Makes an HTTP GET request to retrieve workflows based on the provided parameters.
	 *
	 * @param id                the ID of the workflow (optional)
	 * @param name              the name of the workflow (optional)
	 * @param workflowVersion   the version of the workflow (optional)
	 * @param inputProductClass the input product class of the workflow (optional)
	 * @param recordFrom        the first record to retrieve
	 * @param recordTo          the last record to retrieve
	 * @return a Mono containing the HTTP response
	 */
	private Mono<ClientResponse> makeGetRequest(Long id, String name, String workflowVersion, String inputProductClass,
			Long recordFrom, Long recordTo) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build request URI
		String uri = serviceConfig.getProcessorManagerUrl() + "/workflows";

		if (id != null && id > 0) {
			// If an ID was given, it is the only relevant parameter
			uri += "/" + id.toString();
		} else {
			// Else build a request URI with all other parameters
			String divider = "?";

			if (mission != null && !mission.isEmpty()) {
				uri += divider + "mission=" + mission;
				divider = "&";
			}
			if (name != null && !name.isEmpty()) {
				uri += divider + "name=" + name;
				divider = "&";
			}
			if (workflowVersion != null && !workflowVersion.isEmpty()) {
				uri += divider + "workflowVersion=" + workflowVersion;
				divider = "&";
			}
			if (inputProductClass != null && !inputProductClass.isEmpty()) {
				uri += divider + "inputProductClass=" + inputProductClass;
				divider = "&";
			}
			if (recordFrom != null) {
				uri += divider + "recordFrom=" + recordFrom;
				divider = "&";
			}
			if (recordTo != null) {
				uri += divider + "recordTo=" + recordTo;
				divider = "&";
			}

			uri += divider + "orderBy=name ASC,workflowVersion ASC";
		}

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
		 * The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		 * operations on it, such as extracting the response body or handling any errors that may occur during the request.
		 */
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.exchange();

	}
}
