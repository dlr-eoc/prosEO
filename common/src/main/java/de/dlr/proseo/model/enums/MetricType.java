/**
 * MetricType.java
 * 
 * (C) 2024 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Metric type to use for a Production Metrics
 * 
 * @author Ernst Melchinger
 *
 */
public enum MetricType {
	/** String representation of metric (normally duration in seconds) */
	GAUGE,
	/** Cumulative amount of metric */
	COUNTER
}
