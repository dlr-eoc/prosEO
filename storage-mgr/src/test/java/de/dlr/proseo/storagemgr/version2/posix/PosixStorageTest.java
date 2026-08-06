/**
 * PosixStorageTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.version2.posix;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;

/**
 * Test class for PosixStorage
 * 
 * @author Denys Chaykovskiy
 */
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PosixStorageTest {

	@Autowired
	private BaseStorageTestUtils storageTestUtils;

	@Autowired
	private TestUtils testUtils;
	
	@Autowired
	private StorageProvider storageProvider;

	@Test
	public void testPosixPosixUpload(TestInfo testInfo) throws IOException {

		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyStorageDirectories();

		StorageType storageType = StorageType.POSIX;
		storageProvider.setDefaultStorage(storageType);

		String prefix = "posix-storage-upload-test/";

		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");

		// create source files
		for (String path : pathes) {
			storageTestUtils.createSourceFile(path);
		}

		StorageFile sourceDir = storageProvider.getSourceFile(prefix);
		StorageFile targetDir = storageProvider.getStorageFileFromDefaultStorage(prefix);

		// upload files
		List<String> uploadedPathes = storageProvider.getStorage().upload(sourceDir, targetDir);
		storageTestUtils.printPosixStorage();
		TestUtils.printList("Storage Files: ", uploadedPathes);
		assertEquals(3, uploadedPathes.size(), "Expected: 3, " + " Exists: " + uploadedPathes.size());

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM POSIX, " + " Exists: " + realStorageType);
		
		// clear 
		storageProvider.getStorage().delete(prefix);
	}

	@Test
	public void testPosixPosixDownload(TestInfo testInfo) throws IOException {

		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyStorageDirectories();

		StorageType storageType = StorageType.POSIX;
		storageProvider.setDefaultStorage(storageType);

		String prefix = "posix-storage-download-test/";

		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");

		// create source and upload files
		for (String path : pathes) {
			storageTestUtils.createSourceFile(path);
			storageTestUtils.uploadToPosixStorage(path);
		}
		storageTestUtils.printPosixStorage();

		StorageFile sourceDir = storageProvider.getStorageFileFromDefaultStorage(prefix);
		StorageFile targetDir = storageProvider.getCacheFile(prefix);

		// download files
		List<String> downloadedPathes = storageProvider.getStorage().download(sourceDir, targetDir);
		TestUtils.printList("Source Files: ", downloadedPathes);
		assertEquals(3, downloadedPathes.size(), "Expected: 3, " + " Exists: " + downloadedPathes.size());

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM POSIX, " + " Exists: " + realStorageType);
		
		// clear 
		storageProvider.getStorage().delete(prefix);
	}
}