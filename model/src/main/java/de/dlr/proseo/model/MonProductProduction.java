package de.dlr.proseo.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

@MappedSuperclass
@Table(indexes = {
		@Index(unique = false, columnList = "datetime"),
		@Index(unique = false, columnList = "mission_id, productionType")
	})
abstract public class MonProductProduction extends PersistentObject {
	/**
	 * The mission
	 */
	@ManyToOne
	private Mission mission;
	/**
	 * The file size
	 */
	private long fileSize;
	/**
	 * The product count
	 */
	private int count;
	/**
	 * The production type
	 */
	private String productionType;
	/**
	 * Minimum latency
	 */
	private int productionLatencyMin;
	/**
	 * Maximum latency
	 */
	private int productionLatencyMax;
	/**
	 * Average latency
	 */
	private int productionLatencyAvg;
	/**
	 * Minimum latency
	 */
	private int totalLatencyMin;
	/**
	 * Maximum latency
	 */
	private int totalLatencyMax;
	/**
	 * Average latency
	 */
	private int totalLatencyAvg;
	/**
	 * The time of the states
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
	 * @return the productionType
	 */
	public String getProductionType() {
		return productionType;
	}
	/**
	 * @return the productionLatencyMin
	 */
	public int getProductionLatencyMin() {
		return productionLatencyMin;
	}
	/**
	 * @return the productionLatencyMax
	 */
	public int getProductionLatencyMax() {
		return productionLatencyMax;
	}
	/**
	 * @return the productionLatencyAvg
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

