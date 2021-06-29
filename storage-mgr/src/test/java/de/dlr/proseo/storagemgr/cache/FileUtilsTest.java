package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * @author Denys Chaykovskiy
 *
 */
public class FileUtilsTest {

	@Rule
	public TestName testName = new TestName();
	
	private String testPath = TestUtils.getTestPath();

	/**
	 * 
	 */
	@Test
	public void testCreateSizeContent() {

		TestUtils.printMethodName(this, testName);

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
	}

}
