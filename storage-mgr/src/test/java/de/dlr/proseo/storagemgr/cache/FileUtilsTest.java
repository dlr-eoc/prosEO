package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import java.io.File;

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

/**
 * @author Denys Chaykovskiy
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class FileUtilsTest {

	@Rule
	public TestName testName = new TestName();
	
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
	public void testCreateSizeContent() {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectories();

		String testFile = testPath + "/test.txt";
		String testContent = "Content";
		FileUtils fileUtils = new FileUtils(testFile);
		
		fileUtils.createFile(testContent);

		File file = new File(testFile);

		assertTrue("Test file was not created: " + testFile, file.exists());

		assertTrue("Size is wrong: " + fileUtils.getFileSize(),
				fileUtils.getFileSize() == testContent.length());

		assertTrue("Content is wrong: " + fileUtils.getFileContent(),
				fileUtils.getFileContent().equals(testContent));
		
		TestUtils.deleteTestDirectories();
	}

}
