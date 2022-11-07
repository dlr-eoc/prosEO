package de.dlr.proseo.storagemgr.version2.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;

import de.dlr.proseo.storagemgr.version2.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import de.dlr.proseo.storagemgr.version2.model.DefaultRetryStrategy;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * S3 Atomic Partial File Downloader
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicPartialFileDownloader implements AtomicCommand<HttpHeaders> {

	/** Info */
	private static final String INFO = "S3 ATOMIC Partial File Downloader";

	/** Completed Info */
	private static final String COMPLETED = "file Partial DOWNLOADED";

	/** Failed Info */
	private static final String FAILED = "partial file download FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicPartialFileDownloader.class);

	/** Prefix for temporary files */
	private static final String TEMPORARY_PREFIX = "temporary-";

	/** source file */
	private String sourceFile;

	/** target file or dir */
	private String targetFileOrDir;

	/** S3 Client */
	private S3Client s3Client;

	/** Bucket */
	private String bucket;

	/** From byte */
	private Long fromByte;

	/** To byte */
	private Long toByte;

	/** Max request attempts */
	private int maxRequestAttempts;

	/** Wait time */
	private int waitTime;

	/**
	 * Constructor
	 * 
	 * @param s3Client           s3 client
	 * @param bucket             bucket
	 * @param sourceFile         sourceFile
	 * @param targetFileOrDir    target file or directory
	 * @param fromByte           from byte
	 * @param toByte             toByte
	 * @param maxRequestAttempts max request attempts
	 * @param waitTime           waitTime
	 */
	public S3AtomicPartialFileDownloader(S3Client s3Client, String bucket, String sourceFile, String targetFileOrDir,
			Long fromByte, Long toByte, int maxRequestAttempts, int waitTime) {

		this.s3Client = s3Client;
		this.bucket = bucket;
		this.sourceFile = sourceFile;
		this.targetFileOrDir = targetFileOrDir;

		this.fromByte = fromByte;
		this.toByte = toByte;
		this.maxRequestAttempts = maxRequestAttempts;
		this.waitTime = waitTime;
	}

	/**
	 * Executes download of the file to s3
	 * 
	 * @return downloaded file name
	 */
	public HttpHeaders execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - downloadPartialFile({},{})", sourceFile, targetFileOrDir);

		String sourceS3File = new PathConverter(sourceFile).posixToS3Path().convertToSlash().getPath();
		String targetPosixFile = targetFileOrDir;

		if (new PathConverter(targetFileOrDir).isDirectory()) {
			targetPosixFile = Paths.get(targetFileOrDir, getFileName(sourceS3File)).toString();
		}

		targetPosixFile = new PathConverter(targetPosixFile).s3ToPosixPath().convertToSlash().getPath();
		new FileUtils(targetPosixFile).createParentDirectories();

		// temporary download filename, after download will be renamed
		// String temporaryTargetPosixFile = getTemporaryFilePath(targetPosixFile);
		try {
			InputStream stream = getInputStream(sourceS3File, targetPosixFile);

			HttpHeaders headers = getFilePage(stream, fromByte, toByte);

			// rename temporary file if the file downloaded successfully to cache
			/*
			 * if (!renameFile(temporaryTargetPosixFile, targetPosixFile)) { throw new
			 * IOException("Cannot rename partial file after download " +
			 * temporaryTargetPosixFile + " to " + targetPosixFile); }
			 */

			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - " + targetPosixFile);

			return headers;

		} catch (IOException e) {
			if (logger.isTraceEnabled())
				logger.trace(getFailedInfo() + e.getMessage());
			// new File(temporaryTargetPosixFile).delete();
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
	 * Gets file name
	 * 
	 * @param path path
	 * @return file name
	 */
	private String getFileName(String path) {
		return new File(path).getName();
	}

	/**
	 * Gets temporary file path
	 * 
	 * @param path path
	 * @return temporary file path
	 */
	private String getTemporaryFilePath(String path) {

		String filename = TEMPORARY_PREFIX + new File(path).getName();

		return new PathConverter(new File(path).getParent(), filename).getPath();
	}

	/**
	 * Renames file
	 * 
	 * @param oldPath old path
	 * @param newPath new path
	 * @return true if the file has been renamed successfully
	 */
	private boolean renameFile(String oldName, String newName) {

		if (logger.isTraceEnabled())
			logger.trace(">>> renameFile({}, {})", oldName, newName);

		File oldFile = new File(oldName);
		File newFile = new File(newName);

		if (newFile.exists())
			newFile.delete();

		return oldFile.renameTo(newFile);
	}

	/**
	 * Downloads partial file
	 * 
	 * @param s3Key      s3 key
	 * @param targetPath target path
	 * @throws IOException IO Exception
	 */
	private InputStream getInputStream(String s3Key, String targetPath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> downloadPartialFile({}, {}, {})", bucket, s3Key, targetPath);

		try {
			InputStream stream = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(s3Key).build(),
					ResponseTransformer.toInputStream());

			if (stream == null)
				throw new IOException("Cannot create stream for download partial file");

			return stream;

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			throw new IOException(e);
		}
	}

	/**
	 * @param relativePath
	 * @param stream
	 * @param fromByte
	 * @param toByte
	 * @return
	 * @throws IOException
	 */
	private HttpHeaders getFilePage(InputStream stream, Long fromByte, Long toByte) throws IOException {

		StorageFile storageFile = new S3StorageFile(bucket, sourceFile);

		Long len = getFileSize(storageFile.getRelativePath());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("attachment", storageFile.getFileName());
		long from = 0;
		long to = len - 1;

		if (fromByte != null || toByte != null) {

			List<HttpRange> ranges = new ArrayList<HttpRange>();

			if (fromByte != null) {
				from = fromByte;
				stream.skip(from);
			}

			if (toByte != null) {
				to = Math.min(toByte, len - 1);
			}

			len = to - from + 1;
			HttpRange range = HttpRange.createByteRange(from, to);
			ranges.add(range);
			headers.setRange(ranges);
			headers.setContentType(new MediaType("multipart", "byteranges"));

		} else {
			headers.setContentType(new MediaType("application", storageFile.getExtension()));
		}
		headers.setContentLength(len);

		return headers;
	}

	/**
	 * Gets file size
	 * 
	 * @param filePath file path
	 * @return file size in bytes
	 * @throws IOException
	 */
	public long getFileSize(String filePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFileSize({})", filePath);

		AtomicCommand<String> fileSizeGetter = new S3AtomicFileSizeGetter(s3Client, bucket, filePath);

		String fileSize = new DefaultRetryStrategy<String>(fileSizeGetter, maxRequestAttempts, waitTime).execute();

		return Long.valueOf(fileSize);
	}
}
