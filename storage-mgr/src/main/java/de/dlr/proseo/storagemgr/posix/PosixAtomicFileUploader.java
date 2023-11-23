/**
 * PosixAtomicFileUploader.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.posix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.model.AtomicCommand;
import de.dlr.proseo.storagemgr.utils.FileUtils;
import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * Posix Atomic Uploader
 *
 * @author Denys Chaykovskiy
 *
 */
public class PosixAtomicFileUploader implements AtomicCommand<String> {

	/** Info */
	private static final String INFO = "Posix ATOMIC File Uploader";

	/** Completed Info */
	private static final String COMPLETED = "file UPLOADED";

	/** Failed Info */
	private static final String FAILED = "file upload FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(PosixAtomicFileUploader.class);

	/** source file */
	private String sourceFile;

	/** target file or dir */
	private String targetFileOrDir;

	/**
	 * Constructor
	 *
	 * @param sourceFile      sourceFile
	 * @param targetFileOrDir target file or directory
	 */
	public PosixAtomicFileUploader(String sourceFile, String targetFileOrDir) {

		this.sourceFile = sourceFile;
		this.targetFileOrDir = targetFileOrDir;
	}

	/**
	 * Executes upload of the file to posix
	 *
	 * @return uploaded file name
	 */
	@Override
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - uploadFile({},{})", sourceFile, targetFileOrDir);

		String targetFile = targetFileOrDir;

		if (new PathConverter(targetFileOrDir).isDirectory()) {
			targetFile = new PathConverter(targetFileOrDir, getFileName(sourceFile)).getPath();
		}

		new FileUtils(targetFile).createParentDirectories();

		Path sourceFilePath = new File(sourceFile).toPath();
		Path targetFilePath = new File(targetFile).toPath();

		try {
			Path copiedPath = Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			return copiedPath.toString();

		} catch (Exception e) {
			if (logger.isTraceEnabled())
				logger.trace(getFailedInfo() + e.getMessage());
			throw new IOException(e);
		}
	}

	/**
	 * Gets information about atomic command (mostly for logs)
	 *
	 * @return information about atomic command
	 */
	@Override
	public String getInfo() {
		return INFO + " ";
	}

	/**
	 * Gets information about completed atomic command (mostly for logs)
	 *
	 * @return information about completed atomic command
	 */
	@Override
	public String getCompletedInfo() {
		return INFO + ": " + COMPLETED + " ";
	}

	/**
	 * Gets information about failed atomic command (mostly for logs)
	 *
	 * @return information about failed atomic command
	 */
	@Override
	public String getFailedInfo() {
		return INFO + ": " + FAILED + " ";
	}

	/**
	 * Gets file name
	 *
	 * @param path path
	 * @return file name
	 */
	private String getFileName(String path) {
		return new File(path).getName();
	}
}