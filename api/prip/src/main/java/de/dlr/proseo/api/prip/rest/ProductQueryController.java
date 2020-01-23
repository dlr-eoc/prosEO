/**
 * ProductQueryController.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import javax.servlet.ServletException;
//import javax.persistence.EntityNotFoundException;
//import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.dlr.proseo.api.prip.odata.ProductEdmProvider;
import de.dlr.proseo.api.prip.odata.ProductEntityCollectionProcessor;
import de.dlr.proseo.api.prip.odata.ProductEntityProcessor;
import de.dlr.proseo.api.prip.rest.model.CscProduct;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage processor versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@RestController
@Validated
@RequestMapping(value = ProductQueryController.URI, produces = "application/json")
public class ProductQueryController {

	/* Message ID constants */
	// private static final int MSG_ID_NOT_IMPLEMENTED = 9000;

	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-api-prip ";

	/** The service URI */
	public static final String URI = "/proseo/prip/odata";

	/** The EDM provider for products */
	@Autowired
	private ProductEdmProvider edmProvider;
	
	/** The entity collection processor for products */
	@Autowired
	private ProductEntityCollectionProcessor entityCollectionProcessor;

	/** The entity processor for products */
	@Autowired
	private ProductEntityProcessor entityProcessor;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductQueryController.class);

	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message.replaceAll("\n", " "));
		return responseHeaders;
	}

	/**
	 * Process.
	 *
	 * @param request the req
	 * @param response the Http response
	 */
	@RequestMapping(value = "/**")
	protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> service({}, {})", request, response);
		
		try {
			// create odata handler and configure it with CsdlEdmProvider and Processor
			OData odata = OData.newInstance();
			ServiceMetadata edm = odata.createServiceMetadata(edmProvider, new ArrayList<EdmxReference>());
			ODataHttpHandler handler = odata.createHandler(edm);
			handler.register(entityCollectionProcessor);
			handler.register(entityProcessor);

			// let the handler do the work
			handler.process(new HttpServletRequestWrapper(request) {
				// Spring MVC matches the whole path as the servlet path
				// Olingo wants just the prefix, ie upto /odata, so that it
				// can parse the rest of it as an OData path. So we need to override
				// getServletPath()
				@Override
				public String getServletPath() {
					return ProductQueryController.URI;
				}
			}, response);
		} catch (RuntimeException e) {
			logger.error("Server Error occurred in ProductQueryController", e);
			throw new ServletException(e);
		}
	}
}
