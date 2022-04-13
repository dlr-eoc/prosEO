package de.dlr.proseo.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "datetime"),
		@Index(unique = false, columnList = "mon_ext_service_id"),
		@Index(unique = false, columnList = "mon_service_state_id")
})
public class MonExtServiceStateOperation extends PersistentObject {
	/**
	 * The service state
	 */
	@ManyToOne
	private MonServiceState monServiceState;
	/**
	 * The service name
	 */
	@ManyToOne
	private MonExtService monExtService;
	/**
	 * The time of service state
	 */
	@Column(name = "datetime", columnDefinition = "TIMESTAMP")
	private Instant datetime;
	/**
	 * @return the monServiceState
	 */
	public MonServiceState getMonServiceState() {
		return monServiceState;
	}
	/**
	 * @return the monService
	 */
	public MonExtService getMonExtService() {
		return monExtService;
	}
	/**
	 * @return the datetime
	 */
	public Instant getDatetime() {
		return datetime;
	}
	/**
	 * @param monServiceState the monServiceState to set
	 */
	public void setMonServiceState(MonServiceState monServiceState) {
		this.monServiceState = monServiceState;
	}
	/**
	 * @param monService the monService to set
	 */
	public void setMonExtService(MonExtService monExtService) {
		this.monExtService = monExtService;
	}
	/**
	 * @param datetime the datetime to set
	 */
	public void setDatetime(Instant datetime) {
		this.datetime = datetime;
	}

}
