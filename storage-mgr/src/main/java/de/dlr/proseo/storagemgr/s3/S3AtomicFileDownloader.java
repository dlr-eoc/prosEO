/**
 * S3AtomicFileDownloader.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

import de.dlr.proseo.storagemgr.model.AtomicCommand;
import de.dlr.proseo.storagemgr.version2.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;

/**
 * S3 Atomic File Downloader
 *
 * @author Denys Chaykovskiy
 */
public class S3AtomicFileDownloader implements AtomicCommand<String> {

	/** Info */
	private static final String INFO = "S3 ATOMIC File Downloader";

	/** Completed Info */
	private static final String COMPLETED = "file DOWNLOADED";

	/** Failed Info */
	private static final String FAILED = "file download FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileDownloader.class);

	/** Prefix for temporary files */
	private static final String TEMPORARY_PREFIX = "temporary-";

	/** Chunk size for downloads to S3 storage (128 MB) */
	private static final Long MULTIPART_DOWNLOAD_PARTSIZE_BYTES = (long) (128 * 1024 * 1024);

	/** source file */
	private String sourceFile;

	/** target file or dir */
	private String targetFileOrDir;

	/** S3 Client v1 */
	private AmazonS3 s3ClientV1;

	/** Bucket */
	private String bucket;

	/** Max cycles */
	private long maxCycles;

	/** Wait time */
	private long waitTime;

	/**
	 * Constructor
	 *
	 * @param s3ClientV1      s3 client v1
	 * @param bucket          bucket
	 * @param sourceFile      sourceFile
	 * @param targetFileOrDir target file or directory
	 * @param maxCycles		  max cycles
	 * @param waitTime		  wait time
	 */
	public S3AtomicFileDownloader(AmazonS3 s3ClientV1, String bucket, String sourceFile, String targetFileOrDir, long maxCycles,
			long waitTime) {

		this.s3ClientV1 = s3ClientV1;

		this.bucket = bucket;
		this.sourceFile = sourceFile;
		this.targetFileOrDir = targetFileOrDir;

		this.maxCycles = maxCycles;
		this.waitTime = waitTime;
	}

	/**
	 * Executes download of the file to s3
	 *
	 * @return downloaded file name
	 */
	@Override
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - downloadFile({},{})", sourceFile, targetFileOrDir);

		String sourceS3File = new PathConverter(sourceFile).posixToS3Path().convertToSlash().getPath();
		String targetPosixFile = targetFileOrDir;

		if (new PathConverter(targetFileOrDir).isDirectory()) {
			targetPosixFile = Paths.get(targetFileOrDir, getFileName(sourceS3File)).toString();
		}

		targetPosixFile = new PathConverter(targetPosixFile).s3ToPosixPath().convertToSlash().getPath();
		new FileUtils(targetPosixFile).createParentDirectories();

		// temporary download filename, after download will be renamed
		String temporaryTargetPosixFile = getTemporaryFilePath(targetPosixFile);
		try {
			downloadWithTransferManagerV1(sourceS3File, temporaryTargetPosixFile);

			// rename temporary file if the file downloaded successfully to cache
			if (!renameFile(temporaryTargetPosixFile, targetPosixFile)) {
				throw new IOException(
						"Cannot rename file after download to cache " + temporaryTargetPosixFile + " to " + targetPosixFile);
			}

			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - " + targetPosixFile);

			return targetPosixFile;

		} catch (IOException e) {
			if (logger.isTraceEnabled())
				logger.trace(getFailedInfo() + e.getMessage());
			new File(temporaryTargetPosixFile).delete();
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

	/**
	 * Gets temporary file path
	 *
	 * @param path path
	 * @return temporary file path
	 */
	private String getTemporaryFilePath(String path) {

		String filename = TEMPORARY_PREFIX + new File(path).getName();

		return new PathConverter(new File(path).getParent(), filename).getPath();
	}

	/**
	 * Renames file
	 *
	 * @param oldPath old path
	 * @param newPath new path
	 * @return true if the file has been renamed successfully
	 */
	private boolean renameFile(String oldName, String newName) {

		if (logger.isTraceEnabled())
			logger.trace(">>> renameFile({}, {})", oldName, newName);

		File oldFile = new File(oldName);
		File newFile = new File(newName);

		if (newFile.exists())
			newFile.delete();

		return oldFile.renameTo(newFile);
	}

	private void downloadWithTransferManagerV1(String s3Key, String targetPath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> downloadWithTransferManagerV1({}, {}, {})", bucket, s3Key, targetPath);

		File targetFile = new File(targetPath);

		// Download using TransferManager as per
		// https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-s3-transfermanager.html#transfermanager-downloading
		TransferManager transferManager;
		try {
			if (targetFile.exists()) {
				targetFile.delete();
			}

			transferManager = TransferManagerBuilder.standard()
				.withMultipartCopyPartSize(MULTIPART_DOWNLOAD_PARTSIZE_BYTES)
				.withS3Client(s3ClientV1)
				.build();

			if (null == transferManager) {
				throw new IOException("Unable to create Transfer Manager");
			}

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			throw new IOException(e);
		}

		try {
			Download download = transferManager.download(bucket, s3Key, targetFile);

			download.waitForCompletion();

			// TODO This may not apply to the TransferManager any more (it did for V2
			// getObject() and Files.copy())
			// Unfortunately returning from waitForCompletion() may not mean the file is
			// fully written to disk!
			Long contentLength = download.getObjectMetadata().getContentLength();
			int i = 0;

			while (Files.size(targetFile.toPath()) < contentLength && i < maxCycles) {
				logger.info("... waiting to complete writing of {}", targetFile);
				Thread.sleep(waitTime);
			}

			if (maxCycles <= i) {
				throw new IOException("Read timed out after " + (maxCycles * waitTime) + " ms");
			}

		} catch (InterruptedException e) {
			logger.error("Interrupted while copying S3 object s3:/{}/{} to file {} (cause: {})", bucket, s3Key, targetFile,
					e.getMessage());
			throw new IOException(e);

		} catch (AmazonServiceException e) {
			logger.error("Failed to copy S3 object s3:/{}/{} to file {} (cause: {})", bucket, s3Key, targetFile,
					e.getErrorMessage());
			throw new IOException(e);

		} catch (IOException | AmazonClientException e) {
			logger.error("Failed to copy S3 object s3:/{}/{} to file {} (cause: {})", bucket, s3Key, targetFile, e.getMessage());
			throw new IOException(e);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IOException(e);

		} finally {
			transferManager.shutdownNow(false);
		}
	}
}