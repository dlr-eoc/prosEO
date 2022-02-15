package de.dlr.proseo.storagemgr.version2.model;

import java.util.List;

/**
 * Storage
 * 
 * @author Denys Chaykovskiy
 *
 */

// implements concepts of empty bucket, default bucket and one-bucket storages   

public interface Storage {
	
	public void setBucket(String bucket);
	
	public String getBucket(); 
	
	public boolean fileExists(StorageFile storageFile);

	public List<StorageFile> getFiles();
	
	public boolean uploadFile(StorageFile sourceFile, StorageFile storageFile);
	
	public boolean downloadFile(StorageFile storageFile, StorageFile targetFile);
}
