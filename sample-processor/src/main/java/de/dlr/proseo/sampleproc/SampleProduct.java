/**
 * SampleProduct.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.sampleproc;

import java.time.Instant;

/**
 * Dummy product for use with the prosEO Sample Processor
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class SampleProduct {
	/** Product identifier */
	private String id;
	/** Product type */
	private String type;
	/** Sensing start time */
	private Instant startTime;
	/** Sensing stop time */
	private Instant stopTime;
	/** Product revision */
	private Integer revision;
	
	/**
	 * Gets the product identifier
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Sets the product identifier
	 * 
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Gets the product type
	 * 
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the product type
	 * 
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Gets the sensing start time
	 * 
	 * @return the startTime
	 */
	public Instant getStartTime() {
		return startTime;
	}
	
	/**
	 * Sets the sensing start time
	 * 
	 * @param startTime the startTime to set
	 */
	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * Gets the sensing stop time
	 * 
	 * @return the stopTime
	 */
	public Instant getStopTime() {
		return stopTime;
	}
	
	/**
	 * Sets the sensing stop time
	 * 
	 * @param stopTime the stopTime to set
	 */
	public void setStopTime(Instant stopTime) {
		this.stopTime = stopTime;
	}
	
	/**
	 * @return the revision
	 */
	public Integer getRevision() {
		return revision;
	}
	
	/**
	 * @param revision the revision to set
	 */
	public void setRevision(Integer revision) {
		this.revision = revision;
	}

}
