package de.dlr.proseo.storagemgr.version2;

/**
 * Storage provider profile
 * 
 * @author Denys Chaykovskiy 
 *
 */

public enum StorageProviderProfile {
	/** POSIX */
	STORAGE_POSIX,
	/** S3 */
	STORAGE_S3,
	/** default from application.yml */
	DEFAULT
}
