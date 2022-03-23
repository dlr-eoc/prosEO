package de.dlr.proseo.storagemgr.version2.model;

import java.io.IOException;
import java.util.List;

/**
 * Storage
 * 
 * @author Denys Chaykovskiy
 *
 */

// implements concepts of empty bucket, default bucket and one-bucket storages   

public interface Storage {
	
	public String getBasePath(); 
	
	public void setBucket(String bucket);
	
	public String getBucket(); 
	
	public boolean fileExists(StorageFile storageFile);
	
	public long getFileSize(StorageFile storageFile);
	
	public StorageFile getStorageFile(String relativePath);

	public List<StorageFile> getStorageFiles();
	
	public void uploadFile(StorageFile sourceFile, StorageFile targetFile) throws IOException;
	
	public void downloadFile(StorageFile sourceFile, StorageFile targetFile) throws IOException;
	
	public StorageType getStorageType();

	public StorageFile createStorageFile(String relativePath, String content); 
}
