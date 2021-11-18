/**
 * ProductQueryController.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugSupport;
import org.apache.olingo.server.api.debug.DefaultDebugSupport;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.dlr.proseo.api.prip.ProductionInterfaceSecurity;
import de.dlr.proseo.api.prip.odata.LogUtil;
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
	private static final int MSG_ID_USER_LOGGED_IN = 5099;
	
	/* Message string constants */
	private static final String MSG_USER_LOGGED_IN = "(I%d) User %s\\%s logged in to PRIP API";
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
		
		// Create OData handler
		OData odata = OData.newInstance();

		// Configure OData handler with EDM provider and processors
		ServiceMetadata edm = odata.createServiceMetadata(edmProvider, new ArrayList<EdmxReference>());
		ODataHttpHandler handler = odata.createHandler(edm);
		handler.register(entityCollectionProcessor);
		handler.register(entityProcessor);
		DebugSupport debugSupport = new DefaultDebugSupport();
		debugSupport.init(odata);
		handler.register(debugSupport);

		// Analyze authentication information and make sure user is authorized for using the PRIP API
		try {
			securityConfig.doLogin(request);
		} catch (SecurityException e) {
			try {
				ODataSerializer serializer = odata.createSerializer(ContentType.JSON);
				String message = new String(serializer.error(
						LogUtil.oDataServerError(HttpStatusCode.UNAUTHORIZED.getStatusCode(), e.getMessage()))
						.getContent().readAllBytes());
				response.getWriter().print(message);
			} catch (Exception e1) {
				// Log to Standard Error, but otherwise ignore (we just don't have a response body then)
				logger.error("Exception setting response content: ", e1);
			}
			response.setStatus(HttpStatusCode.UNAUTHORIZED.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (RuntimeException e) {
			logger.error("Server Error occurred in ProductionInterfaceSecurity", e);
			throw new ServletException(e);
		}
		LogUtil.logInfo(logger, MSG_USER_LOGGED_IN, MSG_ID_USER_LOGGED_IN, securityConfig.getMission(), securityConfig.getUser());
		
		// Execute the request
		try {
			// Let the handler do the work
			handler.process(new HttpServletRequestWrapper(request) {
				// Spring MVC matches the whole path as the servlet path, but Olingo wants just the prefix, i.e. up to
				// /odata/{version}, so that it can parse the rest of it as an OData path. Thus we need to override getServletPath().
				@Override
				public String getServletPath() {
					return URI;
				}
			}, response);
			if (logger.isTraceEnabled()) logger.trace("... after processing request, response is: " + response);
		} catch (Exception e) {
			logger.error("Server Error occurred in ProductQueryController", e);
			throw new ServletException(e);
		}
	}
}
