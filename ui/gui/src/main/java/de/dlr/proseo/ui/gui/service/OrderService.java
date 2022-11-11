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

@Service
public class OrderService {
	private static ProseoLogger logger = new ProseoLogger(OrderService.class);
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;
	
	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
	

	public Mono<ClientResponse> get(String identifier, String states, String products, String from, String to, 
			Long recordFrom, Long recordTo, String sortCol, Boolean up) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = config.getOrderManager() + "/orders/select";
		
		String divider = "?";
		if (mission != null && !mission.isEmpty()) {
			uri += divider + "mission=" + mission;
			divider ="&";
		}
		if (identifier != null && !identifier.isEmpty()) {
			uri += divider + "identifier=" + identifier.replaceAll("[*]", "%");
			divider ="&";
		}

		if (states != null && !states.isEmpty()) {
			String [] pcs = states.split(":");
			for (String pc : pcs) {
				uri += divider + "state=" + pc;
				divider ="&";
			}
		}
		if (products != null && !products.isEmpty()) {
			String [] pcs = products.split(":");
			for (String pc : pcs) {
				uri += divider + "productClass=" + pc;
				divider ="&";
			}
		}
		if (from != null && !from.isEmpty()) {
			uri += divider + "startTime=" + from;
			divider ="&";
		}
		if (to != null && !to.isEmpty()) {
			uri += divider + "stopTime=" + to;
			divider ="&";
		}
		if (recordFrom != null) {
			uri += divider + "recordFrom=" + recordFrom;
			divider ="&";
		}
		if (recordTo != null) {
			uri += divider + "recordTo=" + recordTo;
			divider ="&";
		}
		if (sortCol != null && !sortCol.isEmpty()) {
			uri += divider + "orderBy=" + sortCol;
			if (up != null && !up) {
				uri += " DESC";
			} else {
				uri += " ASC";
			}
			divider ="&";
		}
		
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
			));
		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
		return  webclient.build().get().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();

	}

	public Mono<ClientResponse> getId(String id) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = config.getOrderManager() + "/orders";
		// get one order identified by id.
		if(null != id && !id.trim().isEmpty()) {
			uri += "/" + id.trim();
		} else {
			uri += "/0";
		}
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
			));
		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
		return  webclient.build().get().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();

	}
	
	public Mono<ClientResponse> getJobsOfOrder(String id,Long from, Long to, String states) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = config.getOrderManager() + "/orderjobs";
		// get one order identified by id.
		String divider = "?";
		if(null != id && !id.trim().isEmpty()) {
			uri += divider + "orderid=" + id.trim();
			divider ="&";
		}
		if (from != null) {
			uri += divider + "recordFrom=" + from;
			divider ="&";
		}
		if (to != null) {
			uri += divider + "recordTo=" + to;
			divider ="&";
		}
		uri += divider + "logs=false";
		divider ="&";
		if (states != null && !states.isEmpty()) {
			String [] pcs = states.split(":");
			for (String pc : pcs) {
				if (!pc.equalsIgnoreCase("ALL")) {
					uri += divider + "state=" + pc;
					divider ="&";
				}
			}
		}
		uri += divider + "orderBy=startTime ASC";
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
			));
		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
		return  webclient.build().get().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();

	}

	public Mono<ClientResponse> getGraphOfJob(String id) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = config.getProductionPlanner() + "/jobs/graph/";
		// get one order identified by id.
		if(null != id && !id.trim().isEmpty()) {
			uri += id.trim();
		}
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
			));
		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
		return  webclient.build().get().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();

	}
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> getGraphOfJob(String id, GUIAuthenticationToken auth) {
		String mission = auth.getMission();
		
		HashMap<String, Object> result = null;
		try {
			result = serviceConnection.getFromService(config.getProductionPlanner(),
					"/jobs/graph/" + id, HashMap.class,  auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = ProseoLogger.format(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return result;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return result;
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public String getOrderState(String id) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String uri = "/orders";
		// get one order identified by id.
		if(null != id && !id.trim().isEmpty()) {
			uri += "/" + id.trim();
		} else {
			uri += "/0";
		}
		
		HashMap<String, Object> result = null;
		try {
			result = serviceConnection.getFromService(config.getOrderManager(),
					uri, HashMap.class,  auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = ProseoLogger.format(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return (String) result.get("orderState");
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return (String) result.get("orderState");
		}

		return (String) result.get("orderState");
	}
	public Mono<ClientResponse> setState(String orderId, String state, String facility) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
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
			
		// Timeouts: Neither configuring timeouts in tcpConfiguration() nor using the timeout() method on the returned Mono
		// keeps the application from timing out after 30 s sharp
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
				.tcpConfiguration(client -> client
                	.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getTimeout().intValue())
                	.doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler((int)(config.getTimeout() / 1000)))
                        .addHandlerLast(new WriteTimeoutHandler((int)(config.getTimeout() / 1000)))))
			));
		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
		Mono<ClientResponse> answer = null;
		if (method.equals("patch")) {
			answer = webclient.build().patch().uri(uri)
					.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
					.accept(MediaType.APPLICATION_JSON)
					.exchange()
					.timeout(Duration.ofMillis(config.getTimeout()));
		} else if (method.equals("put")) {
			answer = webclient.build().put().uri(uri)
					.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
					.accept(MediaType.APPLICATION_JSON)
					.exchange()
					.timeout(Duration.ofMillis(config.getTimeout()));
		} else if (method.equals("delete")) {
			answer = webclient.build().delete().uri(uri)
					.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
					.accept(MediaType.APPLICATION_JSON)
					.exchange()
					.timeout(Duration.ofMillis(config.getTimeout()));
		}
		return answer;
	}
	
	public Mono<ClientResponse> setJobState(String jobId, String state) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
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
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
			));
		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
		Mono<ClientResponse> answer = null;
		if (method.equals("patch")) {
			answer = webclient.build().patch().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();
		} else if (method.equals("put")) {
			answer = webclient.build().put().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();
		} else if (method.equals("delete")) {
			answer = webclient.build().delete().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();
		}
		return answer;
	}
	
	public Mono<ClientResponse> setJobStepState(String jobStepId, String state) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
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
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
			));
		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
		Mono<ClientResponse> answer = null;
		if (method.equals("patch")) {
			answer = webclient.build().patch().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();
		} else if (method.equals("put")) {
			answer = webclient.build().put().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();
		} else if (method.equals("delete")) {
			answer = webclient.build().delete().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();
		}
		return answer;
	}
}
