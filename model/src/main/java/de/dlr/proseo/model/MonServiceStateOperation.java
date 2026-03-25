/**
 * MonServiceStateOperation.java
 *
 * © 2021 Prophos Informatik GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * The state of a prosEO service at a given point in time
 */
@Entity
@Table(indexes = {
	@Index(unique = false, columnList = "datetime"),
	@Index(unique = false, columnList = "mon_service_id"),
	@Index(unique = false, columnList = "mon_service_state_id")
})
public class MonServiceStateOperation extends PersistentObject {
	/**
	 * The service state
	 */
	@ManyToOne
	private MonServiceState monServiceState;
	/**
	 * The service name
	 */
	@ManyToOne
	private MonService monService;
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
	public MonService getMonService() {
		return monService;
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
	public void setMonService(MonService monService) {
		this.monService = monService;
	}
	/**
	 * @param datetime the datetime to set
	 */
	public void setDatetime(Instant datetime) {
		this.datetime = datetime;
	}

}
