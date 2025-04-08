/**
 * GUIProductArchive.java
 *
 * (C) 2024 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.net.URI;
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
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * A controller for retrieving and handling product archives
 *
 * @author Denys Chaykovskiy
 */
@Controller
public class GUIProductArchiveController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIProductArchiveController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Show the productarchive view
	 *
	 * @return the name of the productarchive view template
	 */
	@RequestMapping(value = "/productarchive-show")
	public String showproductarchive() {
		return "productarchive-show";
	}

	/**
	 * Fetch and return product archives from the processor manager
	 *
	 * @param id             	  	 the productarchive id
	 * @param name             		 the productarchive name
	 * @param archiveType	 		 the product archive type
	 * @param recordFrom        	 the first record to retrieve
	 * @param recordTo           	 the last record to retrieve
	 * @param currentPage       	 the current page (needed for paging logic)
	 * @param pageSize          	 the page size (needed for paging logic)
	 * @param model             	 the attributes to return
	 * @return the result
	 */
	@RequestMapping(value = "/productarchive/get")
	public DeferredResult<String> getProductArchives(@RequestParam(required = false, value = "id") Long id,
			@RequestParam(required = false, value = "name") String name,
			@RequestParam(required = false, value = "archiveType") String archiveType,
			@RequestParam(required = false, value = "recordFrom") Long recordFrom,
			@RequestParam(required = false, value = "recordTo") Long recordTo,
			@RequestParam(required = false, value = "currentPage") Long currentPage,
			@RequestParam(required = false, value = "pageSize") Long pageSize, Model model) {

		logger.trace(">>> getProductArchives({}, {}, {}, {}, {}, model)", id, name, archiveType, recordFrom, recordTo);

		DeferredResult<String> deferredResult = new DeferredResult<>();

		// Count productarchives
		final Long count;
		if (id != null && id > 0) {
			count = -1l;
		} else {
			count = countProductArchives(name, archiveType);
		}

		makeGetRequest(id, name, archiveType, recordFrom, recordTo).subscribe(

				// In case of success, handle HTTP response
				clientResponse -> {

					logger.trace("Now in Consumer::accept({})", clientResponse);

					if (clientResponse.statusCode().is2xxSuccessful()) {

						if (id != null && id > 0) {

							// If an ID was provided, only one productarchive is retrieved

							clientResponse.bodyToMono(HashMap.class).subscribe(productarchive -> {

								List<Object> productarchives = new ArrayList<>();
								productarchives.add(productarchive);

								model.addAttribute("productarchives", productarchives);
								model.addAttribute("numberOfPages", 1);
								model.addAttribute("currentPage", 1);
								model.addAttribute("count", 1);

								// Helper list for the buttons with page numbers
								List<Long> showPages = new ArrayList<>();
								showPages.add(1l);
								model.addAttribute("showPages", showPages);

								if (logger.isTraceEnabled())
									logger.trace(model.toString() + "MODEL TO STRING");
								if (logger.isTraceEnabled())
									logger.trace(">>>>MONO" + productarchives.toString());

								deferredResult.setResult("productarchive-show :: #productarchivecontent");
								logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
							});

						} else {

							// If no ID was provided, several productarchives may be retrieved

							clientResponse.bodyToMono(List.class).subscribe(productarchives -> {

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
								model.addAttribute("productarchives", productarchives);
								model.addAttribute("numberOfPages", numberOfPages);
								model.addAttribute("currentPage", currentPage);
								model.addAttribute("showPages", showPages);
								model.addAttribute("count", count);

								if (logger.isTraceEnabled())
									logger.trace(model.toString() + "MODEL TO STRING");
								if (logger.isTraceEnabled())
									logger.trace(">>>>MONO" + productarchives.toString());
								deferredResult.setResult("productarchive-show :: #productarchivecontent");

								logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
							});
						}

					} else {
						// Handle and display HTTP error
						handleHTTPError(clientResponse, model);
						deferredResult.setResult("productarchive-show :: #errormsg");
					}

					logger.trace(">>>>MODEL" + model.toString());

				},

				// In case of errors, display an error message
				error -> {
					model.addAttribute("errormsg", error.getMessage());
					deferredResult.setResult("productarchive-show :: #errormsg");
				}

		);

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		return deferredResult;
	}

	/**
	 * Retrieve the number of product archives matching a given name, archive type.
	 *
	 * @param name           	      the product archive name
	 * @param archiveType   		  the archive type
	 * @return the number of product archives with the respective name, archive type
	 */
	private Long countProductArchives(String name, String archiveType) {
		
		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build request URI
		String divider = "?";
		String uriString = "/archives/count";

		if (name != null && !name.isEmpty()) {
			uriString += divider + "name=" + name;
			divider = "&";
		}
		if (archiveType != null && !archiveType.isEmpty()) {
			uriString += divider + "archiveType=" + archiveType;
			divider = "&";
		}

		URI uri = UriComponentsBuilder.fromUriString(uriString)
				.build()
				.toUri();
		
		// Initialize the result with -1 to indicate errors if no valid response is
		// given
		Long result = -1l;

		try {
			// Fetch productarchive count from processor manager
			String response = serviceConnection.getFromService(serviceConfig.getArchiveManagerUrl(), uri.toString(), String.class,
					auth.getProseoName(), auth.getPassword());

			if (response != null && response.length() > 0) {
				result = Long.valueOf(response);
			}

			// Return the number of productarchives with the respective name, archive type
			return result;
		} catch (RestClientResponseException e) {
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, auth.getProseoName(), "productarchives", mission);
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
	 * Makes an HTTP GET request to retrieve product archives based on the provided parameters.
	 *
	 * @param id                the ID of the product archive (optional)
	 * @param name              the name of the product archive (optional)
	 * @param archiveType 		the archive type of the product archive (optional)
	 * @param recordFrom        the first record to retrieve
	 * @param recordTo          the last record to retrieve
	 * @return a Mono containing the HTTP response
	 */
	private Mono<ClientResponse> makeGetRequest(Long id, String name, String archiveType,
			Long recordFrom, Long recordTo) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build request URI
		String uriString = serviceConfig.getArchiveManagerUrl() + "/archives";

		if (id != null && id > 0) {
			// If an ID was given, it is the only relevant parameter
			uriString += "/" + id.toString();
		} else {
			// Else build a request URI with all other parameters
			String divider = "?";
			
			if (name != null && !name.isEmpty()) {
				uriString += divider + "name=" + name;
				divider = "&";
			}
			if (archiveType != null && !archiveType.isEmpty()) {
				uriString += divider + "archiveType=" + archiveType;
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

			uriString += divider + "orderBy=name ASC,archiveType ASC";
		}

		URI uri = UriComponentsBuilder.fromUriString(uriString)
				.build()
				.toUri();
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
