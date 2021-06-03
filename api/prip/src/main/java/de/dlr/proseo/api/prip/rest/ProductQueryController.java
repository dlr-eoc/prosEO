/**
 * ProductQueryController.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.rest;

import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.dlr.proseo.api.prip.ProductionInterfaceSecurity;
import de.dlr.proseo.api.prip.odata.ProductEdmProvider;
import de.dlr.proseo.api.prip.odata.ProductEntityCollectionProcessor;
import de.dlr.proseo.api.prip.odata.ProductEntityProcessor;

/**
 * Spring MVC controller for the prosEO PRIP API; implements the services required to provide a RESTful API
 * according to ESA's Production Interface Delivery Point (PRIP) API (ESA-EOPG-EOPGC-IF-3, issue 1.5).
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
//	private static final String HTTP_MSG_PREFIX = "199 proseo-api-prip ";

	/** The service URI */
	public static final String URI = "/proseo/prip/odata/v1";

	/** The security utilities for the PRIP API */
	@Autowired
	private ProductionInterfaceSecurity securityConfig;
	
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
	 * Process the PRIP request.
	 *
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @throws ServletException on any unforeseen runtime exception
	 */
	@RequestMapping(value = "/**")
	protected void service(final HttpServletRequest request, HttpServletResponse response) throws ServletException {
		if (logger.isTraceEnabled()) logger.trace(">>> service({}, {})", request, response);
		
		// Analyze authentication information and make sure user is authorized for using the PRIP API
		try {
			securityConfig.doLogin(request);
		} catch (SecurityException e) {
			response.setStatus(HttpStatusCode.UNAUTHORIZED.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		}
		
		// Execute the request
		try {
			// Create OData handler and configure it with EDM provider and processors
			OData odata = OData.newInstance();
			ServiceMetadata edm = odata.createServiceMetadata(edmProvider, new ArrayList<EdmxReference>());
			ODataHttpHandler handler = odata.createHandler(edm);
			handler.register(entityCollectionProcessor);
			handler.register(entityProcessor);

			// Let the handler do the work
			handler.process(new HttpServletRequestWrapper(request) {
				// Spring MVC matches the whole path as the servlet path, but Olingo wants just the prefix, i.e. up to
				// /odata/{version}, so that it can parse the rest of it as an OData path. Thus we need to override getServletPath().
				@Override
				public String getServletPath() {
					return URI;
				}
			}, response);
		} catch (RuntimeException e) {
			logger.error("Server Error occurred in ProductQueryController", e);
			throw new ServletException(e);
		}
	}
}
