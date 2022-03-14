package de.dlr.proseo.storagemgr.rest;

import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

public class ControllerUtils {
	
	public static RestFileInfo convertToRestFileInfo(StorageFile storageFile, long fileSize) { 
		
		RestFileInfo restFileInfo = new RestFileInfo();
		
		restFileInfo.setStorageType(storageFile.getStorageType().toString());
		restFileInfo.setFilePath(storageFile.getFullPath());
		restFileInfo.setFileName(storageFile.getFileName());
		restFileInfo.setFileSize(fileSize);
		
		return restFileInfo;
	}
}
