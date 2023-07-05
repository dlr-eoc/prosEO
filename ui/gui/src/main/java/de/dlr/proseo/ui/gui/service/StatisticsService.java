/**
 * StatisticsService.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui.service;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

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
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.GUIAuthenticationToken;
import de.dlr.proseo.ui.gui.GUIConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * A bridge between the GUI frontend and the backend services, providing methods to retrieve statistics related to job steps and
 * orders by making HTTP requests to the appropriate endpoints
 *
 * @author Ernst Melchinger
 */
@Service
public class StatisticsService {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(StatisticsService.class);

	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Retrieves job steps based on a specified status and optional last parameter
	 *
	 * @param status the job step status
	 * @param last
	 * @return a Mono\<ClientResponse\> providing access to the response status and headers, and as well as methods to consume the
	 *         response body
	 */
	public Mono<ClientResponse> getJobsteps(String status, Long last) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build the request URI
		URI uri = UriComponentsBuilder.fromUriString(config.getOrderManager())
			.path("/orderjobsteps")
			.queryParam("mission", mission)
			.queryParam("status", Optional.ofNullable(status).filter(s -> !s.isBlank()).orElse(null))
			.queryParam("last", Optional.ofNullable(last).orElse(null))
			.build()
			.toUri();
		logger.trace("URI " + uri);

		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webClientBuilder = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));

		// The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		// operations on it, such as extracting the response body or handling any errors that may occur during the request.
		return webClientBuilder.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.exchange();
	}

	/**
	 * Retrieves the order identifier associated with a job based on its ID
	 *
	 * @param jobId the job id
	 * @param auth  a GUIAuthenticationToken
	 * @return the associated order identifier
	 * @throws RestClientResponseException if the call to the order manager was unsuccessful
	 */
	public String getOrderIdentifierOfJob(String jobId, GUIAuthenticationToken auth)
			throws RestClientResponseException, RuntimeException {

		// Retrieve job with matching id
		HashMap<?, ?> result = serviceConnection.getFromService(config.getOrderManager(), "/orderjobs/" + jobId, HashMap.class,
				auth.getProseoName(), auth.getPassword());

		return result.get("orderIdentifier").toString();
	}

	/**
	 * Retrieves the order ID based on its identifier
	 *
	 * @param identifier the order identifier
	 * @param auth       a GUIAuthenticationToken
	 * @return the order identifier
	 * @throws RestClientResponseException if the call to the order manager was unsuccessful
	 */
	public String getOrderIdOfIdentifier(String identifier, GUIAuthenticationToken auth)
			throws RestClientResponseException, RuntimeException {

		// Retrieve orders with matching identifier
		List<?> result = serviceConnection.getFromService(config.getOrderManager(), "/orders?identifier=" + identifier, List.class,
				auth.getProseoName(), auth.getPassword());

		return ((HashMap<?, ?>) result.get(0)).get("id").toString();
	}
}