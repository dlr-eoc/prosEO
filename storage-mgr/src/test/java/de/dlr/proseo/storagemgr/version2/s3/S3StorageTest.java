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

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class S3StorageTest {

	@Autowired
	private TestUtils testUtils;

	@Autowired
	private BaseStorageTestUtils storageTestUtils;
	
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
		storageProvider.setDefaultStorage(storageType);

		String prefix = "s3-storage-test/";

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
			StorageFile targetDir = storageProvider.getStorageFile(storage, prefix);

			List<String> uploadedPathes = storage.upload(sourceDir, targetDir);
			
			TestUtils.printList("S3 Storage files after upload:", storage.getRelativeFiles());
			TestUtils.printList("Response uploaded pathes:", uploadedPathes);
			
			storage.delete(prefix);
			
			TestUtils.printList("S3 Storage files after delete:", storage.getRelativeFiles());
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

}
