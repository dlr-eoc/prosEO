/**
 * ApiExceptionHandler.java
 *
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Extend the REST Api exception handling.
 * Consume the exception, set the appropriate HTTP status and forward the message to the calling service. 
 *
 * @author Ernst Melchinger
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleValidation(HttpMessageNotReadableException ex) {
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HttpHeaders.WARNING, ex.getMessage());
    	return new ResponseEntity<String>(ex.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
    }
}