package de.dlr.proseo.storagemgr.version2.model;

/**
 * Storage File Structure. The file can exist or it can be just an abstract structure
 * 
 * @author Denys Chaykovskiy
 *
 */
public interface StorageFile {
	
	public final static String NO_BUCKET = ""; 

	public String getFullPath(); 
	// path to bucket 
	public String getBasePath();

	public String getBucket();
	// path from bucket 
	public String getRelativePath();

	public String getFileName();
	
	public StorageType getStorageType(); 
	
	public String getExtension();
}	

