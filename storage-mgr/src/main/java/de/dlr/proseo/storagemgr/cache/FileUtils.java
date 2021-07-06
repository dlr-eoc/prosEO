package de.dlr.proseo.storagemgr.cache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Denys Chaykovskiy
 *
 */
public class FileUtils {
	
	private String path; 
	
	private static final String TEST_DIRECTORY = "/target/testdata";
	
	
	public static String getTestPath() {
		
		return Paths.get(".").toAbsolutePath().normalize().toString() + TEST_DIRECTORY;
	}

	/**
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
	/**
	 * @param path
	 */
	public FileUtils(String path) {
		
		this.path = path; 
	}

	/**
	 * @param path
	 * @param fileName
	 * @param content
	 */
	public void createFile(String content) {

		File file = new File(path);
		file.getParentFile().mkdirs();

		try {

			FileWriter writer = new FileWriter(file, false);
			writer.write(content);
			writer.close();

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	/**
	 * @param path
	 * @return
	 */
	public long getFileSize() {

		checkIfFile("Wrong path or not a file");

		return new File(path).length();
	}

	/**
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public String getFileContent() {

		String content = "";

		checkIfFile("Wrong path or not a file");

		try {
			content = new String(Files.readAllBytes(Paths.get(path)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return content;
	}

	/**
	 * @param path
	 * @param exceptionMessage
	 */
	private void checkIfFile(String exceptionMessage) {

		File file = new File(path);

		if (!file.isFile()) {
			throw new IllegalArgumentException(path + ": " + exceptionMessage);
		}
	}
}
