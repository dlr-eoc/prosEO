package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.servlet.ModelAndView;

import de.dlr.proseo.ui.gui.service.OrderService;
import reactor.core.publisher.Mono;
@Controller
public class GUIOrderController extends GUIBaseController {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIOrderController.class);

	/** WebClient-Service-Builder */
	@Autowired
	private OrderService orderService;

	@RequestMapping(value = "/order-close")
	public String closeOrder() {

		return "order-close";
	}
	@RequestMapping(value = "/order-create")
	public String createOrder() {

		return "order-create";
	}
	@RequestMapping(value = "/order-delete")
	public String deleteOrder() {

		return "order-delete";
	}
	@RequestMapping(value = "/order-plan")
	public String planOrderl() {

		return "order-plan";
	}
	@RequestMapping(value = "/order-release")
	public String releaseOrder() {

		return "order-release";
	}
	@RequestMapping(value = "/order-resume")
	public String resumeOrder() {

		return "order-resume";
	}
	@GetMapping(value ="/order-show")
	public String showOrder() {
		ModelAndView modandview = new ModelAndView("order-show");
		modandview.addObject("message", "TEST");
		return "order-show";
	}
	@RequestMapping(value = "/order-suspend")
	public String suspendOrder() {

		return "order-suspend";
	}
	@RequestMapping(value = "/order-update")
	public String updateOrder() {

		return "order-update";
	}

    
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/order-show/get")
	public DeferredResult<String> getIdentifier(
			@RequestParam(required = false, value = "identifier") String identifier, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getIdentifier({}, {}, model)", identifier, identifier);
		Mono<ClientResponse> mono = orderService.get(identifier);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> orders = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("order-show :: #content");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("order-show :: #content");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				clientResponse.bodyToMono(List.class).subscribe(orderList -> {
					orders.addAll(orderList);
					model.addAttribute("orders", orders);
					logger.trace(model.toString() + "MODEL TO STRING");
					logger.trace(">>>>MONO" + orders.toString());
					deferredResult.setResult("order-show :: #content");
					logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
				});
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + orders.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

}


