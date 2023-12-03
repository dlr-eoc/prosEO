package de.dlr.proseo.storagemgr.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.cache.FileCache;
import de.dlr.proseo.storagemgr.fs.s3.S3Ops;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Proseo file representing S3 file system
 * 
 * @author melchinger
 *
 */
public class ProseoFileS3 extends ProseoFile {

	private static final int MSG_ID_TARGET_PATH_MISSING = 4100;
	private static final int MSG_ID_ALLUXIO_NOT_SUPPORTED = 4101;
	private static final int MSG_ID_RECURSIVE_NOT_SUPPORTED = 4102;
	private static final int MSG_ID_S3_TO_S3_COPY_FAILED = 4103;
	private static final int MSG_ID_S3_TO_POSIX_COPY_FAILED = 4104;
	private static final int MSG_ID_S3_DELETE_FAILED = 4105;
	private static final int MSG_ID_S3_LIST_FAILED = 4106;
	private static final int MSG_ID_S3_GET_STREAM_FAILED = 4107;
	private static final int MSG_ID_S3_WRITE_FAILED = 4108;
	private static final int MSG_ID_S3_REQUEST_FAILED_RETRYING = 4109;

	private static final String MSG_TARGET_PATH_MISSING = "(E%d) No target path given";
	private static final String MSG_ALLUXIO_NOT_SUPPORTED = "(E%d) Copying S3 objects to Alluxio is not supported";
	private static final String MSG_RECURSIVE_NOT_SUPPORTED = "(E%d) Recursive copying from S3 bucket not implemented";
	private static final String MSG_S3_TO_S3_COPY_FAILED = "(E%d) Retrieval of S3 object %s/%s into S3 object %s/%s failed";
	private static final String MSG_S3_TO_POSIX_COPY_FAILED = "(E%d) Retrieval of S3 object %s into POSIX file %s failed";
	private static final String MSG_S3_DELETE_FAILED = "(E%d) Deletion of S3 object %s failed (cause: %s)";
	private static final String MSG_S3_LIST_FAILED = "(E%d) Listing of S3 object %s failed (cause: %s)";
	private static final String MSG_S3_GET_STREAM_FAILED = "(E%d) Retrieving S3 object %s as stream failed (cause: %s)";
	private static final String MSG_S3_WRITE_FAILED = "(E%d) Writing content to S3 object %s failed (cause: %s)";
	private static final String MSG_S3_REQUEST_FAILED_RETRYING = "(I%d) S3 request failed, retrying after %d ms (attempt %d of %d)";


	private static Logger logger = LoggerFactory.getLogger(ProseoFileS3.class);

	/**
	 * Create a new S3 file.
	 * 
	 * @param pathInfo
	 *            The file path
	 * @param fullPath
	 *            Use it as full path if true, otherwise use default bucket +
	 *            path info
	 * @param cfg
	 *            the Storage Manager configuration to use
	 */
	public ProseoFileS3(String pathInfo, Boolean fullPath, StorageManagerConfiguration cfg) {

		if (logger.isTraceEnabled())
			logger.trace(">>> ProseoFileS3({}, {}, {})", pathInfo, fullPath, cfg);

		this.cfg = cfg;
		String aPath = pathInfo.trim();
		this.pathInfo = aPath;
		if (fullPath) {
			if (aPath.startsWith("s3:/") || aPath.startsWith("S3:/")) {
				aPath = aPath.substring(4);
			}
			while (aPath.startsWith("/")) {
				aPath = aPath.substring(1);
			}
			int pos = aPath.indexOf('/');
			if (pos >= 0) {
				basePath = aPath.substring(0, pos);
				relPath = aPath.substring(pos + 1);
			} else {
				basePath = "";
				relPath = aPath;
			}
		} else {
			while (aPath.startsWith("/")) {
				aPath = aPath.substring(1);
			}
			relPath = aPath;
			basePath = cfg.getS3DefaultBucket();
		}
		buildFileName();
		pathInfo = getFullPath();

		logger.trace("ProseoFileS3 created: {}", this.getFullPath());
	}

	/**
	 * Create a new S3 file.
	 * 
	 * @param bucket
	 *            The bucket
	 * @param pathInfo
	 *            The relative path
	 * @param cfg
	 *            the Storage Manager configuration to use
	 */
	public ProseoFileS3(String bucket, String pathInfo, StorageManagerConfiguration cfg) {

		if (logger.isTraceEnabled())
			logger.trace(">>> ProseoFileS3({}, {}, {})", bucket, pathInfo, cfg);

		String aPath = pathInfo.trim();
		relPath = aPath;
		basePath = bucket.trim();
		while (basePath.startsWith("/")) {
			basePath = basePath.substring(1);
		}
		buildFileName();
		pathInfo = getFullPath();

		logger.trace("ProseoFileS3 created: {}", this.getFullPath());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getFsType()
	 */
	@Override
	public StorageType getFsType() {
		return StorageType.S3;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getFullPath()
	 */
	@Override
	public String getFullPath() {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFullPath()");

		return "s3://" + getBasePath() + "/" + getRelPathAndFile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getDataAsInputStream()
	 */
	@Override
	public InputStream getDataAsInputStream() {

		if (logger.isTraceEnabled())
			logger.trace(">>> getDataAsInputStream()");

		InputStream inputStream = null;
		try {
			boolean success = false;
			// *** HACK FOR DDS3 - TODO Replace constants by configurable values !!! ***
			long retryCount = 0, maxRetry = 3, retryInterval = 5000 /* ms */;
			while (retryCount <= maxRetry && !success) {
				try {
					S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(), cfg.getS3Region());
					inputStream = S3Ops.v2FetchStream(s3, getFullPath());
					if (null != inputStream) {
						break; // No exception and a valid result
					}
				} catch (Exception e) {
					if (retryCount >= maxRetry) {
						throw e;
					} // else try again, see below
				}
				++retryCount;
				StorageLogger.logInfo(logger, MSG_S3_REQUEST_FAILED_RETRYING, MSG_ID_S3_REQUEST_FAILED_RETRYING, 
						retryInterval, retryCount, maxRetry);
				Thread.sleep(retryInterval);
			}
			logger.info("Successfully read from {}", getFullPath());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(StorageLogger.logError(logger, MSG_S3_GET_STREAM_FAILED, MSG_ID_S3_GET_STREAM_FAILED,
					this.getFullPath(), e.getMessage()));
		}
		return inputStream;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#writeBytes(byte[])
	 */
	@Override
	public Boolean writeBytes(byte[] bytes) throws Exception {

		if (logger.isTraceEnabled())
			logger.trace(">>> writeBytes({})", bytes.length);

		if (bytes != null) {
			InputStream fis = new ByteArrayInputStream(bytes);
			// create internal buckets, if not existing
			StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(),
					cfg.getS3EndPoint(), getBasePath(), cfg.getS3Region());
			S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),
					cfg.getS3Region());
			boolean putOK = false;
			try {
				s3.putObject(PutObjectRequest.builder().bucket(getBasePath()).key(getRelPathAndFile()).build(),
						RequestBody.fromInputStream(fis, bytes.length));
				putOK = true;
				logger.info("Bytes, written to {}", getFullPath());
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(StorageLogger.logError(logger, MSG_S3_WRITE_FAILED, MSG_ID_S3_WRITE_FAILED,
						this.getFullPath(), e.getMessage()));
			}
			s3.close();
			fis.close();
			return putOK;
		}
		logger.warn("writeBytes, arument bytes not set");
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.dlr.proseo.storagemgr.utils.ProseoFile#copyTo(de.dlr.proseo.storagemgr
	 * .utils.ProseoFile, java.lang.Boolean)
	 */
	@Override
	public ArrayList<String> copyTo(ProseoFile proFile, Boolean recursive) throws Exception {

		if (logger.isTraceEnabled())
			logger.trace(">>> copyTo({}, {})", (null == proFile ? "MISSING" : proFile.getFullPath()), recursive);

		if (null == proFile) {
			throw new IllegalArgumentException(StorageLogger.logError(logger, MSG_TARGET_PATH_MISSING, MSG_ID_TARGET_PATH_MISSING));
		}
		if (null == recursive) {
			recursive = false;
		}

		ArrayList<String> result = null;
		AmazonS3URI s3uri = new AmazonS3URI(this.getFullPath());
		String sourceBucket = s3uri.getBucket();
		String sourceKey = s3uri.getKey();
		switch (proFile.getFsType()) {
		case S3:
			// create internal buckets & prefixes if not exists..
			StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(),
					cfg.getS3EndPoint(), cfg.getS3DefaultBucket(), cfg.getS3Region());
			AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),
					cfg.getS3Region());
			result = S3Ops.v1Copy(
					// the client
					s3,
					// the source S3-Bucket
					sourceBucket,
					// the source key
					sourceKey,
					// the target s3-bucket (=storageId)
					cfg.getS3DefaultBucket(),
					// the final prefix including productId pattern of the
					// file or directory
					proFile.getRelPath());
			if (null == result) {
				throw new RuntimeException(StorageLogger.logError(
						logger, MSG_S3_TO_S3_COPY_FAILED, MSG_ID_S3_TO_S3_COPY_FAILED,
						sourceBucket, sourceKey,
						cfg.getS3DefaultBucket(), proFile.getRelPath()));
			}
			break;
		case POSIX:
			result = new ArrayList<String>();
			// TODO recursive
			if (recursive) {
				throw new UnsupportedOperationException(StorageLogger.logError(logger, MSG_RECURSIVE_NOT_SUPPORTED, MSG_ID_RECURSIVE_NOT_SUPPORTED));
			}
			File targetFile = new File(proFile.getFullPath());
			if (this.isDirectory()) {
				if (!targetFile.exists()) {
					FileUtils.forceMkdir(targetFile);
				}
				result.add(proFile.getFullPath());
			} else {
				if (FileCache.getInstance().containsKey(proFile.getFullPath())) { // if
																					// (targetFile.exists())
					result.add(proFile.getFullPath());
				} else {
					// *** HACK FOR DDS3 - TODO Replace constants by configurable values !!! ***
					long retryCount = 0, maxRetry = 3, retryInterval = 5000 /* ms */;
					boolean success = false;
					while (retryCount <= maxRetry ) {
						if (retryCount < maxRetry) {
							// Try standard download method using AWS S3 SDK V1 Transfer Manager
							AmazonS3 s3v1c = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),
									cfg.getS3Region());
							if (S3Ops.v1FetchFile(
									// the client
									s3v1c,
									// the source S3-Bucket
									this.getBasePath(),
									// the source S3 object key
									this.getRelPathAndFile(),
									// the target path on POSIX file system
									targetFile)) {
								success = true;
								break; // No exception and a valid result
							}
						} else {
							// Last try: Use single-threaded AWS S3 SDK V2 GetObject method
							S3Client s3v2c = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),
							cfg.getS3Region());
							if (S3Ops.v2FetchFile(
									// the client
									s3v2c,
									// the source S3-Bucket
									this.getFullPath(),
									// the final prefix including productId pattern
									// of the file or directory
									proFile.getFullPath())) {
								success = true;
								break; // No exception and a valid result
							}
						}
						++retryCount;
						if (retryCount <= maxRetry) {
							StorageLogger.logInfo(logger, MSG_S3_REQUEST_FAILED_RETRYING, MSG_ID_S3_REQUEST_FAILED_RETRYING,
									retryInterval, retryCount, maxRetry);
							Thread.sleep(retryInterval);
						}
					}
					if (success) {
						targetFile.setWritable(true, false);
						FileCache.getInstance().put(proFile.getFullPath());
						result.add(proFile.getFullPath());
					} else {
						proFile.delete();
						throw new RuntimeException(StorageLogger.logError(
								logger, MSG_S3_TO_POSIX_COPY_FAILED, MSG_ID_S3_TO_POSIX_COPY_FAILED,
								this.getFullPath(), proFile.getFullPath()));
					}
				}
			}
			break;
		case ALLUXIO:
			throw new UnsupportedOperationException(StorageLogger.logError(logger, MSG_ALLUXIO_NOT_SUPPORTED, MSG_ID_ALLUXIO_NOT_SUPPORTED));
		default:
			break;
		}
		
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#delete()
	 */
	@Override
	public ArrayList<String> delete() {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete()");

		ArrayList<String> result = new ArrayList<String>();
		AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(), cfg.getS3Region());
		try {
			S3Ops.deleteDirectory(s3, getBasePath(), getRelPathAndFile());
			result.add(getFullPath());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(StorageLogger.logError(logger, MSG_S3_DELETE_FAILED, MSG_ID_S3_DELETE_FAILED,
					this.getFullPath(), e.getMessage()));
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#list()
	 */
	@Override
	public ArrayList<ProseoFile> list() {

		if (logger.isTraceEnabled())
			logger.trace(">>> list()");

		ArrayList<ProseoFile> list = new ArrayList<ProseoFile>();
		try {
			StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(),
					cfg.getS3EndPoint(), cfg.getS3DefaultBucket(), cfg.getS3Region());
			AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),
					cfg.getS3Region());
			List<String> files = S3Ops.listObjectsInBucket(s3, getBasePath(), getRelPath());
			for (String f : files) {
				list.add(new ProseoFileS3(f, true, cfg));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(StorageLogger.logError(logger, MSG_S3_LIST_FAILED, MSG_ID_S3_LIST_FAILED,
					this.getFullPath(), e.getMessage()));
		}

		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getFileSystemResource()
	 */
	@Override
	public FileSystemResource getFileSystemResource() {
		return new FileSystemResource(getFullPath());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getLength()
	 */
	@Override
	public long getLength() {
		if (isDirectory()) {
			return 0;
		} else {
			AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),
					cfg.getS3Region());
			return S3Ops.getLength(s3, getBasePath(), getRelPathAndFile());
		}
	}
}
