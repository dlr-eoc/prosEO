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
		String path2 = testPath + "/test1/test2/test1.txt";
		String path3 = testPath + "/test1/test2/test2.txt";
		String pathNotExists = testPath + "/xxx/xxx/zzz.txt";
		
		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		TestUtils.createFile(path3, "");
		
		TestUtils.createFile(testPath + "/xx.x", "");
		TestUtils.createFile(testPath + "/123.x", "");

		FileCache pathCache = new FileCache(testPath);

		System.out.println("Before adding the element: " + path1);

		pathCache.put(path1);
		pathCache.put(path2);
		pathCache.put(path3);

		MapCacheTest.printCache("Cache after adding 3 elements:", pathCache.getMapCache());
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
}
