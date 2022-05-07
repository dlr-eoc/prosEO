package de.dlr.proseo.storagemgr.version2.model;

import java.io.IOException;
import java.util.ArrayList;
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
	
	public String uploadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException;
	
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException;
	
	public StorageType getStorageType();

	public StorageFile createStorageFile(String relativePath, String content);

	public List<String> upload(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException;
	
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException;

	public List<String> getFiles(String prefix); 
	
	public List<String> getFiles(); 
	
	public List<String> delete(StorageFile storageFileOrDir) throws IOException;
	
	public String deleteFile(StorageFile storageFileOrDir) throws IOException;
	
	public boolean isFile(StorageFile storageFileOrDir);
	
	public boolean isDirectory(StorageFile storageFileOrDir);
}
