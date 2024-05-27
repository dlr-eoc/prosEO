/**
 * GUIProductController.java
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.netty.http.client.HttpClient;

/**
 * A controller for retrieving and handling products
 *
 * @author David Mazo
 */
@Controller
public class GUIProductController extends GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIProductClassController.class);

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Show the product view
	 *
	 * @return the name of the product view template
	 */
	@GetMapping("/product-show")
	public String showProduct() {
		return "product-show";
	}

	/**
	 * Show the product file view
	 *
	 * @return the name of the product file view template
	 */
	@RequestMapping(value = "/productfile-show")
	public String showProductFile() {
		return "productfile-show";
	}

	/**
	 * Retrieve products matching the provided parameters
	 *
	 * @param productId     the product id
	 * @param productClass  the product class
	 * @param mode          the processing mode
	 * @param fileClass     the file class
	 * @param quality       the product quality
	 * @param startTimeFrom the earliest permitted start time
	 * @param startTimeTo   the latest permitted start time
	 * @param genTimeFrom   the earliest permitted generation time
	 * @param genTimeTo     the latest permitted generation time
	 * @param recordFrom    the first record to retrieve
	 * @param recordTo      the last record to retrieve
	 * @param jobStepId     the job step id
	 * @param sortby        the sort column
	 * @param up            true if the sorting order is to be ascending
	 * @param model         the attributes to return
	 * @return the result
	 */
	@GetMapping("/product/get")
	public DeferredResult<String> getProducts(@RequestParam(required = false, value = "id") Long productId,
			@RequestParam(required = false, value = "productClass") String productClass,
			@RequestParam(required = false, value = "mode") String mode,
			@RequestParam(required = false, value = "fileClass") String fileClass,
			@RequestParam(required = false, value = "quality") String quality,
			@RequestParam(required = false, value = "startTimeFrom") String startTimeFrom,
			@RequestParam(required = false, value = "startTimeTo") String startTimeTo,
			@RequestParam(required = false, value = "genTimeFrom") String genTimeFrom,
			@RequestParam(required = false, value = "genTimeTo") String genTimeTo,
			@RequestParam(required = false, value = "recordFrom") Long recordFrom,
			@RequestParam(required = false, value = "recordTo") Long recordTo,
			@RequestParam(required = false, value = "jobStepId") Long jobStepId,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {

		logger.trace(">>> getProducs({}, {}, {}, {}, {}, model)", productClass, mode, fileClass, quality, startTimeFrom,
				startTimeTo, genTimeFrom, genTimeTo, recordFrom, recordTo);
		Long from = null;
		Long to = null;
		if (recordFrom != null && recordFrom >= 0) {
			from = recordFrom;
		} else {
			from = (long) 0;
		}
		Long count = countProducts(productClass, mode, fileClass, quality, startTimeFrom, startTimeTo, genTimeFrom, genTimeTo,
				jobStepId);
		if (recordTo != null && from != null && recordTo > from) {
			to = recordTo;
		} else if (from != null) {
			to = count;
		}

		// Perform the HTTP request to retrieve missions
		ResponseSpec responseSpec = get(productId, productClass, mode, fileClass, quality, startTimeFrom, startTimeTo, genTimeFrom,
				genTimeTo, from, to, jobStepId);
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> products = new ArrayList<>();

		Long pageSize = to - from;
		long deltaPage = (count % pageSize) == 0 ? 0 : 1;
		Long pages = (count / pageSize) + deltaPage;
		Long page = (from / pageSize) + 1;

		// Subscribe to the response
		responseSpec.toEntityList(Object.class)
			// Handle errors
			.doOnError(e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("product-show :: #errormsg");
			})
			// Handle successful response
			.subscribe(entityList -> {
				logger.trace("Now in Consumer::accept({})", entityList);

				if (entityList.getStatusCode().is2xxSuccessful()) {
					if (productId != null && productId > 0) {
						products.add(entityList.getBody());

						model.addAttribute("products", products);
						model.addAttribute("count", 1);
						model.addAttribute("pageSize", 1);
						model.addAttribute("pageCount", 1);
						model.addAttribute("page", 1);

						List<Long> showPages = new ArrayList<>();
						showPages.add((long) 1);
						model.addAttribute("showPages", showPages);

						if (logger.isTraceEnabled())
							logger.trace(model.toString() + "MODEL TO STRING");
						if (logger.isTraceEnabled())
							logger.trace(">>>>MONO" + products.toString());

						deferredResult.setResult("product-show :: #productcontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					} else {
						products.addAll((Collection<? extends Object>) entityList.getBody());
						// MapComparator oc = new MapComparator("productClass", true);
						// products.sort(oc);
						model.addAttribute("products", products);
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
						if (logger.isTraceEnabled())
							logger.trace(model.toString() + "MODEL TO STRING");
						if (logger.isTraceEnabled())
							logger.trace(">>>>MONO" + products.toString());

						deferredResult.setResult("product-show :: #productcontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					}
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityList.getStatusCode())
						.headers(headers -> headers.addAll(entityList.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("product-show :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("product-show :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + products.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		// Return the deferred result
		return deferredResult;
	}

	/**
	 * Retrieve the product file with id or all if id is null
	 *
	 * @param id     The product file id or null
	 * @param sortby The sort column
	 * @param up     The sort direction (true for up)
	 * @param model  The model to hold the data
	 * @return The result
	 */
	@GetMapping("/productfile/get")
	public DeferredResult<String> getProductFiles(@RequestParam(required = false, value = "id") Long id,
			@RequestParam(required = false, value = "sortby") String sortby,
			@RequestParam(required = false, value = "up") Boolean up, Model model) {

		logger.trace(">>> getProductFiles({}, {}, {}, {}, model)", id);

		// Perform the HTTP request to retrieve missions
		ResponseSpec responseSpec = get(id, null, null, null, null, null, null, null, null, null, null, null);
		DeferredResult<String> deferredResult = new DeferredResult<>();
		List<Object> productfiles = new ArrayList<>();

		// Subscribe to the response
		responseSpec.toEntity(HashMap.class)
			// Handle errors
			.doOnError(e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("productfile-show :: #errormsg");
			})// Handle successful response
			.subscribe(entityMap -> {
				logger.trace("Now in Consumer::accept({})", entityMap);

				if (entityMap.getStatusCode().is2xxSuccessful()) {
					if (id != null && id > 0) {
						productfiles.addAll((Collection<? extends Object>) entityMap.getBody().get("productFile"));
						model.addAttribute("productfiles", productfiles);

						if (logger.isTraceEnabled())
							logger.trace(model.toString() + "MODEL TO STRING");
						if (logger.isTraceEnabled())
							logger.trace(">>>>MONO" + productfiles.toString());

						deferredResult.setResult("productfile-show :: #productfilecontent");
						logger.trace(">>DEFERREDRES: {}", deferredResult.getResult());
					}
				} else {
					ClientResponse errorResponse = ClientResponse.create(entityMap.getStatusCode())
						.headers(headers -> headers.addAll(entityMap.getHeaders()))
						.build();
					handleHTTPError(errorResponse, model);

					deferredResult.setResult("productfile-show :: #errormsg");
				}

				logger.trace(">>>>MODEL" + model.toString());
			}, e -> {
				model.addAttribute("errormsg", e.getMessage());
				deferredResult.setResult("productfile-show :: #errormsg");
			});

		logger.trace(model.toString() + "MODEL TO STRING");
		logger.trace(">>>>MONO" + productfiles.toString());
		logger.trace(">>>>MODEL" + model.toString());
		logger.trace("DEREFFERED STRING: {}", deferredResult);

		return deferredResult;
	}

	/**
	 * Retrieve the number of products matching the specified parameters
	 *
	 * @param productClass  the product class
	 * @param mode          the processing mode
	 * @param fileClass     the file class
	 * @param quality       the product quality
	 * @param startTimeFrom the earliest permitted start time
	 * @param startTimeTo   the latest permitted start time
	 * @param genTimeFrom   the earliest permitted generation time
	 * @param genTimeTo     the latest permitted generation time
	 * @param jobStepId     the id of the job step producing the product
	 * @return the number of products matching the specified parameters
	 */
	private Long countProducts(String productClass, String mode, String fileClass, String quality, String startTimeFrom,
			String startTimeTo, String genTimeFrom, String genTimeTo, Long jobStepId) {

		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();
		String divider = "?";
		String uriString = "/products/count";
		if (productClass != mission && !mission.isEmpty()) {
			uriString += divider + "mission=" + mission;
			divider = "&";
		}
		if (productClass != null && !productClass.isEmpty()) {
			String[] pcs = productClass.split(",");
			for (String pc : pcs) {
				uriString += divider + "productClass=" + pc;
				divider = "&";
			}
		}
		if (mode != null && !mode.isEmpty()) {
			uriString += divider + "mode=" + mode;
			divider = "&";
		}
		if (fileClass != null && !fileClass.isEmpty()) {
			uriString += divider + "fileClass=" + fileClass;
			divider = "&";
		}
		if (quality != null && !quality.isEmpty()) {
			uriString += divider + "quality=" + quality;
			divider = "&";
		}
		if (startTimeFrom != null && !startTimeFrom.isEmpty()) {
			uriString += divider + "startTimeFrom=" + startTimeFrom;
			divider = "&";
		}
		if (startTimeTo != null && !startTimeTo.isEmpty()) {
			uriString += divider + "startTimeTo=" + startTimeTo;
			divider = "&";
		}
		if (genTimeFrom != null && !genTimeFrom.isEmpty()) {
			uriString += divider + "genTimeFrom=" + genTimeFrom;
			divider = "&";
		}
		if (genTimeTo != null && !genTimeTo.isEmpty()) {
			uriString += divider + "genTimeTo=" + genTimeTo;
			divider = "&";
		}
		if (jobStepId != null) {
			uriString += divider + "jobStep=" + jobStepId;
			divider = "&";
		}
		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
		Long result = (long) -1;
		try {
			String resStr = serviceConnection.getFromService(serviceConfig.getIngestorUrl(), uri.toString(), String.class,
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
	 * Makes an HTTP GET request to retrieve products based on the provided parameters.
	 *
	 * @param id            the product id
	 * @param productClass  the product class
	 * @param mode          the processing mode
	 * @param fileClass     the file class
	 * @param quality       the product quality
	 * @param startTimeFrom the earliest permitted start time
	 * @param startTimeTo   the latest permitted start time
	 * @param genTimeFrom   the earliest permitted generation time
	 * @param genTimeTo     the latest permitted generation time
	 * @param recordFrom    the first record to retrieve
	 * @param recordTo      the last record to retrieve
	 * @param jobStepId     the id of the job step producing the product
	 * @return a Mono containing the HTTP response
	 */
	private ResponseSpec get(Long id, String productClass, String mode, String fileClass, String quality, String startTimeFrom,
			String startTimeTo, String genTimeFrom, String genTimeTo, Long recordFrom, Long recordTo, Long jobStepId) {

		// Provide authentication
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		String mission = auth.getMission();

		// Build the request URI
		String uriString = serviceConfig.getIngestorUrl() + "/products";
		if (id != null && id > 0) {
			uriString += "/" + id.toString();
		} else {
			String divider = "?";
			if (mission != null && !mission.isEmpty()) {
				uriString += divider + "mission=" + mission;
				divider = "&";
			}
			if (productClass != null && !productClass.isEmpty()) {
				String[] pcs = productClass.split(",");
				for (String pc : pcs) {
					uriString += divider + "productClass=" + pc;
					divider = "&";
				}
			}
			if (mode != null && !mode.isEmpty()) {
				uriString += divider + "mode=" + mode;
				divider = "&";
			}
			if (fileClass != null && !fileClass.isEmpty()) {
				uriString += divider + "fileClass=" + fileClass;
				divider = "&";
			}
			if (quality != null && !quality.isEmpty()) {
				uriString += divider + "quality=" + quality;
				divider = "&";
			}
			if (startTimeFrom != null && !startTimeFrom.isEmpty()) {
				uriString += divider + "startTimeFrom=" + startTimeFrom;
				divider = "&";
			}
			if (startTimeTo != null && !startTimeTo.isEmpty()) {
				uriString += divider + "startTimeTo=" + startTimeTo;
				divider = "&";
			}
			if (genTimeFrom != null && !genTimeFrom.isEmpty()) {
				uriString += divider + "genTimeFrom=" + genTimeFrom;
				divider = "&";
			}
			if (genTimeTo != null && !genTimeTo.isEmpty()) {
				uriString += divider + "genTimeTo=" + genTimeTo;
				divider = "&";
			}
			if (recordFrom != null) {
				uriString += divider + "recordFrom=" + recordFrom;
				divider = "&";
			}
			if (recordTo != null) {
				uriString += divider + "recordTo=" + recordTo;
				divider = "&";
			}
			if (jobStepId != null) {
				uriString += divider + "jobStep=" + jobStepId;
				divider = "&";
			}
			uriString += divider + "orderBy=productClass.productType ASC,sensingStartTime ASC";
		}
		URI uri = UriComponentsBuilder.fromUriString(uriString).build().toUri();
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
		 * The returned ResponseSpec can be subscribed to in order to retrieve the actual response and perform additional operations
		 * on it, such as extracting the response body or handling any errors that may occur during the request.
		 */
		return webclient.build()
			.get()
			.uri(uri)
			.headers(headers -> headers.setBasicAuth(auth.getProseoName(), auth.getPassword()))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve();
	}

}