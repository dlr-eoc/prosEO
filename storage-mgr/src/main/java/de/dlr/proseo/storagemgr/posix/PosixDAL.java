/**
 * PosixDAL.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.posix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.storagemgr.model.AtomicCommand;
import de.dlr.proseo.storagemgr.model.DefaultRetryStrategy;
import de.dlr.proseo.storagemgr.utils.FileUtils;
import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * A data access layer for interacting with a POSIX-based storage system. It
 * provides various methods to perform operations such as retrieving files,
 * checking file or directory existence, obtaining file content, uploading
 * files, downloading files, and deleting files or directories within the POSIX
 * storage system.
 *
 * @author Denys Chaykovskiy
 */
public class PosixDAL {

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(PosixDAL.class);
	
	/** POSIX configuration */
	private PosixConfiguration cfg;
	
	/**
	 * Constructor
	 *
	 * @param cfg POSIX Configuration
	 */
	public PosixDAL(PosixConfiguration cfg) {

		this.cfg = cfg;
	}

	/**
	 * Gets the POSIX configuration used by this PosixDAL instance.
	 *
	 * @return the PosixConfiguration object
	 */
	public PosixConfiguration getConfiguration() {

		return cfg;
	}
	
	/**
	 * Retrieves a list of files that match a given path (or prefix). It recursively
	 * scans directories and adds the absolute paths of files to the result
	 *
	 * @param path path (prefix)
	 * @return list if files
	 */
	public List<String> getFiles(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", path);

		List<String> returnFiles = new ArrayList<>();

		File dirOrFile = new File(path);

		if (dirOrFile.isFile()) {
			returnFiles.add(path);
		}

		File directory = dirOrFile;

		File[] files = directory.listFiles();
		if (files == null)
			return returnFiles;
		Arrays.sort(files);

		for (File file : files) {
			if (file.isFile()) {
				returnFiles.add(file.getAbsolutePath());
			}
		}

		for (File file : files) {
			if (file.isDirectory()) {
				List<String> dirFiles = getFiles(file.getAbsolutePath());
				returnFiles.addAll(dirFiles);
			}
		}

		return returnFiles;
	}

	/**
	 * Gets file name
	 *
	 * @param path path
	 * @return file name
	 */
	public String getFileName(String path) {
		return new File(path).getName();
	}

	/**
	 * Checks if file physically exists
	 *
	 * @param path path
	 * @return true if file exists
	 */
	public boolean fileExists(String path) {
		return new File(path).isFile();
	}

	/**
	 * Checks if path is existing directory
	 *
	 * @param path path
	 * @return true if path is directory
	 */
	public boolean isDirectory(String path) {
		return new File(path).isDirectory();
	}

	/**
	 * Checks if path is existing file
	 *
	 * @param path path
	 * @return true if file exists
	 */
	public boolean isFile(String path) {
		return new File(path).isFile();
	}

	/**
	 * Retrieves the content of a file specified by its path
	 *
	 * @param path the path to the storage file
	 * @return file content
	 * @throws IOException if an I/O exception occurs
	 */
	public String getFileContent(String path) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFileContent({})", path);

		return Files.readString(Path.of(path));
	}

	/**
	 * Uploads a source file to the POSIX storage system at the specified target
	 * location
	 *
	 * @param sourceFile      source file to upload
	 * @param targetFileOrDir target file or directory in storage
	 * @return path of uploaded file
	 * @throws IOException if file cannot be uploaded
	 */
	public String uploadFile(String sourceFile, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({},{})", sourceFile, targetFileOrDir);
	
		AtomicCommand<String> fileUploader = new PosixAtomicFileUploader(sourceFile, targetFileOrDir);

		return new DefaultRetryStrategy<>(fileUploader, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Uploads files or directories recursively to the storage system
	 *
	 * @param sourceFileOrDir source file or directory to upload
	 * @param targetFileOrDir target file or directory in storage
	 * @return path list of uploaded files
	 * @throws IOException if files cannot be uploaded
	 */
	public List<String> upload(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},{})", sourceFileOrDir, targetFileOrDir);

		List<String> uploadedFiles = new ArrayList<>();

		if (isFile(sourceFileOrDir)) {
			String uploadedFile = uploadFile(sourceFileOrDir, targetFileOrDir);
			uploadedFiles.add(uploadedFile);
			return uploadedFiles;
		}

		String sourceDir = sourceFileOrDir;
		String targetDir = targetFileOrDir;
		targetDir = new PathConverter(targetDir).addSlashAtEnd().getPath();
		File directory = new File(sourceDir);
		File[] files = directory.listFiles();
		if (files == null)
			return uploadedFiles;
		Arrays.sort(files);

		for (File file : files) {
			if (file.isFile()) {
				String sourceFile = file.getAbsolutePath();
				String uploadedFile = uploadFile(sourceFile, targetDir);
				uploadedFiles.add(uploadedFile);
			}
		}

		for (File file : files) {
			if (file.isDirectory()) {
				String sourceSubDir = file.getAbsolutePath();
				String targetSubDir = Paths.get(targetDir, file.getName()).toString();
				targetSubDir = new PathConverter(targetSubDir).addSlashAtEnd().getPath();
				List<String> subDirUploadedFiles = upload(sourceSubDir, targetSubDir);
				uploadedFiles.addAll(subDirUploadedFiles);
			}
		}

		return uploadedFiles;
	}

	/**
	 * Downloads a file from the POSIX storage system to the specified target
	 * location
	 *
	 * @param sourceFile      source file in storage to download
	 * @param targetFileOrDir target file or directory
	 * @return path of downloaded file
	 * @throws IOException if file cannot be downloaded
	 */
	public String downloadFile(String sourceFile, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFile({},{})", sourceFile, targetFileOrDir);
		
		AtomicCommand<String> fileDownloader = new PosixAtomicFileDownloader(sourceFile, targetFileOrDir);

		return new DefaultRetryStrategy<>(fileDownloader, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Downloads files or directories recursively from the storage system
	 *
	 * @param sourceFileOrDir source file or directory in storage to download
	 * @param targetFileOrDir target file or directory
	 * @return path list of downloaded files
	 * @throws IOException true if file or directory cannot be downloaded
	 */
	public List<String> download(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},{})", sourceFileOrDir, targetFileOrDir);

		List<String> downloadedFiles = new ArrayList<>();

		if (isFile(sourceFileOrDir)) {
			String downloadedFile = downloadFile(sourceFileOrDir, targetFileOrDir);
			downloadedFiles.add(downloadedFile);
			return downloadedFiles;
		}

		String sourceDir = sourceFileOrDir;
		String targetDir = targetFileOrDir;
		targetDir = new PathConverter(targetDir).addSlashAtEnd().getPath();

		File directory = new File(sourceDir);
		File[] files = directory.listFiles();
		if (files == null)
			return downloadedFiles;
		Arrays.sort(files);

		for (File file : files) {
			if (file.isFile()) {
				String sourceFile = file.getAbsolutePath();
				String downloadedFile = downloadFile(sourceFile, targetDir);
				downloadedFiles.add(downloadedFile);
			}
		}

		for (File file : files) {
			if (file.isDirectory()) {

				String sourceSubDir = file.getAbsolutePath();
				String targetSubDir = Paths.get(targetDir, file.getName()).toString();
				targetSubDir = new PathConverter(targetSubDir).addSlashAtEnd().getPath();

				// String path = new PathConverter(targetSubDirPath).addSlashAtEnd().getPath();
				// targetSubDir.setRelativePath(path);

				List<String> subDirFiles = download(sourceSubDir, targetSubDir);
				downloadedFiles.addAll(subDirFiles);
			}
		}

		return downloadedFiles;
	}
	
	/**
	 * Copies a file from the POSIX file system to another posix file system (normally, to the cache)
	 * location
	 *
	 * @param sourceFile      posix source file 
	 * @param targetFileOrDir posix target file or directory
	 * @return path of downloaded file
	 * @throws IOException if file cannot be downloaded
	 */
	public String copyFile(String sourceFile, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> copyFile({},{})", sourceFile, targetFileOrDir);
		
		AtomicCommand<String> fileCopier = new PosixAtomicFileCopier(sourceFile, targetFileOrDir);

		return new DefaultRetryStrategy<>(fileCopier, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Copies files or directories recursively from the posix file system to another posix file system (normally, to the cache)
	 *
	 * @param sourceFileOrDir posix source file or directory 
	 * @param targetFileOrDir posix target file or directory
	 * @return path list of copied files
	 * @throws IOException true if file or directory cannot be copied
	 */
	public List<String> copy(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> copy({},{})", sourceFileOrDir, targetFileOrDir);

		List<String> copiedFiles = new ArrayList<>();

		if (isFile(sourceFileOrDir)) {
			String copiedFile = copyFile(sourceFileOrDir, targetFileOrDir);
			copiedFiles.add(copiedFile);
			return copiedFiles;
		}

		String sourceDir = sourceFileOrDir;
		String targetDir = targetFileOrDir;
		targetDir = new PathConverter(targetDir).addSlashAtEnd().getPath();

		File directory = new File(sourceDir);
		File[] files = directory.listFiles();
		if (files == null)
			return copiedFiles;
		Arrays.sort(files);

		for (File file : files) {
			if (file.isFile()) {
				String sourceFile = file.getAbsolutePath();
				String copiedFile = downloadFile(sourceFile, targetDir);
				copiedFiles.add(copiedFile);
			}
		}

		for (File file : files) {
			if (file.isDirectory()) {

				String sourceSubDir = file.getAbsolutePath();
				String targetSubDir = Paths.get(targetDir, file.getName()).toString();
				targetSubDir = new PathConverter(targetSubDir).addSlashAtEnd().getPath();

				// String path = new PathConverter(targetSubDirPath).addSlashAtEnd().getPath();
				// targetSubDir.setRelativePath(path);

				List<String> subDirFiles = copy(sourceSubDir, targetSubDir);
				copiedFiles.addAll(subDirFiles);
			}
		}

		return copiedFiles;
	}

	/**
	 * Deletes file in storage
	 *
	 * @param sourceFile file to delete in storage
	 * @return path of deleted file
	 * @throws IOException true if file cannot be deleted
	 */
	public String deleteFile(String sourceFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFile({})", sourceFile);

		return new FileUtils(sourceFile).deleteFile();
	}

	/**
	 * Deletes file or directory in storage
	 *
	 * @param sourceFileOrDir file or directory to delete
	 * @return path list of deleted file or directory
	 * @throws IOException true if file or directory cannot be deleted
	 */
	public List<String> delete(String sourceFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", sourceFileOrDir);

		return new FileUtils(sourceFileOrDir).delete();
	}
}