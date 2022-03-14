package de.dlr.proseo.storagemgr.version2;

import static org.junit.Assert.*;

import org.junit.Test;


public class PathConverterTest {

	@Test
	public void test() {

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

}
