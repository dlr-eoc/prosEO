package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.s3.model.NoSuchKeyException;

import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

/**
 * S3 Atomic File Exists Getter
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicFileExistsGetter implements AtomicCommand {
	
	/** Info */
	private static final String INFO = "S3 ATOMIC File Exists Getter";
	
	/** Completed Info */
	private static final String COMPLETED = "GOT";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileExistsGetter.class);
	

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
	public S3AtomicFileExistsGetter(S3Client s3Client, String bucket, String path) {
		
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
			logger.trace(">>> execute() - file exists({})", path);

		try {
			
			try {
				s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(path).build());
				return String.valueOf(true); 
			} catch (NoSuchKeyException e) {
				return String.valueOf(false); 
			}

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
}
