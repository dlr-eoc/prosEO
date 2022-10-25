/**
 * ProductQueryController.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.rest;

import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugSupport;
import org.apache.olingo.server.api.debug.DefaultDebugSupport;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.dlr.proseo.api.odip.ProductionInterfaceSecurity;
import de.dlr.proseo.api.odip.odata.LogUtil;
import de.dlr.proseo.api.odip.odata.ProductEdmProvider;
import de.dlr.proseo.api.odip.odata.ProductEntityCollectionProcessor;
import de.dlr.proseo.api.odip.odata.ProductEntityProcessor;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.OdipMessage;

/**
 * Spring MVC controller for the prosEO ODIP API; implements the services required to provide a RESTful API
 * according to ESA's Production Interface Delivery Point (ODIP) API (ESA-EOPG-EOPGC-IF-3, issue 1.5).
 * 
 * @author Dr. Thomas Bassler
 *
 */
@RestController
@Validated
@RequestMapping(value = ProductQueryController.URI, produces = {"application/json", "application/octet-stream"})
public class ProductQueryController {
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
//	private static final String HTTP_MSG_PREFIX = "199 proseo-api-odip ";

	/** The service URI */
	public static final String URI = "/proseo/odip/odata/v1";

	/** The security utilities for the ODIP API */
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
	private static ProseoLogger logger = new ProseoLogger(ProductQueryController.class);

	/**
	 * Process the ODIP request.
	 *
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @throws ServletException on any unforeseen runtime exception
	 */
	@RequestMapping(value = "/**")
	protected void service(final HttpServletRequest request, HttpServletResponse response) throws ServletException {
		if (logger.isTraceEnabled()) logger.trace(">>> service({}, {})", 
				(null == request ? "null" : request.getRequestURL()), response);
		
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

		// Analyze authentication information and make sure user is authorized for using the ODIP API
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
				logger.log(OdipMessage.MSG_EXCEPTION_SET_RESP, e1.getMessage(), e1);
			}
			response.setStatus(HttpStatusCode.UNAUTHORIZED.getStatusCode());
			response.setHeader(HTTP_HEADER_WARNING, e.getMessage()); // Message already logged and formatted
			return;
		} catch (RuntimeException e) {
			logger.log(OdipMessage.MSG_EXCEPTION_PIS, e.getMessage(), e);
			throw new ServletException(e);
		}
		logger.log(OdipMessage.MSG_USER_LOGGED_IN, securityConfig.getMission(), securityConfig.getUser());
		
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
			if (logger.isTraceEnabled()) logger.trace("... after processing request, returning response code: " + response.getStatus());
		} catch (Exception e) {
			logger.log(OdipMessage.MSG_EXCEPTION_PQC, e.getMessage(), e);
			throw new ServletException(e);
		}
	}
}
