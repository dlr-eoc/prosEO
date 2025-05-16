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

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;


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
	public DeferredResult<String> getConfigurations(String configurationVersion, String processorClass, Long recordFrom, Long recordTo, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getConfigurations(model, {}, {})", configurationVersion, processorClass);

		Long from = null;
		Long to = null;
		if (recordFrom != null && recordFrom >= 0) {
			from = recordFrom;
		} else {
			from = (long) 0;
		}
		Long count = countConfigurations(configurationVersion, processorClass);
		if (recordTo != null && from != null && recordTo > from) {
			to = recordTo;
		} else if (from != null) {
			to = count;
		}
		Long pageSize = to - from;
		long deltaPage = (count % pageSize) == 0 ? 0 : 1;
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;

		// Perform the GET request asynchronously
		ResponseSpec responseSpec = get(configurationVersion, processorClass, from, to);
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> configurations = new ArrayList<>();

		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				if (e instanceof WebClientResponseException.NotFound) {
					model.addAttribute("configurations", configurations);

					modelAddAttributes(model, count, pageSize, pages, page);

					if (logger.isTraceEnabled())
						logger.trace(model.toString() + "MODEL TO STRING");

					deferredResult.setResult("configuration-show :: #configurationcontent");
				} else {
					model.addAttribute("errormsg", e.getMessage());
					deferredResult.setResult("configuration-show :: #errormsg");
				}
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);

				if (entityList.getStatusCode().is2xxSuccessful() 
						|| entityList.getStatusCode().compareTo(HttpStatus.NOT_FOUND) == 0) {
					// Process the response body and add the configurations to the model
					configurations.addAll(entityList.getBody());
					model.addAttribute("configurations", configurations);

					modelAddAttributes(model, count, pageSize, pages, page);

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
			})
			;

		// Return the deferred result
		return deferredResult;
	}

	/**
	 * Perform a GET request to retrieve configurations
	 *
	 * @return a ResponseSpec representing the response of the GET request
	 */
	private ResponseSpec get(String configurationVersion, String processorClass, Long from, Long to) {
		// Retrieve the authentication token from the security context
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uriString = serviceConfig.getProcessorManagerUrl() + "/configurations";
		uriString += "?mission=" + mission;
		String divider = "&";
		if (configurationVersion != null && !configurationVersion.isEmpty()) {
			uriString += divider + "configurationVersion=" + configurationVersion;
			divider = "&";
		}
		if (processorClass != null && !processorClass.isEmpty()) {
			uriString += divider + "processorName=" + processorClass;
			divider = "&";
		}
		if (from != null) {
			uriString += divider + "recordFrom=" + from;
			divider = "&";
		}
		if (to != null) {
			uriString += divider + "recordTo=" + to;
			divider = "&";
		}
		uriString += divider + "orderBy=processorClass.processorName ASC&orderBy=configurationVersion ASC";
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
	
	private Long countConfigurations(String configurationVersion, String processorClass) {

		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		// Build the request URI
		String divider = "?";
		String uriString = "/configurations/count";
		if (mission != null && !mission.isEmpty()) {
			uriString += divider + "mission=" + mission;
			divider = "&";
		}
		if (configurationVersion != null && !configurationVersion.isEmpty()) {
			uriString += divider + "configurationVersion=" + configurationVersion;
			divider = "&";
		}
		if (processorClass != null && !processorClass.isEmpty()) {
			uriString += divider + "processorName=" + processorClass;
			divider = "&";
		}
		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(), uri.toString(),
					String.class, auth.getProseoName(), auth.getPassword());

			if (resStr != null && resStr.length() > 0) {
				result = Long.valueOf(resStr);
			}
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
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

}