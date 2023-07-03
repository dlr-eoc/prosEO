/**
 * OrderService.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui.service;

import java.time.Duration;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.GUIAuthenticationToken;
import de.dlr.proseo.ui.gui.GUIConfiguration;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
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
	 * @return a Mono\<ClientResponse\> providing access to the response status and headers, and as well as methods to consume the
	 *         response body
	 */
	public Mono<ClientResponse> get(String identifier, String states, String products, String startTimeFrom, String startTimeTo,
			Long recordFrom, Long recordTo, String sortCol, Boolean up) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build the request URI
		String uri = config.getOrderManager() + "/orders/select";
		String divider = "?";
		if (mission != null && !mission.isEmpty()) {
			uri += divider + "mission=" + mission;
			divider = "&";
		}
		if (identifier != null && !identifier.isEmpty()) {
			uri += divider + "identifier=" + identifier.replaceAll("[*]", "%");
			divider = "&";
		}
		if (states != null && !states.isEmpty()) {
			String[] pcs = states.split(":");
			for (String pc : pcs) {
				uri += divider + "state=" + pc;
				divider = "&";
			}
		}
		if (products != null && !products.isEmpty()) {
			String[] pcs = products.split(":");
			for (String pc : pcs) {
				uri += divider + "productClass=" + pc;
				divider = "&";
			}
		}
		if (startTimeFrom != null && !startTimeFrom.isEmpty()) {
			uri += divider + "startTime=" + startTimeFrom;
			divider = "&";
		}
		if (startTimeTo != null && !startTimeTo.isEmpty()) {
			uri += divider + "stopTime=" + startTimeTo;
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
		if (sortCol != null && !sortCol.isEmpty()) {
			uri += divider + "orderBy=" + sortCol;
			if (up != null && !up) {
				uri += " DESC";
			} else {
				uri += " ASC";
			}
			divider = "&";
		}
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

		// The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.exchange();
	}

	/**
	 * Retrieves information about a specific order based on its ID
	 * 
	 * @param orderId the order id
	 * @return a Mono\<ClientResponse\> providing access to the response status and headers, as well as methods to consume the
	 *         response body
	 */
	public Mono<ClientResponse> getId(String orderId) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uri = config.getOrderManager() + "/orders";
		if (null != orderId && !orderId.trim().isEmpty()) {
			uri += "/" + orderId.trim();
		} else {
			uri += "/0";
		}
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

		// The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.exchange();
	}

	/**
	 * Retrieves the jobs associated with a specific order
	 * 
	 * @param orderId    the order id
	 * @param recordFrom the first result to return
	 * @param recordTo   the last return to return
	 * @param states     the permitted job states
	 * @return a Mono\<ClientResponse\> providing access to the response status and headers, and also methods to consume the
	 *         response body
	 */
	public Mono<ClientResponse> getJobsOfOrder(String orderId, Long recordFrom, Long recordTo, String states) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uri = config.getOrderManager() + "/orderjobs";
		String divider = "?";
		if (null != orderId && !orderId.trim().isEmpty()) {
			uri += divider + "orderid=" + orderId.trim();
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
		uri += divider + "logs=false";
		divider = "&";
		if (states != null && !states.isEmpty()) {
			String[] pcs = states.split(":");
			for (String pc : pcs) {
				if (!pc.equalsIgnoreCase("ALL")) {
					uri += divider + "state=" + pc;
					divider = "&";
				}
			}
		}
		uri += divider + "orderBy=startTime ASC";
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

		// The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.exchange();
	}

	/**
	 * Retrieves the graph of a specific job
	 * 
	 * @param orderId the job id
	 * @return a Mono\<ClientResponse\> providing access to the response status and headers, and also methods to consume the
	 *         response body
	 */
	public Mono<ClientResponse> getGraphOfJob(String orderId) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uri = config.getProductionPlanner() + "/jobs/graph/";
		if (null != orderId && !orderId.trim().isEmpty()) {
			uri += orderId.trim();
		}
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

		// The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.exchange();
	}

	/**
	 * Retrieves the graph of a specific job
	 * 
	 * @param jobId the job id
	 * @param auth
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
	 * @throws RestClientResponseException if an error occurred
	 */
	public String getOrderState(String orderId) throws RestClientResponseException {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uri = "/orders";
		if (null != orderId && !orderId.trim().isEmpty()) {
			uri += "/" + orderId.trim();
		} else {
			uri += "/0";
		}

		// Attempt to retrieve the order state from the production planner
		return serviceConnection
			.getFromService(config.getOrderManager(), uri, HashMap.class, auth.getProseoName(), auth.getPassword())
			.get("orderState")
			.toString();
	}

	/**
	 * Changes the state of an order based on its ID and the desired state
	 * 
	 * @param orderId  the order id
	 * @param state    the desired order state
	 * @param facility the processing facility
	 * @return a Mono\<ClientResponse\> providing access to the response status and headers, as well as methods to consume the
	 *         response body
	 */
	public Mono<ClientResponse> setState(String orderId, String state, String facility) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uri = config.getProductionPlanner() + "/orders";
		String method = "patch";
		if (state.equalsIgnoreCase("approve")) {
			uri += "/approve/" + orderId;
		} else if (state.equalsIgnoreCase("plan")) {
			uri += "/plan/" + orderId;
			uri += "?facility=" + facility;
			method = "put";
		} else if (state.equalsIgnoreCase("release")) {
			uri += "/release/" + orderId;
		} else if (state.equalsIgnoreCase("suspend")) {
			uri += "/suspend/" + orderId;
		} else if (state.equalsIgnoreCase("suspendforce")) {
			uri += "/suspend/" + orderId + "?force=true";
		} else if (state.equalsIgnoreCase("resume")) {
			uri += "/resume/" + orderId;
		} else if (state.equalsIgnoreCase("reset")) {
			uri += "/reset/" + orderId;
		} else if (state.equalsIgnoreCase("cancel")) {
			uri += "/cancel/" + orderId;
		} else if (state.equalsIgnoreCase("retry")) {
			uri += "/retry/" + orderId;
		} else if (state.equalsIgnoreCase("close")) {
			uri += "/close/" + orderId;
		} else if (state.equalsIgnoreCase("delete")) {
			uri += "/" + orderId;
			method = "delete";
		}
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})
				// Timeouts: Neither configuring timeouts in tcpConfiguration() nor using the timeout() method on the returned Mono
				// keeps the application from timing out after 30 s sharp
				.tcpConfiguration(client -> client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getTimeout().intValue())
					.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler((int) (config.getTimeout() / 1000)))
						.addHandlerLast(new WriteTimeoutHandler((int) (config.getTimeout() / 1000)))))));

		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));

		// Build the Mono
		Mono<ClientResponse> answer = null;
		if (method.equals("patch")) {
			answer = webclient.build()
				.patch()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.timeout(Duration.ofMillis(config.getTimeout()));
		} else if (method.equals("put")) {
			answer = webclient.build()
				.put()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.timeout(Duration.ofMillis(config.getTimeout()));
		} else if (method.equals("delete")) {
			answer = webclient.build()
				.delete()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.timeout(Duration.ofMillis(config.getTimeout()));
		}

		// The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return answer;
	}

	/**
	 * Changes the state of a job based on its ID and the desired state
	 * 
	 * @param jobId the job id
	 * @param state the desired state
	 * @return a Mono\<ClientResponse\> providing access to the response status and headers, as well as methods to consume the
	 *         response body
	 */
	public Mono<ClientResponse> setJobState(String jobId, String state) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uri = config.getProductionPlanner() + "/jobs";
		String method = "patch";
		if (state.equalsIgnoreCase("release")) {
			uri += "/resume/" + jobId;
		} else if (state.equalsIgnoreCase("suspend")) {
			uri += "/suspend/" + jobId;
		} else if (state.equalsIgnoreCase("suspendforce")) {
			uri += "/suspend/" + jobId + "?force=true";
		} else if (state.equalsIgnoreCase("resume")) {
			uri += "/resume/" + jobId;
		} else if (state.equalsIgnoreCase("cancel")) {
			uri += "/cancel/" + jobId;
		} else if (state.equalsIgnoreCase("retry")) {
			uri += "/retry/" + jobId;
		}
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

		// Build the Mono
		Mono<ClientResponse> answer = null;
		if (method.equals("patch")) {
			answer = webclient.build()
				.patch()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		} else if (method.equals("put")) {
			answer = webclient.build()
				.put()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		} else if (method.equals("delete")) {
			answer = webclient.build()
				.delete()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		}

		// The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return answer;
	}

	/**
	 * Changes the state of a job step based on its ID and the desired state
	 * 
	 * @param jobStepId the job step state
	 * @param state     the state to set
	 * @return a Mono\<ClientResponse\> providing access to the response status and headers, as well as methods to consume the
	 *         response body
	 */
	public Mono<ClientResponse> setJobStepState(String jobStepId, String state) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		// Build the request URI
		String uri = config.getProductionPlanner() + "/jobsteps";
		String method = "patch";
		if (state.equalsIgnoreCase("release")) {
			uri += "/resume/" + jobStepId;
		} else if (state.equalsIgnoreCase("suspend")) {
			uri += "/suspend/" + jobStepId;
		} else if (state.equalsIgnoreCase("suspendforce")) {
			uri += "/suspend/" + jobStepId + "?force=true";
		} else if (state.equalsIgnoreCase("resume")) {
			uri += "/resume/" + jobStepId;
		} else if (state.equalsIgnoreCase("cancel")) {
			uri += "/cancel/" + jobStepId;
		} else if (state.equalsIgnoreCase("retry")) {
			uri += "/retry/" + jobStepId;
		}
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

		// Build the Mono
		Mono<ClientResponse> answer = null;
		if (method.equals("patch")) {
			answer = webclient.build()
				.patch()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		} else if (method.equals("put")) {
			answer = webclient.build()
				.put()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		} else if (method.equals("delete")) {
			answer = webclient.build()
				.delete()
				.uri(uri)
				.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		}

		// The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return answer;
	}
}