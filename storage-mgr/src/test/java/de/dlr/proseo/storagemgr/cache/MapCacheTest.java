package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * @author Denys Chaykovskiy
 *
 */
public class MapCacheTest {

	@Rule
	public TestName testName = new TestName();

	/**
	 * 
	 */
	@Test
	public void testGetPutContains() {

		TestUtils.printMethodName(this, testName);

		String testFile = "file3";

		FileInfo testFileInfo = new FileInfo(Instant.parse("2020-05-05T11:50:55.00z"), 777l);
		FileInfo replaceFileInfo = new FileInfo(Instant.parse("2018-04-04T11:50:55.00z"), 123l);
		FileInfo fileInfo;

		MapCache mapCache = new MapCache();

		mapCache.put("file1", new FileInfo(Instant.parse("2018-05-05T11:50:55.00z"), 120l));
		mapCache.put("file2", new FileInfo(Instant.parse("2012-05-05T11:50:55.00z"), 150l));
		mapCache.put(testFile, testFileInfo);
		mapCache.put("file4", new FileInfo(Instant.parse("2014-05-05T11:50:55.00z"), 220l));
		mapCache.put("file5", new FileInfo(Instant.parse("2019-05-05T11:50:55.00z"), 1130l));

		printCache("Cache as is", mapCache.getCache());

		fileInfo = mapCache.get(testFile);

		assertTrue("Get is not working: " + testFile,
				fileInfo.getAccessed() == testFileInfo.getAccessed() && fileInfo.getSize() == testFileInfo.getSize());

		mapCache.put(testFile, replaceFileInfo);
		fileInfo = mapCache.get(testFile);

		assertTrue("Replace is not working: " + testFile, fileInfo.getAccessed() == replaceFileInfo.getAccessed()
				&& fileInfo.getSize() == replaceFileInfo.getSize());

		testFileInfo = mapCache.get(testFile);

		assertTrue("Contains is not working: " + testFile, mapCache.containsKey(testFile));
	}

	/**
	 * 
	 */
	@Test
	public void testSort() {

		TestUtils.printMethodName(this, testName);

		String minSizeFile = "file2";
		long minSize = 1L;

		String minTimeStampFile = "file4";
		String minTimeStamp = "1999-01-01T11:11:11.00z";

		String maxSizeFile = "file1";
		long maxSize = 100000L;

		String maxTimeStampFile = "file3";
		String maxTimeStamp = "2021-01-01T11:11:11.00z";

		FileInfo sortedFileInfo;
		FileInfo testFileInfo;

		MapCache mapCache = new MapCache();

		mapCache.put(maxSizeFile, new FileInfo(Instant.parse("2018-05-05T11:50:55.00z"), maxSize));
		mapCache.put(minSizeFile, new FileInfo(Instant.parse("2012-05-05T11:50:55.00z"), minSize));
		mapCache.put(maxTimeStampFile, new FileInfo(Instant.parse(maxTimeStamp), 180l));
		mapCache.put(minTimeStampFile, new FileInfo(Instant.parse(minTimeStamp), 220l));
		mapCache.put("file5", new FileInfo(Instant.parse("2019-05-05T11:50:55.00z"), 1130l));

		System.out.println("Cache as is: ");
		printCache(mapCache.getCache());

		System.out.println("Sorting Size Asc: ");

		mapCache.sortByFileSizeAsc();
		printSortedPathes(mapCache);

		sortedFileInfo = mapCache.getSortedPathes().get(0).getValue();
		testFileInfo = mapCache.get(minSizeFile);

		assertTrue("Sorting size asc is not working. ", sortedFileInfo.getSize() == testFileInfo.getSize());

		System.out.println("Sorting Size Desc: ");

		mapCache.sortByFileSizeDesc();
		printSortedPathes(mapCache);

		sortedFileInfo = mapCache.getSortedPathes().get(0).getValue();
		testFileInfo = mapCache.get(maxSizeFile);

		assertTrue("Sorting size desc is not working. ", sortedFileInfo.getSize() == testFileInfo.getSize());

		System.out.println("Sorting Access Asc: ");

		mapCache.sortByAccessedAsc();
		printSortedPathes(mapCache);

		sortedFileInfo = mapCache.getSortedPathes().get(0).getValue();
		testFileInfo = mapCache.get(minTimeStampFile);

		assertTrue("Sorting timestamp asc is not working. ",
				sortedFileInfo.getAccessed() == testFileInfo.getAccessed());

		System.out.println("Sorting Access Desc: ");

		mapCache.sortByAccessedDesc();
		printSortedPathes(mapCache);

		sortedFileInfo = mapCache.getSortedPathes().get(0).getValue();
		testFileInfo = mapCache.get(maxTimeStampFile);

		assertTrue("Sorting timestamp desc is not working. ",
				sortedFileInfo.getAccessed() == testFileInfo.getAccessed());
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void testPathes() throws IOException {

		TestUtils.printMethodName(this, testName);

		String testPath = "/HelloWorld/src/com/my/prosEO/PathCache.java";
		File file = new File(testPath);

		System.out.println("Path: " + file.getPath());
		System.out.println("Absolute Path: " + file.getAbsolutePath());
		System.out.println("Canonical Path: " + file.getCanonicalPath());
	}

	/**
	 * @param mapCache
	 */
	public static void printCache(Map<String, FileInfo> mapCache) {

		Set<Entry<String, FileInfo>> entries = mapCache.entrySet();

		for (Entry<String, FileInfo> entry : entries) {

			System.out.println(entry.getKey() + " ==> " + entry.getValue());
		}
	}

	/**
	 * @param message
	 * @param mapCache
	 */
	public static void printCache(String message, Map<String, FileInfo> mapCache) {

		System.out.println();
		System.out.println(message);

		printCache(mapCache);
	}

	/**
	 * @param mapCache
	 */
	public void printSortedPathes(MapCache mapCache) {

		List<Entry<String, FileInfo>> sortedPathes = mapCache.getSortedPathes();

		for (Entry<String, FileInfo> entry : sortedPathes) {

			System.out.println(entry.getKey() + " ==> " + entry.getValue());
		}
	}
}
