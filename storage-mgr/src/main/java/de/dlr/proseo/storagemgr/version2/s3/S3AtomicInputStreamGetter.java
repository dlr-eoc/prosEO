/**
 * S3AtomicInputStreamGetter.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * This class implements the AtomicCommand interface to get an input stream
 * of an S3 file. It uses the S3Client from the AWS SDK to retrieve the input
 * stream from the specified bucket and file key. The class also provides
 * information about the execution of the atomic command.
 *
 * @author Denys Chaykovskiy
 */
public class S3AtomicInputStreamGetter implements AtomicCommand<InputStream> {

	/** Info */
	private static final String INFO = "S3 ATOMIC Input Stream Getter";

	/** Completed Info */
	private static final String COMPLETED = "input file stream GOT";

	/** Failed Info */
	private static final String FAILED = "input file stream FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicInputStreamGetter.class);

	/** Source file path */
	private String relativePath;

	/** S3 Client */
	private S3Client s3Client;

	/** Bucket */
	private String bucket;

	/**
	 * Constructor
	 *
	 * @param s3Client     S3 client
	 * @param bucket       Bucket
	 * @param relativePath Relative path of the source file
	 */
	public S3AtomicInputStreamGetter(S3Client s3Client, String bucket, String relativePath) {
		this.s3Client = s3Client;
		this.bucket = bucket;
		this.relativePath = relativePath;
	}

	/**
	 * Executes the input stream getter command and returns the input stream of the
	 * S3 file.
	 *
	 * @return InputStream of the S3 file
	 * @throws IOException if an I/O error occurs during the execution
	 */
	@Override
	public InputStream execute() throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - inputStreamGetter({})", relativePath);

		String sourceS3File = new PathConverter(relativePath).posixToS3Path().convertToSlash().getPath();

		try {
			InputStream stream = getInputStream(sourceS3File);

			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - " + sourceS3File);

			return stream;
		} catch (IOException e) {
			if (logger.isTraceEnabled())
				logger.trace(getFailedInfo() + e.getMessage());
			throw new IOException(e);
		}
	}

	/**
	 * Gets information about the atomic command.
	 *
	 * @return information about the atomic command
	 */
	@Override
	public String getInfo() {
		return INFO + " ";
	}

	/**
	 * Gets information about the completed atomic command.
	 *
	 * @return information about the completed atomic command
	 */
	@Override
	public String getCompletedInfo() {
		return INFO + ": " + COMPLETED + " ";
	}

	/**
	 * Gets information about the failed atomic command.
	 *
	 * @return information about the failed atomic command
	 */
	@Override
	public String getFailedInfo() {
		return INFO + ": " + FAILED + " ";
	}

	/**
	 * Gets the input stream of the S3 file.
	 *
	 * @param s3RelativePath S3 relative path of the file
	 * @return InputStream of the S3 file
	 * @throws IOException if an I/O error occurs while retrieving the input stream
	 */
	private InputStream getInputStream(String s3RelativePath) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getInputStream({}, {})", bucket, s3RelativePath);

		try {
			InputStream stream = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(s3RelativePath).build(),
					ResponseTransformer.toInputStream());

			if (stream == null)
				throw new IOException("Cannot create input stream for S3 file: " + s3RelativePath);

			return stream;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IOException(e);
		}
	}
}
