package de.dlr.proseo.ui.gui.service;

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
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.ui.gui.GUIAuthenticationToken;
import de.dlr.proseo.ui.gui.GUIConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class ProcessorService {
	private static ProseoLogger logger = new ProseoLogger(ProcessorService.class);
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/**
	 * Gets a processor class by its name
	 *
	 * @param processorName the processor class name to search for
	 * @return list of processor classes
	 */
	public Mono<ClientResponse> get(String processorName) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = config.getProcessorManager() + "/processorclasses";
		String divider = "?";
		if (null != mission && !mission.isEmpty()) {
			uri += divider + "mission=" + mission;
			divider = "&";
		}
		if (null != processorName && !processorName.isEmpty()) {
			uri += divider + "processorName=" + processorName;
			divider = "&";
		}
		logger.trace("URI " + uri);
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
			.exchange();

	}

	/**
	 * Get a processor class by its database ID
	 *
	 * @param id the database ID to search for
	 * @return a processor class
	 */
	public Mono<ClientResponse> getById(String id) {
		String uri = config.getProcessorManager() + "/processorclasses";
		if (null != id) {
			uri += id;
		}
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));
		GUIAuthenticationToken auth = (GUIAuthenticationToken) (SecurityContextHolder.getContext().getAuthentication());
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.exchange();

	}

	/**
	 * TODO Create a processor class
	 *
	 * @param mission                    the mission of the new processor class
	 * @param processorClassName         the name of the new processor class
	 * @param processorClassProductClass of the new Processor-Class
	 * @return response from query
	 */
	public Mono<ClientResponse> post(String mission, String processorClassName, String[] processorClassProductClass) {

		logger.trace(">>>>>ResponseSpec POST: {}, {}, {},", mission, processorClassName, processorClassProductClass);
		String uri = "";
		Map<String, Object> map = new HashMap<>();
		if (null != mission && null != processorClassName && null != processorClassProductClass) {
			uri += config.getProcessorManager() + "/processorclasses";
			logger.trace("URI " + uri);
			map.put("missionCode", mission);
			map.put("processorName", processorClassName);
			map.put("productClasses", processorClassProductClass);
			logger.trace(">>PRODUCTCLASSES TO STRING: {}", processorClassProductClass.toString());

			logger.trace(">>PARAMETERS IN POST: {}, {}, {},", mission, processorClassName, processorClassProductClass);
			logger.trace(">>>MAP AFTER PARAMETERS: {}", map);
		}
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}" + res);
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return webclient.build()
			.post()
			.uri(uri)
			.body(BodyInserters.fromObject(map))
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.accept(MediaType.APPLICATION_JSON)
			.exchange();

	}

	/**
	 * TODO Update a processor class by id
	 *
	 * @param id                         database ID of processor class to update
	 * @param mission                    mission of the processor class
	 * @param processorClassName         name of the processor class
	 * @param processorClassProductClass array of product classes the processor class is able to create
	 * @return the updated processor class
	 */
	public ResponseSpec patch(String id, String mission, String processorClassName, String[] processorClassProductClass) {
		String uri = "";
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		if (null != mission && null != processorClassName && null != processorClassProductClass && null != id) {
			uri += config.getProcessorManager() + id;
			logger.trace("URI " + uri);
			map.add("missionCode", mission);
			map.add("processorName", processorClassName);
			map.add("productClasses", processorClassProductClass.toString());
		}
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}" + res);
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return webclient.build()
			.patch()
			.uri(uri)
			.body(BodyInserters.fromFormData(map))
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();

	}

	/**
	 * TODO Delete a processor class by id
	 *
	 * @param id of Processor Class to be removed
	 * @return response from query
	 */
	public ResponseSpec delete(String id) {
		String uri = config.getProcessorManager();
		if (null != id) {
			uri += id;
			logger.trace("URI" + uri);
		}
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}" + res);
				return HttpResponseStatus.FOUND.equals(res.status());
			})));
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return webclient.build()
			.delete()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

	/**
	 * TODO Get a processor by processor class name and processor version
	 *
	 * @param mission          the mission of the processor
	 * @param processorName    the processor class name
	 * @param processorVersion the processor version
	 * @return responseSpec for processors
	 */
	public ResponseSpec get(String mission, String processorName, String processorVersion) {
		String uri = config.getProcessorManager();
		if (null != mission && null != processorName && null != processorVersion) {
			uri += "?mission=" + mission + "&processorName=" + processorName + "%processorVersion=" + processorVersion;
		} else if (null != processorName) {
			uri += "?processorName=" + processorName;
		} else if (null != mission) {
			uri += "?mission=" + mission;
		}
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		ResponseSpec responseSpec = webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
		return responseSpec;

	}

}
