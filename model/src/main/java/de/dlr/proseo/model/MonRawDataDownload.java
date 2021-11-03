
package de.dlr.proseo.model;

import java.time.Instant;

import javax.annotation.concurrent.Immutable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Immutable
public class MonRawDataDownload {    
//	@Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    @Column(name = "anyval", updatable = false, nullable = false)
//    private Long anyval;
	/**
	 * The mission
	 */
	private long missionId;
	/**
	 * The download code
	 */
	@Id
	private String downloadId;
	/**
	 * The spacecraft code
	 */
	private String spacecraftCode;
	/**
	 * The donwlink site
	 */
	private String downlinkSite;
	/**
	 * The data stop time
	 */
	@Column(name = "data_stop_time", columnDefinition = "TIMESTAMP")
	private Instant dataStopTime;
	/**
	 * The raw_data_availability_time
	 */
	@Column(name = "raw_data_availability_time", columnDefinition = "TIMESTAMP")
	private Instant rawDataAvailabilityTime;
	/**
	 * Received
	 */
	private Boolean received;
	/**
	 * The received_time
	 */
	@Column(name = "download_time", columnDefinition = "TIMESTAMP")
	private Instant downloadTime;
	/**
	 * The download_size
	 */
	private long downloadSize;
	/**
	 * @return the missionId
	 */
	public long getMissionId() {
		return missionId;
	}
	/**
	 * @return the downloadId
	 */
	public String getDownloadId() {
		return downloadId;
	}
	/**
	 * @return the spacecraftCode
	 */
	public String getSpacecraftCode() {
		return spacecraftCode;
	}
	/**
	 * @return the downlinkSite
	 */
	public String getDowblinkSite() {
		return downlinkSite;
	}
	/**
	 * @return the dataStopTime
	 */
	public Instant getDataStopTime() {
		return dataStopTime;
	}
	/**
	 * @return the rawDataAvailabilityTime
	 */
	public Instant getRawDataAvailabilityTime() {
		return rawDataAvailabilityTime;
	}
	/**
	 * @return the received
	 */
	public Boolean getReceived() {
		return received;
	}
	/**
	 * @return the downloadTime
	 */
	public Instant getDownloadTime() {
		return downloadTime;
	}
	/**
	 * @return the downloadSize
	 */
	public long getDownloadSize() {
		return downloadSize;
	}
	/**
	 * @param missionId the missionId to set
	 */
	public void setMissionId(long missionId) {
		this.missionId = missionId;
	}
	/**
	 * @param downloadId the downloadId to set
	 */
	public void setDownloadId(String downloadId) {
		this.downloadId = downloadId;
	}
	/**
	 * @param spacecraftCode the spacecraftCode to set
	 */
	public void setSpacecraftCode(String spacecraftCode) {
		this.spacecraftCode = spacecraftCode;
	}
	/**
	 * @param donwlinkSite the downlinkSite to set
	 */
	public void setDownlinkSite(String downlinkSite) {
		this.downlinkSite = downlinkSite;
	}
	/**
	 * @param dataStopTime the dataStopTime to set
	 */
	public void setDataStopTime(Instant dataStopTime) {
		this.dataStopTime = dataStopTime;
	}
	/**
	 * @param rawDataAvailabilityTime the rawDataAvailabilityTime to set
	 */
	public void setRawDataAvailabilityTime(Instant rawDataAvailabilityTime) {
		this.rawDataAvailabilityTime = rawDataAvailabilityTime;
	}
	/**
	 * @param received the received to set
	 */
	public void setReceived(Boolean received) {
		this.received = received;
	}
	/**
	 * @param downloadTime the downloadTime to set
	 */
	public void setDownloadTime(Instant downloadTime) {
		this.downloadTime = downloadTime;
	}
	/**
	 * @param downloadSize the downloadSize to set
	 */
	public void setDownloadSize(long downloadSize) {
		this.downloadSize = downloadSize;
	}
	
	
}

