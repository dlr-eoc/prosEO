/**
 * PosixDALTest.java
 * 
 * (c) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.version2.posix;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;
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
import de.dlr.proseo.storagemgr.posix.PosixDAL;
import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * Test class for PosixDAL
 * 
 * @author Denys Chaykovskiy
 */
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PosixDALTest {

	private static final String SOURCE_DIRECTORY = "source";
	private static final String STORAGE_DIRECTORY = "backend";
	private static final String CACHE_DIRECTORY = "cache";

	private String uniqueTestFolder; // testName_methodName
	private String uniqueSourcePath;  //  /../testdata/source/testName_methodName
	private String uniqueStoragePath;
	private String uniqueCachePath;

	@Autowired
	private BaseStorageTestUtils storageTestUtils;
	
	@Autowired
	private TestUtils testUtils;
	
	@Autowired
	private StorageProvider storageProvider;
	
	public void createUniqueTestFolders(TestInfo testInfo) {

		String className = PosixDALTest.class.getSimpleName();
		String methodName = testInfo.getTestMethod().get().getName();
		uniqueTestFolder = className + "_" + methodName;

		String testPath = testUtils.getTestFolder();

		uniqueSourcePath = Paths.get(testPath, SOURCE_DIRECTORY, uniqueTestFolder).toString();
		uniqueSourcePath = new PathConverter(uniqueSourcePath).convertToSlash().getPath();
		testUtils.createDirectory(uniqueSourcePath);

		uniqueStoragePath = Paths.get(testPath, STORAGE_DIRECTORY, uniqueTestFolder).toString();
		uniqueStoragePath = new PathConverter(uniqueStoragePath).convertToSlash().getPath();
		testUtils.createDirectory(uniqueStoragePath);

		uniqueCachePath = Paths.get(testPath, CACHE_DIRECTORY, uniqueTestFolder).toString();
		uniqueCachePath = new PathConverter(uniqueCachePath).convertToSlash().getPath();
		testUtils.createDirectory(uniqueCachePath);
		
	}

	public void deleteUniqueTestDirectories() {

		TestUtils.deleteDirectory(uniqueSourcePath);
		TestUtils.deleteDirectory(uniqueStoragePath);
		TestUtils.deleteDirectory(uniqueCachePath);
	}

	@Test
	public void test(TestInfo testInfo) {

		TestUtils.printMethodName(this, testInfo);
		createUniqueTestFolders(testInfo);

		// create unique source paths
		List<String> pathes = new ArrayList<>();
		pathes.add(new PathConverter(uniqueTestFolder, "file1.txt").getPath());
		pathes.add(new PathConverter(uniqueTestFolder, "file2.txt").getPath());
		pathes.add(new PathConverter(uniqueTestFolder, "dir/file3.txt").getPath());

		// create source files
		List<String> sourcePathes = new ArrayList<>();
		for (String path : pathes) {

			String sourcePath = storageTestUtils.createSourceFile(path);
			sourcePathes.add(sourcePath);
		}

		String sourcePath = uniqueSourcePath;
		String storagePath = uniqueStoragePath;
		PosixDAL posixDAL = new PosixDAL(storageProvider.getPosixConfigurationFromFile());

		try {
			// print source files
			List<String> sourceFiles = posixDAL.getFiles(sourcePath);
			TestUtils.printList("Source Files: ", sourceFiles);
			assertEquals(3, sourceFiles.size(), "Expected: 3, " + " Exists: " + sourceFiles.size());

			// upload files to storage
			List<String> uploadedFiles = posixDAL.upload(sourcePath, storagePath);
			TestUtils.printList("Uploaded Files: ", uploadedFiles);
			assertEquals(3, uploadedFiles.size(), "Expected: 3, " + " Exists: " + uploadedFiles.size());

			// delete source files
			List<String> deletedFiles = posixDAL.delete(sourcePath);
			TestUtils.printList("Deleted Files: ", deletedFiles);
			assertEquals(3, deletedFiles.size(), "Expected: 3, " + " Exists: " + deletedFiles.size());

			// download files from storage
			List<String> downloadedFiles = posixDAL.download(storagePath, sourcePath);
			TestUtils.printList("Downloaded Files: ", downloadedFiles);
			assertEquals(3, downloadedFiles.size(), "Expected: 3, " + " Exists: " + downloadedFiles.size());

		} catch (IOException e) {
			e.printStackTrace();
		}

		deleteUniqueTestDirectories();
	}
}