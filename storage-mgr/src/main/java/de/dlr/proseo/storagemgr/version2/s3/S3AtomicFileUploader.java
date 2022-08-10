package de.dlr.proseo.storagemgr.version2.s3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/**
 * S3 Atomic Uploader
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicFileUploader implements AtomicCommand {
	
	/** Info */
	private static final String INFO = "S3 ATOMIC File Uploader";
	
	/** Completed Info */
	private static final String COMPLETED = "UPLOADED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileUploader.class);
	
	/** source file */
	private String sourceFile; 
	
	/** target file or dir */
	private String targetFileOrDir; 
	
	/** S3 Client */
	private S3Client s3Client;

	/** Bucket */
	private String bucket;
	
	/**
	 * Constructor
	 * 
	 * @param s3Client s3 client
	 * @param bucket bucket
	 * @param sourceFile sourceFile
	 * @param targetFileOrDir target file or directory
	 */
	public S3AtomicFileUploader(S3Client s3Client, String bucket, String sourceFile, String targetFileOrDir) {
		
		this.s3Client = s3Client; 
		this.bucket = bucket; 
		this.sourceFile = sourceFile; 
		this.targetFileOrDir = targetFileOrDir;  
	}
	
	/**
	 * Executes upload of the file to s3 
	 * 
	 * @return uploaded file name 
	 */
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - uploadFile({},{})", sourceFile, targetFileOrDir);

		String targetFile = targetFileOrDir;

		if (new PathConverter(targetFileOrDir).isDirectory()) {
			targetFile = Paths.get(targetFileOrDir, getFileName(sourceFile)).toString();
			targetFile = new PathConverter(targetFile).posixToS3Path().convertToSlash().getPath();
		}

		try {
			PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(targetFile).build();

			s3Client.putObject(request, RequestBody.fromFile(new File(sourceFile)));

			S3Waiter waiter = s3Client.waiter();
			HeadObjectRequest requestWait = HeadObjectRequest.builder().bucket(bucket).key(targetFile).build();

			WaiterResponse<HeadObjectResponse> waiterResponse = waiter.waitUntilObjectExists(requestWait);
			waiterResponse.matched().response().ifPresent(System.out::println);

			if (logger.isTraceEnabled())
				logger.trace(">>>>> " + getCompletedInfo() + " - " + targetFile);

			return targetFile;

		} catch (Exception e) {
			logger.warn(e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Gets Information about atomic command (mostly for logs)
	 * 
	 * @return Information about atomic command
	 */
	public String getInfo() {	
		return INFO;
	}
	
	/**
	 * Gets Information about completed atomic command (mostly for logs)
	 * 
	 * @return Information about completed atomic command
	 */
	public String getCompletedInfo() {	
		return INFO + ": " + COMPLETED;
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
