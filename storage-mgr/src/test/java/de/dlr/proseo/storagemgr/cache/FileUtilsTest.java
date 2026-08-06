/**
 * FileUtilsTest.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import jakarta.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.utils.FileUtils;

/**
 * Test class for FileUtils
 * 
 * @author Denys Chaykovskiy
 *
 */
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class FileUtilsTest {

	@Autowired
	private TestUtils testUtils;

	String testPath; 
	
	@PostConstruct
	private void init() {
		testPath = testUtils.getTestCachePath();
	}

	/**
	 * 
	 */
	@Test
	public void testCreateSizeContent(TestInfo testInfo) {

		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyTestDirectories();

		String testFile = testPath + "/test.txt";
		String testContent = "Content";
		FileUtils fileUtils = new FileUtils(testFile);
		
		fileUtils.createFile(testContent);

		File file = new File(testFile);

		assertTrue(file.exists(), "Test file was not created: " + testFile);

		assertEquals(testContent.length(), fileUtils.getFileSize(), "Size is wrong: " + fileUtils.getFileSize());

		assertEquals(testContent, fileUtils.getFileContent(), "Content is wrong: " + fileUtils.getFileContent());
		
		testUtils.deleteTestDirectories();
	}

}
