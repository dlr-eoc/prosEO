/**
 * OrderService.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui.service;

import java.net.URI;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.GUIAuthenticationToken;
import de.dlr.proseo.ui.gui.GUIConfiguration;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

/**
 * A bridge between the GUI frontend and the order management backend, providing methods to retrieve, manipulate, and update
 * order-related data and entities
 *
 * @author Ernst Melchinger
 */
@Service
public class OrderService {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderService.class);

	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Retrieves a list of orders based on various search parameters
	 *
	 * @param identifier    the order identifier
	 * @param states        the order state
	 * @param products      the product
	 * @param startTimeFrom the earliest permitted startTime
	 * @param startTimeTo   the latest permitted startTime
	 * @param recordFrom    the first result to return
	 * @param recordTo      the last result to return
	 * @param sortCol       the column on which to base the sorting
	 * @param up            true if the sorting should be ascending, false if it should be descending
	 * @return a ResponseSpec; providing access to the response status and headers, and as well as methods to consume the response
	 *         body
	 */
	public ResponseSpec get(String identifier, String states, String products, String startTimeFrom, String startTimeTo,
			Long recordFrom, Long recordTo, String sortCol, Boolean up) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build the request URI
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(config.getOrderManager()).path("/orders/select");

		if (mission != null && !mission.isBlank()) {
			uriBuilder.queryParam("mission", mission.trim());
		}
		if (identifier != null && !identifier.isBlank()) {
			String identifierQueryParam = identifier.replaceAll("[*]", "%");
			uriBuilder.queryParam("identifier", identifierQueryParam.trim());
		}
		if (states != null && !states.isBlank()) {
			String[] statesQueryParam = states.split(":");
			uriBuilder.queryParam("state", (Object[]) statesQueryParam);
		}
		if (products != null && !products.isBlank()) {
			String[] productsQueryParam = products.split(":");
			uriBuilder.queryParam("productClass", (Object[]) productsQueryParam);
		}
		if (startTimeFrom != null && !startTimeFrom.isBlank()) {
			uriBuilder.queryParam("startTime", startTimeFrom.trim());
		}
		if (startTimeTo != null && !startTimeTo.isBlank()) {
			uriBuilder.queryParam("stopTime", startTimeTo.trim());
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

	/**
	 * Retrieves information about a specific order based on its ID
	 *
	 * @param orderId the order id
	 * @return a ResponseSpec; providing access to the response status and headers, as well as methods to consume the response body
	 */
	public ResponseSpec getId(String orderId) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		URI uri = UriComponentsBuilder.fromUriString(config.getOrderManager())
			.path("/orders")
			.path("/" + (orderId != null && !orderId.isBlank() ? orderId.trim() : "0"))
			.build()
			.toUri();
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

	/**
	 * Retrieves the jobs associated with a specific order
	 *
	 * @param orderId    the order id
	 * @param recordFrom the first result to return
	 * @param recordTo   the last return to return
	 * @param states     the permitted job states
	 * @return a ResponseSpec; providing access to the response status and headers, and also methods to consume the response body
	 */
	public ResponseSpec getJobsOfOrder(String orderId, Long recordFrom, Long recordTo, String states) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(config.getOrderManager()).path("/orderjobs");

		if (orderId != null && !orderId.isBlank()) {
			uriBuilder.queryParam("orderid", orderId);
		}
		if (recordFrom != null) {
			uriBuilder.queryParam("recordFrom", recordFrom);
		}
		if (recordTo != null) {
			uriBuilder.queryParam("recordTo", recordTo);
		}
		uriBuilder.queryParam("logs", "false");
		if (states != null && !states.isBlank() && !states.toLowerCase().contains("all")) {
			String[] statesQueryParam = states.split(":");
			uriBuilder.queryParam("state", (Object[]) statesQueryParam);
		}
		uriBuilder.queryParam("orderBy", "startTime ASC");

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

	/**
	 * Retrieves the graph of a specific job
	 *
	 * @param orderId the job id
	 * @return a ResponseSpec; providing access to the response status and headers, and also methods to consume the response body
	 */
	public ResponseSpec getGraphOfJob(String orderId) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		URI uri = UriComponentsBuilder.fromUriString(config.getOrderManager())
			.path("/jobs/graph")
			.path("/" + (orderId != null && !orderId.isBlank() ? orderId.trim() : "0"))
			.build()
			.toUri();
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

	/**
	 * Retrieves the graph of a specific job
	 *
	 * @param jobId the job id
	 * @param auth  the GUI authentication token
	 * @return the job graph
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> getGraphOfJob(String jobId, GUIAuthenticationToken auth) {

		HashMap<String, Object> result = null;
		try {

			// Attempt to retrieve the graph from the production planner
			result = serviceConnection.getFromService(config.getProductionPlanner(), "/jobs/graph/" + jobId, HashMap.class,
					auth.getProseoName(), auth.getPassword());

		} catch (RestClientResponseException e) {

			// Handle RestClientReponses
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

		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
		}

		return result;
	}

	/**
	 * Retrieves the state of a specific order
	 *
	 * @param orderId the order id
	 * @return the order state in string format
	 * @throws RestClientResponseException if the call to the order manager was unsuccessful
	 */
	public String getOrderState(String orderId) throws RestClientResponseException, RuntimeException {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Attempt to retrieve the order state from the production planner
		return serviceConnection
			.getFromService(config.getOrderManager(),
					"/orders" + (orderId != null && !orderId.isBlank() ? "/" + orderId.trim() : "/0"), HashMap.class,
					auth.getProseoName(), auth.getPassword())
			.get("orderState")
			.toString();
	}

	/**
	 * Changes the state of an order based on its ID and the desired state
	 *
	 * @param orderId  the order id
	 * @param state    the desired order state
	 * @param facility the processing facility
	 * @return aResponseSpec; providing access to the response status and headers, as well as methods to consume the response body
	 */
	public ResponseSpec setState(String orderId, String state, String facility) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uriString = config.getProductionPlanner() + "/orders";
		String method = "patch";
		if (state.equalsIgnoreCase("approve")) {
			uriString += "/approve/" + orderId;
		} else if (state.equalsIgnoreCase("plan")) {
			uriString += "/plan/" + orderId;
			uriString += "?facility=" + facility;
			method = "put";
		} else if (state.equalsIgnoreCase("release")) {
			uriString += "/release/" + orderId;
		} else if (state.equalsIgnoreCase("suspend")) {
			uriString += "/suspend/" + orderId;
		} else if (state.equalsIgnoreCase("suspendforce")) {
			uriString += "/suspend/" + orderId + "?force=true";
		} else if (state.equalsIgnoreCase("resume")) {
			uriString += "/resume/" + orderId;
		} else if (state.equalsIgnoreCase("reset")) {
			uriString += "/reset/" + orderId;
		} else if (state.equalsIgnoreCase("cancel")) {
			uriString += "/cancel/" + orderId;
		} else if (state.equalsIgnoreCase("retry")) {
			uriString += "/retry/" + orderId;
		} else if (state.equalsIgnoreCase("close")) {
			uriString += "/close/" + orderId;
		} else if (state.equalsIgnoreCase("delete")) {
			uriString = config.getOrderManager() + "/orders/" + orderId;
			method = "delete";
		}

		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();

		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webClientBuilder = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})
				// Timeouts: Neither configuring timeouts in tcpConfiguration() nor using the timeout() method on the
				// returnedResponseSpec
				// keeps the application from timing out after 30 s sharp
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getTimeout().intValue())
				.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler((int) (config.getTimeout() / 1000)))
					.addHandlerLast(new WriteTimeoutHandler((int) (config.getTimeout() / 1000))))));
		WebClient webClient = webClientBuilder.build();

		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));

		// Build the ResponseSpec
		ResponseSpec answer = null;

		if (method.equals("patch")) {
			answer = webClient.patch()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
			// timeout should be handled by the connection timeout specified above
//				.timeout(Duration.ofMillis(config.getTimeout()));
		} else if (method.equals("put")) {
			answer = webClient.put()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
//				.timeout(Duration.ofMillis(config.getTimeout()));
		} else if (method.equals("delete")) {
			answer = webClient.delete()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
//				.timeout(Duration.ofMillis(config.getTimeout()));
		}

		// The returned ResponseSpec can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return answer;
	}

	/**
	 * Changes the state of a job based on its ID and the desired state
	 *
	 * @param jobId the job id
	 * @param state the desired state
	 * @return a ResponseSpec; providing access to the response status and headers, as well as methods to consume the response body
	 */
	public ResponseSpec setJobState(String jobId, String state) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uriString = config.getProductionPlanner() + "/jobs";
		String method = "patch";
		if (state.equalsIgnoreCase("release")) {
			uriString += "/resume/" + jobId;
		} else if (state.equalsIgnoreCase("suspend")) {
			uriString += "/suspend/" + jobId;
		} else if (state.equalsIgnoreCase("suspendforce")) {
			uriString += "/suspend/" + jobId + "?force=true";
		} else if (state.equalsIgnoreCase("resume")) {
			uriString += "/resume/" + jobId;
		} else if (state.equalsIgnoreCase("cancel")) {
			uriString += "/cancel/" + jobId;
		} else if (state.equalsIgnoreCase("retry")) {
			uriString += "/retry/" + jobId;
		}
		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webClientBuilder = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));
		WebClient webClient = webClientBuilder.build();

		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));

		// Build the ResponseSpec
		ResponseSpec answer = null;
		if (method.equals("patch")) {
			answer = webClient.patch()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
		} else if (method.equals("put")) {
			answer = webClient.put()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
		} else if (method.equals("delete")) {
			answer = webClient.delete()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
		}

		// The returned ResponseSpec can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return answer;
	}

	/**
	 * Changes the state of a job step based on its ID and the desired state
	 *
	 * @param jobStepId the job step state
	 * @param state     the state to set
	 * @return a ResponseSpec; providing access to the response status and headers, as well as methods to consume the response body
	 */
	public ResponseSpec setJobStepState(String jobStepId, String state) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uriString = config.getProductionPlanner() + "/jobsteps";
		String method = "patch";
		if (state.equalsIgnoreCase("release")) {
			uriString += "/resume/" + jobStepId;
		} else if (state.equalsIgnoreCase("suspend")) {
			uriString += "/suspend/" + jobStepId;
		} else if (state.equalsIgnoreCase("suspendforce")) {
			uriString += "/suspend/" + jobStepId + "?force=true";
		} else if (state.equalsIgnoreCase("resume")) {
			uriString += "/resume/" + jobStepId;
		} else if (state.equalsIgnoreCase("cancel")) {
			uriString += "/cancel/" + jobStepId;
		} else if (state.equalsIgnoreCase("retry")) {
			uriString += "/retry/" + jobStepId;
		}
		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webClientBuilder = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));
		WebClient webClient = webClientBuilder.build();

		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));

		// Build the ResponseSpec
		ResponseSpec answer = null;
		if (method.equals("patch")) {
			answer = webClient.patch()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
		} else if (method.equals("put")) {
			answer = webClient.put()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
		} else if (method.equals("delete")) {
			answer = webClient.delete()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
		}

		// The returned ResponseSpec can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return answer;
	}
}