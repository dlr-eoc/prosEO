package de.dlr.proseo.storagemgr.version2;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PathConverterTest {

	@Test
	public void testRelative() {

		String cachePath = "/target/cache";
		// String storagePath;
		// String sourcePath;

		String[] pathes = { "/mnt/blabla/", "/target/cache/folder1/file.txt", "/target/cache/file.txt",
				"s3://bucket/folder/file.txt", "file.txt" };

		String[] expected = { "mnt/blabla/", "folder1/file.txt", "file.txt", "folder/file.txt", "file.txt" };

		List<String> basePaths = new ArrayList<>();
		basePaths.add(cachePath);
	
		for (int i = 0; i < pathes.length; i++) {

			String relativePath = new PathConverter(pathes[i], basePaths).getRelativePath().getPath();

			assertTrue("Wrong relative path: " + relativePath + " expected: " + expected[i],
					relativePath.equals(expected[i]));
		}
	}

	@Test
	public void testFirstFolder() {

		String cachePath = "/target/cache";

		String path1 = "/first/second/file.txt";
		String expectedFirst1 = "first";
		String expectedWithoutFirst1 = "second/file.txt";

		List<String> basePaths = new ArrayList<>();
		basePaths.add(cachePath);

		String firstPath = new PathConverter(path1, basePaths).getFirstFolder().getPath();
		assertTrue("Wrong first path: " + firstPath + " expected: " + expectedFirst1, firstPath.equals(expectedFirst1));

		String withoutFirst = new PathConverter(path1, basePaths).removeFirstFolder().getPath();
		assertTrue("Wrong without first path: " + withoutFirst + " expected: " + expectedWithoutFirst1,
				withoutFirst.equals(expectedWithoutFirst1));
	}
}
