package de.dlr.proseo.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

@MappedSuperclass
@Table(indexes = {
		@Index(unique = false, columnList = "datetime")
	})
abstract public class MonRawDataDownlinkBase extends PersistentObject {
	/**
	 * The mission
	 */
	@ManyToOne
	private Mission mission;
	/**
	 * The spacecraft code
	 */
	private String spacecraftCode;
	/**
	 * The downliad size
	 */
	private long downloadSize;
	/**
	 * The product count
	 */
	private int count;
	/**
	 * Minimum latency
	 */
	private int downloadLatencyMin;
	/**
	 * Maximum latency
	 */
	private int downloadLatencyMax;
	/**
	 * Average latency
	 */
	private int downloadLatencyAvg;
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
	 * @return the count
	 */
	public int getCount() {
		return count;
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
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
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
	/**
	 * @return the spacecraftCode
	 */
	public String getSpacecraftCode() {
		return spacecraftCode;
	}
	/**
	 * @return the downloadSize
	 */
	public long getDownloadSize() {
		return downloadSize;
	}
	/**
	 * @return the downloadLatencyMin
	 */
	public int getDownloadLatencyMin() {
		return downloadLatencyMin;
	}
	/**
	 * @return the downloadLatencyMax
	 */
	public int getDownloadLatencyMax() {
		return downloadLatencyMax;
	}
	/**
	 * @return the downloadLatencyAvg
	 */
	public int getDownloadLatencyAvg() {
		return downloadLatencyAvg;
	}
	/**
	 * @param spacecraftCode the spacecraftCode to set
	 */
	public void setSpacecraftCode(String spacecraftCode) {
		this.spacecraftCode = spacecraftCode;
	}
	/**
	 * @param downloadSize the downloadSize to set
	 */
	public void setDownloadSize(long downloadSize) {
		this.downloadSize = downloadSize;
	}
	/**
	 * @param downloadLatencyMin the downloadLatencyMin to set
	 */
	public void setDownloadLatencyMin(int downloadLatencyMin) {
		this.downloadLatencyMin = downloadLatencyMin;
	}
	/**
	 * @param downloadLatencyMax the downloadLatencyMax to set
	 */
	public void setDownloadLatencyMax(int downloadLatencyMax) {
		this.downloadLatencyMax = downloadLatencyMax;
	}
	/**
	 * @param downloadLatencyAvg the downloadLatencyAvg to set
	 */
	public void setDownloadLatencyAvg(int downloadLatencyAvg) {
		this.downloadLatencyAvg = downloadLatencyAvg;
	}

}

