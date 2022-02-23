package de.dlr.proseo.storagemgr.version2.s3;

import java.io.File;
import java.nio.file.Paths;

import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

/**
 * S3 Storage File
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3StorageFile implements StorageFile {
	
	// TODO: Make verifications for pathes 

	//   "/" + basepath + bucket (optional) + relativePath (with fileName)    
	private static final String basePath = "S3:/"; 
	private String bucket; 
	private String relativePath;  
	
	
	// default or another bucket
	public S3StorageFile(String bucket, String relativePath) { 

		
		this.bucket = verifyBucket(bucket); 
		this.relativePath = verifyRelativePath(relativePath); 
	}
	
	@Override
	public String getFullPath() {
		return Paths.get(basePath, bucket, relativePath).toString();
	}

	@Override
	public String getBasePath() {
		return basePath;
	}

	@Override
	public String getBucket() {
		return bucket;
	}

	@Override
	public String getRelativePath() {
		return relativePath;
	}

	@Override
	public String getFileName() {
		return new File(relativePath).getName();
	}
	
	@Override
	public StorageType getStorageType() {
		return StorageType.S3;
	}
	
	private String verifyBucket(String bucket) 
	{
		return bucket; 
	}
	
	private String verifyRelativePath(String relativePath) 
	{
		return relativePath; 
	}

}
