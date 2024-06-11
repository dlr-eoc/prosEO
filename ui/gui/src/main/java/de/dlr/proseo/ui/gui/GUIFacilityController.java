/**
 * GUIFacilityController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

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

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.gui.service.MapComparator;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.netty.http.client.HttpClient;

/**
 * A controller for retrieving and handling facility data
 *
 * @author David Mazo
 */
@Controller
public class GUIFacilityController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIFacilityController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/**
	 * Show the facility view
	 *
	 * @return the name of the facility view template
	 */
	@GetMapping("/facility-show")
	public String showFacility() {
		return "facility-show";
	}

	/**
	 * Retrieve the defined processing facilities
	 *
	 * @param sortby The sort column
	 * @param up     The sort direction (true for up)
	 * @param model  The model to hold the data
	 * @return The deferred result containing the result
	 */
	@GetMapping("/facilities/get")
	public DeferredResult<String> getFacilities(@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getFacilities(model)");

		// Perform the HTTP request to retrieve facilities
		ResponseSpec responseSpec = get();
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> facilities = new ArrayList<>();

		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("facility-show :: #errormsg");
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);

				if (entityList.getStatusCode().is2xxSuccessful()) {
					facilities.addAll(entityList.getBody());

					MapComparator oc = new MapComparator("name", true);
					facilities.sort(oc);

					model.addAttribute("facilities", facilities);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + facilities.toString());

					deferredResult.setResult("facility-show :: #facilitycontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("facility-show :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("facility-show :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + facilities.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		// Return the deferred result
		return deferredResult;
	}

	/**
	 * Makes an HTTP GET request to retrieve facilities.
	 *
	 * @return a Mono containing the HTTP response
	 */
	private ResponseSpec get() {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uri = serviceConfig.getFacilityManagerUrl() + "/facilities";
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
		/*
		 * The returned ResponseSpec can be subscribed to in order to retrieve the actual response and perform additional
		 * operations on it, such as extracting the response body or handling any errors that may occur during the request.
		 */
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

}