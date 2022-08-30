package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * S3 Atomic File Content Getter
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicFileContentGetter implements AtomicCommand<String> {
	
	/** Info */
	private static final String INFO = "S3 ATOMIC File Content Getter";
	
	/** Completed Info */
	private static final String COMPLETED = "file content GOT";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileContentGetter.class);
	
	/** path */
	private String path; 
	
	/** S3 Client */
	private S3Client s3Client;

	/** Bucket */
	private String bucket;
	
	/**
	 * Constructor 
	 * 
	 * @param s3Client s3 client
	 * @param bucket bucket
	 * @param path path
	 */
	public S3AtomicFileContentGetter(S3Client s3Client, String bucket, String path) {
		
		this.s3Client = s3Client; 
		this.bucket = bucket; 
		this.path = path; 
	}
	
	/**
	 * Gets if file exists
	 * 
	 * @return "true" if file exists  
	 */
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - file content({})", path);

		try {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(path).build();

			ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);

			// InputStream stream = new
			// ByteArrayInputStream(responseInputStream.readAllBytes());
			
			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo());

			return new String(responseInputStream.readAllBytes(), StandardCharsets.UTF_8);

		} catch (Exception e) {
			if (logger.isTraceEnabled())
				logger.trace(e.getMessage());
			throw new IOException(e);
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
}
