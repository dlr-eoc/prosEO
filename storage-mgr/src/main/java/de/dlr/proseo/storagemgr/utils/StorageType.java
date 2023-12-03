package de.dlr.proseo.storagemgr.utils;

/**
 * Storage types of prosEO
 * 
 * @author melchinger
 *
 */
public enum StorageType {
	/** Type S3 */
	S3,
	/** Type ALLUXIO, not used yet */
	ALLUXIO,
	/** Type POSIX */
	POSIX,
	/** Type OTHER, undefined type */
	OTHER
}
