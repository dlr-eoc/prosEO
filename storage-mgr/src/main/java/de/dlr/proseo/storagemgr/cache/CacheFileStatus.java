/**
 * CacheFileStatus.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.cache;

/**
 * Cache File Status shows the current status of the file in the cache storage
 * and in the cache list
 *
 * @author Denys Chaykovskiy
 */
public enum CacheFileStatus {

	/**
	 * Status READY - a file exists in the cache storage and was put to the cache
	 * list
	 */
	READY,

	/**
	 * Status INCOMPLETE - a file can exist in the cache storage, but can be
	 * corrupted (not completely uploaded and so on), that's why it is not in the cache list
	 */
	INCOMPLETE,
}