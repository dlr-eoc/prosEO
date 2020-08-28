package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.gui.service.OrderComparator;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
@Controller
public class GUIProductClassController extends GUIBaseController {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIProductClassController.class);

	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;
	
	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	    @RequestMapping(value = "/productclass-show")
	    public String showProductClass() {
	    
	    return "productclass-show";
	    }
	    @RequestMapping(value = "/productclass-create")
	    public String createProductClass() {
	    
	    return "productclass-create";
	    }
	    @RequestMapping(value = "/productclass-update")
	    public String updateProductClass() {
	    
	    return "productclass-update";
	    }
	    @RequestMapping(value = "/productclass-delete")
	    public String deleteProductClass() {
	    
	    return "productclass-delete";
	    }
	    @RequestMapping(value = "/productclass-rule-show")
	    public String showProductClassRule() {
	    
	    return "productclass-rule-show";
	    }
	    @RequestMapping(value = "/productclass-rule-create")
	    public String createProductClassRule() {
	    
	    return "productclass-rule-create";
	    }
	    @RequestMapping(value = "/productclass-rule-update")
	    public String updateProductClassRule() {
	    
	    return "productclass-rule-update";
	    }
	    @RequestMapping(value = "/productclass-rule-delete")
	    public String deleteProductClassRule() {
	    
	    return "productclass-rule-delete";
	    }

		@SuppressWarnings("unchecked")
		@RequestMapping(value = "/productclass/get")
		public DeferredResult<String> getProductClasses(
				@RequestParam(required = false, value = "sortby") String sortby,
				@RequestParam(required = false, value = "up") Boolean up, Model model) {
			if (logger.isTraceEnabled())
				logger.trace(">>> getProductClasses(model)");
			Mono<ClientResponse> mono = get();
			DeferredResult<String> deferredResult = new DeferredResult<String>();
			List<Object> productclasses = new ArrayList<>();
			mono.subscribe(clientResponse -> {
				logger.trace("Now in Consumer::accept({})", clientResponse);
				if (clientResponse.statusCode().is5xxServerError()) {
					logger.trace(">>>Server side error (HTTP status 500)");
					model.addAttribute("errormsg", "Server side error (HTTP status 500)");
					deferredResult.setResult("productclass-show :: #productclasscontent");
					logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
				} else if (clientResponse.statusCode().is4xxClientError()) {
					logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
					model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
					deferredResult.setResult("productclass-show :: #productclasscontent");
					logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
				} else if (clientResponse.statusCode().is2xxSuccessful()) {
					clientResponse.bodyToMono(List.class).subscribe(pcList -> {
						productclasses.addAll(pcList);
					
						model.addAttribute("productclasses", productclasses);
						logger.trace(model.toString() + "MODEL TO STRING");
						logger.trace(">>>>MONO" + productclasses.toString());
						deferredResult.setResult("productclass-show :: #productclasscontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				}
				logger.trace(">>>>MODEL" + model.toString());

			});
			logger.trace(model.toString() + "MODEL TO STRING");
			logger.trace(">>>>MONO" + productclasses.toString());
			logger.trace(">>>>MODEL" + model.toString());
			logger.trace("DEREFFERED STRING: {}", deferredResult);
			return deferredResult;
		}

		public Mono<ClientResponse> get() {
			GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
			String mission = auth.getMission();
			String uri = serviceConfig.getProductClassManagerUrl() + "/productclasses";
			uri += "?mission=" + mission;
			logger.trace("URI " + uri);
			Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
					HttpClient.create().followRedirect((req, res) -> {
						logger.trace("response:{}", res.status());
						return HttpResponseStatus.FOUND.equals(res.status());
					})
				));
			logger.trace("Found authentication: " + auth);
			logger.trace("... with username " + auth.getName());
			logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
			return  webclient.build().get().uri(uri).headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword())).accept(MediaType.APPLICATION_JSON).exchange();

		}
	}


