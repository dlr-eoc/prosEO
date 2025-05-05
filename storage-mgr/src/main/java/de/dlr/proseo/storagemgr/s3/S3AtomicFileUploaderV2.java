/**
 * S3AtomicFileUploaderV2.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.model.AtomicCommand;
import de.dlr.proseo.storagemgr.utils.PathConverter;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

/**
 * S3 Atomic Uploader
 *
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicFileUploaderV2 implements AtomicCommand<String> {

	/** Info */
	private static final String INFO = "S3 ATOMIC File Uploader";

	/** Completed Info */
	private static final String COMPLETED = "file UPLOADED";

	/** Failed Info */
	private static final String FAILED = "file upload FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileUploaderV2.class);

	/** source file */
	private String sourceFile;

	/** target file or dir */
	private String targetFileOrDir;

	/** Asynchronous S3 client for v2 */
	private S3AsyncClient s3AsyncClientV2;

	/** Bucket */
	private String bucket;

	/**
	 * Constructor
	 *
	 * @param s3AsyncClientV2 s3 client V2 (asynchronous)
	 * @param bucket          bucket
	 * @param sourceFile      sourceFile
	 * @param targetFileOrDir target file or directory
	 */
	public S3AtomicFileUploaderV2(S3AsyncClient s3AsyncClientV2, String bucket, String sourceFile, String targetFileOrDir) {

		this.s3AsyncClientV2 = s3AsyncClientV2;
		this.bucket = bucket;
		this.sourceFile = sourceFile;
		this.targetFileOrDir = targetFileOrDir;
	}

	/**
	 * Executes upload of the file to s3
	 *
	 * @return uploaded file name
	 */
	@Override
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - uploadFile({},{})", sourceFile, targetFileOrDir);

		String targetFile = targetFileOrDir;

		if (new PathConverter(targetFileOrDir).isDirectory()) {
			targetFile = Paths.get(targetFileOrDir, getFileName(sourceFile)).toString();
			targetFile = new PathConverter(targetFile).posixToS3Path().convertToSlash().getPath();
		}
		final String targetS3Key = targetFile;

		Path sourcePath = Paths.get(sourceFile);
		
		if (!Files.isRegularFile(sourcePath)) {
			throw new IOException("Cannot upload to s3, source file does not exist: " + sourceFile);
		}

		try (S3TransferManager transferManager = S3TransferManager.builder().s3Client(s3AsyncClientV2).build()) {

			UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
				.putObjectRequest(b -> b.bucket(bucket).key(targetS3Key))
				.source(sourcePath)
				.build();

			FileUpload fileUpload = transferManager.uploadFile(uploadFileRequest);

			fileUpload.completionFuture().join();
	            
			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - " + targetFile);

			return targetFile;

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