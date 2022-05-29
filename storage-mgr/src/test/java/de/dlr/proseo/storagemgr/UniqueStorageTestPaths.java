package de.dlr.proseo.storagemgr;

import java.io.File;
import java.nio.file.Paths;

import org.junit.rules.TestName;

import de.dlr.proseo.storagemgr.version2.PathConverter;

/**
 * @author Denys Chaykovskiy
 *
 */
public class UniqueStorageTestPaths {

	private static final String SOURCE_DIRECTORY = "source";
	private static final String STORAGE_DIRECTORY = "backend";
	private static final String CACHE_DIRECTORY = "cache";

	private String uniqueTestPath;

	private String sourcePath;
	private String storagePath;
	private String cachePath;

	public UniqueStorageTestPaths(String uniqueTestFolder) {

		init(uniqueTestFolder);
	}

	public UniqueStorageTestPaths(Object unitTest, TestName testName) {

		String className = unitTest.getClass().getSimpleName();
		String methodName = testName.getMethodName();
		String uniqueTestFolder = className + "_" + methodName;

		init(uniqueTestFolder);

	}

	private void init(String uniqueTestFolder) {

		uniqueTestPath = TestUtils.getTestFolder(); // str-mgr/target/testdata
		uniqueTestPath = Paths.get(uniqueTestPath, uniqueTestFolder).toString(); // testdata/className_methodName

		createStorageFolders();
	}

	public void createStorageFolders() {

		String uniqueTestPath = getUniqueTestPath();

		sourcePath = Paths.get(uniqueTestPath, SOURCE_DIRECTORY).toString();
		sourcePath = new PathConverter(sourcePath).convertToSlash().getPath();
		TestUtils.createDirectory(sourcePath);

		storagePath = Paths.get(uniqueTestPath, STORAGE_DIRECTORY).toString();
		storagePath = new PathConverter(storagePath).convertToSlash().getPath();
		TestUtils.createDirectory(storagePath);

		cachePath = Paths.get(uniqueTestPath, CACHE_DIRECTORY).toString();
		cachePath = new PathConverter(cachePath).convertToSlash().getPath();
		TestUtils.createDirectory(cachePath);
	}

	public String getUniqueTestPath() {

		if (!TestUtils.directoryExists(uniqueTestPath))
			TestUtils.createDirectory(uniqueTestPath);

		return uniqueTestPath;
	}

	public void deleteUniqueTestDirectory() {

		TestUtils.deleteDirectory(uniqueTestPath);
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public String getCachePath() {
		return cachePath;
	}
}
