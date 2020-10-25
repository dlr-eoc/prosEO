package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import de.dlr.proseo.ui.backend.ServiceConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Controller

public class GUIProcessorController extends GUIBaseController {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIProcessorController.class);

	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;
	
	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;
    
    @RequestMapping(value = "/processor-show")
    public String showProcessor() {
   
    return "processor-show";
    }
    @RequestMapping(value = "/configured-processor-show")
    public String showConfiguredProcessor() {
   
    return "configured-processor-show";
    }

	/**
	 * Retrieve the processor with name or all if name is null
	 * 
	 * @param processorName The processor name or null
	 * @param sortby The sort column
	 * @param up The sort direction (true for up)
	 * @param model The model to hold the data
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/processor-show/get")
	public DeferredResult<String> getProcessors(
			@RequestParam(required = false, value = "processorName") String processorName,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		
		logger.trace(">>> getProcessors(model)");
		Mono<ClientResponse> mono = get(processorName);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> processors = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("processor-show :: #processorcontent");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("processor-show :: #processorcontent");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(pList -> {
					processors.addAll(pList);
				
					model.addAttribute("processors", processors);
					if (logger.isTraceEnabled()) logger.trace(model.toString() + "MODEL TO STRING");
					if (logger.isTraceEnabled()) logger.trace(">>>>MONO" + processors.toString());
					deferredResult.setResult("processor-show :: #processorcontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + processors.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	/**
	 * Retrieve the configured processors of a processor or all if processor is null
	 * 
	 * @param processorName The processor name or null
	 * @param sortby The sort column
	 * @param up The sort direction (true for up)
	 * @param model The model to hold the data
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/configuredprocessor/get")
	public DeferredResult<String> getConfiguredProcessors(
			@RequestParam(required = false, value = "processorName") String processorName,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		
		logger.trace(">>> getConfiguredProcessors(model)");
		Mono<ClientResponse> mono = getCP(processorName);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> configuredprocessors = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("configured-processor-show :: #configuredprocessorcontent");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("configured-processor-show :: #configuredprocessorcontent");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(pList -> {
					configuredprocessors.addAll(pList);
				
					model.addAttribute("configuredprocessors", configuredprocessors);
					if (logger.isTraceEnabled()) logger.trace(model.toString() + "MODEL TO STRING");
					if (logger.isTraceEnabled()) logger.trace(">>>>MONO" + configuredprocessors.toString());
					deferredResult.setResult("configured-processor-show :: #configuredprocessorcontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + configuredprocessors.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}
	
	private Mono<ClientResponse> get(String processorName) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = serviceConfig.getProcessorManagerUrl() + "/processors";
		String divider = "?";
		uri += divider + "mission=" + mission;
		divider ="&";
		if (processorName != null) {
			uri += divider + "processorName=" + processorName;
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

	private Mono<ClientResponse> getCP(String processorName) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = serviceConfig.getProcessorManagerUrl() + "/configuredprocessors";
		String divider = "?";
		uri += divider + "mission=" + mission;
		divider ="&";
		if (processorName != null) {
			uri += divider + "identifier=" + processorName;
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
