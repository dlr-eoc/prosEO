package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Controller
public class GUIProductController extends GUIBaseController {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIProductClassController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
	

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
			@RequestParam(required = false, value = "mode") String mode,
			@RequestParam(required = false, value = "fileClass") String fileClass,
			@RequestParam(required = false, value = "quality") String quality,
			@RequestParam(required = false, value = "startTimeFrom") String startTimeFrom,
			@RequestParam(required = false, value = "startTimeTo") String startTimeTo,
			@RequestParam(required = false, value = "genTimeFrom") String genTimeFrom,
			@RequestParam(required = false, value = "genTimeTo") String genTimeTo,
			@RequestParam(required = false, value = "recordFrom") Long fromIndex,
			@RequestParam(required = false, value = "recordTo") Long toIndex,
			@RequestParam(required = false, value = "jobStepId") Long jobStepId,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {
		
		logger.trace(">>> getProducs({}, {}, {}, {}, {}, model)", productClass, mode, fileClass, quality, startTimeFrom, startTimeTo, 
				genTimeFrom, genTimeTo, fromIndex, toIndex);
		Long from = null;
		Long to = null;
		if (fromIndex != null && fromIndex >= 0) {
			from = fromIndex;
		} else {
			from = (long) 0;
		}
		Long count = countProducts(productClass, mode, fileClass, quality, startTimeFrom, startTimeTo, 
				genTimeFrom, genTimeTo, jobStepId);
		if (toIndex != null && from != null && toIndex > from) {
			to = toIndex;
		} else if (from != null) {
			to = count;
		}
		Mono<ClientResponse> mono = get(id, productClass, mode, fileClass, quality, startTimeFrom, startTimeTo, 
				genTimeFrom, genTimeTo, from, to, jobStepId);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> products = new ArrayList<>();

		Long pageSize = to - from;
		Long deltaPage = (long) ((count % pageSize)==0?0:1);
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;
		mono.doOnError(e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("product-show :: #errormsg");
		})
	 	.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is2xxSuccessful()) {
				if (id != null && id > 0) {
					clientResponse.bodyToMono(HashMap.class).subscribe(p -> {
						products.add(p);
						model.addAttribute("products", products);
						model.addAttribute("count", 1);
						model.addAttribute("pageSize", 1);
						model.addAttribute("pageCount", 1);
						model.addAttribute("page", 1);
						List<Long> showPages = new ArrayList<Long>();
						showPages.add((long) 1);
						model.addAttribute("showPages", showPages);
						if (logger.isTraceEnabled()) logger.trace(model.toString() + "MODEL TO STRING");
						if (logger.isTraceEnabled()) logger.trace(">>>>MONO" + products.toString());
						deferredResult.setResult("product-show :: #productcontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				} else {
					clientResponse.bodyToMono(List.class).subscribe(pList -> {
						products.addAll(pList);
						//MapComparator oc = new MapComparator("productClass", true);
						//products.sort(oc);
						model.addAttribute("products", products);
						model.addAttribute("count", count);
						model.addAttribute("pageSize", pageSize);
						model.addAttribute("pageCount", pages);
						model.addAttribute("page", page);
						List<Long> showPages = new ArrayList<Long>();
						Long start = Math.max(page - 4, 1);
						Long end = Math.min(page + 4, pages);
						if (page < 5) {
							end = Math.min(end + (5 - page), pages);
						}
						if (pages - page < 5) {
							start = Math.max(start - (4 - (pages - page)), 1);
						}
						for (Long i = start; i <= end; i++) {
							showPages.add(i);
						}
						model.addAttribute("showPages", showPages);
						if (logger.isTraceEnabled()) logger.trace(model.toString() + "MODEL TO STRING");
						if (logger.isTraceEnabled()) logger.trace(">>>>MONO" + products.toString());
						deferredResult.setResult("product-show :: #productcontent");
						
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				}
			} else {
				handleHTTPError(clientResponse, model);
				deferredResult.setResult("product-show :: #errormsg");
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("product-show :: #errormsg");
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
		Mono<ClientResponse> mono = get(id, null, null, null, null, null, null, null, null, null, null, null);
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		List<Object> productfiles = new ArrayList<>();
		mono.doOnError(e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("productfile-show :: #errormsg");
		})
	 	.subscribe(clientResponse -> {
			logger.trace("Now in Consumer::accept({})", clientResponse);
			if (clientResponse.statusCode().is2xxSuccessful()) {
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
			} else {
				handleHTTPError(clientResponse, model);
				deferredResult.setResult("productfile-show :: #errormsg");
			}
			logger.trace(">>>>MODEL" + model.toString());

		},
		e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("productfile-show :: #errormsg");
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + productfiles.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}
    private Long countProducts(String productClass, String mode, String fileClass, String quality, String startTimeFrom, String startTimeTo, 
    		String genTimeFrom, String genTimeTo, Long jobStepId) {
    	
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String divider = "?";
		String uri = "/products/count";
		if (productClass != mission && !mission.isEmpty()) {
			uri += divider + "mission=" + mission;
			divider ="&";
		}
		if (productClass != null && !productClass.isEmpty()) {
			String [] pcs = productClass.split(",");
			for (String pc : pcs) {
				uri += divider + "productClass=" + pc;
				divider ="&";
			}
		}
		if (mode != null && !mode.isEmpty()) {
			uri += divider + "mode=" + mode;
			divider ="&";
		}
		if (fileClass != null && !fileClass.isEmpty()) {
			uri += divider + "fileClass=" + fileClass;
			divider ="&";
		}
		if (quality != null && !quality.isEmpty()) {
			uri += divider + "quality=" + quality;
			divider ="&";
		}
		if (startTimeFrom != null && !startTimeFrom.isEmpty()) {
			uri += divider + "startTimeFrom=" + startTimeFrom;
			divider ="&";
		}
		if (startTimeTo != null && !startTimeTo.isEmpty()) {
			uri += divider + "startTimeTo=" + startTimeTo;
			divider ="&";
		}
		if (genTimeFrom != null && !genTimeFrom.isEmpty()) {
			uri += divider + "genTimeFrom=" + genTimeFrom;
			divider ="&";
		}
		if (genTimeTo != null && !genTimeTo.isEmpty()) {
			uri += divider + "genTimeTo=" + genTimeTo;
			divider ="&";
		}
		if (jobStepId != null) {
			uri += divider + "jobStep=" + jobStepId;
			divider ="&";
		}
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getIngestorUrl(),
					uri, String.class, auth.getProseoName(), auth.getPassword());

			if (resStr != null && resStr.length() > 0) {
				result = Long.valueOf(resStr);
			}
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return result;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return result;
		}
		
        return result;
    }
	private Mono<ClientResponse> get(Long id, String productClass, String mode, String fileClass, String quality, String startTimeFrom, String startTimeTo, 
    		String genTimeFrom, String genTimeTo, Long from, Long to, Long jobStepId) {
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String uri = serviceConfig.getIngestorUrl() + "/products";
		if (id != null && id > 0) {
			uri += "/" + id.toString();
		} else {
			String divider = "?";
			if (mission != null && !mission.isEmpty()) {
				uri += divider + "mission=" + mission;
				divider ="&";
			}
			if (productClass != null && !productClass.isEmpty()) {
				String [] pcs = productClass.split(",");
				for (String pc : pcs) {
					uri += divider + "productClass=" + pc;
					divider ="&";
				}
			}
			if (mode != null && !mode.isEmpty()) {
				uri += divider + "mode=" + mode;
				divider ="&";
			}
			if (fileClass != null && !fileClass.isEmpty()) {
				uri += divider + "fileClass=" + fileClass;
				divider ="&";
			}
			if (quality != null && !quality.isEmpty()) {
				uri += divider + "quality=" + quality;
				divider ="&";
			}
			if (startTimeFrom != null && !startTimeFrom.isEmpty()) {
				uri += divider + "startTimeFrom=" + startTimeFrom;
				divider ="&";
			}
			if (startTimeTo != null && !startTimeTo.isEmpty()) {
				uri += divider + "startTimeTo=" + startTimeTo;
				divider ="&";
			}
			if (genTimeFrom != null && !genTimeFrom.isEmpty()) {
				uri += divider + "genTimeFrom=" + genTimeFrom;
				divider ="&";
			}
			if (genTimeTo != null && !genTimeTo.isEmpty()) {
				uri += divider + "genTimeTo=" + genTimeTo;
				divider ="&";
			}
			if (from != null) {
				uri += divider + "recordFrom=" + from;
				divider ="&";
			}
			if (to != null) {
				uri += divider + "recordTo=" + to;
				divider ="&";
			}
			if (jobStepId != null) {
				uri += divider + "jobStep=" + jobStepId;
				divider ="&";
			}
			uri += divider + "orderBy=productClass.productType ASC,sensingStartTime ASC";
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
