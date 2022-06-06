package de.dlr.proseo.storagemgr.version2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	 * Checks if the directory is empty
	 * 
	 * @param path full path to the directory
	 * @return true if directory is empty
	 */
	public boolean isEmptyDirectory() {

		File directory = new File(path);

		return directory.list().length > 0 ? false : true;
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
	 * Create parent directories
	 * 
	 * @param exceptionMessage IllegalStateException if cannot create dir
	 */
	public void createParentDirectories() {

		File targetFile = new File(path);
		File parent = targetFile.getParentFile();

		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IllegalStateException("Couldn't create parent dirs: " + parent);
		}
	}

	/**
	 * Create path directories
	 * 
	 * @param exceptionMessage IllegalStateException if cannot create dir
	 */
	public void createDirectories() {
		
		File file = new File(path);

		if (file != null && !file.exists() && !file.mkdirs()) {
			throw new IllegalStateException("Couldn't create dirs: " + file);
		}
	}
	
	// deletes file with empty directories
	public String deleteFile() throws IOException { 
		return deleteFile(path); 
	}

	
	public List<String> delete() throws IOException { 
		return delete(path); 
	}

	/**
	 * Checks if path is a file
	 * 
	 * @param exceptionMessage Exception will be thrown if path is not a file
	 */
	private void checkIfFile(String exceptionMessage) {

		File file = new File(path);

		if (!file.isFile()) {
			throw new IllegalArgumentException(path + ": " + exceptionMessage);
		}
	}
	
	
	/**
	 * Deletes empty directories recursively in the direction of root
	 * 
	 * @param directoryToDelete the path to the directory
	 */
	public void deleteEmptyDirectoriesToTop(String directoryToDelete) {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteEmptyDirectoriesToTop({})", directoryToDelete);

		if (null == directoryToDelete) {
			return;
		}

		File directory = new File(directoryToDelete);

		if (!directory.isDirectory()) {
			return;
		}

		File[] allFiles = directory.listFiles();
		String parent = directory.getParent();

		if (allFiles.length == 0) {

			directory.delete();

			deleteEmptyDirectoriesToTop(parent);
		}
	}

	
	
	/**
	 * Deletes a file 
	 * 
	 * @return
	 * @throws IOException
	 */
	private String deleteFile(String sourceFile) throws IOException {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFile({})", sourceFile);

		try {
			String folder = new File(sourceFile).getParent();
			boolean fileDeleted = new File(sourceFile).delete();

			if (fileDeleted) {
				deleteEmptyDirectoriesToTop(folder);
				return sourceFile;
			} else {
				throw new IOException("Cannot delete file: " + sourceFile);
			}

		} catch (Exception e) {
			e.printStackTrace();
			if (logger.isTraceEnabled())
				logger.error("Cannot download file: " + sourceFile, e.getMessage());
			throw e;
		}
	}
	
	private List<String> delete(String sourceFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", sourceFileOrDir);

		List<String> deletedFiles = new ArrayList<String>();

		if (isFile(sourceFileOrDir)) {

			String deletedFile = deleteFile(sourceFileOrDir);
			deletedFiles.add(deletedFile);
			return deletedFiles;
		}

		String sourceDir = sourceFileOrDir;
		//sourceDir = new PathConverter(sourceDir).addSlashAtEnd().getPath();

		File directory = new File(sourceDir);
		File[] files = directory.listFiles();
		Arrays.sort(files);

		for (File file : files) {
			if (file.isFile()) {
				String deletedFile = deleteFile(file.getAbsolutePath());
				deletedFiles.add(deletedFile);
			}
		}

		for (File file : files) {
			if (file.isDirectory()) {
				List<String> subDirDeletedFiles = delete(file.getAbsolutePath());
				deletedFiles.addAll(subDirDeletedFiles);
			}
		}

		return deletedFiles;
	}
	
	/**
	 * Checks if path is file
	 * 
	 * @return true if path is file
	 */
	private boolean isFile(String sourceFile) {
		return new File(sourceFile).isFile();
	}
	
}
