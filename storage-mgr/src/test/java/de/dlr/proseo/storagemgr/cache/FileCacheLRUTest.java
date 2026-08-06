/**
 * FileCacheLRUTest.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.cache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.TestUtils;

/**
 * Test class for LRU cache handling by FileCache
 * 
 * @author Denys Chaykovskiy
 */
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class FileCacheLRUTest {
	
	@Autowired
	private TestUtils testUtils;

	@Autowired
	private FileCache fileCache;
	
	/**
	 * 
	 */
	@Test
	public void testLRU(TestInfo testInfo) {
			 
		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyTestDirectories();
		String testCachePath = testUtils.getTestCachePath();
		fileCache.setPath(testCachePath);

		String path1 = testCachePath + "/test1.txt";
		String path2 = testCachePath + "/test2.txt";
		String path3 = testCachePath + "/test3.txt";
		
		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		TestUtils.createFile(path3, "");
		
		fileCache.putFilesToCache(testCachePath);
		
		assertEquals(3, fileCache.size(), "Cache has not 3 elements: " + fileCache.size());

		// TO-DO: Test LRU, cfg is null 
		
		fileCache.clear();
		testUtils.deleteTestDirectories();
	}


}
