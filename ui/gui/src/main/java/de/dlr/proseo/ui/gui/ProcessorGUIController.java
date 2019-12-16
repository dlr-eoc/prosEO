package de.dlr.proseo.ui.gui;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.async.DeferredResult;

import de.dlr.proseo.model.rest.model.RestProcessor;
import de.dlr.proseo.model.rest.model.RestProcessorClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Controller
public class ProcessorGUIController {
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	/** WebClient-Service-Builder */
	@Autowired
	private processorService processorService;
	/** List for query Results */
	private List<String> procs1 = new ArrayList<>();
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorGUIController.class);
	/**
	 * 
	 * @param mission            parameter of search
	 * @param processorName of search
	 * @param model              of current view
	 * @return thymeleaf fragment with result from the query
	 */
	@RequestMapping(value = "/processor-show/get")
	public DeferredResult<String> getProcessorName(
			@RequestParam(required = false, value = "mission") String mission,
			@RequestParam(required = false, value = "processorName") String processorName,
			@RequestParam(required = false, value = "processorVersion") String processorVersion, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessor({}, {}, model)", mission, processorName);
		Flux<RestProcessor> mono = processorService.get(mission, processorName,processorVersion)
				.onStatus(HttpStatus::is4xxClientError,clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
				.onStatus(HttpStatus::is5xxServerError,clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
				.bodyToFlux(RestProcessor.class);

		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<RestProcessor> procs = new ArrayList<>();

		mono.subscribe(processorList -> {
			logger.trace("Now in Consumer::accept({})", processorList);
			procs.add(processorList);
			model.addAttribute("procs", procs);
			logger.trace(model.toString() + "MODEL TO STRING");
			logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
			logger.trace(">>>>MONO" + procs.toString());
			logger.trace(">>>>MODEL" + model.toString());
			deferredResult.setResult("processor-show :: #content");
		});

		logger.trace("Immediately returning deferred result");
		return deferredResult;
	}
}
