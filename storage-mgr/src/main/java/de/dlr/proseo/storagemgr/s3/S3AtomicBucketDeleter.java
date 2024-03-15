/**
 * S3AtomicBucketDeleter.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.model.AtomicCommand;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

/**
 * S3 Atomic Bucket Deleter
 *
 * @author Denys Chaykovskiy
 */
public class S3AtomicBucketDeleter implements AtomicCommand<String> {

	/** Info */
	private static final String INFO = "S3 ATOMIC Empty Bucket Deleter";

	/** Completed Info */
	private static final String COMPLETED = "Bucket DELETED";

	/** Failed Info */
	private static final String FAILED = "bucket deletion FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicBucketDeleter.class);

	/** S3 Client */
	private S3Client s3Client;

	/** Bucket */
	private String bucket;

	/**
	 * Constructor
	 *
	 * @param s3Client s3 client
	 * @param bucket   bucket
	 */
	public S3AtomicBucketDeleter(S3Client s3Client, String bucket) {

		this.s3Client = s3Client;
		this.bucket = bucket;
	}

	/**
	 * Executes s3 bucket delete
	 *
	 * @return deleted bucket name
	 */
	@Override
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - delete empty bucket ({})", bucket);

		try {
			DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
			s3Client.deleteBucket(deleteBucketRequest);

			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - " + bucket);

			return bucket;

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
}