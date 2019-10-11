package de.dlr.proseo.samplewrap.rest;


import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RestOps {

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(RestOps.class);
	public enum HttpMethod {GET, POST, PUT, PATCH, DELETE, HEAD };

	/**
	 * generic REST API client
	 * 
	 * @param endPoint an URL of the REST-Endpoint (e.g. http://localhost:8080)
	 * @param endPointPath subroute of REST-URL (e.g. /ingest/param/987398724)
	 * @param payLoad the request payload as string
	 * @param method the HTTP method (defaults to POST)
	 * @return ArrayList holding HTTP return code & response as String
	 */
	public static HttpResponseInfo restApiCall(String endPoint, String endPointPath, String payLoad, HttpMethod method) {
		HttpResponseInfo ri = new HttpResponseInfo();
		if (method == HttpMethod.POST) {
			
			Client client = null;
			WebTarget webTarget = null;
			Invocation.Builder invocationBuilder = null;
			Response response = null;
			client = ClientBuilder.newClient()
					.register(new RestAuth("",""));
			try {
				logger.info(endPoint+endPointPath);
				webTarget = client.target(endPoint).path(endPointPath);
				invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON);
				response = invocationBuilder.post(Entity.entity(payLoad, MediaType.APPLICATION_JSON));
				
				ri.sethttpCode(response.getStatus());
				ri.sethttpResponse(response.readEntity(String.class));
				response.close();
			} catch (Exception e) {
				logger.error(e.getMessage());
				return null;
			}
		}
		return ri;
	}
}
