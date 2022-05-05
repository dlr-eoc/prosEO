package de.dlr.proseo.storagemgr.version2.s3;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class S3StorageTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TestUtils testUtils;

	@Autowired
	private StorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	String testCachePath;
	String cachePath;
	String testSourcePath;

	@PostConstruct
	private void init() {
		testCachePath = testUtils.getTestCachePath();
		cachePath = testUtils.getCachePath();

		testSourcePath = testUtils.getTestSourcePath();
	}

	@Test
	public void testUploadFile() throws IOException {

		TestUtils.createEmptyStorageDirectories();

		String prefix = "files/";

		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");

		for (String path : pathes) {

			storageTestUtils.createSourceFile(path);
		}

		try {

			storageTestUtils.printPosixStorage();

			Storage storage = storageProvider.setStorage(StorageType.S3);

			StorageFile sourceDir = storageProvider.getSourceFile(prefix);
			StorageFile targetDir = storageProvider.getStorageFile(prefix);

			List<String> uploadedPathes = storage.upload(sourceDir, targetDir);

			List<StorageFile> storageFiles = storage.getStorageFiles();

			System.out.println("S3 Storage files: " + storageFiles.size());
			for (StorageFile storageFile : storageFiles) {

				System.out.println(" - " + storageFile.getFullPath());

			}

			for (String uploadedPath : uploadedPathes) {

				System.out.println("Uploaded: " + uploadedPath);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

}
