package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
@ContextConfiguration
public class FileCacheLRUTest {
	
	String testPath = TestUtils.getTestPath();

	@Rule
	public TestName testName = new TestName();

	@Autowired
	private StorageManagerConfiguration cfg;
	
	@Autowired
	private FileCache pathCache;
	

	/**
	 * 
	 */
	@Test
	public void testLRU() {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectory();
		
		if (cfg == null) {
			fail("Config is null");
		}

		String path1 = testPath + "/test1.txt";
		String path2 = testPath + "/test2.txt";
		String path3 = testPath + "/test3.txt";
		
		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		TestUtils.createFile(path3, "");
		
		assertTrue("Cache has not 3 elements: " + pathCache.size(), pathCache.size() == 3);

		// TO-DO: Test LRU, cfg is null 
		
		TestUtils.deleteTestDirectory();
	}


}
