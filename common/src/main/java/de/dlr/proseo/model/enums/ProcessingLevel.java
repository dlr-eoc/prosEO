/**
 * ProcessingLevel.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Processing level, usually indicating the number of processing steps required to create a product of this class from
 * unprocessed ("level 0") data. The enumeration values have been derived from a number of sources (listed below).
 * 
 * @author Dr. Thomas Bassler
 * 
 */
public enum ProcessingLevel {
	/** Reconstructed, unprocessed instrument and payload data */
	L0,
	/** Data derived from level 0 at the instrument’s full time/space resolution */
	L1,
	/** Reconstructed, unprocessed instrument data at full resolution, time-referenced, and annotated with ancillary information */
	L1A,
	/** Level 1A data that have been processed to sensor units */
	L1B,
	/** Further processed data at sensor units */
	L1C,
	/** Data derived from Level 1 data that have been processed to geophysical quantities of interest */
	L2,
	/** Level 2 data in a first or intermediate pro- cessing stage */
	L2A,
	/** Further processed level 2(A) data */
	L2B,
	/** Sometimes referred to as “level 2 cumula- tive”, i. e. aggregation of level 2 data over a larger time frame */
	L2C,
	/** Variables mapped on uniform space-time grid scales (resampling) */
	L3,
	/** Further derived products */
	L4
}
