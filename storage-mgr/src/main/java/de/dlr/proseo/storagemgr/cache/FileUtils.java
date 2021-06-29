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

	/**
	 * @param path
	 * @param fileName
	 * @param content
	 */
	public static void createFile(String path, String content) {

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
	public static long getFileSize(String path) {

		checkIfFile(path, "Wrong path or not a file");

		return new File(path).length();
	}

	/**
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static String getFileContent(String path) {

		String content = "";

		checkIfFile(path, "Wrong path or not a file");

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
	private static void checkIfFile(String path, String exceptionMessage) {

		File file = new File(path);

		if (!file.isFile()) {
			throw new IllegalArgumentException(path + ": " + exceptionMessage);
		}
	}
}
