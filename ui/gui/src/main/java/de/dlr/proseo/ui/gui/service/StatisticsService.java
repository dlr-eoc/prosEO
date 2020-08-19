package de.dlr.proseo.ui.gui.service;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.GUIAuthenticationToken;
import de.dlr.proseo.ui.gui.GUIConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class StatisticsService {

	private static Logger logger = LoggerFactory.getLogger(StatisticsService.class);
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	public Mono<ClientResponse> getJobsteps(String status) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = config.getProductionPlanner() + "/jobsteps?status=" + status;

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

	public String getOrderIdentifierOfJob(String id, GUIAuthenticationToken auth) {
		String mission = auth.getMission();
		
		Object result = null;
		String jobId = "";
		try {
			result = serviceConnection.getFromService(config.getProductionPlanner(),
					"/jobs/" + id, HashMap.class,  auth.getProseoName(), auth.getPassword());
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
			return jobId;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return jobId;
		}

		if (result instanceof HashMap) {
			jobId = (String) ((HashMap)result).get("orderIdentifier");
		}
		return jobId;
	}
	
	public String getOrderIdOfIdentifier(String id, GUIAuthenticationToken auth) {
		Object result = null;
		String jobId = "";
		try {
			result = serviceConnection.getFromService(config.getOrderManager(),
					"/orders?identifier=" + id, List.class, auth.getProseoName(), auth.getPassword());
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
			return jobId;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return jobId;
		}
		if (result instanceof List) {
			List res = (List) result;
			if (res.size() == 1) {
				jobId = (String) ((HashMap)(res.get(0))).get("id").toString();
			}
		}
		return jobId;
	}
}
