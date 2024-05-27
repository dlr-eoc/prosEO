/**
 * GUIOrbitController.java
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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
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
 * A controller for retrieving and handling orbit data
 *
 * @author David Mazo
 */
@Controller
public class GUIOrbitController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIOrbitController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Show the orbit view
	 *
	 * @return the name of the orbit view template
	 */
	@GetMapping("/orbit-show")
	public String showOrbit() {
		return "orbit-show";
	}

	/**
	 * Retrieve the orbits of a spacecraft
	 *
	 * @param spacecraft    The spacecraft identifier
	 * @param startTimeFrom Select start time from
	 * @param startTimeTo   Select stop time until
	 * @param numberFrom    Select orbit number from
	 * @param numberTo      Select orbit number to
	 * @param fromIndex     Paging start
	 * @param toIndex       Paging stop
	 * @param sortby        The sort column
	 * @param up            The sort direction (true for up)
	 * @param model         The model to hold the data
	 * @return The result
	 */
	@GetMapping("/orbits/get")
	public DeferredResult<String> getOrbits(@RequestParam(required = false, value = "spacecraft") String spacecraft,
			@RequestParam(required = false, value = "startTimeFrom") String startTimeFrom,
			@RequestParam(required = false, value = "startTimeTo") String startTimeTo,
			@RequestParam(required = false, value = "numberFrom") Long numberFrom,
			@RequestParam(required = false, value = "numberTo") Long numberTo,
			@RequestParam(required = false, value = "recordFrom") Long fromIndex,
			@RequestParam(required = false, value = "recordTo") Long toIndex,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrbits(model)");

		Long from = null;
		Long to = null;
		if (fromIndex != null && fromIndex >= 0) {
			from = fromIndex;
		} else {
			from = (long) 0;
		}
		Long count = countOrbits(spacecraft);
		if (toIndex != null && from != null && toIndex > from) {
			to = toIndex;
		} else if (from != null) {
			to = count;
		}
		Long pageSize = to - from;
		long deltaPage = (count % pageSize) == 0 ? 0 : 1;
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;

		// Perform the HTTP request to retrieve orbits
		ResponseSpec responseSpec = get(spacecraft, startTimeFrom, startTimeTo, numberFrom, numberTo, from, to);
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> orbits = new ArrayList<>();

		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("orbit-show :: #errormsg");
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);

				if (entityList.getStatusCode().is2xxSuccessful()) {
					orbits.addAll(entityList.getBody());

					model.addAttribute("orbits", orbits);
					model.addAttribute("count", count);
					model.addAttribute("pageSize", pageSize);
					model.addAttribute("pageCount", pages);
					model.addAttribute("page", page);

					List<Long> showPages = new ArrayList<>();
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
					logger.trace(">>>>MONO" + orbits.toString());

					deferredResult.setResult("orbit-show :: #orbitcontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("orbit-show :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());

			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("orbit-show :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + orbits.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		// Return the deferred result
		return deferredResult;
	}

	/**
	 * Makes an HTTP GET request to retrieve orbits based on the provided parameters.
	 *
	 * @param spacecraft    the spacecraft
	 * @param startTimeFrom the earliest permitted start time
	 * @param startTimeTo   the latest permitted
	 * @param numberFrom    the first permitted orbit number
	 * @param numberTo      the last permitted orbit number
	 * @param recordFrom    the first record to retrieve
	 * @param recordTo      the last record to retrieve
	 * @return a Mono containing the HTTP response
	 */
	private ResponseSpec get(String spacecraft, String startTimeFrom, String startTimeTo, Long numberFrom, Long numberTo,
			Long recordFrom, Long recordTo) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uriString = serviceConfig.getOrderManagerUrl() + "/orbits";
		String divider = "?";
		if (spacecraft != null) {
			uriString += divider + "spacecraftCode=" + spacecraft;
			divider = "&";
		}
		if (startTimeFrom != null) {
			uriString += divider + "startTimeFrom=" + startTimeFrom;
			divider = "&";
		}
		if (startTimeTo != null) {
			uriString += divider + "startTimeTo=" + startTimeTo;
			divider = "&";
		}
		if (numberFrom != null) {
			uriString += divider + "orbitNumberFrom=" + numberFrom;
			divider = "&";
		}
		if (numberTo != null) {
			uriString += divider + "orbitNumberTo=" + numberTo;
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
		uriString += divider + "orderBy=orbitNumber ASC,startTime ASC";
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

	private Long countOrbits(String spacecraft) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uri = "/orbits/count";
		String divider = "?";
		if (spacecraft != null) {
			uri += divider + "spacecraftCode=" + spacecraft;
			divider = "&";
		}
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(), uri, String.class,
					auth.getProseoName(), auth.getPassword());

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
