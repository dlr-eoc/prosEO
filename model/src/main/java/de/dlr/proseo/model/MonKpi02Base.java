package de.dlr.proseo.model;

import javax.persistence.Index;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

@MappedSuperclass
@Table(indexes = {
		@Index(unique = false, columnList = "datetime")
	})
abstract public class MonKpi02Base extends MonKpiBase {
	/**
	 * The count of completed events
	 */
	private long countCompleted;

	/**
	 * @return the countCompleted
	 */
	public long getCountCompleted() {
		return countCompleted;
	}

	/**
	 * @param countCompleted the countCompleted to set
	 */
	public void setCountCompleted(long countCompleted) {
		this.countCompleted = countCompleted;
	}
}
