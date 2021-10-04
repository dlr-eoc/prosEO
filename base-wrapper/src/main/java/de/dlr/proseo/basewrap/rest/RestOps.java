package de.dlr.proseo.basewrap.rest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestOps {
	
	private static int MAX_RETRIES = 5;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(RestOps.class);

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
	 * @return ArrayList holding HTTP return code and response as String
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
				Client client =  javax.ws.rs.client.ClientBuilder.newClient().register(new RestAuth(user, pw));
				WebTarget webTarget = client.target(endPoint).path(endPointPath);
				if (queryParams != null) {
					for (Entry<String, String> queryParam : queryParams.entrySet()) {
						webTarget = webTarget.queryParam(queryParam.getKey(), queryParam.getValue());
					}
				}
				switch (method) {
				case POST:
					logger.info(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(content, MediaType.APPLICATION_JSON));
					break;
				case PUT:
					logger.info(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON).put(Entity.entity(content, MediaType.APPLICATION_JSON));
					break;
				case PATCH:
					logger.info(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON).method(HttpMethod.PATCH.toString(), Entity.entity(content, MediaType.APPLICATION_JSON));
					break;
				case GET:
					logger.info(method + " " + webTarget.getUri());
					response = webTarget.request(MediaType.APPLICATION_JSON).get();
					break;
				default:
					throw new UnsupportedOperationException(method + " not implemented");
				}
				if (logger.isDebugEnabled())
					logger.debug("response: " + (null == response ? "null" : 
							" status = " + response.getStatus() + ", has body = " + response.hasEntity()));
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
