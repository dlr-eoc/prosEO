package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import java.io.File;
import java.time.Instant;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * @author Denys Chaykovskiy
 *
 */
public class FileCacheTest {

	String testPath = TestUtils.getTestPath();

	@Rule
	public TestName testName = new TestName();

	/**
	 * 
	 */
	@Test
	public void testDeleteEmptyDirectoriesToTop() {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectory();
		
		String emptyDirectories = testPath + "/d1/d2/d3";
		FileCache pathCache = new FileCache(testPath);
		
		TestUtils.createDirectory(emptyDirectories);
		
		File f = new File(emptyDirectories);
		
		TestUtils.printDirectoryTree(testPath);

		assertTrue("Empty Directories were not created: " + emptyDirectories, f.exists());

		pathCache.deleteEmptyDirectoriesToTop(emptyDirectories);

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

		FileCache pathCache = new FileCache(testPath);

		Instant testInstant;

		testInstant = pathCache.getFileAccessed(path);

		System.out.println("Generated accessed: " + testInstant.toString());

		File f = new File(pathCache.getAccessedPath(path));

		assertTrue("Last Accessed File not exists: " + f.getPath(), f.exists() && !f.isDirectory());
		
		TestUtils.deleteTestDirectory();
	}

	/**
	 * 
	 */
	@Test
	public void testGetAccessedPath() {

		TestUtils.printMethodName(this, testName);

		String dir = "/path";
		String fileName = "file.txt";
		String path = dir + "/" + fileName;

		FileCache pathCache = new FileCache(testPath);

		String accessedPath = pathCache.getAccessedPath(path);
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

		String path1 = testPath + "/test/test1.txt";
		String path2 = testPath + "/test1/test2/test2.txt";
		String path3 = testPath + "/test1/test2/test3.txt";
		String pathNotExists = testPath + "/xxx/xxx/zzz.txt";
		
		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		TestUtils.createFile(path3, "");
		
		TestUtils.createFile(testPath + "/test4.x", "");
		TestUtils.createFile(testPath + "/test5.x", "");

		FileCache pathCache = new FileCache(testPath);
		
		assertTrue("Cache does not contain 5 elements after dir init: " + pathCache.size(), 
				pathCache.size() == 5);

		System.out.println("Before adding the element: " + path1);

		MapCacheTest.printCache("Cache after init, 5 elements:", pathCache.getMapCache());
		TestUtils.printDirectoryTree(testPath);

		assertTrue("Cache Exists failed: " + path2, pathCache.containsKey(path2));

		assertTrue("Cache get failed: " + path3, pathCache.get(path3) != null);

		assertTrue("Cache contains key not found failed: " + pathNotExists, !pathCache.containsKey(pathNotExists));

		assertTrue("Cache get not found failed: " + pathNotExists, pathCache.get(pathNotExists) == null);

		pathCache.remove(path2);

		MapCacheTest.printCache("Cache after deleting 1 element: " + path2, pathCache.getMapCache());
		TestUtils.printDirectoryTree(testPath);

		pathCache.remove(path3);

		MapCacheTest.printCache("Cache after deleting 1 element: " + path3, pathCache.getMapCache());
		TestUtils.printDirectoryTree(testPath);
		
		TestUtils.deleteTestDirectory();
	}
	
	/**
	 * 
	 */
	@Test
	public void testInterface() {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectory();

		String path1 = testPath + "/test1.txt";
		String path2 = testPath + "/test2.txt";
		String path3 = testPath + "/test3.txt";
		String pathNotExists = testPath + "/xxx/xxx/zzz.txt";
		Instant timeNotChanged;
		Instant timeNotChanged2; 
		Instant timeChanged; 
		
		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		
		FileCache pathCache = new FileCache(testPath);
		
		assertTrue("Cache does not contain 2 elements after dir init: " + pathCache.size(), 
				pathCache.size() == 2);
		
		// check containsKey - contains and update accessed 
		
		System.out.println("Subtest: check containsKey - contains and update accessed ");
		
		timeChanged = pathCache.getFileAccessed(path1);
		timeNotChanged = pathCache.getFileAccessed(path2); 
		
		assertTrue("Cache does not contain an elements after dir init: " + path1, 
				pathCache.containsKey(path1));
		
		System.out.println("Path1 Time:                         " + timeChanged);
		System.out.println("Path1 time after contains(changed): " + pathCache.getFileAccessed(path1));
		
		System.out.println();
		
		System.out.println("Path2 Time:          " + timeNotChanged); 
		System.out.println("Path2 time (stable): " + pathCache.getFileAccessed(path2)); 
		
		assertTrue("Last accessed was not updated: ", 
				pathCache.getFileAccessed(path1).compareTo(timeChanged) > 0);
		
		assertTrue("Last accessed must not be updated: ", 
				pathCache.getFileAccessed(path2).compareTo(timeNotChanged) == 0);
		
		assertTrue("Cache does not contain 2 elements after contains: " + pathCache.size(), 
				pathCache.size() == 2);
		
		// check containsKey - not contains 
		
		System.out.println("Subtest: check containsKey - not contains");
		
		timeNotChanged = pathCache.getFileAccessed(path1);
		timeNotChanged2 = pathCache.getFileAccessed(path2); 
		
		assertTrue("Cache contains an element, but must not: " + pathNotExists, 
				!pathCache.containsKey(pathNotExists));
		
		System.out.println("Path1 Time:                        " + timeNotChanged);
		System.out.println("Path1 time after contains(stable): " + pathCache.getFileAccessed(path1));
		
		System.out.println();
		
		System.out.println("Path2 Time:                        " + timeNotChanged2); 
		System.out.println("Path2 time after contains(stable): " + pathCache.getFileAccessed(path2)); 
		
		assertTrue("path1 Last accessed must not be updated: ", 
				pathCache.getFileAccessed(path1).compareTo(timeNotChanged) == 0);
		
		assertTrue("path2 Last accessed must not be updated: ", 
				pathCache.getFileAccessed(path2).compareTo(timeNotChanged2) == 0);
		
		// check put - not contains 
		
		System.out.println("Subtest: check containsKey - not contains");
		
		assertTrue("Cache does not contain 2 elements after contains: " + pathCache.size(), 
				pathCache.size() == 2);
		
		assertTrue("Cache contains an element before put: " + path3, 
				!pathCache.containsKey(path3));
		
		TestUtils.createFile(path3, "");
		pathCache.put(path3);
		
		assertTrue("Cache does not contain 3 elements after contains: " + pathCache.size(), 
				pathCache.size() == 3);
		
		assertTrue("Cache does not contains an element after put: " + path3, 
				pathCache.containsKey(path3));
		
		// check put - contains, update
		
		System.out.println("Subtest: check put - contains, update ");
		
		timeChanged = pathCache.getFileAccessed(path1);
		timeNotChanged = pathCache.getFileAccessed(path2); 
		
		pathCache.put(path1);
		
		System.out.println("Path1 Time:                         " + timeChanged);
		System.out.println("Path1 time after contains(changed): " + pathCache.getFileAccessed(path1));
		
		System.out.println();
		
		System.out.println("Path2 Time:          " + timeNotChanged); 
		System.out.println("Path2 time (stable): " + pathCache.getFileAccessed(path2)); 
		
		assertTrue("Last accessed was not updated: ", 
				pathCache.getFileAccessed(path1).compareTo(timeChanged) > 0);
		
		assertTrue("Last accessed must not be updated: ", 
				pathCache.getFileAccessed(path2).compareTo(timeNotChanged) == 0);
		
		assertTrue("Cache does not contain 3 elements after contains: " + pathCache.size(), 
				pathCache.size() == 3);
	
		
		TestUtils.deleteTestDirectory();
	}
	
		
	
	
	
}
