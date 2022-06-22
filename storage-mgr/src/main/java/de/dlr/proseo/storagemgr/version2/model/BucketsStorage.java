package de.dlr.proseo.storagemgr.version2.model;

import java.util.List;

/**
 * Bucket Storage Interface
 * 
 * @author Denys Chaykovskiy
 *
 */
// concept of default bucket
// 1. No Bucket (now in POSIX) 2. Default bucket 3. Buckets 
public interface BucketsStorage extends Storage {

	/**
	 * Gets buckets from storage
	 * 
	 * @return list of buckets
	 */
	public List<String> getBuckets();

	/**
	 * Checks if the bucket exists
	 * 
	 * @param bucketName the name of the bucket
	 * @return true if the bucket exists
	 */
	public boolean bucketExists(String bucketName);

	/**
	 * Deletes the bucket
	 *
	 * @param bucketName the name of the bucket
	 */
	public void deleteBucket(String bucketName);
}
