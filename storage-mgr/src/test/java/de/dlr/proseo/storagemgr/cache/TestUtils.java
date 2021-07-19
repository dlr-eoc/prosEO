package de.dlr.proseo.storagemgr.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;

/**
 * @author Denys Chaykovskiy
 *
 */
@Component
public class TestUtils {

	private static final String OUTPUT_TAB = "     ";
	private static final String OUTPUT_FILE_SIGN = "- ";
	private static final String TEST_SEPARATOR = "===============================================";
	private static final String PRINT_DIRECTORY_HEADER = "----- Directory";
	private static final String TEST_DIRECTORY = "testdata";
	
	private static final String JAR_PATH_KEY = "+++JAR+++";
	
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
	 * 
	 * @param cfgPath
	 * @return
	 */
	public String getAbsolutePahfromKeyPath(String cfgPath) {

		String path;

		if (cfgPath.contains(JAR_PATH_KEY)) {

			path = cfgPath.replace(JAR_PATH_KEY, "");
			path = new File("").getAbsolutePath() + path;
		} else {

			path = cfgPath;
		}

		return path;
	}

	
	/**
	 * @return
	 */
	public String getTestPath() {
		
		return getAbsolutePahfromKeyPath(cfg.getPosixWorkerMountPoint());
	}
	
	/**
	 * @return
	 */
	public String getSourceTestPath() {

		return getAbsolutePahfromKeyPath(cfg.getPosixMountPoint());
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
	public static void createEmptyTestDirectory() {

		deleteDirectory(TestUtils.getInstance().getTestPath());

		createDirectory(TestUtils.getInstance().getTestPath());
	}

	/**
	 * 
	 */
	public static void deleteTestDirectory() {
		
		deleteDirectory(TestUtils.getInstance().getTestPath());
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
	 */
	public static void deleteDirectory(String path) {

		File file = new File(path);

		if (!file.exists()) {

			return;
		}

		if (!file.getPath().contains(TEST_DIRECTORY)) {

			System.out.println("Attempt to delete file/dir not in test dir: " + file.getPath());
			return;
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
	public static void printArrayList(String message, ArrayList<String> arrayList) {
		
		System.out.println();
		System.out.println(message);
		for (String element : arrayList) {
			
			System.out.println(element);
		}
		System.out.println(); 
	}

	/**
	 * @param directoryPath
	 */
	public static void printDirectoryTree(String directoryPath) {

		System.out.println();
		System.out.println(PRINT_DIRECTORY_HEADER + " tree: " + directoryPath);
		printDirectoryTree(directoryPath, "");
		System.out.println();
	}

	/**
	 * @param directoryPath
	 * @param depth
	 */
	private static void printDirectoryTree(String directoryPath, String depth) {

		File directory = new File(directoryPath);

		File[] files = directory.listFiles();
		Arrays.sort(files);

		if (depth == "") {
			System.out.println(directory.getName());
			printDirectoryTree(directory.getPath(), OUTPUT_TAB);
			return;
		}

		for (File file : files) {
			if (file.isFile()) {
				System.out.println(depth + OUTPUT_FILE_SIGN + file.getName());
			}
		}

		for (File file : files) {
			if (file.isDirectory()) {
				System.out.println(depth + file.getName());
				printDirectoryTree(file.getPath(), OUTPUT_TAB + depth);
			}
		}
	}

}
