/**
 * FileUtils.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;
import de.dlr.proseo.storagemgr.Exceptions.FileLockedAfterMaxCyclesException;

/**
 * A utility class for common file operations. It provides methods to create,
 * manipulate, and delete files and directories, along with supporting
 * functionalities such as path handling and file property retrieval.
 *
 * @author Denys Chaykovskiy
 */
public class FileUtils {

	/** the full path to file */
	private String path;

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(FileUtils.class);

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
	 * @param path the path to set
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
	 * @return true if file was successfully created
	 */
	public boolean createFile(String content) {

		if (logger.isTraceEnabled())
			logger.trace(">>> createFile(<content>)");

		logger.debug("... file content (first 20 characters): " + new StringUtils(content).getMaxSubstring(20));
		logger.debug("... file size: " + content.length());

		File file = new File(path);
		file.getParentFile().mkdirs();

		try {

			FileWriter writer = new FileWriter(file, false);
			writer.write(content);
			writer.close();
			return true;

		} catch (IOException e) {

			if (logger.isDebugEnabled()) {
				logger.debug("An exception occurred. Cause: ", e);
			}
			return false;
		}
	}

	/**
	 * Creates the file with the content in a synchronized modus. Returns false if the file was not created/rewritten. 
	 * creation of the file was not successfully
	 *
	 * @param content            Content of the file
	 * @param waitTime           the wait time between each cycle of checking the
	 *                           file lock status
	 * @param fileCheckMaxCycles file check max cycles
	 * @return true if file was successfully created
	 */
	public boolean synchroCreateFile(String content, long waitTime, long fileCheckMaxCycles) {

		if (logger.isTraceEnabled())
			logger.trace(">>> synchroCreateFile(<content>, {}, {})", waitTime, fileCheckMaxCycles);

		logger.debug("... file content (first 20 characters): " + new StringUtils(content).getMaxSubstring(20));
		logger.debug("... file size: " + content.length());

		StorageFileLocker fileLocker = new StorageFileLocker(path, waitTime, fileCheckMaxCycles);
		boolean fileCreatedStatus = false;

		try {

			fileLocker.lockOrWaitUntilUnlockedAndLock();
			fileCreatedStatus = createFile(content);
			fileLocker.unlock();
			fileCreatedStatus = true;

		} catch (FileLockedAfterMaxCyclesException e) {

			logger.debug("... the synchronized file was not created/rewritten: waiting timeout (possible locked from another thread): "
					+ path + " " + e.getMessage());

		} catch (InterruptedException e) {

			logger.debug("... the syncronized file was not created/rewritten: interrupted exception: " + path + " "
					+ e.getMessage());
		}

		return fileCreatedStatus;
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
	 * Checks if the path points to an empty directory
	 *
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
			if (logger.isDebugEnabled()) {
				logger.debug("An exception occurred. Cause: ", e);
			}
		}

		return content;
	}

	/**
	 * Create parent directories
	 *
	 * @throws IllegalStateException if cannot create dir
	 */
	public void createParentDirectories() throws IllegalArgumentException {

		File targetFile = new File(path);
		File parent = targetFile.getParentFile();

		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IllegalStateException("Couldn't create parent dirs: " + parent);
		}
	}

	/**
	 * Create path directories
	 *
	 * @throws IllegalStateException if cannot create dir
	 */
	public void createDirectories() throws IllegalArgumentException {

		File file = new File(path);

		if (file != null && !file.exists() && !file.mkdirs()) {
			throw new IllegalStateException("Couldn't create dirs: " + file);
		}
	}

	/**
	 * Delete the file
	 * 
	 * @return path to the deleted file
	 * @throws IOException if the file cannot be deleted
	 */
	public String deleteFile() throws IOException {
		return deleteFile(path);
	}

	/**
	 * Delete the file or the directory with its sub-directories and contained files
	 * 
	 * @return the paths to the deleted files and directories
	 * @throws IOException if the file or directory cannot be deleted
	 */
	public List<String> delete() throws IOException {
		return delete(path);
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
	 * @return path to deleted file
	 * @throws IOException if file cannot be deleted
	 */
	private String deleteFile(String sourceFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFile({})", sourceFile);

		try {
			String folder = new File(sourceFile).getParent();

			if (folder == null) {
				throw new IOException("Cannot delete file - no parent folder: " + sourceFile);
			}

			File file = new File(sourceFile);

			if (!file.exists()) {
				throw new IOException("Cannot delete file - file does not exist: " + sourceFile);
			}

			boolean fileDeleted = file.delete();

			if (!fileDeleted) {
				throw new IOException("File was not deleted: " + sourceFile);
			}

			deleteEmptyDirectoriesToTop(folder);
			return sourceFile;

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("An exception occurred. Cause: ", e);
			}
			if (logger.isTraceEnabled())
				logger.log(StorageMgrMessage.FILE_NOT_DELETED, sourceFile, e.getMessage());
			throw e;
		}
	}

	/**
	 * Deletes file or directory with subdirectories
	 *
	 * @param sourceFileOrDir file or directory to delete
	 * @return list of deleted files
	 * @throws IOException if file or dir cannot be deleted
	 */
	private List<String> delete(String sourceFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", sourceFileOrDir);

		List<String> deletedFiles = new ArrayList<>();

		if (isFile(sourceFileOrDir)) {

			String deletedFile = deleteFile(sourceFileOrDir);
			deletedFiles.add(deletedFile);
			return deletedFiles;
		}

		String sourceDir = sourceFileOrDir;
		// sourceDir = new PathConverter(sourceDir).addSlashAtEnd().getPath();

		File directory = new File(sourceDir);
		File[] files = directory.listFiles();
		if (files == null)
			return deletedFiles;
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
}