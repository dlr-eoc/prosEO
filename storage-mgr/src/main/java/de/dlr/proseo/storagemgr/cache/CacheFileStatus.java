/**
 * CacheFileStatus.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.cache;

/**
 * Cache File Status shows the current status of the file in the cache
 *
 * @author Denys Chaykovskiy
 */
public enum CacheFileStatus {
	/** Status READY */
	READY,
	/** Status UPLOADING */
	UPLOADING, 
	/** Status DOWNLOADING */
	DOWNLOADING, 
}