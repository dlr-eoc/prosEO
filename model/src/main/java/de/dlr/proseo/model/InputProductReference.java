/**
 * InputProductReference.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Identification of the input product to use for On-Demand Production; its intention is to be unambiguous (as per ODPRIP ICD),
 * however this is not guaranteed given the limited number of specification options (uniqueness may be guaranteed when specifying
 * a file name, but it is not, when only specifying a sensing time range). The input product will be retrieved from some LTA,
 * if it is not readily available in prosEO.
 * 
 * If only the "ContentDate" is given and multiple products fulfilling the criteria are found in the LTA or in prosEO,
 * the product with the most recent generation time will be used. In the (unlikely) case of several products having the same
 * generation time, the product with the greatest file name (alphanumeric string comparison) will be used. Duplicate file names
 * are not allowed on any LTA or PRIP (and hence not in prosEO).
 * 
 * At least either the input file name or the sensing time interval must be specified.
 * 
 * @author Dr. Thomas Bassler
 */
@Embeddable
public class InputProductReference {

	/** The file name of the input product to retrieve */
	private String inputFileName;
	
	/** Sensing start time of the input product */
	@Column(name = "input_sensing_start_time", columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStartTime;
	
	/** Sensing stop time of the input product */
	@Column(name = "input_sensing_stop_time", columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStopTime;

	/**
	 * Gets the input file name
	 * 
	 * @return the input file name
	 */
	public String getInputFileName() {
		return inputFileName;
	}

	/**
	 * Sets the input file name
	 * 
	 * @param inputFileName the input file name to set
	 */
	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}

	/**
	 * Gets the sensing start time of the input product
	 * 
	 * @return the sensing start time
	 */
	public Instant getSensingStartTime() {
		return sensingStartTime;
	}

	/**
	 * Sets the sensing start time of the input product
	 * 
	 * @param sensingStartTime the sensing start time to set
	 */
	public void setSensingStartTime(Instant sensingStartTime) {
		this.sensingStartTime = sensingStartTime;
	}

	/**
	 * Gets the sensing stop time of the input product
	 * 
	 * @return the sensing stop time
	 */
	public Instant getSensingStopTime() {
		return sensingStopTime;
	}

	/**
	 * Sets the sensing stop time of the input product
	 * 
	 * @param sensingStopTime the sensing stop time to set
	 */
	public void setSensingStopTime(Instant sensingStopTime) {
		this.sensingStopTime = sensingStopTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(inputFileName, sensingStartTime, sensingStopTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof InputProductReference))
			return false;
		InputProductReference other = (InputProductReference) obj;
		return Objects.equals(inputFileName, other.getInputFileName()) 
				&& Objects.equals(sensingStartTime, other.getSensingStartTime())
				&& Objects.equals(sensingStopTime, other.getSensingStopTime());
	}

	@Override
	public String toString() {
		return "InputProductReference [inputFileName=" + inputFileName + ", sensingStartTime=" + sensingStartTime
				+ ", sensingStopTime=" + sensingStopTime + "]";
	}
	
}
