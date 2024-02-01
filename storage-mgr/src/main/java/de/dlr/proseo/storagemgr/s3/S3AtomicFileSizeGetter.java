/**
 * S3AtomicFileSizeGetter.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.model.AtomicCommand;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * S3 Atomic File Size Getter
 *
 * @author Denys Chaykovskiy
 */
public class S3AtomicFileSizeGetter implements AtomicCommand<String> {

	/** Info */
	private static final String INFO = "S3 ATOMIC File Size Getter";

	/** Completed Info */
	private static final String COMPLETED = "file size GOT";

	/** Failed Info */
	private static final String FAILED = "file size receiving FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileSizeGetter.class);

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
	public S3AtomicFileSizeGetter(S3Client s3Client, String bucket, String path) {

		this.s3Client = s3Client;
		this.bucket = bucket;
		this.path = path;
	}

	/**
	 * Gets if file exists
	 *
	 * @return "true" if file exists
	 */
	@Override
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - file size({})", path);

		try {
			HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucket).key(path).build();
			HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

			Long fileSize = headObjectResponse.contentLength();

			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - size: " + fileSize.toString());

			return fileSize.toString();

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