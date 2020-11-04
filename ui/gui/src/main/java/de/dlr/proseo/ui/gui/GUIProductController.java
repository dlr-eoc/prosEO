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
public class GUIProductController extends GUIBaseController {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIProductClassController.class);

	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;
	
	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

    @RequestMapping(value = "/product-show")
    public String showProduct(){
    	return "product-show";
    }
    
    @RequestMapping(value = "/productfile-show")
    public String showProductFile(){
    	return "productfile-show";
    }
   
	/**
	 * Retrieve products selected by id, product class and start time  
	 * @param id The product id or null for all
	 * @param productClass The product class or null for all
	 * @param startTimeFrom The from of start time range
	 * @param startTimeTo The to of start time range
	 * @param sortby The sort column
	 * @param up The sort direction (true for up)
	 * @param model The model to hold the data
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/product/get")
	public DeferredResult<String> getProducts(
			@RequestParam(required = false, value = "id") Long id,
			@RequestParam(required = false, value = "productClass") String productClass,
			@RequestParam(required = false, value = "startTimeFrom") String startTimeFrom,
			@RequestParam(required = false, value = "startTimeTo") String startTimeTo,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		
		logger.trace(">>> getProducs({}, {}, {}, model)", productClass, startTimeFrom, startTimeTo);
		Mono<ClientResponse> mono = get(id, productClass, startTimeFrom, startTimeTo);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> products = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("product-show :: #productcontent");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("product-show :: #productcontent");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				if (id != null && id > 0) {
					clientResponse.bodyToMono(HashMap.class).subscribe(p -> {
						products.add(p);
						model.addAttribute("products", products);
						if (logger.isTraceEnabled()) logger.trace(model.toString() + "MODEL TO STRING");
						if (logger.isTraceEnabled()) logger.trace(">>>>MONO" + products.toString());
						deferredResult.setResult("product-show :: #productcontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				} else {
					clientResponse.bodyToMono(List.class).subscribe(pList -> {
						products.addAll(pList);
						MapComparator oc = new MapComparator("productClass", true);
						products.sort(oc);
						model.addAttribute("products", products);
						if (logger.isTraceEnabled()) logger.trace(model.toString() + "MODEL TO STRING");
						if (logger.isTraceEnabled()) logger.trace(">>>>MONO" + products.toString());
						deferredResult.setResult("product-show :: #productcontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				}
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + products.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	/**
	 * Retrieve the product file with id or all if id is null
	 * @param id The product file id or null
	 * @param sortby The sort column
	 * @param up The sort direction (true for up)
	 * @param model The model to hold the data
	 * @return The result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/productfile/get")
	public DeferredResult<String> getProductFiles(
			@RequestParam(required = false, value = "id") Long id,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		
		logger.trace(">>> getProductFiles({}, {}, {}, {}, model)", id);
		Mono<ClientResponse> mono = get(id, null, null, null);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> productfiles = new ArrayList<>();
		mono.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is5xxServerError()) {
				logger.trace(">>>Server side error (HTTP status 500)");
				model.addAttribute("errormsg", "Server side error (HTTP status 500)");
				deferredResult.setResult("productfile-show :: #productfilecontent");
				logger.trace(">>DEFERREDRES 500: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is4xxClientError()) {
				logger.trace(">>>Warning Header: {}", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				model.addAttribute("errormsg", clientResponse.headers().asHttpHeaders().getFirst("Warning"));
				deferredResult.setResult("productfile-show :: #productfilecontent");
				logger.trace(">>DEFERREDRES 4xx: {}", deferredResult.getResult());
			} else if (clientResponse.statusCode().is2xxSuccessful()) {
				if (id != null && id > 0) {
					clientResponse.bodyToMono(HashMap.class).subscribe(p -> {
						productfiles.addAll((Collection<? extends Object>) p.get("productFile"));
						model.addAttribute("productfiles", productfiles);
						if (logger.isTraceEnabled()) logger.trace(model.toString() + "MODEL TO STRING");
						if (logger.isTraceEnabled()) logger.trace(">>>>MONO" + productfiles.toString());
						deferredResult.setResult("productfile-show :: #productfilecontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				}
			}
			logger.trace(">>>>MODEL" + model.toString());

		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + productfiles.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	private Mono<ClientResponse> get(Long id, String productClass, String startTimeFrom, String startTimeTo) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = serviceConfig.getIngestorUrl() + "/products";
		if (id != null && id > 0) {
			uri += "/" + id.toString();
		} else {
			String divider = "?";
			if (productClass != mission && !mission.isEmpty()) {
				uri += divider + "mission=" + mission;
				divider ="&";
			}
			if (productClass != null && !productClass.isEmpty()) {
				uri += divider + "productClass=" + productClass;
			}
			if (startTimeFrom != null && !startTimeFrom.isEmpty()) {
				uri += divider + "startTimeFrom=" + startTimeFrom;
			}
			if (startTimeTo != null && !startTimeTo.isEmpty()) {
				uri += divider + "startTimeTo=" + startTimeTo;
			}
		}
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
