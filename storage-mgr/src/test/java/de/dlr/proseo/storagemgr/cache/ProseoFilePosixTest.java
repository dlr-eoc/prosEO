package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import java.util.ArrayList;

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

	String testPath;
	String sourceTestPath;

	@PostConstruct
	private void init() {
		testPath = testUtils.getTestPath();
		sourceTestPath = testUtils.getSourceTestPath();
	}

	@Test
	public void testCopyTo() {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectory();

		String sourcePath1 = sourceTestPath + "/test1.txt";
		String sourcePath2 = sourceTestPath + "/test2.txt";

		String targetPath = testPath;

		TestUtils.createFile(sourcePath1, "");
		TestUtils.createFile(sourcePath2, "");
		
		ProseoFile targetFile = ProseoFile.fromPathInfo(targetPath, TestUtils.getInstance().getCfg());
		ProseoFile sourceFile;
		
		// add new - file not exists and not in cache, cache is empty 

		sourceFile = ProseoFile.fromPathInfo(sourcePath1, TestUtils.getInstance().getCfg());
		try {
			ArrayList<String> transfered = sourceFile.copyTo(targetFile, false);
			
			if (transfered != null && !transfered.isEmpty()) {

				TestUtils.printArrayList("File copied to cache storage successfully", transfered);
			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}

		assertTrue("Cache has not 1 element: " + fileCache.size(), fileCache.size() == 1);
		
		// add new - file not exists and not in cache, but cache has already 1 element 

		sourceFile = ProseoFile.fromPathInfo(sourcePath2, TestUtils.getInstance().getCfg());
		try {
			ArrayList<String> transfered = sourceFile.copyTo(targetFile, false);
			
			if (transfered != null && !transfered.isEmpty()) {

				TestUtils.printArrayList("File copied to cache storage successfully", transfered);
			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}

		assertTrue("Cache has not 2 elements: " + fileCache.size(), fileCache.size() == 2);
		
		
		// update - file exists and in cache, cache has 2 elements

		sourceFile = ProseoFile.fromPathInfo(sourcePath1, TestUtils.getInstance().getCfg());
		try {
			ArrayList<String> transfered = sourceFile.copyTo(targetFile, false);
			
			if (transfered != null && !transfered.isEmpty()) {

				TestUtils.printArrayList("File copied to cache storage successfully", transfered);
			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}

		assertTrue("Cache has not 2 elements: " + fileCache.size(), fileCache.size() == 2);
		
	

		fileCache.clear();
		TestUtils.deleteTestDirectory();

	}

}
