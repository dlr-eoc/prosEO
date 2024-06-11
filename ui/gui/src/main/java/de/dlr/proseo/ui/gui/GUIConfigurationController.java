/**
 * GUIConfigurationController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.net.URI;
import java.util.ArrayList;
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
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.netty.http.client.HttpClient;

/**
 * GUI controller that handles requests related to displaying and retrieving configurations, interacts with the service layer, and
 * populates the model with the necessary data for rendering the views
 *
 * @author David Mazo
 */
@Controller
public class GUIConfigurationController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIConfigurationController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/**
	 * Display the configuration show page
	 *
	 * @return the view name for the configuration show page
	 */
	@GetMapping("/configuration-show")
	public String showConfiguration() {
		return "configuration-show";
	}

	/**
	 * Retrieve the configurations of a mission
	 *
	 * @param sortby The sort column
	 * @param up     The sort direction (true for ascending)
	 * @param model  The model to hold the data
	 * @return a DeferredResult object representing the result of the asynchronous request
	 */
	@GetMapping("/configurations/get")
	public DeferredResult<String> getConfigurations(@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getConfigurations(model)");

		// Perform the GET request asynchronously
		ResponseSpec responseSpec = get();
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> configurations = new ArrayList<>();

		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("configuration-show :: #errormsg");
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);

				if (entityList.getStatusCode().is2xxSuccessful()) {
					// Process the response body and add the configurations to the model
					configurations.addAll(entityList.getBody());
					model.addAttribute("configurations", configurations);
					deferredResult.setResult("configuration-show :: #configurationcontent");
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("configuration-show :: #errormsg");
				}
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("configuration-show :: #errormsg");
			});

		// Return the deferred result
		return deferredResult;
	}

	/**
	 * Perform a GET request to retrieve configurations
	 *
	 * @return a Mono<ClientResponse> representing the response of the GET request
	 */
	private ResponseSpec get() {
		// Retrieve the authentication token from the security context
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uriString = serviceConfig.getProcessorManagerUrl() + "/configurations";
		uriString += "?mission=" + mission;
		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make the GET request
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));

		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

}