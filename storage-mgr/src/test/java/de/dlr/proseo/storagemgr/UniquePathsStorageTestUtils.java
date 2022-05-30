package de.dlr.proseo.storagemgr;

import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.version2.StorageProvider;

public class UniquePathsStorageTestUtils extends BaseStorageTestUtils {

	private StorageProvider storageProvider;

	private UniqueStorageTestPaths uniquePaths;

	public UniquePathsStorageTestUtils(Object unitTest, TestName testName, StorageProvider storageProvider) {

		uniquePaths = new UniqueStorageTestPaths(unitTest, testName);

		sourcePath = uniquePaths.getSourcePath();
		storagePath = uniquePaths.getStoragePath();
		cachePath = uniquePaths.getCachePath();
		
		this.storageProvider = storageProvider;

		this.storageProvider.setSourcePath(uniquePaths.getSourcePath());
		this.storageProvider.setStoragePath(uniquePaths.getStoragePath());
		this.storageProvider.setCachePath(uniquePaths.getCachePath());
	}
	
	public void deleteUniqueTestDirectory() {
		uniquePaths.deleteUniqueTestDirectory();
	}
}
