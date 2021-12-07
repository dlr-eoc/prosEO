package de.dlr.proseo.model;

import javax.persistence.Index;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

@MappedSuperclass
@Table(indexes = {
		@Index(unique = false, columnList = "datetime")
	})
abstract public class MonKpi01Base extends MonKpiBase {
	/**
	 * The count of successful events
	 */
	private long countSuccessful;

	/**
	 * @return the countSuccessful
	 */
	public long getCountSuccessful() {
		return countSuccessful;
	}

	/**
	 * @param countSuccessful the countSuccessful to set
	 */
	public void setCountSuccessful(long countSuccessful) {
		this.countSuccessful = countSuccessful;
	}
}
