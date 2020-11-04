package de.dlr.proseo.ui.gui.service;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.GUIAuthenticationToken;
import de.dlr.proseo.ui.gui.GUIConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class OrderService {
	private static Logger logger = LoggerFactory.getLogger(OrderService.class);
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;
	
	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
	

	public Mono<ClientResponse> get(String orderName) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = config.getOrderManager() + "/orders";
		if(null != mission && null != orderName && !orderName.trim().isEmpty()) {
			uri += "?mission=" + mission + "&identifier=" + orderName.trim();
		} else if (null != orderName && !orderName.trim().isEmpty()) {
			uri += "?identifier=" + orderName.trim();
		} else if (null != mission) { 
			uri += "?mission=" + mission;
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
	
	public Mono<ClientResponse> getJobsOfOrder(String id) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = config.getProductionPlanner() + "/jobs";
		// get one order identified by id.
		if(null != id && !id.trim().isEmpty()) {
			uri += "?orderid=" + id.trim();
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
				message = uiMsg(MSG_ID_NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return result;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return result;
		}

		return result;
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
