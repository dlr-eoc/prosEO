package de.dlr.proseo.storagemgr.version2.posix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;

/**
 * Posix Data Access Layer
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PosixDAL {

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(PosixDAL.class);

	/**
	 * Gets files which match path (prefix)
	 * 
	 * @param path path (prefix)
	 * @return list if files
	 */
	public List<String> getFiles(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", path);

		List<String> returnFiles = new ArrayList<String>();

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
	 * Gets file content
	 * 
	 * @param storageFile storage file
	 * @return file content
	 */
	public String getFileContent(String path) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFileContent({})", path);

		return Files.readString(Path.of(path));
	}

	/**
	 * Uploads file to posix storage
	 * 
	 * @param sourceFile      source file to upload
	 * @param targetFileOrDir target file or directory in storage
	 * @return path of uploaded file
	 * @throws IOException if file cannot be uploaded
	 */
	public String uploadFile(String sourceFile, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({},{})", sourceFile, targetFileOrDir);

		String targetFile = targetFileOrDir;

		if (new PathConverter(targetFileOrDir).isDirectory()) {
			targetFile = new PathConverter(targetFileOrDir, getFileName(sourceFile)).getPath();
		}

		createParentDirectories(targetFile);

		Path sourceFilePath = new File(sourceFile).toPath();
		Path targetFilePath = new File(targetFile).toPath();

		try {
			Path copiedPath = Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			return copiedPath.toString();

		} catch (IOException e) {
			e.printStackTrace();
			if (logger.isTraceEnabled())
				logger.error("Cannot upload file from " + sourceFile + " to " + targetFileOrDir + " ", e.getMessage());
			throw e;
		}
	}

	/**
	 * Uploads file or directory to storage
	 * 
	 * @param sourceFileOrDir source file or directory to upload
	 * @param targetFileOrDir target file or directory in storage
	 * @return path list of uploaded files
	 * @throws IOException if files cannot be uploaded
	 */
	public List<String> upload(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},{})", sourceFileOrDir, targetFileOrDir);

		List<String> uploadedFiles = new ArrayList<String>();

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
	 * Downloads file from storage
	 * 
	 * @param sourceFile      source file in storage to download
	 * @param targetFileOrDir target file or directory
	 * @return path of downloaded file
	 * @throws IOException if file cannot be downloaded
	 */
	public String downloadFile(String sourceFile, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFile({},{})", sourceFile, targetFileOrDir);

		String targetFile = targetFileOrDir;

		if (new PathConverter(targetFileOrDir).isDirectory()) {
			targetFile = new PathConverter(targetFileOrDir, getFileName(sourceFile)).getPath();
		}

		createParentDirectories(targetFile);

		Path sourceFilePath = new File(sourceFile).toPath();
		Path targetFilePath = new File(targetFile).toPath();

		try {
			Path copiedPath = Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			return copiedPath.toString();

		} catch (Exception e) {
			e.printStackTrace();
			if (logger.isTraceEnabled())
				logger.error("Cannot download file/folder from " + sourceFile + " to " + targetFileOrDir + " ",
						e.getMessage());
			throw e;
		}
	}

	/**
	 * Downloads file or directory from storage
	 * 
	 * @param sourceFileOrDir source file or directory in storage to download
	 * @param targetFileOrDir target file or directory
	 * @return path list of downloaded files
	 * @throws IOException true if file or directory cannot be downloaded
	 */
	public List<String> download(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},{})", sourceFileOrDir, targetFileOrDir);

		List<String> downloadedFiles = new ArrayList<String>();

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

	/**
	 * Creates parent directories
	 * 
	 * @param path path
	 */
	private void createParentDirectories(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", path);

		File targetFile = new File(path);
		File parent = targetFile.getParentFile();

		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IllegalStateException("Couldn't create dir: " + parent);
		}
	}
}
