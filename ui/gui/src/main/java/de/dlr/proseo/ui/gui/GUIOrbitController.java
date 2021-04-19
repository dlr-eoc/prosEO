package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

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
public class GUIOrbitController extends GUIBaseController {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIConfigurationController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;
	
	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
	
			    
	    @RequestMapping(value = "/orbit-show")
	    public String showOrbit() {
	    
	    return "orbit-show";
	    }

		/**
		 * Retrieve the orbits of a spacecraft
		 * 
		 * @param spacecraft The spacecraft identifier
		 * @param sortby The sort column
		 * @param up The sort direction (true for up)
		 * @param model The model to hold the data
		 * @return The result
		 */
		@SuppressWarnings("unchecked")
		@RequestMapping(value = "/orbits/get")
		public DeferredResult<String> getOrbits(
				@RequestParam(required = false, value = "spacecraft") String spacecraft,
				@RequestParam(required = false, value = "recordFrom") Long fromIndex,
				@RequestParam(required = false, value = "recordTo") Long toIndex,
				@RequestParam(required = false, value = "sortby") String sortby,
				@RequestParam(required = false, value = "up") Boolean up, Model model) {
			if (logger.isTraceEnabled())
				logger.trace(">>> getOrbits(model)");
			Long from = null;
			Long to = null;
			if (fromIndex != null && fromIndex >= 0) {
				from = fromIndex;
			} else {
				from = (long) 0;
			}
			Long count = countOrbits(spacecraft);
			if (toIndex != null && from != null && toIndex > from) {
				to = toIndex;
			} else if (from != null) {
				to = count;
			}
			Long pageSize = to - from;
			Long deltaPage = (long) ((count % pageSize)==0?0:1);
			Long pages = (count / pageSize) + deltaPage;
			Long page = (from / pageSize) + 1;
			
			Mono<ClientResponse> mono = get(spacecraft, from, to);
			DeferredResult<String> deferredResult = new DeferredResult<String>();
			List<Object> orbits = new ArrayList<>();

			mono.doOnError(e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("orbit-show :: #errormsg");
			})
		 	.subscribe(clientResponse -> {
				logger.trace("Now in Consumer::accept({})", clientResponse);
				if (clientResponse.statusCode().is2xxSuccessful()) {
					clientResponse.bodyToMono(List.class).subscribe(pcList -> {
						orbits.addAll(pcList);
					
						model.addAttribute("orbits", orbits);
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
						logger.trace(">>>>MONO" + orbits.toString());
						deferredResult.setResult("orbit-show :: #orbitcontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					});
				} else {
					handleHTTPError(clientResponse, model);
					deferredResult.setResult("orbit-show :: #errormsg");
				}
				logger.trace(">>>>MODEL" + model.toString());

			},
			e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("orbit-show :: #errormsg");
			});
			logger.trace(model.toString() + "MODEL TO STRING");
			logger.trace(">>>>MONO" + orbits.toString());
			logger.trace(">>>>MODEL" + model.toString());
			logger.trace("DEREFFERED STRING: {}", deferredResult);
			return deferredResult;
		}

		private Mono<ClientResponse> get(String spacecraft, Long from, Long to) {
			GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
			String uri = serviceConfig.getOrderManagerUrl() + "/orbits";
			String divider = "?";
			if (spacecraft != null) {
				uri += divider + "spacecraftCode=" + spacecraft;
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
			uri += divider + "orderBy=orbitNumber ASC,startTime ASC";
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

	    private Long countOrbits(String spacecraft)  {	    	
			GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
			String uri = "/orbits/count";
			String divider = "?";
			if (spacecraft != null) {
				uri += divider + "spacecraftCode=" + spacecraft;
				divider ="&";
			}
			Long result = (long) -1;
			try {
				String resStr = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
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
	}





