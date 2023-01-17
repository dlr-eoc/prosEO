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
 * S3 Atomic Input Stream Getter
 * 
 * @author Denys Chaykovskiy
 *
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

	/** source file */
	private String relativePath;

	/** S3 Client */
	private S3Client s3Client;

	/** Bucket */
	private String bucket;

	/**
	 * Constructor
	 * 
	 * @param s3Client           s3 client
	 * @param bucket             bucket
	 * @param relativePath         sourceFile
	 * @param targetFileOrDir    target file or directory
	 * @param fromByte           from byte
	 * @param toByte             toByte
	 * @param maxRequestAttempts max request attempts
	 * @param waitTime           waitTime
	 */
	public S3AtomicInputStreamGetter(S3Client s3Client, String bucket, String relativePath) {

		this.s3Client = s3Client;
		this.bucket = bucket;
		this.relativePath = relativePath;
	}

	/**
	 * Executes input stream getter
	 * 
	 * @return input stream of s3 file
	 */
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

	/**
	 * Gets input stream
	 * 
	 * @param s3RelativePath      s3 relative Path
	 * @param targetPath target path
	 * @throws IOException IO Exception
	 */
	private InputStream getInputStream(String s3RelativePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getInputStream({}, {}, {})", bucket, s3RelativePath);

		try {
			InputStream stream = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(s3RelativePath).build(),
					ResponseTransformer.toInputStream());

			if (stream == null)
				throw new IOException("Cannot create input stream for s3 file: " + s3RelativePath);

			return stream;

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			throw new IOException(e);
		}
	}
}
