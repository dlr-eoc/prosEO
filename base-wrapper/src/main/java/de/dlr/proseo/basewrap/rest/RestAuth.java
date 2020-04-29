package de.dlr.proseo.basewrap.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.DatatypeConverter;

public class RestAuth implements ClientRequestFilter {

    private final String user;
    private final String password;

    public RestAuth(String user, String password) {
        this.user = user;
        this.password = password;
    }

    /**
     * Adds an HTTP Authorization header to the given client request context
     * 
     * @param requestContext the client request context
     * @throws IOException if Base64 encoding of the authorization header text fails
     */
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
    	
    	// Encode username and password in Base64
        String basicAuthentication = this.user + ":" + this.password;
        try {
        	basicAuthentication = "Basic " + DatatypeConverter.printBase64Binary(basicAuthentication.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new IOException("Cannot encode authentication string with UTF-8", ex);
        }

        // Add the authorization string as
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        headers.add("Authorization", basicAuthentication);
    }
}