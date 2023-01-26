package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.utils.ProseoFile;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class ProseoFilePosixTest {

	@Autowired
	private TestUtils testUtils;

	@Rule
	public TestName testName = new TestName();

	@Autowired
	private FileCache fileCache;

	String testCachePath;
	String sourceTestPath;

	@PostConstruct
	private void init() {
		testCachePath = testUtils.getTestCachePath();
		sourceTestPath = testUtils.getTestStoragePath();
	}

	@Test
	public void testCopyTo() {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectories();
		fileCache.setPath(testCachePath);

		String sourcePath1 = sourceTestPath + "/" + "test1.txt";
		String sourcePath2 = sourceTestPath + "/" + "test2.txt";

		String targetPath1 = testCachePath + "/" + "test1.txt";
		String targetPath2 = testCachePath + "/" + "test2.txt";

		TestUtils.createFile(sourcePath1, "");
		TestUtils.createFile(sourcePath2, "");

		ProseoFile targetFile1 = ProseoFile.fromPathInfo(targetPath1, TestUtils.getInstance().getCfg());
		ProseoFile targetFile2 = ProseoFile.fromPathInfo(targetPath2, TestUtils.getInstance().getCfg());

		ProseoFile sourceFile;

		// add new - file not exists and not in cache, cache is empty

		sourceFile = ProseoFile.fromPathInfo(sourcePath1, TestUtils.getInstance().getCfg());
		try {
			List<String> transfered = sourceFile.copyTo(targetFile1, false);

			if (transfered != null && !transfered.isEmpty()) {

				TestUtils.printList("File copied to cache storage successfully", transfered);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		assertTrue("Cache has not 1 element: " + fileCache.size(), fileCache.size() == 1);

		// add new - file not exists and not in cache, but cache has already 1 element

		sourceFile = ProseoFile.fromPathInfo(sourcePath2, TestUtils.getInstance().getCfg());
		try {
			List<String> transfered = sourceFile.copyTo(targetFile2, false);

			if (transfered != null && !transfered.isEmpty()) {

				TestUtils.printList("File copied to cache storage successfully", transfered);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		assertTrue("Cache has not 2 elements: " + fileCache.size(), fileCache.size() == 2);

		// update - file exists and in cache, cache has 2 elements

		sourceFile = ProseoFile.fromPathInfo(sourcePath1, TestUtils.getInstance().getCfg());
		try {
			List<String> transfered = sourceFile.copyTo(targetFile1, false);

			if (transfered != null && !transfered.isEmpty()) {

				TestUtils.printList("File copied to cache storage successfully", transfered);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		assertTrue("Cache has not 2 elements: " + fileCache.size(), fileCache.size() == 2);

		fileCache.clear();
		TestUtils.deleteTestDirectories();
	}
}
