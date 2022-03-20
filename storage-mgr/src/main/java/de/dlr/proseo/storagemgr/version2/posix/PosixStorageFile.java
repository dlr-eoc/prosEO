package de.dlr.proseo.storagemgr.version2.posix;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

/**
 * Posix Storage File
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PosixStorageFile implements StorageFile {
	
	// TODO: Make verifications for pathes 

	//   "/" + basepath + bucket (optional) + relativePath (with fileName)    
	private String basePath; 
	private String bucket; 
	private String relativePath;  
	
	
	// no bucket
	public PosixStorageFile(String basePath, String relativePath) { 
		this(basePath, StorageFile.NO_BUCKET,  relativePath);
	}
	
	// default or another bucket
	public PosixStorageFile(String basePath, String bucket, String relativePath) { 

		this.basePath = verifyBasePath(basePath); 
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
		return StorageType.POSIX;
	}
	
	
	
	
	private String verifyBasePath(String basePath) 
	{
		if (!basePath.startsWith("/")) basePath = "/" + basePath;
	
		return basePath; 
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
