/**
 * Product.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;

import javax.persistence.Entity;

/**
 * Representation of a data product
 * 
 * @author Dr. Thomas Bassler
 *
 */

@Entity

public class Product extends PersistentObject {
	
	/** The unique ID used in the DIMS Product Library */
	private String plUniqueId;
	/** Product file location in the archive storage (e. g. S3) */
	private String archiveLocation;
	/** Product file location in the working cache */
	private String cacheLocation;
	/** Sensing start time */
	private Instant sensingStartTime;
	/** Sensing stop time */
	private Instant sensingStopTime;
	/** Product revision */
	private Integer revision;
	
	/**
	 * Gets the unique ID used in the DIMS Product Library
	 * 
	 * @return the plUniqueId
	 */
	public String getPlUniqueId() {
		return plUniqueId;
	}
	
	/**
	 * Sets the PL unique ID
	 * 
	 * @param plUniqueId the plUniqueId to set
	 */
	public void setPlUniqueId(String plUniqueId) {
		this.plUniqueId = plUniqueId;
	}
	
	/**
	 * Gets the location of the product files in the archive storage
	 * 
	 * @return the archiveLocation
	 */
	public String getArchiveLocation() {
		return archiveLocation;
	}
	
	/**
	 * Sets the location of the product files in the archive storage
	 * 
	 * @param archiveLocation the archiveLocation to set
	 */
	public void setArchiveLocation(String archiveLocation) {
		this.archiveLocation = archiveLocation;
	}
	
	/**
	 * Gets the location of the product files in the working cache
	 * 
	 * @return the cacheLocation
	 */
	public String getCacheLocation() {
		return cacheLocation;
	}
	/**
	 * Sets the location of the product files in the working cache
	 * 
	 * @param cacheLocation the cacheLocation to set
	 */
	public void setCacheLocation(String cacheLocation) {
		this.cacheLocation = cacheLocation;
	}
	
	/**
	 * Gets the sensing start time
	 * 
	 * @return the sensingStartTime
	 */
	public Instant getSensingStartTime() {
		return sensingStartTime;
	}
	/**
	 * Sets the sensing start time
	 * 
	 * @param sensingStartTime the sensingStartTime to set
	 */
	public void setSensingStartTime(Instant sensingStartTime) {
		this.sensingStartTime = sensingStartTime;
	}
	/**
	 * Gets the sensing stop time
	 * 
	 * @return the sensingStopTime
	 */
	public Instant getSensingStopTime() {
		return sensingStopTime;
	}
	
	/**
	 * Sets the sensing stop time
	 * 
	 * @param sensingStopTime the sensingStopTime to set
	 */
	public void setSensingStopTime(Instant sensingStopTime) {
		this.sensingStopTime = sensingStopTime;
	}

	/**
	 * Gets the product revision
	 * 
	 * @return the revision
	 */
	public Integer getRevision() {
		return revision;
	}

	/**
	 * Sets the product revision
	 * 
	 * @param revision the revision to set
	 */
	public void setRevision(Integer revision) {
		this.revision = revision;
	}

}
