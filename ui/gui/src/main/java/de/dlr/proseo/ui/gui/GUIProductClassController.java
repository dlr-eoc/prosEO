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
import de.dlr.proseo.ui.gui.service.MapComparator;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
@Controller
public class GUIProductClassController extends GUIBaseController {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIProductClassController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;
	
	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	    @RequestMapping(value = "/productclass-show")
	    public String showProductClass() {
	    
	    return "productclass-show";
	    }

		@SuppressWarnings("unchecked")
		@RequestMapping(value = "/productclass/get")
		public DeferredResult<String> getProductClasses(
				@RequestParam(required = false, value = "productClass") String productClass,
				@RequestParam(required = false, value = "processorClass") String processorClass,
				@RequestParam(required = false, value = "level") String level,
				@RequestParam(required = false, value = "visibility") String visibility,
				@RequestParam(required = false, value = "sortby") String sortby,
				@RequestParam(required = false, value = "up") Boolean up, 
				@RequestParam(required = false, value = "recordFrom") Long fromIndex,
				@RequestParam(required = false, value = "recordTo") Long toIndex, Model model) {
			if (logger.isTraceEnabled())
				logger.trace(">>> getProductClasses({}, {}, model)", fromIndex, toIndex);
			Long from = null;
			Long to = null;
			if (fromIndex != null && fromIndex >= 0) {
				from = fromIndex;
			} else {
				from = (long) 0;
			}
			Long count = countProductClasses(productClass, processorClass, level, visibility);
			if (toIndex != null && from != null && toIndex > from) {
				to = toIndex;
			} else if (from != null) {
				to = count;
			}
			Long pageSize = to - from;
			Long deltaPage = (long) ((count % pageSize)==0?0:1);
			Long pages = (count / pageSize) + deltaPage;
			Long page = (from / pageSize) + 1;
			Mono<ClientResponse> mono = get(productClass, processorClass, level, visibility, from, to);
			DeferredResult<String> deferredResult = new DeferredResult<String>();
			List<Object> productclasses = new ArrayList<>();
			mono.doOnError(e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("productclass-show :: #errormsg");
			})
		 	.subscribe(clientResponse -> {
				logger.trace("Now in Consumer::accept({})", clientResponse);
				if (clientResponse.statusCode().is2xxSuccessful()) {
					clientResponse.bodyToMono(List.class).subscribe(pcList -> {
						productclasses.addAll(pcList);
						
						MapComparator oc = new MapComparator("productType", true);
						productclasses.sort(oc);
						
						sortSelectionRules(productclasses);
						
						model.addAttribute("productclasses", productclasses);
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
						logger.trace(model.toString() + "MODEL TO STRING");
						logger.trace(">>>>MONO" + productclasses.toString());
						deferredResult.setResult("productclass-show :: #productclasscontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				} else {
					handleHTTPError(clientResponse, model);
					deferredResult.setResult("productclass-show :: #errormsg");
				}
				logger.trace(">>>>MODEL" + model.toString());

			},
			e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("productclass-show :: #errormsg");
			});
			logger.trace(model.toString() + "MODEL TO STRING");
			logger.trace(">>>>MONO" + productclasses.toString());
			logger.trace(">>>>MODEL" + model.toString());
			logger.trace("DEREFFERED STRING: {}", deferredResult);
			return deferredResult;
		}

	    private Long countProductClasses(String productType, String processorClass, String level, String visibility) {
	    	
			GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
			String mission = auth.getMission();
			String divider = "?";
			String uri = "/productclasses/count";
			if (mission != null && !mission.isEmpty()) {
				uri += divider + "mission=" + mission;
				divider ="&";
			}
			if (productType != null && !productType.isEmpty()) {
				String [] pcs = productType.split(",");
				for (String pc : pcs) {
					uri += divider + "productType=" + pc;
					divider ="&";
				}
			}
			if (processorClass != null && !processorClass.isEmpty()) {
				String [] pcs = processorClass.split(",");
				for (String pc : pcs) {
					uri += divider + "processorClass=" + pc;
					divider ="&";
				}
			}
			if (level != null) {
				uri += divider + "level=" + level;
				divider ="&";
			}
			if (visibility != null) {
				uri += divider + "visibility=" + visibility;
				divider ="&";
			}
			Long result = (long) -1;
			try {
				String resStr = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
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
		private Mono<ClientResponse> get(String productType, String processorClass, String level, String visibility, Long from, Long to) {
			GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
			String mission = auth.getMission();
			String uri = serviceConfig.getProductClassManagerUrl() + "/productclasses";
			String divider = "?";
			if (mission != null && !mission.isEmpty()) {
				uri += divider + "mission=" + mission;
				divider ="&";
			}
			if (productType != null && !productType.isEmpty()) {
				String [] pcs = productType.split(",");
				for (String pc : pcs) {
					uri += divider + "productType=" + pc;
					divider ="&";
				}
			}
			if (processorClass != null && !processorClass.isEmpty()) {
				String [] pcs = processorClass.split(",");
				for (String pc : pcs) {
					uri += divider + "processorClass=" + pc;
					divider ="&";
				}
			}
			if (level != null) {
				uri += divider + "level=" + level;
				divider ="&";
			}
			if (visibility != null) {
				uri += divider + "visibility=" + visibility;
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
			uri += divider + "orderBy=productType ASC";
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
		
		@SuppressWarnings("unchecked")
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


