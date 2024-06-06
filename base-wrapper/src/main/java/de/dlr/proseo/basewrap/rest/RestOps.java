/**
 * RestOps.java
 *
 *  (C) 2019 Hubert Asamer, DLR
 *  (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.basewrap.rest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;


import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform REST API calls to a provided endpoint.
 *
 * @author Hubert Asamer
 */
public class RestOps {

	/** Maximum numer of retries in case of failure to connect */
	private static int MAX_RETRIES = 3;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(RestOps.class);

	/** Timeout for connecting to or reading from a service, in milliseconds. */
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
			logger.info("HTTP_TIMEOUT environment variable cannot be parsed as a long, using default value: {}",
					DEFAULT_HTTP_TIMEOUT);
		}
		ENV_HTTP_TIMEOUT = parsedTimeout > 0 ? parsedTimeout : DEFAULT_HTTP_TIMEOUT;
	}

	/** A collection of HTTP Methods. */
	public enum HttpMethod {
		GET, POST, PUT, PATCH, DELETE, HEAD
	}

	/**
	 * Generic REST API client (currently only supporting GET, PUT and POST)
	 *
	 * @param user         the user to authenticate with
	 * @param pw           the password to authenticate with
	 * @param endPoint     an URL of the REST-Endpoint (e.g. http://localhost:8080)
	 * @param endPointPath subroute of REST-URL (e.g. /ingest/param/987398724)
	 * @param payLoad      the request payload as string
	 * @param queryParams  the http query parameter (mandatory for PUT, optional for
	 *                     GET)
	 * @param method       the HTTP method (defaults to POST)
	 * @return response object holding HTTP return code, "Warning" header, if
	 *         available, and response as String, or null, if the REST request
	 *         failed with an exception
	 */
	public static HttpResponseInfo restApiCall(String user, String pw, String endPoint, String endPointPath, String payLoad,
			Map<String, String> queryParams, HttpMethod method) {

		if (logger.isTraceEnabled())
			logger.trace(">>> restApiCall({}, PWD, {}, {}, {}, {}, {}", user, endPoint, endPointPath, payLoad,
					(null == queryParams ? "null" : queryParams.toString()), (null == method ? "null" : method.toString()));

		HttpResponseInfo responseInfo = new HttpResponseInfo();
		Response response = null;
		String content = (payLoad == null) ? "" : payLoad;

		// Retry the REST API call until a configured maximum of retries is reached
		int retry = 0;
		while (retry < MAX_RETRIES) {
			try {

				// Build a new client to build and execute a HTTP request
				ResteasyClient client = (ResteasyClient) ClientBuilder.newBuilder().connectTimeout(ENV_HTTP_TIMEOUT, TimeUnit.SECONDS)
					.readTimeout(ENV_HTTP_TIMEOUT, TimeUnit.SECONDS)
					.build()
					.register(new RestAuth(user, pw));

				// Create a resource target identified by an URI with query parameters
				WebTarget webTarget = client.target(endPoint).path(endPointPath);
				if (queryParams != null) {
					for (Entry<String, String> queryParam : queryParams.entrySet()) {
						webTarget = webTarget.queryParam(queryParam.getKey(), queryParam.getValue());
					}
				}

				// Handle the request according to the HTTP method
				switch (method) {
				case POST:
					if (logger.isDebugEnabled())
						logger.debug(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON)
						.post(Entity.entity(content, MediaType.APPLICATION_JSON));
					break;
				case PUT:
					if (logger.isDebugEnabled())
						logger.debug(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON)
						.put(Entity.entity(content, MediaType.APPLICATION_JSON));
					break;
				case PATCH:
					if (logger.isDebugEnabled())
						logger.debug(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON)
						.method(HttpMethod.PATCH.toString(), Entity.entity(content, MediaType.APPLICATION_JSON));
					break;
				case GET:
					if (logger.isDebugEnabled())
						logger.debug(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON).get();
					break;
				default:
					throw new UnsupportedOperationException(method + " not implemented");
				}

				// Extract the response information
				if (logger.isDebugEnabled())
					logger.debug(
							"response: " + (null == response ? "null"
									: " status = " + response.getStatus() + ", has body = " + response.hasEntity()),
							" warning = " + response.getHeaderString("Warning"));
				responseInfo.sethttpCode(response.getStatus());
				responseInfo.setHttpWarning(response.getHeaderString("Warning"));
				if (response.hasEntity()) {
					responseInfo.sethttpResponse(response.readEntity(String.class));
				} else {
					responseInfo.sethttpResponse("");
				}

				// Close the response and the client
				response.close();
				client.close();

				return responseInfo;
			} catch (ProcessingException e) {
				logger.error("Exception during REST API call: " + e.getMessage(), e);

				if (retry < (MAX_RETRIES - 1)) {
					// Sometimes there is the exception "no route to host" which isn't really true
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
					// If the retries are exhausted unsuccessfully, return null
					return null;
				}
			}
			retry++;
		}

		return responseInfo;
	}

}