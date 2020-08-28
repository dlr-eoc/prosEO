package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import de.dlr.proseo.ui.gui.service.ProcessorService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class GUIProcessorClassController extends GUIBaseController {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIProcessorClassController.class);

	/** WebClient-Service-Builder */
	@Autowired
	private ProcessorService processorService;
	/** List for query Results */
	private List<String> procs1 = new ArrayList<>();

	    
	    @RequestMapping(value = "/processor-class-show")
	    public String showProcessorClass() {
	    	return "processor-class-show";
	    }	    

		/**
		 * 
		 * @param mission            parameter of search
		 * @param processorclassname of search
		 * @param model              of current view
		 * @return thymeleaf fragment with result from the query
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
			mono.subscribe(clientResponse -> {
				logger.trace("Now in Consumer::accept({})", clientResponse);
				if (clientResponse.statusCode().is5xxServerError()) {
					logger.trace(">>>Server side error (HTTP status 500)");
					model.addAttribute("errormsg", "Server side error (HTTP status 500)");
					deferredResult.setResult("processor-class-show :: #processorclasscontent");
					logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
				} else if (clientResponse.statusCode().is4xxClientError()) {
					logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
					model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
					deferredResult.setResult("processor-class-show :: #processorclasscontent");
					logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
				} else if (clientResponse.statusCode().is2xxSuccessful()) {
					clientResponse.bodyToMono(List.class).subscribe(processorClassList -> {
						procs.addAll(processorClassList);
						model.addAttribute("procs", procs);
						logger.trace(model.toString() + "MODEL TO STRING");
						logger.trace(">>>>MONO" + procs.toString());
						deferredResult.setResult("processor-class-show :: #processorclasscontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				}
				logger.trace(">>>>MODEL" + model.toString());

			});
			logger.trace(model.toString() + "MODEL TO STRING");
			logger.trace(">>>>MONO" + procs.toString());
			logger.trace(">>>>MODEL" + model.toString());
			logger.trace("DEREFFERED STRING: {}", deferredResult);
			return deferredResult;
		}

		@ExceptionHandler(WebClientResponseException.class)
		public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
			logger.error("Error from WebClient - Status {}, Body {}", ex.getRawStatusCode(), ex.getResponseBodyAsString(),
					ex);
			return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
		}

	}


