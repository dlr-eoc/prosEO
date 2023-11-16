/**
 * S3DAL.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import de.dlr.proseo.storagemgr.model.AtomicCommand;
import de.dlr.proseo.storagemgr.model.DefaultRetryStrategy;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * A data access layer for interacting with a S3-based storage system. It
 * provides various methods to perform operations such as retrieving files,
 * checking file or directory existence, obtaining file content, uploading
 * files, downloading files, and deleting files or directories within the S3
 * storage system.
 *
 * This class utilizes the Amazon S3 SDK v2 for S3 client operations and
 * requires AWS credentials and a S3 configuration to be initialized.
 *
 * @author Denys Chaykovskiy
 */
public class S3DAL {

	/** S3 client for v2 */
	private S3Client s3ClientV2;

	/** S3 client for v1 */
	private AmazonS3 s3ClientV1;

	/** s3 configuration */
	private S3Configuration cfg;

	/** AWS credentials */
	private AwsBasicCredentials credentials;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3DAL.class);

	/**
	 * Constructor
	 *
	 * @param cfg s3Configuration
	 * @throws IOException if an I/O exception occurs
	 */
	public S3DAL(S3Configuration cfg) throws IOException {

		this.cfg = cfg;

		initS3ClientV2();
		initS3ClientV1(); // for transfer manager only
	}

	/**
	 * Gets the S3 configuration used by this S3DAL instance.
	 *
	 * @return the S3Configuration object
	 */
	public S3Configuration getConfiguration() {

		return cfg;
	}

	/**
	 * Initializes the S3 v1 client.
	 *
	 * @throws IOException if an I/O exception occurs
	 */
	public void initS3ClientV1() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> initS3ClientV1()");

		logger.trace("... using access key {} and secret {}", cfg.getS3AccessKey(), "***");
		AWSCredentials awsCredentialsV1 = new BasicAWSCredentials(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey());

		if (cfg.isDefaultEndPoint()) {
			Regions region = Regions.fromName(cfg.getS3Region());

			s3ClientV1 = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentialsV1))
				.withRegion(region)
				.build();
		} else {
			ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setSignerOverride("AWSS3V4SignerType");
			String s3EndPoint = cfg.getS3EndPoint();

			s3ClientV1 = AmazonS3ClientBuilder.standard()
				.withEndpointConfiguration(
						new AwsClientBuilder.EndpointConfiguration(s3EndPoint, Region.of(cfg.getS3Region()).id()))
				.withPathStyleAccessEnabled(true)
				.withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentialsV1))
				.build();
		}
	}

	/**
	 * Initializes the S3 v2 client.
	 *
	 * @throws IOException if an I/O exception occurs
	 */
	public void initS3ClientV2() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> initS3ClientV2()");

		Region s3Region = Region.of(cfg.getS3Region()); // Region.EU_CENTRAL_1;

		logger.trace("... using access key {} and secret {}", cfg.getS3AccessKey(), "***");
		initCredentials(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey());

		if (cfg.isDefaultEndPoint()) {
			s3ClientV2 = S3Client.builder()
				.region(s3Region)
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.build();
		} else {
			s3ClientV2 = S3Client.builder()
				.region(s3Region)
				.endpointOverride(URI.create(cfg.getS3EndPoint()))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.build();
		}

		setBucket(cfg.getBucket());
	}

	/**
	 * Retrieves a list of all files in the current bucket.
	 *
	 * @return a list of all file paths
	 * @throws IOException if an I/O exception occurs
	 */
	public List<String> getFiles() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles()");

		AtomicCommand<List<String>> fileGetter = new S3AtomicFileListGetter(s3ClientV2, cfg.getBucket());

		return new DefaultRetryStrategy<>(fileGetter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Retrieves a list of files that match the specified folder (prefix).
	 *
	 * @param folder the folder (prefix) to match
	 * @return a list of file paths that match the folder
	 * @throws IOException if an I/O exception occurs
	 */
	public List<String> getFiles(String folder) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", folder);

		AtomicCommand<List<String>> fileGetter = new S3AtomicFileListGetter(s3ClientV2, cfg.getBucket(), folder);

		return new DefaultRetryStrategy<>(fileGetter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Checks if a file exists in the storage system.
	 *
	 * @param filePath the file path
	 * @return true if the file exists, false otherwise
	 * @throws IOException if an I/O exception occurs
	 */
	public boolean fileExists(String filePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> fileExists({},{})", filePath);

		AtomicCommand<String> fileExistsGetter = new S3AtomicFileExistsGetter(s3ClientV2, cfg.getBucket(), filePath);

		String fileExists = new DefaultRetryStrategy<>(fileExistsGetter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime())
			.execute();

		return Boolean.valueOf(fileExists);
	}

	/**
	 * Checks if the specified path represents a file (not a directory).
	 *
	 * @param path the path to check
	 * @return true if the path represents a file, false otherwise
	 */
	public boolean isFile(String path) {
		return new File(path).isFile();
	}

	/**
	 * Gets the name of the file from the specified path.
	 *
	 * @param path the file path
	 * @return the name of the file
	 */
	public String getFileName(String path) {
		return new File(path).getName();
	}

	/**
	 * Retrieves the size of the file specified by the file path.
	 *
	 * @param filePath the file path
	 * @return the size of the file in bytes
	 * @throws IOException if an I/O exception occurs
	 */
	public long getFileSize(String filePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFileSize({})", filePath);

		AtomicCommand<String> fileSizeGetter = new S3AtomicFileSizeGetter(s3ClientV2, cfg.getBucket(), filePath);

		String fileSize = new DefaultRetryStrategy<>(fileSizeGetter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime())
			.execute();

		return Long.valueOf(fileSize);
	}

	/**
	 * Retrieves the content of the file specified by the file path.
	 *
	 * @param filePath the file path
	 * @return the content of the file
	 * @throws IOException if an I/O exception occurs
	 */
	public String getFileContent(String filePath) throws IOException {

		AtomicCommand<String> fileContentGetter = new S3AtomicFileContentGetter(s3ClientV2, cfg.getBucket(), filePath);

		return new DefaultRetryStrategy<>(fileContentGetter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Uploads a file to the storage system.
	 *
	 * @param sourceFile      the source file to upload
	 * @param targetFileOrDir the target file or directory in the storage system
	 * @return the uploaded file path in the storage system
	 * @throws IOException if an I/O exception occurs if the file cannot be uploaded
	 */
	public String uploadFile(String sourceFile, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({},{})", sourceFile, targetFileOrDir);

		AtomicCommand<String> fileUploader = new S3AtomicFileUploader(s3ClientV1, cfg.getBucket(), sourceFile, targetFileOrDir);

		return new DefaultRetryStrategy<>(fileUploader, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Uploads a file or directory to the storage system.
	 *
	 * @param sourceFileOrDir the source file or directory to upload
	 * @param targetFileOrDir the target file or directory in the storage system
	 * @return a list of uploaded file paths in the storage system
	 * @throws IOException if an I/O exception occurs if the file or directory cannot be uploaded
	 */
	public List<String> upload(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},{})", sourceFileOrDir, targetFileOrDir);

		List<String> uploadedFiles = new ArrayList<>();

		if (isFile(sourceFileOrDir)) {
			String uploadedFile = uploadFile(sourceFileOrDir, targetFileOrDir);
			uploadedFiles.add(uploadedFile);
			return uploadedFiles;
		}

		String sourceDir = sourceFileOrDir;
		String targetDir = targetFileOrDir;
		targetDir = new PathConverter(targetDir).posixToS3Path().addSlashAtEnd().getPath();
		File directory = new File(sourceDir);
		File[] files = directory.listFiles();
		Arrays.sort(files);

		for (File file : files) {
			if (file.isFile()) {
				String sourceFile = file.getAbsolutePath();
				String uploadedFile = uploadFile(sourceFile, targetDir);
				uploadedFiles.add(uploadedFile);
			}
		}

		for (File file : files) {
			if (file.isDirectory()) {
				String sourceSubDir = file.getAbsolutePath();
				String targetSubDir = Paths.get(targetDir, file.getName()).toString();
				targetSubDir = new PathConverter(targetSubDir).posixToS3Path().addSlashAtEnd().getPath();
				List<String> subDirUploadedFiles = upload(sourceSubDir, targetSubDir);
				uploadedFiles.addAll(subDirUploadedFiles);
			}
		}

		return uploadedFiles;
	}

	/**
	 * Uploads a file or directory to the storage system.
	 *
	 * @param sourceFileOrDir the source file or directory to upload
	 * @return a list of uploaded file or directory paths in the storage system
	 * @throws IOException if an I/O exception occurs if the file or directory cannot be uploaded
	 */
	public List<String> upload(String sourceFileOrDir) throws IOException {
		return upload(sourceFileOrDir, sourceFileOrDir);
	}

	/**
	 * Downloads a file from the storage system.
	 *
	 * @param sourceFile      the source file in the storage system to download
	 * @param targetFileOrDir the target file or directory to download to
	 * @return the downloaded file path
	 * @throws IOException if an I/O exception occurs if the file cannot be downloaded
	 */
	public String downloadFile(String sourceFile, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFile({},{})", sourceFile, targetFileOrDir);

		AtomicCommand<String> fileDownloader = new S3AtomicFileDownloader(s3ClientV1, cfg.getBucket(), sourceFile, targetFileOrDir,
				cfg.getFileCheckWaitTime(), cfg.getMaxRequestAttempts());
		return new DefaultRetryStrategy<>(fileDownloader, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Downloads a file or directory from the storage system.
	 *
	 * @param sourceFileOrDir the source file or directory in the storage system to
	 *                        download
	 * @param targetFileOrDir the target file or directory to download to
	 * @return a list of downloaded file or directory paths
	 * @throws IOException if an I/O exception occurs if the file or directory cannot be downloaded
	 */
	public List<String> download(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> download({},{})", sourceFileOrDir, targetFileOrDir);

		String sourcePosixFileOrDir = new PathConverter(sourceFileOrDir).posixToS3Path().convertToSlash().getPath();

		List<String> toDownloadFiles = getFiles(sourcePosixFileOrDir);
		List<String> downloadedFiles = new ArrayList<>();

		for (String sourceFile : toDownloadFiles) {
			String downloadedFile = downloadFile(sourceFile, targetFileOrDir);
			downloadedFiles.add(downloadedFile);
		}

		return downloadedFiles;
	}

	/**
	 * Downloads a file or directory with a prefix match from the storage system.
	 *
	 * @param prefixFileOrDir the prefix file or directory to match
	 * @return a list of downloaded file paths
	 * @throws IOException if an I/O exception occurs if the file or directory cannot be downloaded
	 */
	public List<String> download(String prefixFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> download({})", prefixFileOrDir);

		String s3PrefixFileOrDir = new PathConverter(prefixFileOrDir).posixToS3Path().convertToSlash().getPath();

		List<String> toDownloadFiles = getFiles(s3PrefixFileOrDir);
		List<String> downloadedFiles = new ArrayList<>();

		for (String sourceFile : toDownloadFiles) {
			String downloadedFile = downloadFile(sourceFile, sourceFile); // download as is
			downloadedFiles.add(downloadedFile);
		}

		return downloadedFiles;
	}

	/**
	 * Deletes a file or directory.
	 *
	 * @param fileOrDir the file or directory to delete
	 * @return a list of deleted file paths
	 * @throws IOException if an I/O exception occurs
	 */
	public List<String> delete(String fileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", fileOrDir);

		AtomicCommand<List<String>> fileListDeleter = new S3AtomicFileListDeleter(s3ClientV2, cfg.getBucket(), fileOrDir);

		return new DefaultRetryStrategy<>(fileListDeleter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Deletes a file.
	 *
	 * @param filepath the file path
	 * @return the deleted file path
	 * @throws IOException if an I/O exception occurs
	 */
	public String deleteFile(String filepath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", filepath);

		AtomicCommand<String> fileDeleter = new S3AtomicFileDeleter(s3ClientV2, cfg.getBucket(), filepath);

		return new DefaultRetryStrategy<>(fileDeleter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Deletes files.
	 *
	 * @return a list of deleted file paths
	 * @throws IOException if an I/O exception occurs
	 */
	public List<String> deleteFiles() throws IOException {

		List<String> deletedFiles = new ArrayList<>();

		for (String file : getFiles()) {
			String deletedFile = deleteFile(file);
			deletedFiles.add(deletedFile);
		}

		return deletedFiles;
	}

	/**
	 * Deletes files.
	 *
	 * @param toDeleteList a list of file or directory paths to delete
	 * @return a list of deleted file paths
	 */
	public List<String> deleteFiles(List<String> toDeleteList) {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFiles({})", "size:" + toDeleteList.size());

		ArrayList<ObjectIdentifier> keys = new ArrayList<>();
		ObjectIdentifier objectId = null;

		for (String toDelete : toDeleteList) {

			objectId = ObjectIdentifier.builder().key(toDelete).build();
			keys.add(objectId);
		}

		Delete del = Delete.builder().objects(keys).build();

		try {
			DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
				.bucket(cfg.getBucket())
				.delete(del)
				.build();
			System.out.println("Multiple objects are deleted!");

			DeleteObjectsResponse deleteResponse = s3ClientV2.deleteObjects(multiObjectDeleteRequest);
			return toStringDeletedObjects(deleteResponse.deleted());

		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
			if (logger.isDebugEnabled()) {
				logger.debug("An exception occurred. Cause: ", e);
			}
			throw e;
		}
	}

	/**
	 * Sets the bucket to use for operations.
	 *
	 * @param bucket the bucket name
	 * @throws IOException if an I/O exception occurs
	 */
	public void setBucket(String bucket) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> setBucket({})", bucket);

		if (!bucketExists(bucket)) {
			createBucket(bucket);
		}

		cfg.setBucket(bucket);
	}

	/**
	 * Gets the current bucket.
	 *
	 * @return the current bucket name
	 */
	public String getBucket() {
		return cfg.getBucket();
	}

	/**
	 * Gets the list of available buckets.
	 *
	 * @return a list of bucket names
	 * @throws IOException if an I/O exception occurs
	 */
	public List<String> getBuckets() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getBuckets()");

		AtomicCommand<List<String>> bucketGetter = new S3AtomicBucketListGetter(s3ClientV2);

		return new DefaultRetryStrategy<>(bucketGetter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Checks if a bucket exists in the storage system.
	 *
	 * @param bucketName the bucket name
	 * @return true if the bucket exists, false otherwise
	 * @throws IOException if an I/O exception occurs
	 */
	public boolean bucketExists(String bucketName) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> bucketExists({})", bucketName);

		List<String> buckets = getBuckets();

		for (String bucket : buckets) {
			if (bucket.equals(bucketName)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Deletes a bucket from the storage system.
	 *
	 * @param bucketName the bucket name
	 * @throws IOException if an I/O exception occurs
	 */
	public void deleteBucket(String bucketName) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteBucket({})", bucketName);

		deleteFiles();
		deleteEmptyBucket(bucketName);
	}

	/**
	 * Initializes the AWS credentials.
	 *
	 * @param s3AccessKey       the S3 access key
	 * @param s3SecretAccessKey the S3 secret access key
	 */
	private void initCredentials(String s3AccessKey, String s3SecretAccessKey) {

		credentials = AwsBasicCredentials.create(s3AccessKey, s3SecretAccessKey);

	}

	/**
	 * Creates a bucket in the storage system.
	 *
	 * @param bucketName the bucket name
	 * @return the created bucket name
	 * @throws IOException if an I/O exception occurs
	 */
	private String createBucket(String bucketName) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> createBucket({})", bucketName);

		AtomicCommand<String> bucketCreator = new S3AtomicBucketCreator(s3ClientV2, cfg.getBucket());

		return new DefaultRetryStrategy<>(bucketCreator, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Deletes an empty bucket from the storage system.
	 *
	 * @param bucketName the bucket name
	 * @return the deleted bucket name
	 * @throws IOException if an I/O exception occurs
	 */
	private String deleteEmptyBucket(String bucketName) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteEmptyBucket({})", bucketName);

		AtomicCommand<String> bucketDeleter = new S3AtomicBucketDeleter(s3ClientV2, cfg.getBucket());

		return new DefaultRetryStrategy<>(bucketDeleter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Converts a list of deleted objects to a list of deleted file paths.
	 *
	 * @param files the list of deleted object files
	 * @return a list of deleted file paths
	 */
	private List<String> toStringDeletedObjects(List<DeletedObject> files) {

		List<String> fileNames = new ArrayList<>();

		for (DeletedObject f : files) {
			fileNames.add(f.key());
		}

		return fileNames;
	}

	/**
	 * Gets an input stream from a file in the storage system.
	 *
	 * @param relativePath the relative file path
	 * @return an InputStream object for reading the file content
	 * @throws IOException if an I/O exception occurs
	 */
	public InputStream getInputStream(String relativePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getInputStream({})", relativePath);

		AtomicCommand<InputStream> inputStream = new S3AtomicInputStreamGetter(s3ClientV2, cfg.getBucket(), relativePath);

		return new DefaultRetryStrategy<>(inputStream, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime()).execute();
	}
}