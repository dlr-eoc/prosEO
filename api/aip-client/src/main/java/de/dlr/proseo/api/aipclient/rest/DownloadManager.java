/**
 * DownloadManager.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient.rest;

import java.util.List;

import de.dlr.proseo.api.aipclient.rest.model.RestProduct;
import de.dlr.proseo.model.ProcessingFacility;

/**
 * Class to handle product downloads from remote Long-term Archives
 * 
 * @author Dr. Thomas Bassler
 */
public class DownloadManager {

	public RestProduct downloadByName(String filename, ProcessingFacility processingFacility) {
		// TODO Auto-generated method stub
		return null;
	}

	public RestProduct downloadBySensingTime(String productType, String startTime, String stopTime,
			ProcessingFacility processingFacility) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<RestProduct> downloadAllBySensingTime(String productType, String startTime, String stopTime,
			ProcessingFacility processingFacility) {
		// TODO Auto-generated method stub
		return null;
	}

}
