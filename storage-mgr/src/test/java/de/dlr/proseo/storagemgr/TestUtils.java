package de.dlr.proseo.storagemgr;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import de.dlr.proseo.storagemgr.cache.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;

/**
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

	@Autowired
	private StorageManagerConfiguration cfg;

	private static TestUtils theTestUtils;

	public static TestUtils getInstance() {

		return theTestUtils;
	}

	@PostConstruct
	public void init() {

		theTestUtils = this;
	}

	/**
	 * @return
	 */
	public StorageManagerConfiguration getCfg() {

		return cfg;
	}

	/**
	 * @return
	 */
	public String getStoragePath() {

		return new PathConverter().convertToSlash(cfg.getPosixBackendPath());
	}

	/**
	 * @return
	 */
	public String getSourcePath() {

		return new PathConverter().convertToSlash(cfg.getPosixSourcePath());
	}

	/**
	 * @return
	 */
	public String getCachePath() {

		return new PathConverter().convertToSlash(cfg.getPosixCachePath());
	}

	/**
	 * @return
	 */

	public String getTestSourcePath() {

		return new PathConverter().convertToSlash(getTestPath(getSourcePath()));
	}

	/**
	 * @return
	 */
	public String getTestStoragePath() {

		return new PathConverter().convertToSlash(getTestPath(getStoragePath()));
	}

	/**
	 * @return
	 */
	public String getTestCachePath() {

		return new PathConverter().convertToSlash(getTestPath(getCachePath()));
	}

	/**
	 * @param path
	 * @return
	 */
	private String getTestPath(String path) {

		return new PathConverter().convertToSlash(Paths.get(path, TEST_DIRECTORY).toString());
	}

	/**
	 * 
	 */
	public static void printMethodName(Object object, TestName testName) {

		System.out.println();
		System.out.println(TEST_SEPARATOR + TEST_SEPARATOR);
		System.out.println("TEST " + object.getClass().getSimpleName() + "." + testName.getMethodName());
		System.out.println();
	}

	/**
	 * @param path
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
	 * @param path
	 */
	public static boolean fileExists(String path) {

		return new File(path).exists();
	}

	/**
	 * @param path
	 */
	public static boolean directoryExists(String path) {

		return new File(path).isDirectory();
	}

	/**
	 * @param path
	 */
	public static void deleteFile(String path) {

		File file = new File(path);

		if (!file.getPath().contains(TEST_DIRECTORY)) {

			System.out.println("Attempt to delete file not in test dir: " + file.getPath());
			return;
		}

		if (!file.delete()) {

			System.out.println("File was NOT deleted: " + file.getPath());
		}
	}

	/**
	 * 
	 */
	public static void createEmptyTestDirectories() {

		deleteDirectory(TestUtils.getInstance().getTestCachePath());
		createDirectory(TestUtils.getInstance().getTestCachePath());

		deleteDirectory(TestUtils.getInstance().getTestStoragePath());
		createDirectory(TestUtils.getInstance().getTestStoragePath());
	}

	/**
	 * 
	 */
	public static void createEmptyStorageDirectories() {

		deleteDirectory(TestUtils.getInstance().getSourcePath());
		createDirectory(TestUtils.getInstance().getSourcePath());

		deleteDirectory(TestUtils.getInstance().getStoragePath());
		createDirectory(TestUtils.getInstance().getStoragePath());

		deleteDirectory(TestUtils.getInstance().getCachePath());
		createDirectory(TestUtils.getInstance().getCachePath());
	}

	/**
	 * 
	 */
	public static void deleteStorageDirectories() {

		deleteDirectory(TestUtils.getInstance().getSourcePath());
		deleteDirectory(TestUtils.getInstance().getStoragePath());
		deleteDirectory(TestUtils.getInstance().getCachePath());
	}

	/**
	 * 
	 */
	public static void deleteTestDirectories() {

		deleteDirectory(TestUtils.getInstance().getTestCachePath());
		deleteDirectory(TestUtils.getInstance().getTestStoragePath());
	}

	/**
	 * @param path
	 */
	public static void createDirectory(String path) {

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
	 * @param path
	 * @throws Exception
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
	 * @param path
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
	 * @param message
	 * @param arrayList
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
	 * @param directoryPath
	 */
	public static void printDirectoryTree(String directoryPath) {

		System.out.println();
		String directory = new File(directoryPath).getName();
		
		System.out.print("FOLDER: " + directory + " PATH: " + directoryPath);
		System.out.print(" Files: " + countFilesInDirectory(directoryPath));
		System.out.println(" Folders: " + countDirectoriesInDirectory(directoryPath));
		
		printDirectoryTree(directoryPath, "");
		System.out.println();
	}

	/**
	 * @param directoryPath
	 * @param depth
	 */
	private static void printDirectoryTree(String directoryPath, String depth) {

		File directory = new File(directoryPath);
		
		// System.out.println("FOLDER: " + directoryPath + " " + " DEPTH: " + depth);

		File[] files = directory.listFiles();
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
				printDirectoryTree(file.getPath(), OUTPUT_TAB + depth);
			}
		}
	}

	public static int countFilesInDirectory(String directory) {

		return countFilesInDirectory(new File(directory));
	}

	private static int countFilesInDirectory(File directory) {
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

	public static int countDirectoriesInDirectory(String directory) {

		return countDirectoriesInDirectory(new File(directory));
	}

	private static int countDirectoriesInDirectory(File directory) {
		int count = 0;
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				count++;
				count += countDirectoriesInDirectory(file);
			}
		}
		return count;
	}
}
