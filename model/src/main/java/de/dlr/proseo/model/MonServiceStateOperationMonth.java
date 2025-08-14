package de.dlr.proseo.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "datetime")
	})
public class MonServiceStateOperationMonth extends PersistentObject {

	/**
	 * The time of the states
	 */
	@Column(name = "datetime", columnDefinition = "TIMESTAMP")
	private Instant datetime;

	/**
	 * The service id
	 */
	@Column(name = "mon_service_id")
	private long monServiceId;

	/**
	 * The service up time
	 */
	@Column(name = "up_time")
	private double upTime;

	/**
	 * @return the datetime
	 */
	public Instant getDatetime() {
		return datetime;
	}

	/**
	 * @return the monServiceId
	 */
	public long getMonServiceId() {
		return monServiceId;
	}

	/**
	 * @return the upTime
	 */
	public double getUpTime() {
		return upTime;
	}

	/**
	 * @param datetime the datetime to set
	 */
	public void setDatetime(Instant datetime) {
		this.datetime = datetime;
	}

	/**
	 * @param monServiceId the monServiceId to set
	 */
	public void setMonServiceId(long monServiceId) {
		this.monServiceId = monServiceId;
	}

	/**
	 * @param upTime the upTime to set
	 */
	public void setUpTime(double upTime) {
		this.upTime = upTime;
	}	
	
}
