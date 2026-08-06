/**
 * FileCacheTest.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;

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
 * Test class for FileCache
 * @see FileCacheLRUTest.java
 * 
 * @author Denys Chaykovskiy
 *
 */

@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class FileCacheTest {

	@Autowired
	private TestUtils testUtils;

	@Autowired
	private FileCache fileCache;

	private String testCachePath;
	private String cachePath;

	@PostConstruct
	private void init() {
		testCachePath = testUtils.getTestCachePath();
		cachePath = testUtils.getCachePath();
	}

	@Test
	public void testDeleteEmptyDirectoriesToTop(TestInfo testInfo) {

		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyTestDirectories();

		String emptyDirectories = testCachePath + "/d1/d2/d3";
		File f = new File(emptyDirectories);

		// create test directories

		testUtils.createDirectory(emptyDirectories);

		TestUtils.printDirectoryTree("Directories after creation (expectected: /d1/d2/d3 ):", testCachePath);

		assertTrue(f.exists(), "Empty Directories were not created: " + emptyDirectories);

		// delete test directories

		fileCache.deleteEmptyDirectoriesToTop(emptyDirectories);

		TestUtils.printDirectoryTree("Directories after deletion (expectected: nothing)", cachePath);

		assertFalse(f.exists(), "Empty Directories were not deleted: " + emptyDirectories);

		// clear

		testUtils.deleteTestDirectories();
	}

	/**
	 * 
	 */
	@Test
	public void testGetLastAccessed(TestInfo testInfo) {

		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyTestDirectories();
		fileCache.setPath(testCachePath);

		String testFile = "testLastAccessed.txt";
		String path = testCachePath + "/" + testFile;
		FileUtils fileUtils = new FileUtils(path);

		fileUtils.createFile("");

		Instant testInstant;

		testInstant = fileCache.getFileAccessed(path);

		System.out.println("Generated accessed: " + testInstant.toString());

		File f = new File(fileCache.getAccessedPath(path));

		assertTrue(f.exists() && !f.isDirectory(), "Last Accessed File not exists: " + f.getPath());

		fileCache.clear();
		testUtils.deleteTestDirectories();
	}

	/**
	 * 
	 */
	@Test
	public void testGetAccessedPath(TestInfo testInfo) {

		TestUtils.printMethodName(this, testInfo);

		String dir = "path";
		String fileName = "file.txt";
		String path = dir + "/" + fileName;

		String accessedPath = fileCache.getAccessedPath(path);
		String expectedAccessedPath = dir + "/" + FileCache.getAccessedPrefix() + fileName;

		System.out.println("Path:                   " + path);
		System.out.println("Accessed Path:          " + accessedPath);
		System.out.println("Expected Accessed Path: " + expectedAccessedPath);

		assertEquals(expectedAccessedPath, accessedPath, "Accessed Path is wrong: " + accessedPath);
	}

	/**
	 * 
	 */
	@Test
	public void testGetPutContainsRemove(TestInfo testInfo) {

		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyTestDirectories();
		fileCache.setPath(testCachePath);

		String path1 = Paths.get(testCachePath + "/test/test1.txt").toString();
		String path2 = Paths.get(testCachePath + "/test1/test2/test2.txt").toString();
		String path3 = Paths.get(testCachePath + "/test1/test2/test3.txt").toString();
		String path4 = Paths.get(testCachePath + "/test4.x").toString();
		String path5 = Paths.get(testCachePath + "/test5.x").toString();

		String pathNotExists = Paths.get(testCachePath + "/xxx/xxx/zzz.txt").toString();

		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		TestUtils.createFile(path3, "");

		TestUtils.createFile(path4, "");
		TestUtils.createFile(path5, "");

		fileCache.putFilesToCache(testCachePath);

		assertEquals(5, fileCache.size(), "Cache does not contain 5 elements after dir init: " + fileCache.size());

		System.out.println("Before adding the element: " + path1);

		MapCacheTest.printCache("Cache after init, 5 elements:", fileCache.getMapCache());
		TestUtils.printDirectoryTree(testCachePath);

		assertTrue(fileCache.containsKey(path2), "Cache Exists failed: " + path2);

		assertNotNull(fileCache.get(path3), "Cache get failed: " + path3);

		assertFalse(fileCache.containsKey(pathNotExists), "Cache contains key not found failed: " + pathNotExists);

		assertNull(fileCache.get(pathNotExists), "Cache get not found failed: " + pathNotExists);

		fileCache.remove(path2);

		MapCacheTest.printCache("Cache after deleting 1 element: " + path2, fileCache.getMapCache());
		TestUtils.printDirectoryTree(testCachePath);

		fileCache.remove(path3);

		MapCacheTest.printCache("Cache after deleting 1 element: " + path3, fileCache.getMapCache());
		TestUtils.printDirectoryTree(testCachePath);

		fileCache.clear();
		testUtils.deleteTestDirectories();
	}

	/**
	 * 
	 */
	@Test
	public void testStatus(TestInfo testInfo) {

		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyTestDirectories();
		fileCache.setPath(testCachePath);

		String path1 = Paths.get(testCachePath + "/" + "testStatus1.txt").toString();
		String path2 = Paths.get(testCachePath + "/" + "testStatus2.txt").toString();
		String path3 = Paths.get(testCachePath + "/" + "testStatus3.txt").toString();


		CacheFileStatus status1;
		CacheFileStatus status2;
		CacheFileStatus status3;


		// create 2 files for cache

		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");
		
		// put 2 files to cache 

		fileCache.putFilesToCache(testCachePath);

		assertEquals(2, fileCache.size(), "Expected Cache size is 2 elements after put(). Exists: " + fileCache.size());

		// check status after put()

		status1 = fileCache.getCacheFileStatus(path1);
		status2 = fileCache.getCacheFileStatus(path2);

		assertEquals(CacheFileStatus.READY, status1,
				"Expected cache file1 status after put() is Ready. Exists: " + status1.toString());

		assertEquals(CacheFileStatus.READY, status2, 
				"Expected cache file2 status after put() is Ready. Exists: " + status2.toString());
		
		// check not exists status - file is not in cache, but has a status
		
		fileCache.setCacheFileStatus(path3, CacheFileStatus.INCOMPLETE);
		
		status3 = fileCache.getCacheFileStatus(path3);
		
		assertFalse(fileCache.containsKey(path3),
				"Expected cache file3 does not exist in the cache. Exists:" + fileCache.containsKey(path3));
		
		assertEquals(CacheFileStatus.INCOMPLETE, status3,
				"Expected cache file3 status after setStatus(INCOMPLETE) is INCOMPLETE. Exists:" + status3.toString());
		
		// changing status1 to INCOMPLETE 

		fileCache.setCacheFileStatus(path1, CacheFileStatus.INCOMPLETE);

		status1 = fileCache.getCacheFileStatus(path1);
		status2 = fileCache.getCacheFileStatus(path2);

		assertEquals(CacheFileStatus.INCOMPLETE, status1,
				"Expected cache file1 status after setStatus(INCOMPLETE) is INCOMPLETE. Exists:" + status1.toString());

		assertEquals(CacheFileStatus.READY, status2,
				"Expected cache file2 status after <no changes> is Ready. Exists:" + status2.toString());

		// changing status2 to INCOMPLETE

		fileCache.setCacheFileStatus(path2, CacheFileStatus.INCOMPLETE);

		status1 = fileCache.getCacheFileStatus(path1);
		status2 = fileCache.getCacheFileStatus(path2);

		assertEquals(CacheFileStatus.INCOMPLETE, status1,
				"Expected cache file1 status after <no changes> is INCOMPLETE. Exists: " + status1.toString());

		assertEquals(CacheFileStatus.INCOMPLETE, status2,
				"Expected cache file2 status after setStatus(INCOMPLETE) is INCOMPLETE. Exists: " + status2.toString());

		// changing status1 to Ready

		fileCache.put(path1); // status is set as ready

		status1 = fileCache.getCacheFileStatus(path1);
		status2 = fileCache.getCacheFileStatus(path2);

		assertEquals(CacheFileStatus.READY, status1,
				"Expected cache file1 status after setStatus(Ready) is Ready. Exists: " + status1.toString());

		assertEquals(CacheFileStatus.INCOMPLETE, status2,
				"Expected cache file2 status after <no changes> is Downloading. Exists: " + status2.toString());
	
		// check cache after status changes 

		assertEquals(2, fileCache.size(), "Expected 2 elements in the cache. Exists: " + fileCache.size());

		// clear
		
		fileCache.clear();
		testUtils.deleteTestDirectories();
	}

	/**
	 * 
	 */
	@Test
	public void testInterface(TestInfo testInfo) {

		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyTestDirectories();
		fileCache.setPath(testCachePath);

		String path1 = Paths.get(testCachePath + "/" + "test1.txt").toString();
		String path2 = Paths.get(testCachePath + "/" + "test2.txt").toString();
		String path3 = Paths.get(testCachePath + "/" + "test3.txt").toString();
		String pathNotExists = Paths.get(testCachePath + "/xxx/xxx/" + " zzz.txt").toString();
		Instant timeNotChanged;
		Instant timeNotChanged2;
		Instant timeChanged;

		TestUtils.createFile(path1, "");
		TestUtils.createFile(path2, "");

		fileCache.putFilesToCache(testCachePath);

		assertEquals(2, fileCache.size(), "Cache does not contain 2 elements after dir init: " + fileCache.size());

		// check containsKey - contains and update accessed

		System.out.println("Subtest: check containsKey - contains and update accessed ");

		timeChanged = fileCache.getFileAccessed(path1);
		timeNotChanged = fileCache.getFileAccessed(path2);

		assertTrue(fileCache.containsKey(path1), "Cache does not contain an elements after dir init: " + path1);

		System.out.println("Path1 Time:                         " + timeChanged);
		System.out.println("Path1 time after contains(changed): " + fileCache.getFileAccessed(path1));

		System.out.println();

		System.out.println("Path2 Time:          " + timeNotChanged);
		System.out.println("Path2 time (stable): " + fileCache.getFileAccessed(path2));

		assertTrue(fileCache.getFileAccessed(path1).compareTo(timeChanged) > 0, "Last accessed was not updated: ");

		assertEquals(timeNotChanged, fileCache.getFileAccessed(path2), "Last accessed must not be updated: ");

		assertEquals(2, fileCache.size(), "Cache does not contain 2 elements after contains: " + fileCache.size());

		// check containsKey - not contains

		System.out.println("Subtest: check containsKey - not contains");

		timeNotChanged = fileCache.getFileAccessed(path1);
		timeNotChanged2 = fileCache.getFileAccessed(path2);

		assertFalse(fileCache.containsKey(pathNotExists), "Cache contains an element, but must not: " + pathNotExists);

		System.out.println("Path1 Time:                        " + timeNotChanged);
		System.out.println("Path1 time after contains(stable): " + fileCache.getFileAccessed(path1));

		System.out.println();

		System.out.println("Path2 Time:                        " + timeNotChanged2);
		System.out.println("Path2 time after contains(stable): " + fileCache.getFileAccessed(path2));

		assertEquals(timeNotChanged, fileCache.getFileAccessed(path1),
				"path1 Last accessed must not be updated: ");

		assertEquals(timeNotChanged2, fileCache.getFileAccessed(path2),
				"path2 Last accessed must not be updated: ");

		// check put - not contains

		System.out.println("Subtest: check containsKey - not contains");

		assertEquals(2, fileCache.size(), "Cache does not contain 2 elements after contains: " + fileCache.size());

		assertFalse(fileCache.containsKey(path3), "Cache contains an element before put: " + path3);

		TestUtils.createFile(path3, "");
		fileCache.put(path3);

		assertEquals(3, fileCache.size(), "Cache does not contain 3 elements after contains: " + fileCache.size());

		assertTrue(fileCache.containsKey(path3), "Cache does not contains an element after put: " + path3);

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

		assertTrue(fileCache.getFileAccessed(path1).compareTo(timeChanged) > 0, "Last accessed was not updated: ");

		assertEquals(timeNotChanged, fileCache.getFileAccessed(path2), "Last accessed must not be updated: ");

		assertEquals(3, fileCache.size(), "Cache does not contain 3 elements after contains: " + fileCache.size());

		fileCache.clear();
		testUtils.deleteTestDirectories();
	}
}
