package de.dlr.proseo.storagemgr.version2.model;

import java.util.List;

/**
 * Bucket Storage
 * 
 * @author Denys Chaykovskiy
 *
 */

// concept of default bucket

// 1. No Bucket (now in POSIX) 
// 2. Default bucket 
// 3. Buckets 

public interface BucketsStorage extends Storage {
	
	public List<String> getBuckets(); 
	
	public boolean bucketExists(String bucketName); 
	
	public void deleteBucket(String bucketName);
}
