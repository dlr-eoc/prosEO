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
import de.dlr.proseo.storagemgr.UniqueStorageTestPaths;
import de.dlr.proseo.storagemgr.posix.PosixDAL;
import de.dlr.proseo.storagemgr.utils.PathConverter;

@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PosixDALTest {

	@Autowired
	private BaseStorageTestUtils storageTestUtils;
	
	@Autowired
	private StorageProvider storageProvider;

	@Test
	public void test(TestInfo testInfo) {

		TestUtils.printMethodName(this, testInfo);
		UniqueStorageTestPaths uniquePaths = new UniqueStorageTestPaths(this, testInfo);

		// create unique source paths
		List<String> pathes = new ArrayList<>();
		pathes.add(new PathConverter(uniquePaths.getUniqueTestFolder(), "file1.txt").getPath());
		pathes.add(new PathConverter(uniquePaths.getUniqueTestFolder(), "file2.txt").getPath());
		pathes.add(new PathConverter(uniquePaths.getUniqueTestFolder(), "dir/file3.txt").getPath());

		// create source files
		List<String> sourcePathes = new ArrayList<>();
		for (String path : pathes) {

			String sourcePath = storageTestUtils.createSourceFile(path);
			sourcePathes.add(sourcePath);
		}

		String sourcePath = uniquePaths.getUniqueSourcePath();
		String storagePath = uniquePaths.getUniqueStoragePath();
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

		uniquePaths.deleteUniqueTestDirectories();
	}
}