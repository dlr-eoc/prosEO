package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

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

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class FileCacheLRUTest {
	
	@Autowired
	private TestUtils testUtils;

	@Rule
	public TestName testName = new TestName();

	@Autowired
	private FileCache fileCache;
	
	String testCachePath; 
	
	@PostConstruct
	private void init() {
		testCachePath = testUtils.getTestCachePath();
	}
	
	/**
	 * 
	 */
	@Test
	public void testLRU() {
			 
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectories();
		fileCache.setPath(testCachePath);

		String path1 = testCachePath + "/test1.txt";
		String path2 = testCachePath + "/test2.txt";
		String path3 = testCachePath + "/test3.txt";
		
		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		TestUtils.createFile(path3, "");
		
		fileCache.putFilesToCache(testCachePath);
		
		assertTrue("Cache has not 3 elements: " + fileCache.size(), fileCache.size() == 3);

		// TO-DO: Test LRU, cfg is null 
		
		fileCache.clear();
		TestUtils.deleteTestDirectories();
	}


}
