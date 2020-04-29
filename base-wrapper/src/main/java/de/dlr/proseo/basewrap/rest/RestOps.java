package de.dlr.proseo.basewrap.rest;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestOps {

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(RestOps.class);

	public enum HttpMethod {
		GET, POST, PUT, PATCH, DELETE, HEAD
	};

	/**
	 * generic REST API client
	 * 
	 * @param endPoint
	 *            an URL of the REST-Endpoint (e.g. http://localhost:8080)
	 * @param endPointPath
	 *            subroute of REST-URL (e.g. /ingest/param/987398724)
	 * @param payLoad
	 *            the request payload as string
	 * @param queryParam
	 *            the http query parameter
	 * @param method
	 *            the HTTP method (defaults to POST)
	 * @return ArrayList holding HTTP return code & response as String
	 */
	public static HttpResponseInfo restApiCall(String user, String pw, String endPoint, String endPointPath, String payLoad,
			@Nullable String queryParam, HttpMethod method) {
		if (logger.isTraceEnabled()) logger.trace(">>> restApiCall({}, PWD, {}, {}, {}, {}, {}", user, endPoint, endPointPath, payLoad, queryParam, method.toString());
		
		HttpResponseInfo responseInfo = new HttpResponseInfo();
		Client client = ClientBuilder.newClient().register(new RestAuth(user, pw));
		WebTarget webTarget = client.target(endPoint).path(endPointPath);
		Response response = null;

		try {
			switch (method) {
			case POST:
				logger.info(method + " " + webTarget.getUri());
				response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(payLoad, MediaType.APPLICATION_JSON));
				break;
			case PUT:
				webTarget = webTarget.queryParam(queryParam, payLoad);
				logger.info(method + " " + webTarget.getUri());
				response = webTarget.request(MediaType.APPLICATION_JSON).put(Entity.entity(payLoad, MediaType.APPLICATION_JSON));
				break;
			case PATCH:
				webTarget = webTarget.queryParam(queryParam, payLoad);
				// workaround to support HTTP-PATCH
				webTarget.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);

				logger.info(method + " " + webTarget.getUri());
				response = webTarget.request(MediaType.APPLICATION_JSON).method("PATCH", Entity.entity("", MediaType.APPLICATION_JSON));
				break;
			case GET:
				if (queryParam != null) {
					webTarget = webTarget.queryParam(queryParam, payLoad);
				}
				logger.info(method + " " + webTarget.getUri());
				response = webTarget.request(MediaType.APPLICATION_JSON).method("GET");

				break;
			default:
				throw new UnsupportedOperationException(method + " not implemented");
			}
		} catch (Exception e) {
			logger.error("exception: " + e.getMessage());
			return null;
		}

		if (logger.isDebugEnabled()) logger.debug("response = " + response);
		responseInfo.sethttpCode(response.getStatus());
		responseInfo.sethttpResponse(response.readEntity(String.class));
		response.close();
		return responseInfo;
	}
}
