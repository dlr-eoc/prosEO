package de.dlr.proseo.ui.gui.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

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
}
