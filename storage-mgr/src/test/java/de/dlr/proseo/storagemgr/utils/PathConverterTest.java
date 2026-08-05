package de.dlr.proseo.storagemgr.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

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

			assertEquals(expected[i], relativePath,
					"Wrong relative path: " + relativePath + " expected: " + expected[i]);
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
		assertEquals(expectedFirst1, firstPath, "Wrong first path: " + firstPath + " expected: " + expectedFirst1);

		String withoutFirst = new PathConverter(path1, basePaths).removeFirstFolder().getPath();
		assertEquals(expectedWithoutFirst1, withoutFirst,
				"Wrong without first path: " + withoutFirst + " expected: " + expectedWithoutFirst1);
	}
}
