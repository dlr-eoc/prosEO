/**
 * ApiMetrics.java
 * 
 * (C) 2024 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import de.dlr.proseo.model.enums.MetricType;

/**
 * Collect production metrics
 * 
 * @author Ernst Melchinger
 *
 */
@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "name"),
		@Index(unique = false, columnList = "timestamp")
	})
public class ApiMetrics extends PersistentObject {

	/** The metric name e.g. <productionType>.<platformShortName>.<platformSerialIdentifier>.size */
	private String name;

	/**
	 * Date/time of metric reporting. Time is in UTC in the format YYYY-MM-DDThh:mm:ss.sssZ
	 * For daily entries it is in the format YYYY-MM-DDT00:00:00.000Z
	 * and monthly entries it is in the format YYYY-MM-01T00:00:00.000Z
	 */
	@Column(name = "timestamp", columnDefinition = "TIMESTAMP")
	private Instant timestamp;

	/**
	 * Gauge or Counter
	 */
	private MetricType metrictype;

	/**
	 * Value of Gauge at reporting timestamp (mandatory if MetricType=Gauge)
	 */
	private String gauge;

	/**
	 * Value of Counter at reporting timestamp (mandatory if MetricType=Counter)
	 */
	private long count;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the metrictype
	 */
	public MetricType getMetrictype() {
		return metrictype;
	}

	/**
	 * @return the gauge
	 */
	public String getGauge() {
		return gauge;
	}

	/**
	 * @return the count
	 */
	public long getCount() {
		return count;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @param metrictype the metrictype to set
	 */
	public void setMetrictype(MetricType metrictype) {
		this.metrictype = metrictype;
	}

	/**
	 * @param gauge the gauge to set
	 */
	public void setGauge(String gauge) {
		this.gauge = gauge;
	}

	/**
	 * @param count the count to set
	 */
	public void setCount(long count) {
		this.count = count;
	}

	
	
}
