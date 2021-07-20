package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;

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

/**
 * @author Denys Chaykovskiy
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class FileCacheTest {

	@Autowired
	private TestUtils testUtils;

	@Rule
	public TestName testName = new TestName();

	@Autowired
	private FileCache fileCache;
	
	String testPath; 
	
	@PostConstruct
	private void init() {
		testPath = testUtils.getTestPath();
	}
	
	/**
	 * 
	 */
	@Test
	public void testDeleteEmptyDirectoriesToTop() {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectory();
		
		String emptyDirectories = testPath + "/d1/d2/d3";
		
		TestUtils.createDirectory(emptyDirectories);
		
		File f = new File(emptyDirectories);
		
		TestUtils.printDirectoryTree(testPath);

		assertTrue("Empty Directories were not created: " + emptyDirectories, f.exists());

		fileCache.deleteEmptyDirectoriesToTop(emptyDirectories);

		TestUtils.printDirectoryTree(testPath);

		assertTrue("Empty Directories were not deleted: " + emptyDirectories, !f.exists());
		
		TestUtils.deleteTestDirectory();
	}

	/**
	 * 
	 */
	@Test
	public void testGetLastAccessed() {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectory();
		
		String testFile = "testLastAccessed.txt";
		String path = testPath + "/" + testFile;
		FileUtils fileUtils = new FileUtils(path);

		fileUtils.createFile("");
	
		Instant testInstant;

		testInstant = fileCache.getFileAccessed(path);

		System.out.println("Generated accessed: " + testInstant.toString());

		File f = new File(fileCache.getAccessedPath(path));

		assertTrue("Last Accessed File not exists: " + f.getPath(), f.exists() && !f.isDirectory());
		
		TestUtils.deleteTestDirectory();
	}

	/**
	 * 
	 */
	@Test
	public void testGetAccessedPath() {

		TestUtils.printMethodName(this, testName);

		String dir = "path";
		String fileName = "file.txt";
		String path = dir + "/" + fileName;

		String accessedPath = fileCache.getAccessedPath(path);
		String expectedAccessedPath = dir + "/" + FileCache.getPrefix() + fileName;

		System.out.println("Path:                   " + path);
		System.out.println("Accessed Path:          " + accessedPath);
		System.out.println("Expected Accessed Path: " + expectedAccessedPath);

		assertTrue("Accessed Path is wrong: " + accessedPath, accessedPath.equals(expectedAccessedPath));		
	}

	/**
	 * 
	 */
	@Test
	public void testGetPutContainsRemove() {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectory();
		
		fileCache.clear();

		String path1 = Paths.get(testPath + "/test/test1.txt").toString();
		String path2 = Paths.get(testPath + "/test1/test2/test2.txt").toString();
		String path3 = Paths.get(testPath + "/test1/test2/test3.txt").toString();
		String path4 = Paths.get(testPath + "/test4.x").toString();
		String path5 = Paths.get(testPath + "/test5.x").toString();
		
		String pathNotExists =  Paths.get(testPath + "/xxx/xxx/zzz.txt").toString();
		
		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		TestUtils.createFile(path3, "");
		
		TestUtils.createFile(path4, "");
		TestUtils.createFile(path5, "");
			
		fileCache.putFilesToCache(testPath);

		assertTrue("Cache does not contain 5 elements after dir init: " + fileCache.size(), 
				fileCache.size() == 5);

		System.out.println("Before adding the element: " + path1);

		MapCacheTest.printCache("Cache after init, 5 elements:", fileCache.getMapCache());
		TestUtils.printDirectoryTree(testPath);

		assertTrue("Cache Exists failed: " + path2, fileCache.containsKey(path2));

		assertTrue("Cache get failed: " + path3, fileCache.get(path3) != null);

		assertTrue("Cache contains key not found failed: " + pathNotExists, !fileCache.containsKey(pathNotExists));

		assertTrue("Cache get not found failed: " + pathNotExists, fileCache.get(pathNotExists) == null);

		fileCache.remove(path2);

		MapCacheTest.printCache("Cache after deleting 1 element: " + path2, fileCache.getMapCache());
		TestUtils.printDirectoryTree(testPath);

		fileCache.remove(path3);

		MapCacheTest.printCache("Cache after deleting 1 element: " + path3, fileCache.getMapCache());
		TestUtils.printDirectoryTree(testPath);
		
		fileCache.clear();
		TestUtils.deleteTestDirectory();
	}
	
	/**
	 * 
	 */
	@Test
	public void testInterface() {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectory();
		
		String path1 = Paths.get(testPath + "/" + "test1.txt").toString();
		String path2 = Paths.get(testPath + "/" + "test2.txt").toString();
		String path3 = Paths.get(testPath + "/" + "test3.txt").toString();
		String pathNotExists = Paths.get(testPath + "/xxx/xxx/" + " zzz.txt").toString();
		Instant timeNotChanged;
		Instant timeNotChanged2; 
		Instant timeChanged; 
		
		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		
		fileCache.putFilesToCache(testPath);
	
		assertTrue("Cache does not contain 2 elements after dir init: " + fileCache.size(), 
				fileCache.size() == 2);
		
		// check containsKey - contains and update accessed 
		
		System.out.println("Subtest: check containsKey - contains and update accessed ");
		
		timeChanged = fileCache.getFileAccessed(path1);
		timeNotChanged = fileCache.getFileAccessed(path2); 
		
		assertTrue("Cache does not contain an elements after dir init: " + path1, 
				fileCache.containsKey(path1));
		
		System.out.println("Path1 Time:                         " + timeChanged);
		System.out.println("Path1 time after contains(changed): " + fileCache.getFileAccessed(path1));
		
		System.out.println();
		
		System.out.println("Path2 Time:          " + timeNotChanged); 
		System.out.println("Path2 time (stable): " + fileCache.getFileAccessed(path2)); 
		
		assertTrue("Last accessed was not updated: ", 
				fileCache.getFileAccessed(path1).compareTo(timeChanged) > 0);
		
		assertTrue("Last accessed must not be updated: ", 
				fileCache.getFileAccessed(path2).compareTo(timeNotChanged) == 0);
		
		assertTrue("Cache does not contain 2 elements after contains: " + fileCache.size(), 
				fileCache.size() == 2);
		
		// check containsKey - not contains 
		
		System.out.println("Subtest: check containsKey - not contains");
		
		timeNotChanged = fileCache.getFileAccessed(path1);
		timeNotChanged2 = fileCache.getFileAccessed(path2); 
		
		assertTrue("Cache contains an element, but must not: " + pathNotExists, 
				!fileCache.containsKey(pathNotExists));
		
		System.out.println("Path1 Time:                        " + timeNotChanged);
		System.out.println("Path1 time after contains(stable): " + fileCache.getFileAccessed(path1));
		
		System.out.println();
		
		System.out.println("Path2 Time:                        " + timeNotChanged2); 
		System.out.println("Path2 time after contains(stable): " + fileCache.getFileAccessed(path2)); 
		
		assertTrue("path1 Last accessed must not be updated: ", 
				fileCache.getFileAccessed(path1).compareTo(timeNotChanged) == 0);
		
		assertTrue("path2 Last accessed must not be updated: ", 
				fileCache.getFileAccessed(path2).compareTo(timeNotChanged2) == 0);
		
		// check put - not contains 
		
		System.out.println("Subtest: check containsKey - not contains");
		
		assertTrue("Cache does not contain 2 elements after contains: " + fileCache.size(), 
				fileCache.size() == 2);
		
		assertTrue("Cache contains an element before put: " + path3, 
				!fileCache.containsKey(path3));
		
		TestUtils.createFile(path3, "");
		fileCache.put(path3);
		
		assertTrue("Cache does not contain 3 elements after contains: " + fileCache.size(), 
				fileCache.size() == 3);
		
		assertTrue("Cache does not contains an element after put: " + path3, 
				fileCache.containsKey(path3));
		
		// check put - contains, update
		
		System.out.println("Subtest: check put - contains, update ");
		
		timeChanged = fileCache.getFileAccessed(path1);
		timeNotChanged = fileCache.getFileAccessed(path2); 
		
		fileCache.put(path1);
		
		System.out.println("Path1 Time:                         " + timeChanged);
		System.out.println("Path1 time after contains(changed): " + fileCache.getFileAccessed(path1));
		
		System.out.println();
		
		System.out.println("Path2 Time:          " + timeNotChanged); 
		System.out.println("Path2 time (stable): " + fileCache.getFileAccessed(path2)); 
		
		assertTrue("Last accessed was not updated: ", 
				fileCache.getFileAccessed(path1).compareTo(timeChanged) > 0);
		
		assertTrue("Last accessed must not be updated: ", 
				fileCache.getFileAccessed(path2).compareTo(timeNotChanged) == 0);
		
		assertTrue("Cache does not contain 3 elements after contains: " + fileCache.size(), 
				fileCache.size() == 3);
	
		fileCache.clear();
		TestUtils.deleteTestDirectory();
	}
}
