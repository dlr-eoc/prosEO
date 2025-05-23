/**
 * GUIProcessorController.java
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
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.service.MapComparator;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.netty.http.client.HttpClient;

/**
 * A controller for retrieving and handling processors
 *
 * @author David Mazo
 */
@Controller
public class GUIProcessorController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIProcessorController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Show the processor view
	 *
	 * @return the name of the processor view template
	 */
	@GetMapping("/processor-show")
	public String showProcessor() {
		return "processor-show";
	}

	/**
	 * Show the configured processor view
	 *
	 * @return the name of the configured processor view template
	 */
	@GetMapping("/configured-processor-show")
	public String showConfiguredProcessor() {

		return "configured-processor-show";
	}

	/**
	 * Retrieve the processor with name or all if name is null
	 *
	 * @param processorName The processor name or null
	 * @param sortby        The sort column
	 * @param up            The sort direction (true for up)
	 * @param model         The model to hold the data
	 * @return The result
	 */
	@GetMapping("/processor-show/get")
	public DeferredResult<String> getProcessors(@RequestParam(required = false, value = "processorName") String processorName,
			@RequestParam(required = false, value = "processorVersion") String processorVersion,
			Long recordFrom, Long recordTo, Model model) {

		logger.trace(">>> getProcessors(model)");


		Long from = null;
		Long to = null;
		if (recordFrom != null && recordFrom >= 0) {
			from = recordFrom;
		} else {
			from = (long) 0;
		}
		Long count = countProcessors(processorName, processorVersion);
		if (recordTo != null && from != null && recordTo > from) {
			to = recordTo;
		} else if (from != null) {
			to = count;
		}
		Long pageSize = to - from;
		long deltaPage = (count % pageSize) == 0 ? 0 : 1;
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;

		// Perform the HTTP request to retrieve processors
		ResponseSpec responseSpec = get(processorName, processorVersion, from, to);
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> processors = new ArrayList<>();
		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				if (e instanceof WebClientResponseException.NotFound) {
					model.addAttribute("processors", processors);

					modelAddAttributes(model, count, pageSize, pages, page);
					
					logger.trace(model.toString() + "MODEL TO STRING");
					deferredResult.setResult("processor-show :: #processorcontent");
				} else {
					model.addAttribute("errormsg", e.getMessage());
					deferredResult.setResult("processor-show :: #errormsg");
				}
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);

				if (entityList.getStatusCode().is2xxSuccessful() 
						|| entityList.getStatusCode().compareTo(HttpStatus.NOT_FOUND) == 0) {
					processors.addAll(entityList.getBody());

					MapComparator oc = new MapComparator("processorName", true);
					processors.sort(oc);

					model.addAttribute("processors", processors);

					modelAddAttributes(model, count, pageSize, pages, page);
					
					if (logger.isTraceEnabled())
						logger.trace(model.toString() + "MODEL TO STRING");
					if (logger.isTraceEnabled())
						logger.trace(">>>>MONO" + processors.toString());

					deferredResult.setResult("processor-show :: #processorcontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("processor-show :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("processor-show :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + processors.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		// Return the deferred result
		return deferredResult;
	}

	private Long countProcessors(String processorName, String processorVersion) {

		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uriString = "/processors/count";
		String divider = "?";
		uriString += divider + "mission=" + mission;
		divider = "&";
		if (processorName != null) {
			uriString += divider + "processorName=" + processorName;
		}
		if (processorVersion != null) {
			uriString += divider + "processorVersion=" + processorVersion;
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

	private Long countConfiguredProcessors(String identifier) {

		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uriString = "/configuredprocessors/count";
		String divider = "?";
		uriString += divider + "mission=" + mission;
		divider = "&";
		if (identifier != null) {
			uriString += divider + "identifier=" + identifier;
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

	/**
	 * Retrieve the configured processors of a processor or all if processor is null
	 *
	 * @param processorName The processor name or null
	 * @param sortby        The sort column
	 * @param up            The sort direction (true for up)
	 * @param model         The model to hold the data
	 * @return The result
	 */
	@GetMapping("/configuredprocessor/get")
	public DeferredResult<String> getConfiguredProcessors(
			@RequestParam(required = false, value = "identifier") String identifier,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, 
			Long recordFrom, Long recordTo, Model model) {

		logger.trace(">>> getConfiguredProcessors(model)");

		Long from = null;
		Long to = null;
		if (recordFrom != null && recordFrom >= 0) {
			from = recordFrom;
		} else {
			from = (long) 0;
		}
		Long count = countConfiguredProcessors(identifier);
		if (recordTo != null && from != null && recordTo > from) {
			to = recordTo;
		} else if (from != null) {
			to = count;
		}
		Long pageSize = to - from;
		long deltaPage = (count % pageSize) == 0 ? 0 : 1;
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;

		// Perform the HTTP request to retrieve configured processors
		ResponseSpec responseSpec = getCP(identifier, from, to, sortby, up);
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> configuredprocessors = new ArrayList<>();

		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				if (e instanceof WebClientResponseException.NotFound) {
					model.addAttribute("configuredprocessors", configuredprocessors);

					modelAddAttributes(model, count, pageSize, pages, page);
					
					logger.trace(model.toString() + "MODEL TO STRING");
					deferredResult.setResult("configured-processor-show :: #configuredprocessorcontent");
				} else {
					model.addAttribute("errormsg", e.getMessage());
					deferredResult.setResult("configured-processor-show :: #errormsg");
				}
			})// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);

				if (entityList.getStatusCode().is2xxSuccessful() 
						|| entityList.getStatusCode().compareTo(HttpStatus.NOT_FOUND) == 0) {
					configuredprocessors.addAll(entityList.getBody());

					model.addAttribute("configuredprocessors", configuredprocessors);

					modelAddAttributes(model, count, pageSize, pages, page);
					
					if (logger.isTraceEnabled())
						logger.trace(model.toString() + "MODEL TO STRING");
					if (logger.isTraceEnabled())
						logger.trace(">>>>MONO" + configuredprocessors.toString());

					deferredResult.setResult("configured-processor-show :: #configuredprocessorcontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("configured-processor-show :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("configured-processor-show :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + configuredprocessors.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		// Return the deferred result
		return deferredResult;
	}

	/**
	 * Makes an HTTP GET request to retrieve the processors with the given name.
	 *
	 * @param processorName the processor name
	 * @return a Mono containing the HTTP response
	 */
	private ResponseSpec get(String processorName, String processorVersion, Long from, Long to) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build the request URI
		String uriString = serviceConfig.getProcessorManagerUrl() + "/processors";
		String divider = "?";
		uriString += divider + "mission=" + mission;
		divider = "&";
		if (processorName != null) {
			uriString += divider + "processorName=" + processorName;
		}
		if (processorVersion != null) {
			uriString += divider + "processorVersion=" + processorVersion;
		}
		if (from != null) {
			uriString += divider + "recordFrom=" + from;
			divider = "&";
		}
		if (to != null) {
			uriString += divider + "recordTo=" + to;
			divider = "&";
		}
		uriString += divider + "orderBy=processorClass.processorName ASC, processorVersion ASC";
		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
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

	/**
	 * Makes an HTTP GET request to retrieve the configured processors with the given processor name.
	 *
	 * @param processorName the processor name
	 * @return a Mono containing the HTTP response
	 */
	private ResponseSpec getCP(String identifier, Long from, Long to,
			String sortby, Boolean up) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build the request URI
		String uriString = serviceConfig.getProcessorManagerUrl() + "/configuredprocessors";
		String divider = "?";
		uriString += divider + "mission=" + mission;
		divider = "&";
		if (identifier != null) {
			uriString += divider + "identifier=" + identifier;
		}
		if (from != null) {
			uriString += divider + "recordFrom=" + from;
			divider = "&";
		}
		if (to != null) {
			uriString += divider + "recordTo=" + to;
			divider = "&";
		}
		String sortString = "orderBy=identifier ASC";
		String direction = "ASC";
		if (up != null && !up) {
			direction = "DESC";
		}
		if (sortby != null) {
			if (sortby.equals("name")) {
				sortString = "orderBy=identifier " + direction;
			} else if (sortby.equals("processorname")) {
				sortString = "orderBy=processor.processorClass.processorName " + direction + ",processor.processorVersion ASC";
			} else if (sortby.equals("processorversion")) {
				sortString = "orderBy=processor.processorVersion " + direction + ",processor.processorClass.processorName ASC";
			} else if (sortby.equals("configuration")) {
				sortString = "orderBy=configuration.configurationVersion " + direction + ",processor.processorClass.processorName ASC";
			} else if (sortby.equals("enabled")) {
				sortString = "orderBy=enabled " + direction + ",processor.processorClass.processorName ASC";
			}
		}
		uriString += divider + sortString;
		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
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