package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

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

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.gui.service.MapComparator;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
@Controller
public class GUIFacilityController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIFacilityController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;
			    
	    @RequestMapping(value = "/facility-show")
	    public String showFacility() {
	    
	    return "facility-show";
	    }

		/**
		 * Retrieve the defined processing facilities
		 * 
		 * @param sortby The sort column
		 * @param up The sort direction (true for up)
		 * @param model The model to hold the data
		 * @return
		 */
		@SuppressWarnings("unchecked")
		@RequestMapping(value = "/facilities/get")
		public DeferredResult<String> getFacilities(
				@RequestParam(required = false, value = "sortby") String sortby,
				@RequestParam(required = false, value = "up") Boolean up, Model model) {
			if (logger.isTraceEnabled())
				logger.trace(">>> getFacilities(model)");
			Mono<ClientResponse> mono = get();
			DeferredResult<String> deferredResult = new DeferredResult<String>();
			List<Object> facilities = new ArrayList<>();
			mono.doOnError(e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("facility-show :: #errormsg");
			})
		 	.subscribe(clientResponse -> {
				logger.trace("Now in Consumer::accept({})", clientResponse);
				if (clientResponse.statusCode().is2xxSuccessful()) {
					clientResponse.bodyToMono(List.class).subscribe(pcList -> {
						facilities.addAll(pcList);
						
						MapComparator oc = new MapComparator("name", true);
						facilities.sort(oc);
					
						model.addAttribute("facilities", facilities);
						logger.trace(model.toString() + "MODEL TO STRING");
						logger.trace(">>>>MONO" + facilities.toString());
						deferredResult.setResult("facility-show :: #facilitycontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				} else {
					handleHTTPError(clientResponse, model);
					deferredResult.setResult("facility-show :: #errormsg");
				}
				logger.trace(">>>>MODEL" + model.toString());

			},
			e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("facility-show :: #errormsg");
			});
			logger.trace(model.toString() + "MODEL TO STRING");
			logger.trace(">>>>MONO" + facilities.toString());
			logger.trace(">>>>MODEL" + model.toString());
			logger.trace("DEREFFERED STRING: {}", deferredResult);
			return deferredResult;
		}

		private Mono<ClientResponse> get() {
			GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
			String uri = serviceConfig.getFacilityManagerUrl() + "/facilities";
			
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





