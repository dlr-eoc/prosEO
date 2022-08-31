package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * S3 Atomic File Exists Getter
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicFileExistsGetter implements AtomicCommand<String> {

	/** Info */
	private static final String INFO = "S3 ATOMIC File Exists Getter";

	/** Completed Info */
	private static final String COMPLETED = "file existence GOT";
	
	/** Failed Info */
	private static final String FAILED = "file existence verification FAILED";

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
	 * @param bucket   bucket
	 * @param path     path
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
			s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(path).build());
			
			if (logger.isTraceEnabled())
				logger.trace(">>>>> " + getCompletedInfo() + " - exists");
			
			return String.valueOf(true);

		} catch (NoSuchKeyException e) {
			
			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - not exists");
			
			return String.valueOf(false);

		} catch (Exception e) {
			if (logger.isTraceEnabled())
				logger.trace(getFailedInfo() + e.getMessage());
			throw new IOException(e);
		}
	}
	
	/**
	 * Gets Information about atomic command (mostly for logs)
	 * 
	 * @return Information about atomic command
	 */
	public String getInfo() {	
		return INFO + " ";
	}
	
	/**
	 * Gets Information about completed atomic command (mostly for logs)
	 * 
	 * @return Information about completed atomic command
	 */
	public String getCompletedInfo() {	
		return INFO + ": " + COMPLETED + " ";
	}
	
	/**
	 * Gets Information about failed atomic command (mostly for logs)
	 * 
	 * @return Information about failed atomic command
	 */
	public String getFailedInfo() {
		return INFO + ": " + FAILED + " ";
	}	
}
