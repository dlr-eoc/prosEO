package de.dlr.proseo.storagemgr.version2;

import static org.junit.Assert.*;

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

		PathConverter pathConverter = new PathConverter();
		pathConverter.addBasePath(cachePath);

		for (int i = 0; i < pathes.length; i++) {

			String relativePath = pathConverter.getRelativePath(pathes[i]);

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

		PathConverter pathConverter = new PathConverter();
		pathConverter.addBasePath(cachePath);

		String firstPath = pathConverter.getFirstFolder(path1);
		assertTrue("Wrong first path: " + firstPath + " expected: " + expectedFirst1, firstPath.equals(expectedFirst1));

		String withoutFirst = pathConverter.removeFirstFolder(path1);
		assertTrue("Wrong without first path: " + withoutFirst + " expected: " + expectedWithoutFirst1,
				withoutFirst.equals(expectedWithoutFirst1));
	}
}
