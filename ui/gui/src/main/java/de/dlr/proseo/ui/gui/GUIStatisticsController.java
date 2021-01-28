package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.service.StatisticsService;
import reactor.core.publisher.Mono;

@Controller
public class GUIStatisticsController extends GUIBaseController {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIStatisticsController.class);

	/** WebClient-Service-Builder */
	@Autowired
	private StatisticsService statisticsService;

	@GetMapping(value = "/dashboard")
	public String dashboard() {

		return "dashboard";
	}

	@GetMapping("/")
	public String index(Model model) {
		return "proseo-home";

	}

	/**
	 * Gets the latest failed job steps
	 * 
	 * @param count the maximum number of failed job steps to return
	 * @param model the Thymeleaf model to update
	 * @return a Thymeleaf fragment
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/failedjobsteps/get")
	public DeferredResult<String> getFailedJobsteps(@RequestParam(required = true, value = "latest") Integer count, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getIdentifier({}, model)", count);
		Mono<ClientResponse> mono = statisticsService.getJobsteps("FAILED", count.longValue());
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> jobsteps = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("dashboard :: #failedjs");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("dashboard :: #failedjs");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(jobstepList -> {
					jobsteps.addAll(jobstepList);
					List<Object> failedjobsteps = null;
					if (jobsteps.size() > count) {
						failedjobsteps = jobsteps.subList(0, count - 1);
					} else {
						failedjobsteps = jobsteps;
					}
					model.addAttribute("failedjobsteps", failedjobsteps);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + failedjobsteps.toString());
					deferredResult.setResult("dashboard :: #failedjs");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + jobsteps.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	/**
	 * Gets the latest job steps completed successfully
	 * 
	 * @param count the maximum number of completed job steps to return
	 * @param model the Thymeleaf model to update
	 * @return a Thymeleaf fragment
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/completedjobsteps/get")
	public DeferredResult<String> getCompletedJobsteps(@RequestParam(required = false, value = "latest") Integer count,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getIdentifier({}, model)", count);
		Mono<ClientResponse> mono = statisticsService.getJobsteps("COMPLETED", count.longValue());
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> jobsteps = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("dashboard :: #completedjs");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("dashboard :: #completedjs");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(jobstepList -> {
					jobsteps.addAll(jobstepList);
					List<Object> completedjobsteps = null;
					if (jobsteps.size() > count) {
						completedjobsteps = jobsteps.subList(0, count);
					} else {
						completedjobsteps = jobsteps;
					}
					model.addAttribute("completedjobsteps", completedjobsteps);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + completedjobsteps.toString());
					deferredResult.setResult("dashboard :: #completedjs");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + jobsteps.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}
}
