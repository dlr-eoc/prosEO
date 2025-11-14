/**
 * RestOps.java
 *
 *  (C) 2019 Hubert Asamer, DLR
 *  (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.basewrap.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform REST API calls to a provided endpoint.
 *
 * @author Hubert Asamer
 */
public class RestOps {

	/** Maximum numer of retries in case of failure to connect */
	private static int MAX_RETRIES = 5;

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
		String content = (payLoad == null) ? "" : payLoad;
		
		// Retry the REST API call until a configured maximum of retries is reached
		int retry = 0;
		while (retry < MAX_RETRIES) {
			
			// Create and configure an HTTP client
			
			final RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofSeconds(ENV_HTTP_TIMEOUT))
                    .setConnectTimeout(Timeout.ofSeconds(ENV_HTTP_TIMEOUT))
                    .setResponseTimeout(Timeout.ofSeconds(ENV_HTTP_TIMEOUT))
                    .build();
			
			final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
	        credsProvider.setCredentials(
	                new AuthScope(null, -1),
	                new UsernamePasswordCredentials(user, pw.toCharArray()));
			
			if (logger.isDebugEnabled()) logger.debug("About to build HTTP client ");
	        
			try (CloseableHttpClient httpClient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider)
					.setDefaultRequestConfig(requestConfig)
					.build()) {
				
				
				// Build the HTTP request

				URI uri = new URIBuilder(endPoint + endPointPath)
						.addParameters(null == queryParams ? new ArrayList<NameValuePair>() :
							queryParams.entrySet().stream()
							.map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
							.collect(Collectors.toList()))
						.build();
				
				ClassicHttpRequest request = ClassicRequestBuilder
						.create(method.toString())
						.setUri(uri)
						.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString())
						.build();

				
				// For POST, PUT, PATCH add a request body
								
				if (HttpMethod.GET.equals(method)) {
					// NOP: No request body
				} else if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method)) {
					request.setEntity(
							EntityBuilder.create().setContentType(ContentType.APPLICATION_JSON).setText(content).build());
				} else {
					throw new UnsupportedOperationException(method + " not implemented");
				}
				
				if (logger.isDebugEnabled()) logger.debug("Sending HTTP request " + request.toString());
				
				
				// Execute the request
				
				httpClient.execute(request, response -> {
					responseInfo.sethttpCode(response.getCode());
					if (null != response.getEntity()) {
						responseInfo.sethttpResponse(EntityUtils.toString(response.getEntity()));
					}
					return null;
				});
				
				if (logger.isDebugEnabled())
					logger.debug("Request execution completed with status code " + responseInfo.gethttpCode());
				
				return responseInfo;
				
			} catch (IOException e) {
				logger.error("I/O Exception during REST API call: " + e.getMessage(), e);

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
			} catch (URISyntaxException e) {
				String message = String.format("Invalid URI components for endpoint %s and query parameters %s", 
						endPoint + endPointPath, queryParams.toString());
				logger.error(message, e);
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
					throw new RuntimeException(message, e);
				}
			} catch (RuntimeException e) {
				logger.error("Exception during REST API call: ", e);
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
					throw e;
				}
			}

			retry++;
		}

		return responseInfo;
	}

}