/**
 * GUIProductClassController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.gui.service.MapComparator;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * A controller for retrieving and handling product classes
 *
 * @author David Mazo
 */
@Controller
public class GUIProductClassController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIProductClassController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Show the product class view
	 *
	 * @return the name of the product class view template
	 */
	@RequestMapping(value = "/productclass-show")
	public String showProductClass() {
		return "productclass-show";
	}

	/**
	 * Fetch and return product classes from the product class manager
	 *
	 * @param productClass   the product class
	 * @param processorClass the processor class
	 * @param level          the product level
	 * @param visibility     the level of visibility
	 * @param sortby         the sort column
	 * @param up             true if the sorting is to happen in ascending order
	 * @param recordFrom     the first record to retrieve
	 * @param recordTo       the last record to retrieve
	 * @param model          the attributes to return
	 * @return the result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/productclass/get")
	public DeferredResult<String> getProductClasses(@RequestParam(required = false, value = "productClass") String productClass,
			@RequestParam(required = false, value = "processorClass") String processorClass,
			@RequestParam(required = false, value = "level") String level,
			@RequestParam(required = false, value = "visibility") String visibility,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up,
			@RequestParam(required = false, value = "recordFrom") Long recordFrom,
			@RequestParam(required = false, value = "recordTo") Long recordTo, Model model) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProductClasses({}, {}, model)", recordFrom, recordTo);
		Long from = null;
		Long to = null;
		if (recordFrom != null && recordFrom >= 0) {
			from = recordFrom;
		} else {
			from = (long) 0;
		}
		Long count = countProductClasses(productClass, processorClass, level, visibility);
		if (recordTo != null && from != null && recordTo > from) {
			to = recordTo;
		} else if (from != null) {
			to = count;
		}
		Long pageSize = to - from;
		long deltaPage = (count % pageSize) == 0 ? 0 : 1;
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;
		Mono<ClientResponse> mono = get(productClass, processorClass, level, visibility, from, to);
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> productclasses = new ArrayList<>();
		mono.doOnError(e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("productclass-show :: #errormsg");
		}).subscribe(clientResponse -> {
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
					List<Long> showPages = new ArrayList<>();
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

		}, e -> {
			model.addAttribute("errormsg", e.getMessage());
			deferredResult.setResult("productclass-show :: #errormsg");
		});
		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + productclasses.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);
		return deferredResult;
	}

	/**
	 * Retrieve the number of product classes matching the given parameters.
	 *
	 * @param productClass   the product class
	 * @param processorClass the processor class
	 * @param level          the product level
	 * @param visibility     the level of visibility
	 * @return the number of workflows matching the given parameters
	 */
	private Long countProductClasses(String productType, String processorClass, String level, String visibility) {

		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String divider = "?";
		String uriString = "/productclasses/count";
		if (mission != null && !mission.isEmpty()) {
			uriString += divider + "mission=" + mission;
			divider = "&";
		}
		if (productType != null && !productType.isEmpty()) {
			String[] pcs = productType.split(",");
			for (String pc : pcs) {
				uriString += divider + "productType=" + pc;
				divider = "&";
			}
		}
		if (processorClass != null && !processorClass.isEmpty()) {
			String[] pcs = processorClass.split(",");
			for (String pc : pcs) {
				uriString += divider + "processorClass=" + pc;
				divider = "&";
			}
		}
		if (level != null) {
			uriString += divider + "level=" + level;
			divider = "&";
		}
		if (visibility != null) {
			uriString += divider + "visibility=" + visibility;
			divider = "&";
		}
		URI uri = UriComponentsBuilder.fromUriString(uriString)
				.build()
				.toUri();
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(), uri.toString(), String.class,
					auth.getProseoName(), auth.getPassword());

			if (resStr != null && resStr.length() > 0) {
				result = Long.valueOf(resStr);
			}
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return result;
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return result;
		}

		return result;
	}

	/**
	 * Makes an HTTP GET request to retrieve product classes based on the provided parameters.
	 *
	 * @param productClass   the product class
	 * @param processorClass the processor class
	 * @param level          the product level
	 * @param visibility     the level of visibility
	 * @param recordFrom     the first record to retrieve
	 * @param recordTo       the last record to retrieve
	 * @return a Mono containing the HTTP response
	 */
	private Mono<ClientResponse> get(String productType, String processorClass, String level, String visibility, Long from,
			Long to) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build the request URI
		String uriString = serviceConfig.getProductClassManagerUrl() + "/productclasses";
		String divider = "?";
		if (mission != null && !mission.isEmpty()) {
			uriString += divider + "mission=" + mission;
			divider = "&";
		}
		if (productType != null && !productType.isEmpty()) {
			String[] pcs = productType.split(",");
			for (String pc : pcs) {
				uriString += divider + "productType=" + pc;
				divider = "&";
			}
		}
		if (processorClass != null && !processorClass.isEmpty()) {
			String[] pcs = processorClass.split(",");
			for (String pc : pcs) {
				uriString += divider + "processorClass=" + pc;
				divider = "&";
			}
		}
		if (level != null) {
			uriString += divider + "level=" + level;
			divider = "&";
		}
		if (visibility != null) {
			uriString += divider + "visibility=" + visibility;
			divider = "&";
		}
		if (from != null) {
			uriString += divider + "recordFrom=" + from;
			divider = "&";
		}
		if (to != null) {
			uriString += divider + "recordTo=" + to;
			divider = "&";
		}
		uriString += divider + "orderBy=productType ASC";

		URI uri = UriComponentsBuilder.fromUriString(uriString)
				.build()
				.toUri();
		logger.trace("URI " + uri);
		// Create and configure a WebClient to make a HTTP request to the URI
		Builder webclient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect((req, res) -> {
				logger.trace("response:{}", res.status());
				return HttpResponseStatus.FOUND.equals(res.status());
			})));

		logger.trace("Found authentication: " + auth);
		logger.trace("... with username " + auth.getName());
		logger.trace("... with password " + (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));

		/*
		 * The returned Mono<ClientResponse> can be subscribed to in order to retrieve the actual response and perform additional
		 * operations on it, such as extracting the response body or handling any errors that may occur during the request.
		 */
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.exchange();
	}

	/**
	 * Sort the selection rules
	 *
	 * @param productclasses a list of product classes
	 */
	@SuppressWarnings("unchecked")
	private void sortSelectionRules(List<Object> productclasses) {
		if (productclasses != null) {
			for (Object o1 : productclasses) {
				if (o1 instanceof HashMap) {
					HashMap<String, HashMap<String, Object>> sortedModeList = new HashMap<>();
					HashMap<String, HashMap<String, Object>> sortedProcessorsList = new HashMap<>();
					HashMap<String, Object> h1 = (HashMap<String, Object>) o1;
					Object sro = h1.get("selectionRule");
					if (sro instanceof List) {
						List<Object> srl = (List<Object>) sro;
						for (Object o2 : srl) {
							if (o2 instanceof HashMap) {
								HashMap<?, ?> sr = (HashMap<?, ?>) o2;
								// collect all configuredProcessors
								List<String> procs = (List<String>) sr.get("configuredProcessors");
								if (procs == null || procs.isEmpty()) {
									procs = new ArrayList<>();
									procs.add("");
								}
								for (String proc : procs) {
									if (!sortedProcessorsList.containsKey(proc)) {
										HashMap<String, Object> localList = new HashMap<>();
										localList.put("configuredProcessor", proc);
										localList.put("selRules", new ArrayList<>());
										sortedProcessorsList.put(proc, localList);
									}
								}
							}
						}
						for (Object o2 : srl) {
							if (o2 instanceof HashMap) {
								HashMap<?, ?> sr = (HashMap<?, ?>) o2;
								// now we have a selection rule
								// collect all modes in a new hash map
								String mode = (String) sr.get("mode");
								if (mode == null) {
									mode = "";
								}
								for (String acp : sortedProcessorsList.keySet()) {
									if (!sortedModeList.containsKey(mode + acp)) {
										HashMap<String, Object> localList = new HashMap<>();
										localList.put("mode", mode);
										localList.put("applicableConfiguredProcessor", acp);
										localList.put("selRules", new ArrayList<>());
										sortedModeList.put(mode + acp, localList);
									}
								}
							}
						}
						for (Object o2 : srl) {
							if (o2 instanceof HashMap) {
								HashMap<?, ?> sr = (HashMap<?, ?>) o2;
								// now we have a selection rule
								// collect all modes in a new hash map
								String mode = (String) sr.get("mode");
								if (mode == null) {
									mode = "";
								}
								List<String> procs = (List<String>) sr.get("configuredProcessors");
								if (procs == null || procs.isEmpty()) {
									procs = new ArrayList<>();
									procs.add("");
								}
								for (String acp : procs) {
									((List<Object>) sortedModeList.get(mode + acp).get("selRules")).add(sr);
								}
							}
						}
						for (HashMap<String, Object> modeList : sortedModeList.values()) {
							Object listObj = modeList.get("selRules");
							if (listObj instanceof List) {
								List<Object> list = (List<Object>) listObj;
								MapComparator oc = new MapComparator("sourceProductClass", true);
								list.sort(oc);
							}
						}
						MapComparator mlc = new MapComparator("mode", "applicableConfiguredProcessor", true);
						Collection<HashMap<String, Object>> mList = sortedModeList.values();
						List<Object> cList = new ArrayList<>();
						cList.addAll(mList);
						cList.sort(mlc);
						h1.put("sortedSelectionRules", cList);
					}
				}
			}
		}
	}

}