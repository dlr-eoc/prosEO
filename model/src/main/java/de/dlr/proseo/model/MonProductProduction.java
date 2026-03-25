/**
 * MonProductProduction.java
 *
 * © 2021 Prophos Informatik GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * Abstract superclass for all entities monitoring product generation performance for a given period of time
 * per production type (systematic/on-demand/reprocessing)
 */
@MappedSuperclass
abstract public class MonProductProduction extends PersistentObject {
	/**
	 * The mission
	 */
	@ManyToOne
	private Mission mission;
	/**
	 * The aggregated file sizes
	 */
	private long fileSize;
	/**
	 * The number of products generated
	 */
	private int count;
	/**
	 * The production type
	 */
	private String productionType;
	/**
	 * Minimum latency since raw data availability at ground station (in seconds)
	 */
	private int productionLatencyMin;
	/**
	 * Maximum latency since raw data availability at ground station (in seconds)
	 */
	private int productionLatencyMax;
	/**
	 * Average latency since raw data availability at ground station (in seconds)
	 */
	private int productionLatencyAvg;
	/**
	 * Minimum total latency since sensing stop time (in seconds)
	 */
	private int totalLatencyMin;
	/**
	 * Maximum total latency since sensing stop time (in seconds)
	 */
	private int totalLatencyMax;
	/**
	 * Average total latency since sensing stop time (in seconds)
	 */
	private int totalLatencyAvg;
	/**
	 * The observation time period
	 */
	@Column(name = "datetime", columnDefinition = "TIMESTAMP")
	private Instant datetime;
	/**
	 * @return the mission
	 */
	public Mission getMission() {
		return mission;
	}
	/**
	 * @return the fileSize
	 */
	public long getFileSize() {
		return fileSize;
	}
	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}
	/**
	 * @return the production type
	 */
	public String getProductionType() {
		return productionType;
	}
	/**
	 * @return the minimum production latency in seconds
	 */
	public int getProductionLatencyMin() {
		return productionLatencyMin;
	}
	/**
	 * @return the maximum production latency in seconds
	 */
	public int getProductionLatencyMax() {
		return productionLatencyMax;
	}
	/**
	 * @return the average production latency in seconds
	 */
	public int getProductionLatencyAvg() {
		return productionLatencyAvg;
	}
	/**
	 * @return the totalLatencyMin
	 */
	public int getTotalLatencyMin() {
		return totalLatencyMin;
	}
	/**
	 * @return the totalLatencyMax
	 */
	public int getTotalLatencyMax() {
		return totalLatencyMax;
	}
	/**
	 * @return the totalLatencyAvg
	 */
	public int getTotalLatencyAvg() {
		return totalLatencyAvg;
	}
	/**
	 * @return the datetime
	 */
	public Instant getDatetime() {
		return datetime;
	}
	/**
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}
	/**
	 * @param fileSize the fileSize to set
	 */
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	/**
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}
	/**
	 * @param productionType the productionType to set
	 */
	public void setProductionType(String productionType) {
		this.productionType = productionType;
	}
	/**
	 * @param productionLatencyMin the productionLatencyMin to set
	 */
	public void setProductionLatencyMin(int productionLatencyMin) {
		this.productionLatencyMin = productionLatencyMin;
	}
	/**
	 * @param productionLatencyMax the productionLatencyMax to set
	 */
	public void setProductionLatencyMax(int productionLatencyMax) {
		this.productionLatencyMax = productionLatencyMax;
	}
	/**
	 * @param productionLatencyAvg the productionLatencyAvg to set
	 */
	public void setProductionLatencyAvg(int productionLatencyAvg) {
		this.productionLatencyAvg = productionLatencyAvg;
	}
	/**
	 * @param totalLatencyMin the totalLatencyMin to set
	 */
	public void setTotalLatencyMin(int totalLatencyMin) {
		this.totalLatencyMin = totalLatencyMin;
	}
	/**
	 * @param totalLatencyMax the totalLatencyMax to set
	 */
	public void setTotalLatencyMax(int totalLatencyMax) {
		this.totalLatencyMax = totalLatencyMax;
	}
	/**
	 * @param totalLatencyAvg the totalLatencyAvg to set
	 */
	public void setTotalLatencyAvg(int totalLatencyAvg) {
		this.totalLatencyAvg = totalLatencyAvg;
	}
	/**
	 * @param datetime the datetime to set
	 */
	public void setDatetime(Instant datetime) {
		this.datetime = datetime;
	}

}

