/**
 * GUIProcessorClassController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.gui.service.MapComparator;
import de.dlr.proseo.ui.gui.service.ProcessorService;

/**
 * A controller for retrieving and handling processor class data
 *
 * @author David Mazo
 */
@Controller
public class GUIProcessorClassController extends GUIBaseController {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIProcessorClassController.class);

	/** WebClient-Service-Builder */
	@Autowired
	private ProcessorService processorService;

	/**
	 * Show the processor class view
	 *
	 * @return the name of the processor class view template
	 */
	@GetMapping("/processor-class-show")
	public String showProcessorClass() {
		return "processor-class-show";
	}

	/**
	 * Get a processor class by name
	 *
	 * @param processorClassName the processor class name to look for
	 * @param model              the model to prepare for Thymeleaf
	 * @return Thymeleaf fragment with result from the query
	 */
	@GetMapping("/processor-class-show/get")
	public DeferredResult<String> getProcessorClassName(
			@RequestParam(required = false, value = "processorclassName") String processorClassName, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClassName({}, {}, model)", processorClassName);

		// Perform the HTTP request to retrieve processor classes
		ResponseSpec responseSpec = processorService.get(processorClassName);
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> procs = new ArrayList<>();

		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("processor-class-show :: #errormsg");
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);
				if (entityList.getStatusCode().is2xxSuccessful()) {
					procs.addAll(entityList.getBody());

					MapComparator oc = new MapComparator("processorName", true);
					procs.sort(oc);

					model.addAttribute("procs", procs);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + procs.toString());

					deferredResult.setResult("processor-class-show :: #processorclasscontent");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("processor-class-show :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("processor-class-show :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + procs.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		// Return the deferred result
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
		logger.log(UIMessage.WEBCLIENT_ERROR, ex.getRawStatusCode(), ex.getResponseBodyAsString(), ex);
		return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
	}
}
