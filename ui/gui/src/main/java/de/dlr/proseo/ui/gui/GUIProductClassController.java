package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import de.dlr.proseo.ui.gui.service.MapComparator;
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
						sortSelectionRules(productclasses);
						
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

		private Mono<ClientResponse> get() {
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
		
		private void sortSelectionRules(List<Object> productclasses) {
			if (productclasses != null) {
				for(Object o1 : productclasses) {
					if (o1 instanceof HashMap) {
						HashMap<String, HashMap<String, Object>> sortedList = new HashMap<String, HashMap<String, Object>>();
						HashMap<String, Object> h1 = (HashMap<String, Object>) o1;
						Object sro = h1.get("selectionRule");
						if (sro instanceof List) {
							List<Object> srl = (List<Object>) sro;
							for (Object o2 : srl) {
								if (o2 instanceof HashMap) {
									HashMap<?, ?> sr = (HashMap<?, ?>) o2;
									// now we have a selection rule
									// collect all modes in a new hash map
									String mode = (String)sr.get("mode");
									if (!sortedList.containsKey(mode)) {
										HashMap<String, Object> localList = new HashMap<String, Object>();
										localList.put("mode", mode);
										localList.put("selRules", new ArrayList<Object>());
										sortedList.put(mode, localList);
									}
								}
							}
							for (Object o2 : srl) {
								if (o2 instanceof HashMap) {
									HashMap<?, ?> sr = (HashMap<?, ?>) o2;
									// now we have a selection rule
									// collect all modes in a new hash map
									String mode = (String)sr.get("mode");								
									((List<Object>)sortedList.get(mode).get("selRules")).add(sr);
								}
							}
							for (HashMap<String, Object> modeList : sortedList.values()) {
								Object listObj = modeList.get("selRules");
								if (listObj instanceof List ) {
									List<Object> list = (List<Object>)listObj;
									MapComparator oc = new MapComparator("sourceProductClass", true);
									list.sort(oc);
								}
							}	
							MapComparator mlc = new MapComparator("mode", true);
							Collection<HashMap<String, Object>> mList = sortedList.values();
							List<Object> cList = new ArrayList<Object>();
							cList.addAll(mList);
							cList.sort(mlc);
							((HashMap<String, Object>)h1).put("sortedSelectionRules", cList);
						}
					}
				}
			}
		}
	}


