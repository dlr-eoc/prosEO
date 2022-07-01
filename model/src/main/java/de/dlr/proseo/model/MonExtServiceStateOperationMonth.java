package de.dlr.proseo.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "datetime")
	})
public class MonExtServiceStateOperationMonth extends PersistentObject {

	/**
	 * The time of the states
	 */
	@Column(name = "datetime", columnDefinition = "TIMESTAMP")
	private Instant datetime;

	/**
	 * The service id
	 */
	@Column(name = "mon_ext_service_id")
	private long monExtServiceId;

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
	 * @return the monExtServiceId
	 */
	public long getMonExtServiceId() {
		return monExtServiceId;
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
	 * @param monExtServiceId the monExtServiceId to set
	 */
	public void setMonExtServiceId(long monServiceId) {
		this.monExtServiceId = monServiceId;
	}

	/**
	 * @param upTime the upTime to set
	 */
	public void setUpTime(double upTime) {
		this.upTime = upTime;
	}	
	
}
