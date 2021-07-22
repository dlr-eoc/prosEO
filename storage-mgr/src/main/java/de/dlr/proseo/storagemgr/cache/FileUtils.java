package de.dlr.proseo.storagemgr.cache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common file utilities
 * 
 * @author Denys Chaykovskiy
 *
 */
public class FileUtils {

	/** the full path to file */
	private String path;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

	/**
	 * Gets the path to file
	 * 
	 * @return the path to file
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the path to file
	 * 
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Constructor sets the path
	 * 
	 * @param path Path to file
	 */
	public FileUtils(String path) {

		this.path = path;
	}

	/**
	 * Creates the file with the content
	 * 
	 * @param content Content of the file
	 */
	public void createFile(String content) {

		if (logger.isTraceEnabled())
			logger.trace(">>> createFile({})", content);

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
	 * Gets the file size
	 * 
	 * @return the file size
	 */
	public long getFileSize() {

		checkIfFile("Wrong path or not a file");

		return new File(path).length();
	}

	/**
	 * Gets the file content
	 * 
	 * @return the content of the file
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
	 * Checks if path is a file
	 * 
	 * @param exceptionMessage Exception Message will be shown if path is not a file
	 */
	private void checkIfFile(String exceptionMessage) {

		if (logger.isTraceEnabled())
			logger.trace(">>> checkIfFile({})", exceptionMessage);

		File file = new File(path);

		if (!file.isFile()) {
			throw new IllegalArgumentException(path + ": " + exceptionMessage);
		}
	}
}
