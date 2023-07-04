/**
 * S3AtomicFileExistsGetter.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
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
	@Override
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