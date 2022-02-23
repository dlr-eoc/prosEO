package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import de.dlr.proseo.ui.gui.service.MapComparator;
import de.dlr.proseo.ui.gui.service.ProcessorService;
import reactor.core.publisher.Mono;

@Controller
public class GUIProcessorClassController extends GUIBaseController {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIProcessorClassController.class);

	/** WebClient-Service-Builder */
	@Autowired
	private ProcessorService processorService;


	@RequestMapping(value = "/processor-class-show")
	public String showProcessorClass() {
		return "processor-class-show";
	}	    

	/**
	 * Get a processor class by name
	 * 
	 * @param processorClassName the processor class name to look for
	 * @param model the model to prepare for Thymeleaf
	 * @return Thymeleaf fragment with result from the query
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/processor-class-show/get")
	public DeferredResult<String> getProcessorClassName(
			@RequestParam(required = false, value = "processorclassName") String processorClassName, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClassName({}, {}, model)", processorClassName);
		Mono<ClientResponse> mono = processorService.get(processorClassName);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> procs = new ArrayList<>();
		mono.doOnError(e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("processor-class-show :: #errormsg");
		})
	 	.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(processorClassList -> {
					procs.addAll(processorClassList);
					
					MapComparator oc = new MapComparator("processorName", true);
					procs.sort(oc);
					
					model.addAttribute("procs", procs);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + procs.toString());
					deferredResult.setResult("processor-class-show :: #processorclasscontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			} else {
				handleHTTPError(clientResponse, model);
				deferredResult.setResult("processor-class-show :: #errormsg");
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("processor-class-show :: #errormsg");
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + procs.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	/**
	 * Handler for web client exceptions
	 * 
	 * @param ex the exception to handle
	 * @return the exception converted into a response entity
	 */
	@ExceptionHandler(WebClientResponseException.class)
	public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
		logger.error("Error from WebClient - Status {}, Body {}", ex.getRawStatusCode(), ex.getResponseBodyAsString(),
				ex);
		return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
	}

}


