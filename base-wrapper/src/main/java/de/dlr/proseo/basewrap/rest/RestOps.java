package de.dlr.proseo.basewrap.rest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
//import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

public class RestOps {
	
	private static int MAX_RETRIES = 3;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(RestOps.class);

	/**
	 * Timeout for connecting to or reading from a service, in milliseconds.
	 */
	private static final Long ENV_HTTP_TIMEOUT;

	/**
	 * Default HTTP timeout value in milliseconds, used if the "HTTP_TIMEOUT"
	 * environment variable is not set or cannot be parsed as a long.
	 */
	private static final long DEFAULT_HTTP_TIMEOUT = 7_200_000L;

	// set the timeout either from the environment variable or as a default
	static {
		Long parsedTimeout;
		try {
			parsedTimeout = Long.parseLong(System.getenv("HTTP_TIMEOUT"));
		} catch (NumberFormatException e) {
			parsedTimeout = DEFAULT_HTTP_TIMEOUT;
			logger.error("HTTP_TIMEOUT environment variable cannot be parsed as a long, using default value: {}",
					DEFAULT_HTTP_TIMEOUT);
		}
		ENV_HTTP_TIMEOUT = parsedTimeout > 0 ? parsedTimeout : DEFAULT_HTTP_TIMEOUT;
	}
	
	public enum HttpMethod {
		GET, POST, PUT, PATCH, DELETE, HEAD
	};

	/**
	 * Generic REST API client (currently only supporting GET, PUT and POST)
	 * 
	 * @param user the user to authenticate with
	 * @param pw the password to authenticate with
	 * @param endPoint an URL of the REST-Endpoint (e.g. http://localhost:8080)
	 * @param endPointPath subroute of REST-URL (e.g. /ingest/param/987398724)
	 * @param payLoad the request payload as string
	 * @param queryParams the http query parameter (mandatory for PUT, optional for GET)
	 * @param method the HTTP method (defaults to POST)
	 * @return response object holding HTTP return code, "Warning" header, if available, and response as String,
	 *         or null, if the REST request failed with an exception
	 */
	public static HttpResponseInfo restApiCall(String user, String pw, String endPoint, String endPointPath, String payLoad,
			Map<String,String> queryParams, HttpMethod method) {
		if (logger.isTraceEnabled()) logger.trace(">>> restApiCall({}, PWD, {}, {}, {}, {}, {}", user, endPoint, endPointPath,
				payLoad, (null == queryParams ? "null" : queryParams.toString()), (null == method ? "null" : method.toString()));
		
		HttpResponseInfo responseInfo = new HttpResponseInfo();
		Response response = null;
		String content = payLoad==null?"":payLoad;
		int retry = 0;
		while (retry < MAX_RETRIES) {
			try {
				ResteasyClient client = new ResteasyClientBuilder()
						.connectTimeout(ENV_HTTP_TIMEOUT, TimeUnit.SECONDS)
						.readTimeout(ENV_HTTP_TIMEOUT, TimeUnit.SECONDS)
						.build()
						.register(new RestAuth(user, pw));
				
				WebTarget webTarget = client.target(endPoint).path(endPointPath);
				if (queryParams != null) {
					for (Entry<String, String> queryParam : queryParams.entrySet()) {
						webTarget = webTarget.queryParam(queryParam.getKey(), queryParam.getValue());
					}
				}
				
				switch (method) {
				case POST:
					if (logger.isDebugEnabled()) logger.debug(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(content, MediaType.APPLICATION_JSON));
					break;
				case PUT:
					if (logger.isDebugEnabled()) logger.debug(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON).put(Entity.entity(content, MediaType.APPLICATION_JSON));
					break;
				case PATCH:
					if (logger.isDebugEnabled()) logger.debug(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON).method(HttpMethod.PATCH.toString(), Entity.entity(content, MediaType.APPLICATION_JSON));
					break;
				case GET:
					if (logger.isDebugEnabled()) logger.debug(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON).get();
					break;
				default:
					throw new UnsupportedOperationException(method + " not implemented");
				}
				if (logger.isDebugEnabled())
					logger.debug("response: " + (null == response ? "null" : 
							" status = " + response.getStatus() + ", has body = " + response.hasEntity()),
							" warning = " + response.getHeaderString("Warning"));
				responseInfo.sethttpCode(response.getStatus());
				responseInfo.setHttpWarning(response.getHeaderString("Warning"));
				if (response.hasEntity()) {
					responseInfo.sethttpResponse(response.readEntity(String.class));
				} else {
					responseInfo.sethttpResponse("");
				}
				response.close();
				client.close();
				return responseInfo;
			} catch (ProcessingException e) {
				logger.error("Exception during REST API call: " + e.getMessage(), e);
				if (retry < (MAX_RETRIES - 1)) {
					// sometimes there is the exception "no route to host" which isn't really true
					// therefore wait a little bit and try again.
					//
					// TODO is there a possibility to avoid this?
					logger.info("Retry...");
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (Exception e1) {
						logger.error("Exception during retry wait: " + e.getMessage(), e1);
						return null;
					}
				} else {
					return null;
				}
			}
			retry++;
		}

		return responseInfo;
	}
}
