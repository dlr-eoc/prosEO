package de.dlr.proseo.storagemgr.version2.s3;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * S3 Atomic File Downloader
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicFileDownloader implements AtomicCommand {
	
	/** Info */
	private static final String INFO = "S3 ATOMIC File Downloader";
	
	/** Completed Info */
	private static final String COMPLETED = "DOWNLOADED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileDownloader.class);
	
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
	public S3AtomicFileDownloader(S3Client s3Client, String bucket, String sourceFile, String targetFileOrDir) {
		
		this.s3Client = s3Client; 
		this.bucket = bucket; 
		this.sourceFile = sourceFile; 
		this.targetFileOrDir = targetFileOrDir;  
	}
	
	/**
	 * Executes download of the file to s3 
	 * 
	 * @return downloaded file name 
	 */
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

		GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(sourceS3File).build();
		ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
		BufferedOutputStream outputStream;

		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(targetPosixFile));
			byte[] buffer = new byte[4096];
			int bytesRead = -1;

			while ((bytesRead = response.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			response.close();
			outputStream.close();
			
			if (logger.isTraceEnabled())
				logger.trace(">>>>> " + getCompletedInfo() + " - " + targetPosixFile);

			return targetPosixFile;

		} catch (IOException e) {
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
