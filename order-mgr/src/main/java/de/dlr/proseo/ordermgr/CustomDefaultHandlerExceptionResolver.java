/**
 * CustomDefaultHandlerExceptionResolver.java
 *
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import java.io.IOException;

/**
 * Extend the REST Api exception handling forward message to the calling service. 
 * 
 * This handler keep the exception and throw it again, then the ApiExceptionHandler consumes it.
 *
 * @author Ernst Melchinger
 */
@Component
public class CustomDefaultHandlerExceptionResolver extends DefaultHandlerExceptionResolver {

    public CustomDefaultHandlerExceptionResolver() {
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    protected ModelAndView handleHttpMessageNotReadable(
    		HttpMessageNotReadableException ex,
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

    	throw ex;
    }
}