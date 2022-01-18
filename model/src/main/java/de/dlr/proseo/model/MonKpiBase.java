package de.dlr.proseo.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

@MappedSuperclass
@Table(indexes = {
		@Index(unique = false, columnList = "datetime")
	})
abstract public class MonKpiBase extends PersistentObject {
	/**
	 * The count of all events
	 */
	private long countAll;
	/**
	 * The time of the states
	 */
	@Column(name = "datetime", columnDefinition = "TIMESTAMP")
	private Instant datetime;
	/**
	 * @return the countAll
	 */
	public long getCountAll() {
		return countAll;
	}
	/**
	 * @return the datetime
	 */
	public Instant getDatetime() {
		return datetime;
	}
	/**
	 * @param countAll the countAll to set
	 */
	public void setCountAll(long countAll) {
		this.countAll = countAll;
	}
	/**
	 * @param datetime the datetime to set
	 */
	public void setDatetime(Instant datetime) {
		this.datetime = datetime;
	}
}