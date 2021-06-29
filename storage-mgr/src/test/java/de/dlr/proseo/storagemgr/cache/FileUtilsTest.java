package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Paths;

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

	/**
	 * 
	 */
	@Test
	public void testCreateSizeContent() {

		TestUtils.printMethodName(this, testName);

		String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
		String testFile = currentPath + "/DATA/test.txt";
		String testContent = "Content";
		
		FileUtils.createFile(testFile, testContent);

		File file = new File(testFile);

		assertTrue("Test file was not created: " + testFile, file.exists());

		assertTrue("Size is wrong: " + FileUtils.getFileSize(testFile),
				FileUtils.getFileSize(testFile) == testContent.length());

		assertTrue("Content is wrong: " + FileUtils.getFileContent(testFile),
				FileUtils.getFileContent(testFile).equals(testContent));
	}

}
