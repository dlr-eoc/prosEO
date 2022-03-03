package de.dlr.proseo.storagemgr.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;

public class HttpResponses {
	
	public static ResponseEntity<RestFileInfo> createOk(RestFileInfo restFileInfo) { 
		return new ResponseEntity<>(restFileInfo, HttpStatus.OK);
	}
	
	
	public static ResponseEntity<RestFileInfo> createError(String message, Exception e) { 
		
		// TODO: switch for exception 
		// getHTTPStatus from Exception 
		
		return new ResponseEntity<>(httpErrorHeaders(message), HttpStatus.BAD_REQUEST);
	}
	
	private static  HttpHeaders httpErrorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Warning", "199 proseo-storage-mgr " + message.replaceAll("\n", " "));
		return responseHeaders;
	}

}
