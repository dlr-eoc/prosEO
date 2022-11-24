package de.dlr.proseo.storagemgr;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.Storage;

/**
 * @author Denys Chaykovskiy
 *
 */
@Component
public class StorageTestUtils extends BaseStorageTestUtils {


	@Autowired
	private TestUtils testUtils;

 	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	@PostConstruct
	private void init() {

		sourcePath = testUtils.getSourcePath();
		storagePath = testUtils.getStoragePath();
		cachePath = testUtils.getCachePath();

		theTestUtils = this;
		
		// storageProvider = new StorageProvider();
	}

	private static StorageTestUtils theTestUtils;

	public static StorageTestUtils getInstance() {

		return theTestUtils;
	}

	public void printVersion(String msg) {

		System.out.println(msg + (storageProvider.isVersion2() ? " Version-2" : " Version-1"));
	}
	
	public static void printStorageFiles(String message, Storage storage) {
		
		List<String> storageFiles;
		try {
			storageFiles = storage.getRelativeFiles();	
			String storageType = storage.getStorageType().toString();
			TestUtils.printList(message + ". Storage " + storageType + " files || " + storage.getAbsoluteBasePath(), storageFiles);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void printStorageFilesWithPrefix(String message, Storage storage, String prefix) {
		
		List<String> storageFiles;
		try {
			storageFiles = storage.getRelativeFiles(prefix);	
			String storageType = storage.getStorageType().toString();
			TestUtils.printList(message + ". Storage " + storageType + " files || Prefix: " + prefix + " || " + storage.getAbsoluteBasePath(), storageFiles);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
