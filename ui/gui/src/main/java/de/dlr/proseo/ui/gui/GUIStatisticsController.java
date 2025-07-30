/**
 * GUIStatisticsController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.ui.gui.service.StatisticsService;

/**
 * A controller for retrieving the dashboard and prosEO home view, as well as the latest successful and failed job steps
 *
 * @author David Mazo
 */
@Controller
public class GUIStatisticsController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIStatisticsController.class);

	/** WebClient-Service-Builder */
	@Autowired
	private StatisticsService statisticsService;

	/**
	 * Show the dashboard view
	 *
	 * @return the name of the dashboard view template
	 */
	@GetMapping(value = "/dashboard")
	public String dashboard() {
		return "dashboard";
	}

	/**
	 * Show the prosEO home view
	 *
	 * @param model the attributes to return
	 * @return the name of the prosEO home view template
	 */
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
	@GetMapping("/failedjobsteps/get")
	public DeferredResult<String> getFailedJobsteps(@RequestParam(required = true, value = "latest") Integer count, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getIdentifier({}, model)", count);

		// Perform the HTTP request to retrieve the failed job steps
		ResponseSpec responseSpec = statisticsService.getJobsteps("FAILED", count.longValue());
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> jobsteps = new ArrayList<>();

		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				if (e instanceof WebClientResponseException.NotFound) {
					model.addAttribute("failedjobsteps", jobsteps);
					
					logger.trace(model.toString() + "MODEL TO STRING");
					deferredResult.setResult("dashboard :: #failedjs");
				} else {
					model.addAttribute("errormsg", e.getMessage());
					deferredResult.setResult("dashboard :: #errormsg");
				}
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);

				if (entityList.getStatusCode().is2xxSuccessful()) {
					jobsteps.addAll(entityList.getBody());
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
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("dashboard :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("dashboard :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + jobsteps.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		// Return the deferred result
		return deferredResult;
	}

	/**
	 * Gets the latest job steps completed successfully
	 *
	 * @param count the maximum number of completed job steps to return
	 * @param model the Thymeleaf model to update
	 * @return a Thymeleaf fragment
	 */
	@GetMapping("/completedjobsteps/get")
	public DeferredResult<String> getCompletedJobsteps(@RequestParam(required = false, value = "latest") Integer count,
			Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getIdentifier({}, model)", count);

		// Perform the HTTP request to retrieve the completed job steps
		ResponseSpec responseSpec = statisticsService.getJobsteps("COMPLETED:CLOSED", count.longValue());
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> jobsteps = new ArrayList<>();

		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				if (e instanceof WebClientResponseException.NotFound) {
					model.addAttribute("completedjobsteps", jobsteps);
					
					logger.trace(model.toString() + "MODEL TO STRING");
					deferredResult.setResult("dashboard :: #completedjs");
				} else {
					model.addAttribute("errormsg", e.getMessage());
					deferredResult.setResult("dashboard :: #errormsg");
				}
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);
				if (entityList.getStatusCode().is2xxSuccessful()) {
					jobsteps.addAll(entityList.getBody());
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
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("dashboard :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("dashboard :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + jobsteps.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		// Return the deferred result
		return deferredResult;
	}

}