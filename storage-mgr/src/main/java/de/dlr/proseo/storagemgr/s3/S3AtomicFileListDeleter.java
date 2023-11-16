/**
 * S3AtomicFileListDeleter.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.model.AtomicCommand;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * S3 Atomic File List Deleter
 *
 * @author Denys Chaykovskiy
 */
public class S3AtomicFileListDeleter implements AtomicCommand<List<String>> {

	/** Info */
	private static final String INFO = "S3 ATOMIC File List Deleter";

	/** Completed Info */
	private static final String COMPLETED = "file list DELETED";

	/** Failed Info */
	private static final String FAILED = "file list deletion FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileListDeleter.class);

	/** path */
	private String directory;

	/** S3 Client */
	private S3Client s3Client;

	/** Bucket */
	private String bucket;

	/**
	 * Constructor
	 *
	 * @param s3Client  s3 client
	 * @param bucket    bucket
	 * @param directory directory
	 */
	public S3AtomicFileListDeleter(S3Client s3Client, String bucket, String directory) {

		this.s3Client = s3Client;
		this.bucket = bucket;
		this.directory = directory;
	}

	/**
	 * Deletes a list of files
	 *
	 * @return list of deleted files
	 */
	@Override
	public List<String> execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - delete file list({})", directory);

		try {
			ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).prefix(directory).build();
			ListObjectsV2Iterable list = s3Client.listObjectsV2Paginator(request);

			List<ObjectIdentifier> objectIdentifiers = list.stream()
				.flatMap(r -> r.contents().stream())
				.map(o -> ObjectIdentifier.builder().key(o.key()).build())
				.collect(Collectors.toList());

			if (objectIdentifiers.isEmpty())
				return new ArrayList<>();

			DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
				.bucket(bucket)
				.delete(Delete.builder().objects(objectIdentifiers).build())
				.build();

			DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(deleteObjectsRequest);

			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - amount: " + deleteResponse.deleted().size());

			return toStringDeletedObjects(deleteResponse.deleted());

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
	 * Converts deleted object file list to deleted file string list
	 *
	 * @param files deleted object file list
	 * @return deleted file string list
	 */
	private List<String> toStringDeletedObjects(List<DeletedObject> files) {

		List<String> fileNames = new ArrayList<>();

		for (DeletedObject f : files) {
			fileNames.add(f.key());
		}

		return fileNames;
	}
}