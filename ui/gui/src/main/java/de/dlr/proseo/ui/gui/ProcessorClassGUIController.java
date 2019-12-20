package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.*;

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
import de.dlr.proseo.model.rest.model.RestProcessorClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class ProcessorClassGUIController {


	/** WebClient-Service-Builder */
	@Autowired
	private processorService processorService;
	/** List for query Results */
	private List<String> procs1 = new ArrayList<>();

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorClassGUIController.class);

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
			@RequestParam(required = false, value = "mission") String mission,
			@RequestParam(required = false, value = "processorclassName") String processorClassName, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClassName({}, {}, model)", mission, processorClassName);
//		Flux<RestProcessorClass> mono = processorService.get(mission, processorclassname)
//				.onStatus(HttpStatus::is4xxClientError,
//						clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
//				.onStatus(HttpStatus::is5xxServerError,
//						clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
//				.bodyToFlux(RestProcessorClass.class);
//
//		DeferredResult<String> deferredResult = new DeferredResult<String>();
//		List<RestProcessorClass> procs = new ArrayList<>();
//
//		mono.subscribe(processorClassList -> {
//			logger.trace("Now in Consumer::accept({})", processorClassList);
//			procs.add(processorClassList);
//			model.addAttribute("procs", procs);
//			logger.trace(model.toString() + "MODEL TO STRING");
//			logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
//			logger.trace(">>>>MONO" + procs.toString());
//			logger.trace(">>>>MODEL" + model.toString());
//			deferredResult.setResult("processor-class-show :: #content");
//		});
//
//		logger.trace("Immediately returning deferred result");
//		return deferredResult;
		Mono<ClientResponse> mono = processorService.get(mission, processorClassName);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> procs = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("processor-class-show :: #content");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("processor-class-show :: #content");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(processorClassList -> {
					procs.addAll(processorClassList);
					model.addAttribute("procs", procs);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + procs.toString());
					deferredResult.setResult("processor-class-show :: #content");
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

	@RequestMapping(value = "/processor-class-show-id/get")
	public DeferredResult<String> getProcessorClassById(@RequestParam(required = true, value = "id") String id,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClassById({},model)", id);
//		Flux<RestProcessorClass> mono = processorService.getById(id)
//				.onStatus(HttpStatus::is4xxClientError,
//						clientResponse -> Mono.error(
//								new HttpClientErrorException(clientResponse.statusCode()).getMostSpecificCause()))
//				.onStatus(HttpStatus::is5xxServerError,
//						clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
//
//				.bodyToFlux(RestProcessorClass.class);
//
//		DeferredResult<String> deferredResult = new DeferredResult<String>();
//		List<RestProcessorClass> procs = new ArrayList<>();
//		mono.subscribe(processorClassList -> {
//			logger.trace("Now in Consumer::accept({})", processorClassList);
//			procs.add(processorClassList);
//			model.addAttribute("procs", procs);
//
//			logger.trace(model.toString() + "MODEL TO STRING");
//			logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
//			logger.trace(">>>>MONO" + procs.toString());
//			logger.trace(">>>>MODEL" + model.toString());
//			deferredResult.setResult("processor-class-show-id :: #content");
//		});
//
//		logger.trace("Immediately returning deferred result");
//		return deferredResult;
		Mono<ClientResponse> mono = processorService.getById(id);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<RestProcessorClass> procs = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				//model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				model.addAttribute("errormsg", uiMsg(MSG_ID_EXCEPTION, clientResponse.statusCode().getReasonPhrase()));
				deferredResult.setResult("processor-class-show-id :: #content");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("processor-class-show-id :: #content");
			} else {
				clientResponse.bodyToMono(RestProcessorClass.class).subscribe(processorClass -> {
					procs.add(processorClass);
					model.addAttribute("procs", procs);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + procs.toString());
					deferredResult.setResult("processor-class-show-id :: #content");
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

	/**
	 * @param processorClassmission      of the new Processor-Class
	 * @param processorClassName         of the new Processor-Class
	 * @param processorClassProductClass of the new Processor-Class
	 * @param model                      of current view
	 * @return thymeleaf fragment with result from query
	 * 
	 * 
	 * 
	 */
	@RequestMapping(value = "/processor-class-create/post", method = RequestMethod.POST)
	public DeferredResult<String> postProcessorClassName(@RequestParam("missionCode") String processorClassmission,
			@RequestParam("processorName") String processorClassName,
			@RequestParam("productClasses") String[] processorClassProductClass, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> postProcessorClassName({}, {}, {})", processorClassmission, processorClassName,
					processorClassProductClass.toString(), model.toString());
		Mono<ClientResponse> mono = processorService.post(processorClassmission, processorClassName,
				processorClassProductClass);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<RestProcessorClass> procs = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("processor-class-create :: #content");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("processor-class-create :: #content");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else {
				clientResponse.bodyToMono(RestProcessorClass.class).subscribe(processorClass -> {
					procs.add(processorClass);
					model.addAttribute("procs", procs);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + procs.toString());
					deferredResult.setResult("processor-class-create :: #content");
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

	@RequestMapping(value = "/processor-class-update/patch", method = RequestMethod.PATCH)
	public DeferredResult<String> patchProcessorClassName(@RequestParam("id") String id,
			@RequestParam("missionCode") String processorClassmission,
			@RequestParam("processorName") String processorClassName,
			@RequestParam("productClasses") String[] processorClassProductClass, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> patchProcessorClassName({}, {}, {}, {})", id, processorClassmission, processorClassName,
					processorClassmission, model.toString());
		Flux<RestProcessorClass> mono = processorService
				.patch(id, processorClassmission, processorClassName, processorClassProductClass)
				.onStatus(HttpStatus::is4xxClientError,
						clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
				.onStatus(HttpStatus::is5xxServerError,
						clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
				.bodyToFlux(RestProcessorClass.class);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<RestProcessorClass> procs = new ArrayList<>();

		mono.subscribe(processorClassList -> {
			logger.trace("Now in Consumer::accept({})", processorClassList);
			procs.add(processorClassList);
			model.addAttribute("procs", procs);

			logger.trace(model.toString() + "MODEL TO STRING");
			logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
			logger.trace(">>>>MONO" + procs.toString());
			logger.trace(">>>>MODEL" + model.toString());
			deferredResult.setResult("processor-class-update :: #content");
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
		logger.trace(">>>>MONO" + procs.toString());
		logger.trace(">>>>MODEL" + model.toString());
		return deferredResult;
	}

	@RequestMapping(value = "/processor-class-delete/", method = RequestMethod.DELETE)
	public DeferredResult<String> deleteProcessorClass(@RequestParam("processorClassId") String id, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteProcessorClassName({}, {}, {})", id, model.toString());
		Flux<RestProcessorClass> mono = processorService.delete(id)
				.onStatus(HttpStatus::is4xxClientError,
						clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
				.onStatus(HttpStatus::is5xxServerError,
						clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
				.bodyToFlux(RestProcessorClass.class);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<RestProcessorClass> procs = new ArrayList<>();

		mono.subscribe(processorClassList -> {
			logger.trace("Now in Consumer::accept({})", processorClassList);
			procs.add(processorClassList);
			model.addAttribute("status", processorClassList);

			logger.trace(model.toString() + "MODEL TO STRING");
			logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
			logger.trace(">>>>MONO" + procs.toString());
			logger.trace(">>>>MODEL" + model.toString());
			deferredResult.setResult("processor-class-show :: #content");
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
		logger.trace(">>>>MONO" + procs.toString());
		logger.trace(">>>>MODEL" + model.toString());
		return deferredResult;
	}


	

}
