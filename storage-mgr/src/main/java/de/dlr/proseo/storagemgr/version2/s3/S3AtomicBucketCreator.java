package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/**
 * S3 Atomic Bucket Creator
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicBucketCreator implements AtomicCommand {

	/** Info */
	private static final String INFO = "S3 ATOMIC Bucket creator";

	/** Completed Info */
	private static final String COMPLETED = "bucket CREATED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicBucketCreator.class);

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
	public S3AtomicBucketCreator(S3Client s3Client, String bucket) {

		this.s3Client = s3Client;
		this.bucket = bucket;
	}

	/**
	 * Gets if file exists
	 * 
	 * @return "true" if file exists
	 */
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - bucket creator({})", bucket);

		try {
			S3Waiter s3Waiter = s3Client.waiter();
			CreateBucketRequest bucketRequest = CreateBucketRequest.builder().bucket(bucket).build();

			s3Client.createBucket(bucketRequest);
			HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder().bucket(bucket).build();

			// Wait until the bucket is created and print out the response
			WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
			waiterResponse.matched().response().ifPresent(System.out::println);

			if (logger.isTraceEnabled())
				logger.trace(">>>>> " + getCompletedInfo() + " - " + bucket);

			return bucket;

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
