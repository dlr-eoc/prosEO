package de.dlr.proseo.storagemgr.cache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.rest.ProductControllerImpl;

/**
 * @author Denys Chaykovskiy
 *
 */
public class FileUtils {

	private String path;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

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

		if (logger.isTraceEnabled())
			logger.trace(">>> checkIfFile({})", exceptionMessage);

		File file = new File(path);

		if (!file.isFile()) {
			throw new IllegalArgumentException(path + ": " + exceptionMessage);
		}
	}
}
