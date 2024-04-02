/**
 * S3AtomicFileDeleter.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.model.AtomicCommand;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

/**
 * S3 Atomic File Getter
 *
 * @author Denys Chaykovskiy
 */
public class S3AtomicFileDeleter implements AtomicCommand<String> {

	/** Info */
	private static final String INFO = "S3 ATOMIC File Deleter";

	/** Completed Info */
	private static final String COMPLETED = "file DELETED";

	/** Failed Info */
	private static final String FAILED = "file deletion FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileDeleter.class);

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
	 * @param path     the path
	 */
	public S3AtomicFileDeleter(S3Client s3Client, String bucket, String path) {

		this.s3Client = s3Client;
		this.bucket = bucket;
		this.path = path;
	}

	/**
	 * Executes s3 get files
	 *
	 * @return uploaded file name
	 */
	@Override
	public String execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - delete file({})", path);

		try {
			ArrayList<ObjectIdentifier> toDelete = new ArrayList<>();
			toDelete.add(ObjectIdentifier.builder().key(path).build());

			DeleteObjectsRequest dor = DeleteObjectsRequest.builder()
				.bucket(bucket)
				.delete(Delete.builder().objects(toDelete).build())
				.build();
			s3Client.deleteObjects(dor);

			DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(dor);

			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - " + path);

			return toStringDeletedObject(deleteResponse.deleted());

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
	 * Converts deleted object file to deleted file string
	 *
	 * @param files files
	 * @return deleted storage file path
	 */
	private String toStringDeletedObject(List<DeletedObject> files) {

		List<String> fileNames = new ArrayList<>();

		for (DeletedObject f : files) {
			fileNames.add(f.key());
		}

		if (fileNames.size() > 1) {
			System.out.println("Expected 1 s3 object to delete. Deleted:" + fileNames.size());
		}

		return fileNames.get(0);
	}
}