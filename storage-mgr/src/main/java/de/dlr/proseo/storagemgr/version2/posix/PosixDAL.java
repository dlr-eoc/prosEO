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

import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import de.dlr.proseo.storagemgr.version2.s3.S3DataAccessLayer;

/**
 * Posix Data Access Layer
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PosixDAL {

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(PosixDAL.class);

	public String getFileName(String path) {
		return new File(path).getName();
	}

	public boolean fileExists(String path) {
		return new File(path).isFile();
	}

	public boolean isDirectory(String path) {
		return new File(path).isDirectory();
	}

	public boolean isFile(String path) {
		return new File(path).isFile();
	}

	public String uploadFile(String sourceFile, String targetFileOrDir) throws IOException {

		createParentDirectories(targetFileOrDir);

		Path sourceFilePath = new File(sourceFile).toPath();

		// if targetDir, takes filename from sourceFile automatically, if targetFile,
		// takes name from sourceFile
		Path targetFileOrDirPath = new File(targetFileOrDir).toPath();
		
		if (targetFileOrDir.endsWith("/") || targetFileOrDir.endsWith("\\")) { 
			
			
			
		}
		

		try {
			Path copiedPath = Files.copy(sourceFilePath, targetFileOrDirPath, StandardCopyOption.REPLACE_EXISTING);
			return copiedPath.toString();

		} catch (IOException e) {
			e.printStackTrace();
			if (logger.isTraceEnabled())
				logger.error("Cannot upload file from " + sourceFile + " to " + targetFileOrDir + " ", e.getMessage());
			throw e;
		}
	}

	public List<String> upload(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},())", sourceFileOrDir, targetFileOrDir);

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

	public String downloadFile(String sourceFile, String targetFileOrDir) throws IOException {

		createParentDirectories(targetFileOrDir);

		String targetPath = targetFileOrDir;

		if (isDirectory(targetFileOrDir)) {
			targetPath = Paths.get(targetPath, getFileName(sourceFile)).toString();
			createParentDirectories(targetPath);
		}

		Path sourceFilePath = new File(sourceFile).toPath();

		// if targetDir, takes filename from sourceFile automatically,
		// if targetFile, takes name from sourceFile
		Path targetFileOrDirPath = new File(targetPath).toPath();

		try {
			Path copiedPath = Files.copy(sourceFilePath, targetFileOrDirPath, StandardCopyOption.REPLACE_EXISTING);
			return copiedPath.toString();

		} catch (Exception e) {
			e.printStackTrace();
			if (logger.isTraceEnabled())
				logger.error("Cannot download file/folder from " + sourceFile + " to " + targetFileOrDir + " ",
						e.getMessage());
			throw e;
		}
	}

	public List<String> download(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},())", sourceFileOrDir, targetFileOrDir);

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

	public String deleteFile(String filepath) throws IOException {

		try {
			boolean fileDeleted = new File(filepath).delete();

			if (fileDeleted) {
				return filepath;
			} else {
				throw new IOException("Cannot delete file: " + filepath);
			}

		} catch (Exception e) {
			e.printStackTrace();
			if (logger.isTraceEnabled())
				logger.error("Cannot download file: " + filepath, e.getMessage());
			throw e;
		}
	}

	public List<String> delete(String sourceFileOrDir) throws IOException {

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

	private void createParentDirectories(String fullPath) {

		File targetFile = new File(fullPath);
		File parent = targetFile.getParentFile();

		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IllegalStateException("Couldn't create dir: " + parent);
		}
	}

}
