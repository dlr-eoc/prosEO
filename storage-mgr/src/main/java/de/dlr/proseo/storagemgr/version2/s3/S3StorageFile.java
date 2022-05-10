package de.dlr.proseo.storagemgr.version2.s3;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorageFile;

/**
 * S3 Storage File
 * 
 * s3:/ + basepath + bucket (optional) + relativePath (with fileName)  
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3StorageFile implements StorageFile {
	
	private static final String S3PREFIX = "s3:/"; 
	private static final String SLASH = "/"; 

	private String bucket; 
	private String relativePath;  
	
	
	public S3StorageFile(String bucket, String relativePath) { 

		this.bucket = bucket; 
		this.relativePath = relativePath; 
	}
	
	public S3StorageFile(StorageFile storageFile) { 
		this(storageFile.getBucket(), storageFile.getRelativePath() );
	}
	
	@Override
	public String getFullPath() {
		String path = Paths.get(bucket, relativePath).toString();
		return addS3Prefix( new PathConverter(path).convertToSlash().getPath());
	}
	
	private String addS3Prefix(String path) {
		return path.startsWith(SLASH) ? S3PREFIX + path : S3PREFIX + SLASH + path;
	}

	@Override
	public String getBasePath() {
		return ""; // no base path in s3
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

	@Override
	public String getExtension() {
		return FilenameUtils.getExtension(relativePath);
	}

	@Override
	public void setBasePath(String basePath) {
	}

	@Override
	public void setBucket(String bucket) {
		this.bucket = bucket; 		
	}

	@Override
	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath; 	
	}

	@Override
	public boolean isDirectory() {
		
		return relativePath.endsWith(SLASH) ? true : false; 
	}

}
