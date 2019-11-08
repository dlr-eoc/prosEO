package de.dlr.proseo.basewrap.rest;

public class HttpResponseInfo {

	private int httpCode;
	private String httpResponse;

	public int gethttpCode() {
		return httpCode;
	}

	public void sethttpCode(int code) {
		this.httpCode = code;
	}

	public String gethttpResponse() {
		return httpResponse;
	}

	public void sethttpResponse(String response) {
		this.httpResponse = response;
	}
}
