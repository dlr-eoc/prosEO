/**
 * S3AtomicSynchroFileUploader.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

import de.dlr.proseo.storagemgr.Exceptions.FileLockedAfterMaxCyclesException;
import de.dlr.proseo.storagemgr.model.AtomicCommand;
import de.dlr.proseo.storagemgr.utils.PathConverter;
import de.dlr.proseo.storagemgr.utils.StorageFileLocker;

/**
 * S3 Atomic Synchro Uploader
 *
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicSynchroFileUploader implements AtomicCommand<String> {

	/** Info */
	private static final String INFO = "S3 ATOMIC Synchro File Uploader";

	/** Completed Info */
	private static final String COMPLETED = "file UPLOADED";

	/** Failed Info */
	private static final String FAILED = "file upload FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicSynchroFileUploader.class);

	/** Chunk size for uploads to S3 storage (128 MB) */
	private static final Long MULTIPART_UPLOAD_PARTSIZE_BYTES = (long) (128 * 1024 * 1024);

	/** source file */
	private String sourceFile;

	/** target file or dir */
	private String targetFileOrDir;

	/** S3 Client */
	private AmazonS3 s3ClientV1;

	/** Bucket */
	private String bucket;
	
	/** S3 Configuration */
	private S3Configuration cfg;
	

	/**
	 * Constructor
	 *
	 * @param s3ClientV1      s3 client
	 * @param bucket          bucket
	 * @param sourceFile      sourceFile
	 * @param targetFileOrDir target file or directory
	 * @param cfg			  s3 Configuration

	 */
	public S3AtomicSynchroFileUploader(AmazonS3 s3ClientV1, String bucket, String sourceFile, String targetFileOrDir, S3Configuration cfg) {

		this.s3ClientV1 = s3ClientV1;
		this.bucket = bucket;
		this.sourceFile = sourceFile;
		this.targetFileOrDir = targetFileOrDir;
		this.cfg = cfg; 
	}

	/**
	 * Executes synchro upload of the file to s3
	 *
	 * @return uploaded file name
	 */
	@Override
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - synchroUploadFile({},{})", sourceFile, targetFileOrDir);

		String targetFile = targetFileOrDir;

		if (new PathConverter(targetFileOrDir).isDirectory()) {
			targetFile = Paths.get(targetFileOrDir, getFileName(sourceFile)).toString();
			targetFile = new PathConverter(targetFile).posixToS3Path().convertToSlash().getPath();
		}

		File f = new File(sourceFile);

		if (f == null || !f.isFile()) {
			throw new IOException("Cannot upload to s3, source file does not exist: " + sourceFile);
		}

		StorageFileLocker fileLocker = new StorageFileLocker(targetFile, cfg.getFileCheckWaitTime(),
					cfg.getMaxRequestAttempts());
		
		try {
			
			fileLocker.lockOrWaitUntilUnlockedAndLock();
			return uploadWithTranferManager(sourceFile, targetFile);

		} catch (FileLockedAfterMaxCyclesException e) {
			
			logger.debug("... the file is locked after max check cycles: ", targetFile, " ", e.getMessage());
			throw new IOException(e);
			
		} catch (InterruptedException e) {
			
			logger.debug("... the file is locked after max check cycles: ", targetFile, " ", e.getMessage());
			throw new IOException(e);
		
		} finally {

			fileLocker.unlock();
			logger.debug("... unlocked the file: ", targetFile);
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
	 * @param sourceFile     source file 
	 * @param targetFile	 target file
	 * @return				 uploaded file (target file)
	 * @throws IOException   if file uploading was not successful 
	 */
	private String uploadWithTranferManager(String sourceFile, String targetFile) throws IOException {
		
		TransferManager transferManager;
		
		try {
			transferManager = TransferManagerBuilder.standard()
				.withMultipartCopyPartSize(MULTIPART_UPLOAD_PARTSIZE_BYTES)
				.withS3Client(s3ClientV1)
				.build();

		} catch (Exception e) {
			if (logger.isTraceEnabled())
				logger.trace(getFailedInfo() + e.getMessage());
			throw new IOException(e);
		}

		try {
			transferManager.upload(bucket, targetFile, new File(sourceFile)).waitForCompletion();

			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - " + targetFile);

			return targetFile;

		} catch (Exception e) {
			if (logger.isTraceEnabled())
				logger.trace(getFailedInfo() + e.getMessage());
			throw new IOException(e);
		} finally {
			transferManager.shutdownNow(false);
		}		
	}
}