package de.dlr.proseo.ui.gui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerResponse;

import de.dlr.proseo.model.rest.model.RestProcessorClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class ProcessorClassGUIController {

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

	private boolean clientError = false;
//TABLE processor-class-show backup
//	    <table class="table">
//  	<thead class="thead-dark">
// 	 <tr>
//  		<th scope="col">ID</th>
//  		<th scope="col">version</th>
//  		<th scope="col">missionCode</th>
//  		<th scope="col">processorName</th>
//  		<th scope="col">productClasses</th>
//  	</tr>
//   </thead>
//   <tbody>
//  		<tr th:each"processor : ${processors}">
//  		<td th:text="${processor.id}"></td>
//  		<td th:text="${processor.version}"></td>
//  		<td th:text="${processor.mission}"></td>
//  		<td th:text="${processor.processorclassname}"></td>
//  		<td th:text="${processor.productclasses}"></td>
//  	</tr>
//  	</tbody>
// 	</table>

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorClassGUIController.class);
//	 @RequestMapping(value = "/processor-class-show/get") 
//	 public String getProcessorClassName(@RequestParam("missioncode") String mission, 
//			 @RequestParam("processorclassnamecode") String processorclassname,Model model) { 
//		 if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassName({}, {}, {})", mission,  processorclassname, model.toString());
//		 
//		 Mono<String> mono = processorService.get(mission, processorclassname).bodyToMono(String.class);
//	     processorDto ps = new processorDto();
//		 ps.setMono(mono);
//		 List<String> procList = new ArrayList<>();;
//		 Mono<String> res = ps.getMono();
//		 res.subscribe(value-> procList.add(value));
//		 mono.subscribe(value-> model.addAttribute("proc", value));
//		 logger.trace(model.containsAttribute("proc") + "VALUE IN MODEL");
//		 logger.trace(model.toString() + "MODEL TO STRING");
//		 return "processor-class-show";
//	 }
	/*
	 * @RequestMapping(value = "/processor-class-show/get") public String
	 * getProcessorClassName(@RequestParam("missioncode") String
	 * mission, @RequestParam("processorclassnamecode") String processorclassname,
	 * Model model) { if (logger.isTraceEnabled())
	 * logger.trace(">>> getProcessorClassName({}, {}, {})", mission,
	 * processorclassname, model.toString());
	 * 
	 * // TODO mission und processorname nur dann in URI, wenn nicht null bzw. "" //
	 * QueryParam durch RequestParam ersetzen
	 * 
	 * RestTemplate restTemplate = rtb.basicAuthentication("s5p-proseo",
	 * "sieb37.Schlaefer").build(); //application.yml wurde nicht übernommen, base
	 * uri manuell übernommen config.getProcessorManager() String uri =
	 * config.getProcessorManager() + "/processorclasses?mission=" + mission +
	 * "&processorName=" + processorclassname;
	 * 
	 * @SuppressWarnings("rawtypes") ResponseEntity<List> response =
	 * restTemplate.getForEntity(uri, List.class);
	 * 
	 * // HTTP Status 404 abfangen
	 * 
	 * // Rest muss einen Loop in eine Tabelle ergeben
	 * 
	 * // Aus response.getBody() Daten rausholen und in model schreiben ObjectMapper
	 * mapper = new ObjectMapper(); RestProcessorClass restProcessorClass =
	 * mapper.convertValue(response.getBody().iterator().next(),
	 * RestProcessorClass.class);
	 * 
	 * if (logger.isDebugEnabled()) logger.debug("Received response: {}",
	 * response.getBody());
	 * 
	 * model.addAttribute("id", restProcessorClass.getId());
	 * model.addAttribute("mission", restProcessorClass.getMissionCode());
	 * model.addAttribute("processorclassname",
	 * restProcessorClass.getProcessorName()); model.addAttribute("version",
	 * restProcessorClass.getVersion()); model.addAttribute("productclasses",
	 * restProcessorClass.getProductClasses()); model.addAttribute("all",
	 * restProcessorClass); //....
	 * 
	 * return "processor-class-show"; }
	 */
//	@RequestMapping(value = "/processor-class-show/get")
//	public String getProcessorClassName(@RequestParam("missioncode") String mission,
//			@RequestParam("processorclassnamecode") String processorclassname, Model model) {
//		if (logger.isTraceEnabled())
//			logger.trace(">>> getProcessorClassName({}, {}, {})", mission, processorclassname, model.toString());
//
//		// TODO mission und processorname nur dann in URI, wenn nicht null bzw. ""
//		// QueryParam durch RequestParam ersetzen
//
//		RestTemplate restTemplate = rtb.basicAuthentication("s5p-proseo", "sieb37.Schlaefer").build();
//		// application.yml wurde nicht übernommen, base uri manuell übernommen
//		// config.getProcessorManager()
//		String uri = config.getProcessorManager() + "/processorclasses?mission=" + mission + "&processorName="
//				+ processorclassname;
//		@SuppressWarnings("rawtypes")
//		ResponseEntity<List> response = restTemplate.getForEntity(uri, List.class);
//		if (logger.isDebugEnabled())
//			logger.debug("Received response BEFORE JACKSON: {}", response.getBody());
//
//		// HTTP Status 404 abfangen
//
//		// Rest muss einen Loop in eine Tabelle ergeben
//		WebClient client1 = WebClient.create();
//		
//		// Aus response.getBody() Daten rausholen und in model schreiben
//		ObjectMapper mapper = new ObjectMapper();
//		
////		  RestProcessorClass restProcessorClass =
////		  mapper.convertValue(response.getBody(), RestProcessorClass.class);
//		List<RestProcessorClass> responsee = response.getBody();
//	  if (logger.isDebugEnabled()) logger.debug("Received response: {}",responsee);
//	  model.addAttribute("processors", responsee);
//	  if (logger.isDebugEnabled()) logger.debug("MODEL: {}",model.toString());
////		  model.addAttribute("id", restProcessorClass.getId());
////		  model.addAttribute("mission", restProcessorClass.getMissionCode());
////		  model.addAttribute("processorclassname",
////		  restProcessorClass.getProcessorName()); model.addAttribute("version",
////		  restProcessorClass.getVersion()); model.addAttribute("productclasses",
////		  restProcessorClass.getProductClasses()); model.addAttribute("all",
////		  restProcessorClass); //....
////		 
//		return "processor-class-show";
//	}

//	@RequestMapping(value= "/processor-class-show/get")
//	public String getProcessorClassName(@RequestParam("missioncode") String mission, 
//			 @RequestParam("processorclassnamecode") String processorclassname,Model model) { 
//		 if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassName({}, {}, {})", mission,  processorclassname, model.toString());
//		 
//		 Mono<String> mono = processorService.get(mission, processorclassname);
//	    processorDto ps = new processorDto();
//		 
//	    ObjectMapper mapper = new ObjectMapper();	
//		 mono.subscribe(value -> this.procs.add(value));
//		 model.addAttribute("procs", this.procs.toString());
//		 logger.trace(model.containsAttribute("procs") + "VALUE IN MODEL");
//		 logger.trace(model.toString() + "MODEL TO STRING");
//		 logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
//		 logger.trace(">>>>MONO" + procs.toString());
//		 logger.trace(">>>>MODEL" + model.toString());
//		 ps.setMono(mono);
//		 Mono<String> res = ps.getMono();
//		 res.subscribe(value-> this.procs1.add("" + value));
//		 logger.trace(">>>>RES PROCS1 LIST" + this.procs1.toString());
//		 return "processor-class-show :: #processors";
//	}
//	@RequestMapping(value= "/processor-class-show/get")
//	public String getProcessorClassName(@RequestParam("missioncode") String mission, 
//			 @RequestParam("processorclassnamecode") String processorclassname, Model model) { 
//		 if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassName({}, {}, {})", mission,  processorclassname);
//		 Mono<String> mono = processorService.get(mission, processorclassname);
//	    processorDto ps = new processorDto();
//		 
//	    ObjectMapper mapper = new ObjectMapper();	
//		 mono.subscribe(value -> this.procs.add(value));
//		 model.addAttribute("procs", messages());
//		
//		 logger.trace(model.toString() + "MODEL TO STRING");
//		 logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
//		 logger.trace(">>>>MONO" + procs.toString());
//		 logger.trace(">>>>MODEL" + model.toString());
//		 ps.setMono(mono);
//		 Mono<String> res = ps.getMono();
//		 res.subscribe(value-> this.procs1.add("" + value));
//		 logger.trace(">>>>RES PROCS1 LIST" + this.procs1.toString());
//		 logger.trace(model.toString());
//		 return "processor-class-show::form-basic";
//	}
	/**
	 * 
	 * @param mission            parameter of search
	 * @param processorclassname of search
	 * @param model              of current view
	 * @return thymeleaf fragment with result from the query
	 */
	@RequestMapping(value = "/processor-class-show/get")
	public DeferredResult<String> getProcessorClassName(
			@RequestParam(required = false, value = "mission") String mission,
			@RequestParam(required = false, value = "processorclassName") String processorclassname, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClassName({}, {}, model)", mission, processorclassname);
		Flux<RestProcessorClass> mono = processorService.get(mission, processorclassname)
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
			deferredResult.setResult("processor-class-show :: #content");
		});

		logger.trace("Immediately returning deferred result");
		return deferredResult;
	}

	@RequestMapping(value = "/processor-class-show-id/get")
	public DeferredResult<String> getProcessorClassById(@RequestParam(required = true, value = "id") String id,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClassById({}, {}, model)", id);
		List<Object> e = new ArrayList<>();
		Mono<Object> e21 = null;
		Flux<RestProcessorClass> mono = processorService.getById(id)
				.onStatus(HttpStatus::is4xxClientError,
						clientResponse -> Mono.error(
								new HttpClientErrorException(clientResponse.statusCode()).getMostSpecificCause()))
				.onStatus(HttpStatus::is5xxServerError,
						clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))

				.bodyToFlux(RestProcessorClass.class);

		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<RestProcessorClass> procs = new ArrayList<>();
		List<HttpClientErrorException> error = new ArrayList<>();
		mono.subscribe(processorClassList -> {
			logger.trace("Now in Consumer::accept({})", processorClassList);
			procs.add(processorClassList);
			model.addAttribute("procs", procs);

			logger.trace(model.toString() + "MODEL TO STRING");
			logger.trace(">>>> WRAPPER MONO: " + procs1.toString());
			logger.trace(">>>>MONO" + procs.toString());
			logger.trace(">>>>MODEL" + model.toString());
			deferredResult.setResult("processor-class-show-id :: #content");
		});

		logger.trace("Immediately returning deferred result");
		return deferredResult;
	}

	/**
	 * ONSTATUS Für Fehlermeldungen abarbeiten (typisch: 400 BAD REQUEST, 401 NOT
	 * AUTHORIZED, 404 NOT FOUND, 500 INTERNAL, CREATE=201 (FALLS 200 = LOGGEN),
	 * DELETE=204, REST=200
	 * 
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
//		Flux<RestProcessorClass> mono = processorService
//				.post(processorClassmission, processorClassName, processorClassProductClass)
//				.onStatus(HttpStatus::is4xxClientError,clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode()).getMostSpecificCause()))
//				.onStatus(HttpStatus::is5xxServerError,clientResponse -> Mono.error(new HttpClientErrorException(clientResponse.statusCode())))
//				.bodyToFlux(RestProcessorClass.class);
		StringBuilder sb = new StringBuilder();
		Mono<ClientResponse> mono = processorService.post(processorClassmission, processorClassName,
				processorClassProductClass);
//				.flatMap(clientResponse -> {
//					if(clientResponse.statusCode().is5xxServerError() || clientResponse.statusCode().is4xxClientError()) {
//						clientResponse.body((clientHttpResponse, context) -> {
//							this.clientError = true;
//						logger.trace(">>ClientError === {}", this.clientError);
//						logger.trace(">>>Warning Header: {}", clientHttpResponse.getHeaders().getFirst("Warning"));
//							logger.trace(">>>CLIENTRESPONSERRORBODY: {}", clientHttpResponse.getBody());
//							return clientHttpResponse.getBody();
//						});
//						
//						return clientResponse.bodyToMono(RestProcessorClass.class);
//				} else {
//					return clientResponse.bodyToMono(RestProcessorClass.class);
//				}
//				});

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
			logger.trace(">>> postProcessorClassName({}, {}, {}, {})", id, processorClassmission, processorClassName,
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

	private Function<? super Throwable, ? extends Throwable> mapper() {
		Function<? super Throwable, ? extends Throwable> s = mapper();

		return s;
	}

	// ALTE FUNKTIONEN MIT RESTTEMPLATE
//	@RequestMapping(value = "/processor-class-create/post")
//	public String postProcessorClass(@RequestParam("processorClassId") String processorClassId,
//			@RequestParam("processorClassVersion") String processorClassVersion,
//			@RequestParam("processorClassmissioncode") String processorClassmission,
//			@RequestParam("processorClassname") String processorClassName,
//			@RequestParam("processorClassProductClass") String processorClassProductClass, Model model) {
//		if (logger.isTraceEnabled())
//			logger.trace(">>> getProcessorClassName({}, {}, {})", processorClassmission, processorClassName,
//					processorClassId, processorClassVersion, processorClassmission, model.toString());
//
//		RestTemplate restTemplate = new RestTemplate();
//		// application.yml wurde nicht übernommen, base uri manuell übernommen
//		// config.getProcessorManager()
//		String uri = "http://localhost:8090/castlemock/mock/rest/project/eW2ujU/application/79M2j9"
//				+ "/processorclasses?mission=" + processorClassmission + "&processorname=" + processorClassName + "&id="
//				+ processorClassId + "&version=" + processorClassVersion + "&productClass" + processorClassProductClass;
//
//		@SuppressWarnings("rawtypes")
//		ResponseEntity<Map> response = restTemplate.postForEntity(uri, null, Map.class);
//		// Aus response.getBody() Daten rausholen und in model schreiben
//		ObjectMapper mapper = new ObjectMapper();
//		RestProcessorClass restProcessorClass = mapper.convertValue(response.getBody(), RestProcessorClass.class);
//
//		if (logger.isDebugEnabled())
//			logger.debug("Received response: {}", response.getBody());
//
//		model.addAttribute("id", restProcessorClass.getId());
//		model.addAttribute("mission", restProcessorClass.getMissionCode());
//		model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
//		model.addAttribute("version", restProcessorClass.getVersion());
//		model.addAttribute("productclasses", restProcessorClass.getProductClasses());
//		model.addAttribute("all", restProcessorClass);
//		// ....
//
//		return "processor-class-create";
//	}
//
//	@RequestMapping(value = "/processor-class-show-id/get")
//	public String getProcessorClassName(@RequestParam("processorClassId") String processorClassId, Model model) {
//		if (logger.isTraceEnabled())
//			logger.trace(">>> postProcessorClassName({}, {}, {})", processorClassId, model.toString());
//
//		RestTemplate restTemplate = new RestTemplate();
//		// application.yml wurde nicht übernommen, base uri manuell übernommen
//		// config.getProcessorManager()
//		String uri = "http://localhost:8090/castlemock/mock/rest/project/eW2ujU/application/79M2j9"
//				+ "/processorclasses?id=" + processorClassId;
//		@SuppressWarnings("rawtypes")
//		ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
//
//		// Aus response.getBody() Daten rausholen und in model schreiben
//		ObjectMapper mapper = new ObjectMapper();
//		RestProcessorClass restProcessorClass = mapper.convertValue(response.getBody(), RestProcessorClass.class);
//
//		if (logger.isDebugEnabled())
//			logger.debug("Received response: {}", response.getBody());
//
//		model.addAttribute("id", restProcessorClass.getId());
//		model.addAttribute("mission", restProcessorClass.getMissionCode());
//		model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
//		model.addAttribute("version", restProcessorClass.getVersion());
//		model.addAttribute("productclasses", restProcessorClass.getProductClasses());
//		model.addAttribute("all", restProcessorClass);
//		// ....
//
//		return "processor-class-show-id";
//	}
//
//	@RequestMapping(value = "/processor-class-update/patch")
//	public String patchProcessorClass(@RequestParam("processorClassId") String processorClassId,
//			@RequestParam("processorClassVersion") String processorClassVersion,
//			@RequestParam("processorClassmissioncode") String processorClassmission,
//			@RequestParam("processorClassname") String processorClassName,
//			@RequestParam("processorClassProductClass") String processorClassProductClass, Model model) {
//		if (logger.isTraceEnabled())
//			logger.trace(">>> patchProcessorClassName({}, {}, {})", processorClassmission, processorClassName,
//					processorClassId, processorClassVersion, processorClassmission, model.toString());
//
//		RestTemplate restTemplate = new RestTemplate();
//		// application.yml wurde nicht übernommen, base uri manuell übernommen
//		// config.getProcessorManager()
//		String uri = "http://localhost:8090/castlemock/mock/rest/project/eW2ujU/application/79M2j9"
//				+ "/processorclasses?id=" + processorClassId + "&mission=" + processorClassmission + "&processorname="
//				+ processorClassName + "&version=" + processorClassVersion + "&productClass="
//				+ processorClassProductClass;
//
//		HttpClient httpclient = HttpClientBuilder.create().build();
//		HttpUriRequest req = new HttpPatch(uri);
//		try {
//			ObjectMapper mapper = new ObjectMapper();
//			// mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
//			HttpResponse response = httpclient.execute(req);
//			RestProcessorClass restProcessorClass = mapper.readValue(response.getEntity().getContent(),
//					RestProcessorClass.class);
//
//			model.addAttribute("id", restProcessorClass.getId());
//			model.addAttribute("mission", restProcessorClass.getMissionCode());
//			model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
//			model.addAttribute("version", restProcessorClass.getVersion());
//			model.addAttribute("productclasses", restProcessorClass.getProductClasses());
//			model.addAttribute("all", restProcessorClass);
//
//		} catch (ClientProtocolException e) {
//			model.addAttribute("client", e.getMessage());
//		} catch (IOException e) {
//			model.addAttribute("IO", e.getMessage());
//		}
//		// Aus response.getBody() Daten rausholen und in model schreiben
//
//		// if (logger.isDebugEnabled()) logger.debug("Received response: {}",
//		// response.getBody());
//
//		// ....
//
//		return "processor-class-update";
//	}
//
//	@RequestMapping(value = "/processor-class-delete/id")
//	public String deleteProcessorClassById(@RequestParam("processorClassId") String processorClassId, Model model) {
//		if (logger.isTraceEnabled())
//			logger.trace(">>> deleteProcessorClassName({}, {}, {})", processorClassId, model.toString());
//
//		RestTemplate restTemplate = new RestTemplate();
//		// application.yml wurde nicht übernommen, base uri manuell übernommen
//		// config.getProcessorManager()
//		String uri = "http://localhost:8090/castlemock/mock/rest/project/eW2ujU/application/79M2j9"
//				+ "/processorclasses?id=" + processorClassId;
//
//		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, String.class);
//		model.addAttribute("status", response.getStatusCodeValue());
//		// Aus response.getBody() Daten rausholen und in model schreiben
//		ObjectMapper mapper = new ObjectMapper();
//		// RestProcessorClass restProcessorClass =
//		// mapper.convertValue(response.getBody(), RestProcessorClass.class);
//
//		// if (logger.isDebugEnabled()) logger.debug("Received response: {}",
//		// response.getBody());
//
////		model.addAttribute("id", restProcessorClass.getId());
////		model.addAttribute("mission", restProcessorClass.getMissionCode());
////		model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
////		model.addAttribute("version", restProcessorClass.getVersion());
////		model.addAttribute("productclasses", restProcessorClass.getProductClasses());
////		model.addAttribute("all", restProcessorClass);
////		model.addAttribute("message", "ERFOLGREICH");
////		//....
//
//		return "processor-class-delete";
//	}

	@RequestMapping(value = "/processor-class-show/{id}?get")
	public String getProcessorClassById(@PathVariable long id) {
		RestTemplate restTemplate = new RestTemplate();
		String uri = "processorclasses/" + id + "?get";
		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		return response.toString();
	}

	@RequestMapping(value = "/processor-class-update/{id}?patch")
	public String patchProcessorClassById(@PathVariable long id) {
		RestTemplate restTemplate = new RestTemplate();
		String uri = "processorclasses/" + id + "?patch";
		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		return response.toString();
	}

	@RequestMapping(value = "/processor-class-delete/{id}?delete")
	public String deleteProcessorClassById(@PathVariable long id) {
		RestTemplate restTemplate = new RestTemplate();
		String uri = "processorclasses/" + id + "?delete";
		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		return response.toString();
	}

}
