package de.dlr.proseo.storagemgr;

import java.io.File;
import java.nio.file.Paths;

import org.junit.rules.TestName;

/**
 * @author Denys Chaykovskiy
 *
 */
public class UniqueStorageTestPathes {

	private static final String SOURCE_DIRECTORY = "source";
	private static final String STORAGE_DIRECTORY = "backend";
	private static final String CACHE_DIRECTORY = "cache";

	private TestName testName;
	private Object unitTest;

	private String sourceDir;
	private String storageDir;
	private String cacheDir;

	public UniqueStorageTestPathes(Object unitTest, TestName testName) {

		this.unitTest = unitTest;
		this.testName = testName;

		createStorageFolders();
	}

	public void createStorageFolders() {

		String uniqueTestPath = getUniqueTestPath();

		sourceDir = Paths.get(uniqueTestPath, SOURCE_DIRECTORY).toString();
		TestUtils.createDirectory(sourceDir);

		storageDir = Paths.get(uniqueTestPath, STORAGE_DIRECTORY).toString();
		TestUtils.createDirectory(storageDir);

		cacheDir = Paths.get(uniqueTestPath, CACHE_DIRECTORY).toString();
		TestUtils.createDirectory(cacheDir);
	}

	public String getUniqueTestPath() {

		String testPath = TestUtils.getTestFolder(); // str-mgr/target/testdata

		String className = unitTest.getClass().getSimpleName();
		String methodName = testName.getMethodName();

		testPath = Paths.get(testPath, className + "_" + methodName).toString();
		testPath = new File(testPath).getAbsolutePath();

		TestUtils.createDirectory(testPath);

		return testPath;
	}

	public String getSourcePath() {
		return sourceDir;
	}

	public String getStoragePath() {
		return storageDir;
	}

	public String getCachePath() {
		return storageDir;
	}
}
