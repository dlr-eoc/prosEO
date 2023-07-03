/**
 * StorageType.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.version2.model;

/**
 * Storage types supported by the prosEO Storage Manager, currently S3 and
 * POSIX.
 *
 * @author Denys Chaykovskiy
 */
public enum StorageType {
	/** Type S3 */
	S3,
	/** Type POSIX */
	POSIX
}