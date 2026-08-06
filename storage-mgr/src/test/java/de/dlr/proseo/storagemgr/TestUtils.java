/**
 * TestUtils.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.utils.FileUtils;

/**
 * Utility methods for unit test execution
 * 
 * @author Denys Chaykovskiy
 *
 */
@Component
public class TestUtils {

	private static final String OUTPUT_TAB = "     ";
	private static final String OUTPUT_FILE_SIGN = "- ";
	private static final String TEST_SEPARATOR = "===============================================";
	private static final String PRINT_DIRECTORY_HEADER = "----- Folder";
	private static final String TEST_DIRECTORY = "testdata";
	private static final String TARGET_DIRECTORY = "target";

	@Autowired
	private StorageProvider storageProvider;

	@Autowired
	private StorageManagerConfiguration cfg;

	/**
	 * @return the Storage Manager configuration
	 */
	public StorageManagerConfiguration getCfg() {

		return cfg;
	}

	/**
	 * @return the test storage path
	 */
	public String getStoragePath() {

		return cfg.getPosixBackendPath();
	}

	/**
	 * @return the test source path
	 */
	public String getSourcePath() {

		return cfg.getDefaultSourcePath();
	}

	/**
	 * @return the test cache path
	 */
	public String getCachePath() {

		return cfg.getPosixCachePath();
	}

	/**
	 * @return the test source directory path
	 */

	public String getTestSourcePath() {

		return getTestPath(getSourcePath());
	}

	/**
	 * @return the test storage directory path
	 */
	public String getTestStoragePath() {

		return getTestPath(getStoragePath());
	}

	/**
	 * @return the test cache directory path
	 */
	public String getTestCachePath() {

		return getTestPath(getCachePath());
	}

	/**
	 * @param path the path to add the test directory name to
	 * @return a test directory path relative to the input path
	 */
	private String getTestPath(String path) {
		return Paths.get(path, TEST_DIRECTORY).toString();
	}

	/**
	 * Create a set of empty test directories
	 */
	public void createEmptyTestDirectories() {

		deleteDirectory(getTestCachePath());
		createDirectory(getTestCachePath());

		deleteDirectory(getTestStoragePath());
		createDirectory(getTestStoragePath());
	}

	/**
	 * Create a set of empty storage directories
	 */
	public void createEmptyStorageDirectories() {

		deleteDirectory(getSourcePath());
		createDirectory(getSourcePath());

		deleteDirectory(getStoragePath());
		createDirectory(getStoragePath());

		deleteDirectory(getCachePath());
		createDirectory(getCachePath());
	}

	/**
	 * Delete the test storage directories
	 */
	public void deleteStorageDirectories() {

		deleteDirectory(getSourcePath());
		deleteDirectory(getStoragePath());
		deleteDirectory(getCachePath());
	}

	/**
	 * Delete the test data folders
	 */
	public void deleteTestDirectories() {

		deleteDirectory(getTestCachePath());
		deleteDirectory(getTestStoragePath());
	}

	/**
	 * Create a directory at the given path (restricted to the test folder)
	 * 
	 * @param path the requested directory path
	 */
	public void createDirectory(String path) {

		File file = new File(path);

		if (!file.getPath().contains(TEST_DIRECTORY)) {

			System.out.println("Attempt to create dir not in test dir: " + file.getPath());
			return;
		}

		if (!file.exists()) {

			file.mkdirs();
		}
	}

	/**
	 * Delete all test files from the S3 backend storage
	 * 
	 * @throws IOException on any S3-related error
	 */
	public void deleteFilesinS3Storage() throws IOException {

		deleteFilesInStorage(StorageType.S3);
	}

	/**
	 * Delete all test files from the POSIX backend storage
	 * 
	 * @throws IOException on any file system-related error
	 */
	public void deleteFilesinPosixStorage() throws IOException {

		deleteFilesInStorage(StorageType.POSIX);
	}

	/**
	 * Delete all test files for the given storage type
	 * 
	 * @throws IOException on any storage-related error
	 */
	private void deleteFilesInStorage(StorageType storageType) throws IOException {

		File file = new File(getStoragePath());

		if (!file.getPath().contains(TEST_DIRECTORY)) {

			System.out.println("Attempt to delete " + storageType.toString() + " storage files not from unit test: "
					+ getStoragePath());
			return;
		}

		Storage storage = storageProvider.getDefaultStorage(storageType);
		List<String> relativePaths = storage.getRelativeFiles();

		for (String relativePath : relativePaths) {
			StorageFile storageFile = storage.getStorageFile(relativePath);
			try {
				storage.delete(storageFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		List<String> storageFilesAfterDelete = storage.getRelativeFiles();
		TestUtils.printList("Storage after delete all " + storageType + " files:", storageFilesAfterDelete);
	}

	/**
	 * 
	 * @return the path to the newly created test folde
	 */
	public String getTestFolder() {

		String testPath = Paths.get(TARGET_DIRECTORY, TEST_DIRECTORY).toString(); // str-mgr/target/testdata
		testPath = new File(testPath).getAbsolutePath();

		createDirectory(testPath);

		return testPath;
	}

	/**
	 * Formatted output of the name of the method under test
	 */
	public static void printMethodName(Object object, TestInfo testInfo) {

		System.out.println();
		System.out.println(TEST_SEPARATOR + TEST_SEPARATOR);
		System.out.println("TEST " + object.getClass().getSimpleName() + "." + testInfo.getTestMethod().get().getName());
		System.out.println();
	}

	/**
	 * Create a file with the given content with the given path (restricted to the test folder)
	 * 
	 * @param path the requested file path (including file name)
	 */
	public static void createFile(String path, String content) {

		File file = new File(path);

		if (!file.getPath().contains(TEST_DIRECTORY)) {

			System.out.println("Attempt to create file not in test dir: " + file.getPath());
			return;
		}

		FileUtils fileUtils = new FileUtils(path);
		fileUtils.createFile(content);
	}

	/**
	 * Create a large file at the given path (restricted to the test folder)
	 * 
	 * For example: 
	 * long fileSizeInBytes = 100L * 1024 * 1024; // 100 MB
	 * 
	 * @param filePath the requested file path (including file name)
	 * @param fileSizeInBytes the requested minimum file size (will be rounded up to full 1k blocks)
	 */
	public static void createLargeFile(String filePath, long fileSizeInBytes) {
		
		File file = new File(filePath);

		if (!file.getPath().contains(TEST_DIRECTORY)) {

			System.out.println("Attempt to create file not in test dir: " + file.getPath());
			return;
		}
		
		file.getParentFile().mkdirs();
		
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
			
			byte[] buffer = new byte[1024];
			long bytesWritten = 0;

			while (bytesWritten < fileSizeInBytes) {
				
				int bytesToWrite = (int) Math.min(buffer.length, fileSizeInBytes - bytesWritten);
				bos.write(buffer, 0, bytesToWrite);
				bytesWritten += bytesToWrite;
			}

			System.out.println("Large file created successfully.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test whether the file at the given path exists
	 * 
	 * @param path file path (including file name) as String
	 */
	public static boolean fileExists(String path) {
		
		return new File(path).exists();
	}

	/**
	 * Test whether the given path exists and points to a directory
	 * 
	 * @param path the directory path to test
	 */
	public static boolean directoryExists(String path) {

		return new File(path).isDirectory();
	}

	/**
	 * Delete the file at the given path (restricted to the test folder)
	 * 
	 * @param path file path (including file name) as String
	 */
	public static void deleteFile(String path) {

		File file = new File(path);

		if (!file.getPath().contains(TEST_DIRECTORY)) {

			System.out.println("Attempt to delete file not in test dir: " + file.getPath());
			return;
		}

		if (!file.delete()) {

			System.out.println("File was NOT deleted: " + file.getPath());
		} else {
			System.out.println("File was deleted: " + file.getPath());
		}
	}

	/**
	 * Delete the directory at the given path (restricted to test folder)
	 * 
	 * @param path the path of the directory to delete
	 */
	public static void deleteDirectory(String path) {

		File file = new File(path);

		if (!file.exists()) {

			return;
		}

		if (!file.getPath().contains(TEST_DIRECTORY)) {

			String errorMsg = "ERROR! Attempt to delete file/dir not in test dir(" + TEST_DIRECTORY + "): "
					+ file.getPath();

			System.out.println(errorMsg);
			throw new UncheckedIOException(new IOException(errorMsg));
		}

		for (File subFile : file.listFiles()) {
			if (subFile.isDirectory()) {
				deleteDirectory(subFile.getPath());
			} else {

				subFile.delete();
			}
		}
		file.delete();
	}

	/**
	 * List the files in the given directory on Standard Output
	 * 
	 * @param path the path of the directory to list
	 */
	public static void printDirectory(String path) {

		File f = new File(path);
		File[] files = f.listFiles();

		System.out.println();
		System.out.println(PRINT_DIRECTORY_HEADER + ": " + path);
		for (File file : files) {
			System.out.println(file.getName());
		}
		System.out.println();
	}

	/**
	 * Print a numbered list of strings on Standard Output
	 * 
	 * @param message the message to print before the list of strings
	 * @param arrayList the list of strings to print
	 */
	public static void printList(String message, List<String> list) {

		System.out.println();
		System.out.println(message + " || LIST SIZE: " + list.size());
		for (String element : list) {

			System.out.println(" - " + element);
		}
		System.out.println();
	}

	/**
	 * Print an indented list of nested directories with an introductory message on Standard Output
	 * 
	 * @param message the message to output before the directory list
	 * @param directoryPath the root path of the directory structure to print
	 */
	public static void printDirectoryTree(String message, String directoryPath) {

		System.out.println();
		System.out.println(message);

		printDirectoryTree(directoryPath);
	}

	/**
	 * Print an indented list of nested directories on Standard Output
	 * 
	 * @param directoryPath the root path of the directory structure to print
	 */
	public static void printDirectoryTree(String directoryPath) {

		System.out.println();

		File dir = new File(directoryPath);

		if ((null == dir) || !dir.exists()) {
			System.out.println("Error in printDirectoryTree: Directory does not exist: " + directoryPath);
			return;
		}

		String directory = new File(directoryPath).getName();

		System.out.print("FOLDER: " + directory + " PATH: " + directoryPath);
		System.out.print(" Files: " + countFilesInDirectory(directoryPath));
		System.out.println(" Folders: " + countDirectoriesInDirectory(directoryPath));

		printDirectoryTreeWithDepth(directoryPath, "");
		System.out.println();
	}

	/**
	 * Recursively print an indented list of nested directories on Standard Output
	 * 
	 * @param directoryPath the sub-path of the directory structure to print
	 * @param depth the requested indentation of the sub-structure
	 */
	private static void printDirectoryTreeWithDepth(String directoryPath, String depth) {

		File directory = new File(directoryPath);

		// System.out.println("FOLDER: " + directoryPath + " " + " DEPTH: " + depth);

		File[] files = directory.listFiles();
		if (files == null)
			return;
		Arrays.sort(files);

		/*
		 * if (depth == "") { System.out.println(directory.getName());
		 * printDirectoryTree(directory.getPath(), OUTPUT_TAB); return; }
		 */

		for (File file : files) {
			if (file.isFile()) {
				System.out.println(depth + OUTPUT_FILE_SIGN + file.getName());
			}
		}

		for (File file : files) {
			if (file.isDirectory()) {
				System.out.println(depth + file.getName() + " <DIR>");
				printDirectoryTreeWithDepth(file.getPath(), OUTPUT_TAB + depth);
			}
		}
	}

	/**
	 * Count the files in the given directory including nested directories
	 * 
	 * @param directory the path to the directory
	 * @return the number of files in the directory structure
	 */
	public static int countFilesInDirectory(String directory) {

		return countFilesInDirectory(new File(directory));
	}

	/**
	 * Recursively count the files in the given directory
	 * 
	 * @param directory the path to the directory
	 * @return the number of files in the directory structure
	 */
	private static int countFilesInDirectory(File directory) {

		if ((null == directory) || !directory.exists()) {
			System.out.println("Error in countFilesInDirectory: Directory does not exist: " + directory);
			return 0;
		}

		int count = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				count++;
			}
			if (file.isDirectory()) {
				count += countFilesInDirectory(file);
			}
		}
		return count;
	}

	/**
	 * Count the nested directories in the given directory
	 * 
	 * @param directory the path to the directory
	 * @return the number of directories in the directory structure (excluding the top-level directory)
	 */
	public static int countDirectoriesInDirectory(String directory) {

		return countDirectoriesInDirectory(new File(directory));
	}

	private static int countDirectoriesInDirectory(File directory) {

		if ((null == directory) || !directory.exists()) {
			System.out.println("Error in countDirectoriesInDirectory: Directory does not exist: " + directory);
			return 0;
		}

		int count = 0;
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				count++;
				count += countDirectoriesInDirectory(file);
			}
		}
		return count;
	}

	/**
	 * Convert the given object to a JSON structure (re-throwing any exceptions as RuntimeException)
	 * 
	 * @param obj the object to convert
	 * @return a JSON string representing the object
	 */
	public static String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Print the result of an HTTP request on Standard Output
	 * 
	 * @param requestString the request that was sent
	 * @param mvcResult the result that was received
	 */
	public static void printMvcResult(String requestString, MvcResult mvcResult) {

		System.out.println();
		System.out.println("HTTP Response");
		System.out.println("Request: " + requestString);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());

		try {
			System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		System.out.println();
	}
}
