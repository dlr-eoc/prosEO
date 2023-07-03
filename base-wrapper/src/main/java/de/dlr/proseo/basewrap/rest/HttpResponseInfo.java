/**
 * HttpResponseInfo.java
 *
 *  (C) 2019 Hubert Asamer, DLR
 *  (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.basewrap.rest;

/**
 * A class to hold information extracted from a HTTP response.
 * 
 * @author Hubert Asamer
 */
public class HttpResponseInfo {

	/** The HTTP code */
	private int httpCode;

	/** The HTTP warning */
	private String httpWarning;

	/** The HTTP response */
	private String httpResponse;

	/**
	 * Gets the HTTP code
	 * 
	 * @return the HTTP code
	 */
	public int gethttpCode() {
		return httpCode;
	}

	/**
	 * Sets the HTTP code
	 * 
	 * @param code the HTTP code
	 */
	public void sethttpCode(int code) {
		this.httpCode = code;
	}

	/**
	 * Gets the HTTP warning
	 * 
	 * @return the HTTP warning
	 */
	public String getHttpWarning() {
		return httpWarning;
	}

	/**
	 * Sets the HTTP warning
	 * 
	 * @param httpWarning the HTTP warning
	 */
	public void setHttpWarning(String httpWarning) {
		this.httpWarning = httpWarning;
	}

	/**
	 * Gets the HTTP response
	 * 
	 * @return the HTTP response
	 */
	public String gethttpResponse() {
		return httpResponse;
	}

	/**
	 * Sets the HTTP response
	 * 
	 * @param response the HTTP response
	 */
	public void sethttpResponse(String response) {
		this.httpResponse = response;
	}
}