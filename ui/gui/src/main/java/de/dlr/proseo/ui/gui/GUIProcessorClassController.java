/**
 * GUIProcessorClassController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
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
	
	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
	
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

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
			@RequestParam(required = false, value = "processorclassName") String processorClassName,
			@RequestParam(required = false, value = "productClass") String productClass,
			Long recordFrom, Long recordTo, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClassName({}, {}, model)", processorClassName, productClass);

		Long from = null;
		Long to = null;
		if (recordFrom != null && recordFrom >= 0) {
			from = recordFrom;
		} else {
			from = (long) 0;
		}
		Long count = countProcessorClasses(processorClassName, productClass);
		if (recordTo != null && from != null && recordTo > from) {
			to = recordTo;
		} else if (from != null) {
			to = count;
		}
		Long pageSize = to - from;
		long deltaPage = (count % pageSize) == 0 ? 0 : 1;
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;

		// Perform the HTTP request to retrieve processor classes
		ResponseSpec responseSpec = processorService.get(processorClassName, productClass, from, to);
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

					model.addAttribute("procs", procs);

					modelAddAttributes(model, count, pageSize, pages, page);
					
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

	private Long countProcessorClasses(String processorName, String productClass) {

		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		// Build the request URI
		String divider = "?";
		String uriString = "/processorclasses/count";
		if (mission != null && !mission.isEmpty()) {
			uriString += divider + "mission=" + mission;
			divider = "&";
		}
		if (processorName != null && !processorName.isEmpty()) {
			uriString += divider + "processorName=" + processorName;
			divider = "&";
	}
		if (productClass != null && !productClass.isEmpty()) {
			uriString += divider + "productClass=" + productClass;
			divider = "&";
	}
		
		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(), uri.toString(),
					String.class, auth.getProseoName(), auth.getPassword());

			if (resStr != null && resStr.length() > 0) {
				result = Long.valueOf(resStr);
			}
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return result;
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return result;
		}

		return result;
	}

}
