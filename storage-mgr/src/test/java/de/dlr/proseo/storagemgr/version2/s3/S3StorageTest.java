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
		
		// StorageProvider storageProvider = new StorageProvider();

		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		String prefix = "files/";

		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");
		pathes.add(prefix + "dir/dir2/file4.txt");
		pathes.add(prefix + "dir/dir2/file5.txt");

		for (String path : pathes) {
			storageTestUtils.createSourceFile(path);
		}

		try {

			storageTestUtils.printSource();

			Storage storage = storageProvider.getStorage();

			StorageFile sourceDir = storageProvider.getSourceFile(prefix);
			
			// change prefix to source absolute path to have right uploaded path - sourceDir.fullPath instead prefix
			StorageFile targetDir = storageProvider.getStorageFile(prefix);

			List<String> uploadedPathes = storage.upload(sourceDir, targetDir);
			
			TestUtils.printList("S3 Storage files after upload:", storage.getFiles());
			TestUtils.printList("Response uploaded pathes:", uploadedPathes);
			
			List<String> deletedPathes = new ArrayList<>();
			for (String path : pathes) {
				StorageFile storageFile = storage.getStorageFile(path);
				String deletedFile = storage.deleteFile(storageFile);
				deletedPathes.add(deletedFile);
			}
			
			// storage.deleteBucket(storage.getBucket());
			
			TestUtils.printList("Deleted storage pathes:", deletedPathes);
			TestUtils.printList("S3 Storage files after delete:", storage.getFiles());
			

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);

	}

}
