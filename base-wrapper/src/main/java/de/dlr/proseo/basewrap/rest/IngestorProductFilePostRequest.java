package de.dlr.proseo.basewrap.rest;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;


@XmlAccessorType(XmlAccessType.PROPERTY)
public class IngestorProductFilePostRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3599822452503983329L;
	private long id;
	private int version;
	private long productId;
	private String processingFacilityName;
	private String productFileName;
	private String[] auxFileNames;
	private String filePath;
	private String storageType;
	
	
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}
	/**
	 * @return the productId
	 */
	public long getProductId() {
		return productId;
	}
	/**
	 * @param productId the productId to set
	 */
	public void setProductId(long productId) {
		this.productId = productId;
	}
	/**
	 * @return the processingFacilityName
	 */
	public String getProcessingFacilityName() {
		return processingFacilityName;
	}
	/**
	 * @param processingFacilityName the processingFacilityName to set
	 */
	public void setProcessingFacilityName(String processingFacilityName) {
		this.processingFacilityName = processingFacilityName;
	}
	/**
	 * @return the productFileName
	 */
	public String getProductFileName() {
		return productFileName;
	}
	/**
	 * @param productFileName the productFileName to set
	 */
	public void setProductFileName(String productFileName) {
		this.productFileName = productFileName;
	}
	/**
	 * @return the auxFileNames
	 */
	public String[] getAuxFileNames() {
		return auxFileNames;
	}
	/**
	 * @param auxFileNames the auxFileNames to set
	 */
	public void setAuxFileNames(String[] auxFileNames) {
		this.auxFileNames = auxFileNames;
	}
	/**
	 * @return the filePath
	 */
	public String getFilePath() {
		return filePath;
	}
	/**
	 * @param filePath the filePath to set
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	/**
	 * @return the storageType
	 */
	public String getStorageType() {
		return storageType;
	}
	/**
	 * @param storageType the storageType to set
	 */
	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}
	
	@Override
    public String toString() 
    { 
        return "IngestorProductFilePostRequest ["
        	+ "id="+ id
            + ", version="+ version 
            + ", productId="+ productId
            + ", processingFacilityName="+ processingFacilityName
            + ", productFileName="+ productFileName
            + ", auxFileNames="+ auxFileNames
            + ", filePath="+ filePath
            + ", storageType="+ storageType
            + "]"; 
    } 
}
