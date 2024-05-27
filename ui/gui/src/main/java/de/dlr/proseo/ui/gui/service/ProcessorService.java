/**
 * ProcessorService.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui.service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.gui.GUIAuthenticationToken;
import de.dlr.proseo.ui.gui.GUIConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.netty.http.client.HttpClient;

/**
 * A bridge between the GUI frontend and the backend services related to processors. It provides methods to interact with
 * processor-related functionalities by making HTTP requests to the appropriate endpoints.
 *
 * @author David Mazo
 */
@Service
public class ProcessorService {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorService.class);

	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/**
	 * Gets a processor class by its name
	 *
	 * @param processorName the processor class name to search for
	 * @return a ResponseSpec; providing access to the response status and headers, and as well as methods to consume the response
	 *         body
	 */
	public ResponseSpec get(String processorName) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build the request URI
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(config.getProcessorManager())
			.path("/processorclasses");

		if (mission != null && !mission.isBlank()) {
			uriBuilder.queryParam("mission", mission.trim());
		}
		if (processorName != null && !processorName.isBlank()) {
			uriBuilder.queryParam("processorName", processorName.trim());
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

		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

	/**
	 * Get a processor class by its database ID
	 *
	 * @param processorId the database ID to search for
	 * @return a ResponseSpec; providing access to the response status and headers, and as well as methods to consume the response
	 *         body
	 */
	public ResponseSpec getById(String processorId) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) (SecurityContextHolder.getContext().getAuthentication());

		// Build the request URI
		URI uri = UriComponentsBuilder.fromUriString(config.getProcessorManager())
			.path("/processorclasses")
			.path("/" + (processorId != null && !processorId.isBlank() ? processorId.trim() : "0"))
			.build()
			.toUri();
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

	/**
	 * Create a processor class
	 *
	 * @param missionCode    the code of the mission to which the new processor class belongs
	 * @param processorName  user-defined processor class name (Processor_Name from Generic IPF Interface Specifications, sec.
	 *                       4.1.3), unique within a mission
	 * @param productClasses the product classes a processor of this class can generate
	 * @return a ResponseSpec; providing access to the response status and headers, and as well as methods to consume the response
	 *         body
	 *
	 *         TODO verify
	 */
	public ResponseSpec post(String missionCode, String processorName, String[] productClasses) {
		logger.trace(">>>>>ResponseSpec POST: {}, {}, {},", missionCode, processorName, productClasses);

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		URI uri = UriComponentsBuilder.fromUriString(config.getProcessorManager()).path("/processorclasses").build().toUri();
		logger.trace("URI " + uri);

		// Build the request body
		Map<String, Object> map = new HashMap<>();
		if (null != missionCode && null != processorName && null != productClasses) {
			map.put("missionCode", missionCode);
			map.put("processorName", processorName);
			map.put("productClasses", productClasses);

			logger.trace(">>PRODUCTCLASSES TO STRING: {}", productClasses.toString());
			logger.trace(">>PARAMETERS IN POST: {}, {}, {},", missionCode, processorName, productClasses);
			logger.trace(">>>MAP AFTER PARAMETERS: {}", map);
		} else {
			throw new IllegalArgumentException(logger.log(UIMessage.PROCESSOR_DATA_INVALID,
					"Either missionCode, or processorName, or productClasses are missing."));
		}

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}" + res);
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		// The returned ResponseSpec can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return webclient.build()
			.post()
			.uri(uri)
			.body(BodyInserters.fromProducer(map, Map.class))
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

	/**
	 * Update a processor class by id
	 *
	 * @param processorClassId database ID of processor class to update
	 * @param missionCode      the code of the mission to which the new processor class belongs
	 * @param processorName    user-defined processor class name (Processor_Name from Generic IPF Interface Specifications, sec.
	 *                         4.1.3), unique within a mission
	 * @param productClasses   the product classes a processor of this class can generate
	 * @return the updated processor class
	 *
	 *         TODO verify
	 */
	public ResponseSpec patch(String processorClassId, String missionCode, String processorName, String[] productClasses) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(config.getProcessorManager())
			.path("/processorclasses");

		if (null != processorClassId && !processorClassId.isBlank()) {
			uriBuilder.path("/" + processorClassId.trim());
		}

		URI uri = uriBuilder.build().toUri();
		logger.trace("URI " + uri);

		// Build the request body
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		if (null != missionCode && null != processorName && null != productClasses && null != processorClassId) {
			map.add("missionCode", missionCode);
			map.add("processorName", processorName);
			map.add("productClasses", productClasses.toString());
		}

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}" + res);
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		// The returned ResponseSpec can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return webclient.build()
			.patch()
			.uri(uri)
			.body(BodyInserters.fromFormData(map))
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

	/**
	 * Delete a processor class by id
	 *
	 * @param processorClassId of Processor Class to be removed
	 * @return a ResponseSpec that the caller can subscribe to, e.g. for extracting the response body or handling errors
	 *
	 *         TODO verify
	 */
	public ResponseSpec delete(String processorClassId) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(config.getProcessorManager())
			.path("/processorclasses");

		if (null != processorClassId && !processorClassId.isBlank()) {
			uriBuilder.path("/" + processorClassId.trim());
		}

		URI uri = uriBuilder.build().toUri();
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}" + res);
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		// The returned ResponseSpec can be subscribed to, e.g. for extracting the response body or handling errors
		return webclient.build()
			.delete()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

	/**
	 * Get a processor by processor class name and processor version
	 *
	 * @param missionCode      the mission of the processor
	 * @param processorName    the processor class name
	 * @param processorVersion the processor version
	 * @return a ResponseSpec that the caller can subscribe to, e.g. for extracting the response body or handling errors
	 *
	 *         TODO verify
	 */
	public ResponseSpec get(String missionCode, String processorName, String processorVersion) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(config.getProcessorManager())
			.path("/processorclasses");

		if (missionCode != null && !missionCode.isBlank()) {
			uriBuilder.queryParam("mission", missionCode.trim());
		}
		if (processorName != null && !processorName.isBlank()) {
			uriBuilder.queryParam("processorName", processorName.trim());
		}
		if (processorVersion != null && !processorVersion.isBlank()) {
			uriBuilder.queryParam("processorVersion", processorVersion.trim());
		}

		URI uri = uriBuilder.build().toUri();
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		// The returned ResponseSpec can be subscribed to, e.g. for extracting the response body or handling errors
		ResponseSpec responseSpec = webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
		return responseSpec;
	}

}