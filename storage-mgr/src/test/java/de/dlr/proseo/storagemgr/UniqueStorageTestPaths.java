package de.dlr.proseo.storagemgr;

import java.nio.file.Paths;

import org.junit.rules.TestName;

import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * @author Denys Chaykovskiy
 *
 */
public class UniqueStorageTestPaths {

	private static final String SOURCE_DIRECTORY = "source";
	private static final String STORAGE_DIRECTORY = "backend";
	private static final String CACHE_DIRECTORY = "cache";

	private String uniqueTestFolder; // testName_methodName

	private String uniqueSourcePath;  //  /../testdata/source/testName_methodName
	private String uniqueStoragePath;
	private String uniqueCachePath;

	public UniqueStorageTestPaths(String uniqueTestFolder) {

		this.uniqueTestFolder = uniqueTestFolder;
		
		createUniqueTestFolders();
	}

	public UniqueStorageTestPaths(Object unitTest, TestName testName) {

		String className = unitTest.getClass().getSimpleName();
		String methodName = testName.getMethodName();
		uniqueTestFolder = className + "_" + methodName;

		createUniqueTestFolders();
	}

	public void createUniqueTestFolders() {

		String testPath = TestUtils.getTestFolder();

		uniqueSourcePath = Paths.get(testPath, SOURCE_DIRECTORY, uniqueTestFolder).toString();
		uniqueSourcePath = new PathConverter(uniqueSourcePath).convertToSlash().getPath();
		TestUtils.createDirectory(uniqueSourcePath);

		uniqueStoragePath = Paths.get(testPath, STORAGE_DIRECTORY, uniqueTestFolder).toString();
		uniqueStoragePath = new PathConverter(uniqueStoragePath).convertToSlash().getPath();
		TestUtils.createDirectory(uniqueStoragePath);

		uniqueCachePath = Paths.get(testPath, CACHE_DIRECTORY, uniqueTestFolder).toString();
		uniqueCachePath = new PathConverter(uniqueCachePath).convertToSlash().getPath();
		TestUtils.createDirectory(uniqueCachePath);
	}

	public void deleteUniqueTestDirectories() {

		TestUtils.deleteDirectory(uniqueSourcePath);
		TestUtils.deleteDirectory(uniqueStoragePath);
		TestUtils.deleteDirectory(uniqueCachePath);
	}

	public String getUniqueSourcePath() {
		return uniqueSourcePath;
	}

	public String getUniqueStoragePath() {
		return uniqueStoragePath;
	}

	public String getUniqueCachePath() {
		return uniqueCachePath;
	}
	
	public String getUniqueTestFolder() {
		return uniqueTestFolder; 
	}
}
