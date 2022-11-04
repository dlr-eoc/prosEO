package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.io.File;
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

import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import de.dlr.proseo.storagemgr.version2.model.DefaultRetryStrategy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.FileUpload;

/**
 * S3 Data Access Layer based on Amazon S3 SDK v2
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3DAL {

	/** S3 Client for v2 */
	private S3Client s3ClientV2;

	/** S3 Client for v1 */
	private AmazonS3 s3ClientV1;

	/** s3 configuration */
	private S3Configuration cfg;

	/** AWS Basic Credentials */
	private AwsBasicCredentials credentials;

	/** AWS Credentials Provider */
	private AwsCredentialsProvider credentialsProvider;

	/** S3 Transfer Manager */
	private S3TransferManager transferManager;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3DAL.class);

	/**
	 * Constructor
	 * 
	 * @param cfg s3Configuration
	 * @throws IOException
	 */
	public S3DAL(S3Configuration cfg) throws IOException {

		this.cfg = cfg;

		initS3ClientV2();
		initS3ClientV1(); // for transfer manager only
	}

	/**
	 * Gets Configuration
	 * 
	 * @return cfg s3Configuration
	 */
	public S3Configuration getConfiguration() {

		return cfg;
	}

	/**
	 * Initialization of s3 client v1
	 * 
	 * @throws IOException
	 */
	public void initS3ClientV1() throws IOException {

		AWSCredentials awsCredentialsV1 = new BasicAWSCredentials(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey());

		Regions region = Regions.fromName(cfg.getS3Region());

		if (cfg.isDefaultEndPoint()) {
			s3ClientV1 = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentialsV1)).withRegion(region).build();
		}
		else {
			ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setSignerOverride("AWSS3V4SignerType");
			String s3EndPoint = cfg.getS3EndPoint();
			
			s3ClientV1 = AmazonS3ClientBuilder.standard()
					.withEndpointConfiguration(
							new AwsClientBuilder.EndpointConfiguration(s3EndPoint, Region.of(region.name()).id()))
					.withPathStyleAccessEnabled(true).withClientConfiguration(clientConfiguration)
					.withCredentials(new AWSStaticCredentialsProvider(awsCredentialsV1)).build();
		}
	}

	/**
	 * Initialization of s3 client v2
	 * 
	 * @throws IOException
	 */
	public void initS3ClientV2() throws IOException {

		Region s3Region = Region.of(cfg.getS3Region()); // Region.EU_CENTRAL_1;

		initCredentials(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey());
		initTransferManager(s3Region);

		if (cfg.isDefaultEndPoint()) {
			s3ClientV2 = S3Client.builder().region(s3Region)
					.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
		} else {
			s3ClientV2 = S3Client.builder().region(s3Region).endpointOverride(URI.create(cfg.getS3EndPoint()))
					.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
		}

		setBucket(cfg.getBucket());
	}

	/**
	 * Gets all files from current bucket
	 *
	 * @return list of all files
	 * @throws IOException
	 */
	public List<String> getFiles() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles()");

		AtomicCommand<List<String>> fileGetter = new S3AtomicFileListGetter(s3ClientV2, cfg.getBucket());

		return new DefaultRetryStrategy<List<String>>(fileGetter, cfg.getMaxRequestAttempts(),
				cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Gets files which match path (prefix)
	 * 
	 * @param path path (prefix)
	 * @return list if files
	 * @throws IOException
	 */
	public List<String> getFiles(String folder) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", folder);

		AtomicCommand<List<String>> fileGetter = new S3AtomicFileListGetter(s3ClientV2, cfg.getBucket(), folder);

		return new DefaultRetryStrategy<List<String>>(fileGetter, cfg.getMaxRequestAttempts(),
				cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Checks if file exists in storage
	 * 
	 * @param filePath file path
	 * @return
	 * @throws IOException
	 */
	public boolean fileExists(String filePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> fileExists({},{})", filePath);

		AtomicCommand<String> fileExistsGetter = new S3AtomicFileExistsGetter(s3ClientV2, cfg.getBucket(), filePath);

		String fileExists = new DefaultRetryStrategy<String>(fileExistsGetter, cfg.getMaxRequestAttempts(),
				cfg.getFileCheckWaitTime()).execute();

		return Boolean.valueOf(fileExists);
	}

	/**
	 * Checks if the path is file (not a directory)
	 * 
	 * @param path
	 * @return
	 */
	public boolean isFile(String path) {
		return new File(path).isFile();
	}

	/**
	 * Gets file name
	 * 
	 * @param path path
	 * @return file name
	 */
	public String getFileName(String path) {
		return new File(path).getName();
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

		AtomicCommand<String> fileSizeGetter = new S3AtomicFileSizeGetter(s3ClientV2, cfg.getBucket(), filePath);

		String fileSize = new DefaultRetryStrategy<String>(fileSizeGetter, cfg.getMaxRequestAttempts(),
				cfg.getFileCheckWaitTime()).execute();

		return Long.valueOf(fileSize);
	}

	/**
	 * Gets file content
	 * 
	 * @param filePath file path
	 * @return file content
	 */
	public String getFileContent(String filePath) throws IOException {

		AtomicCommand<String> fileContentGetter = new S3AtomicFileContentGetter(s3ClientV2, cfg.getBucket(), filePath);

		return new DefaultRetryStrategy<String>(fileContentGetter, cfg.getMaxRequestAttempts(),
				cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Uploads file to storage
	 * 
	 * @param sourceFile      source file to upload
	 * @param targetFileOrDir target file or directory in storage
	 * @param maxAttempts     max attempts
	 * @return uploaded to storage file path
	 * @throws IOException if file cannot be uploaded
	 */
	public String uploadFile(String sourceFile, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({},{})", sourceFile, targetFileOrDir);

		AtomicCommand<String> fileUploader = new S3AtomicFileUploader(s3ClientV2, cfg.getBucket(), sourceFile,
				targetFileOrDir);

		return new DefaultRetryStrategy<String>(fileUploader, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime())
				.execute();
	}

	/**
	 * Uploads file or directory to storage
	 * 
	 * @param sourceFileOrDir source file or directory to upload
	 * @param targetFileOrDir target file or directory in storage
	 * @param maxAttempts     max attempts
	 * @return uploaded to storage file path list
	 * @throws IOException
	 */
	public List<String> upload(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},{})", sourceFileOrDir, targetFileOrDir);

		List<String> uploadedFiles = new ArrayList<String>();

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
	 * Uploads file or directory to storage
	 * 
	 * @param sourceFileOrDir source file or directory to upload
	 * @param maxAttempts     max attempts
	 * @return uploaded file or directory file path list
	 * @throws IOException if file or directory cannot be uploaded
	 */
	public List<String> upload(String sourceFileOrDir) throws IOException {
		return upload(sourceFileOrDir, sourceFileOrDir);
	}

	/**
	 * Downloads file from storage
	 * 
	 * @param sourceFile      source file in storage to download
	 * @param targetFileOrDir target file or directory
	 * @return downloaded file path
	 * @throws IOException if file cannot be downloaded
	 */
	public String downloadFile(String sourceFile, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFile({},{})", sourceFile, targetFileOrDir);

		AtomicCommand<String> fileDownloader = new S3AtomicFileDownloader(s3ClientV1, cfg.getBucket(), sourceFile,
				targetFileOrDir);
		return new DefaultRetryStrategy<String>(fileDownloader, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime())
				.execute();
	}

	/**
	 * Downloads file or directory from storage
	 * 
	 * @param sourceFileOrDir source file or directory in storage to download
	 * @param targetFileOrDir target file or directory
	 * @return downloaded file or directory file path list
	 * @throws IOException if file or directory cannot be downloaded
	 */
	public List<String> download(String sourceFileOrDir, String targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> download({},{})", sourceFileOrDir, targetFileOrDir);

		String sourcePosixFileOrDir = new PathConverter(sourceFileOrDir).posixToS3Path().convertToSlash().getPath();

		List<String> toDownloadFiles = getFiles(sourcePosixFileOrDir);
		List<String> downloadedFiles = new ArrayList<String>();

		for (String sourceFile : toDownloadFiles) {
			String downloadedFile = downloadFile(sourceFile, targetFileOrDir);
			downloadedFiles.add(downloadedFile);
		}

		return downloadedFiles;
	}

	/**
	 * Downloads file or directory with prefix match
	 * 
	 * @param prefixFileOrDir prefix file or directory
	 * @return downloaded file path list
	 * @throws IOException if file or directory cannot be downloaded
	 */
	public List<String> download(String prefixFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> download({})", prefixFileOrDir);

		String s3PrefixFileOrDir = new PathConverter(prefixFileOrDir).posixToS3Path().convertToSlash().getPath();

		List<String> toDownloadFiles = getFiles(s3PrefixFileOrDir);
		List<String> downloadedFiles = new ArrayList<String>();

		for (String sourceFile : toDownloadFiles) {
			String downloadedFile = downloadFile(sourceFile, sourceFile); // download as is
			downloadedFiles.add(downloadedFile);
		}

		return downloadedFiles;
	}

	/**
	 * Deletes file or directory
	 * 
	 * @param fileOrDir file or directory
	 * @return deleted file path list
	 * @throws IOException
	 */
	public List<String> delete(String fileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", fileOrDir);

		AtomicCommand<List<String>> fileListDeleter = new S3AtomicFileListDeleter(s3ClientV2, cfg.getBucket(),
				fileOrDir);

		return new DefaultRetryStrategy<List<String>>(fileListDeleter, cfg.getMaxRequestAttempts(),
				cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Deletes file
	 * 
	 * @param filepath file path
	 * @return deleted file path
	 * @throws IOException
	 */
	public String deleteFile(String filepath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", filepath);

		AtomicCommand<String> fileDeleter = new S3AtomicFileDeleter(s3ClientV2, cfg.getBucket(), filepath);

		return new DefaultRetryStrategy<String>(fileDeleter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime())
				.execute();
	}

	/**
	 * Deletes files
	 * 
	 * @return deleted file path list
	 * @throws IOException
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
	 * Deletes files
	 * 
	 * @param toDeleteList list of file or directory paths to delete
	 * @return deleted file path list
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
			DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder().bucket(cfg.getBucket())
					.delete(del).build();
			System.out.println("Multiple objects are deleted!");

			DeleteObjectsResponse deleteResponse = s3ClientV2.deleteObjects(multiObjectDeleteRequest);
			return toStringDeletedObjects(deleteResponse.deleted());

		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Uploads file or directory with file transfer manager v2
	 * 
	 * @param sourcePath source path to upload
	 * @param targetPath target path in storage
	 * @return uploaded file path
	 * @throws IOException if file or directory cannot be uploaded
	 */
	public String uploadFileTransferManager(String sourcePath, String targetPath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFileTransferManager({},{})", sourcePath, targetPath);

		try {
			FileUpload upload = transferManager.uploadFile(b -> b.source(Paths.get(sourcePath))
					.putObjectRequest(req -> req.bucket(cfg.getBucket()).key(targetPath)));

			upload.completionFuture().join();

			return targetPath;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw e;
		}
	}

	/**
	 * Downloads file or directory with file transfer manager v2
	 * 
	 * @param sourcePath source path in storage to download
	 * @param targetPath target path
	 * @return downloaded file path
	 * @throws IOException if file or directory cannot be downloaded
	 */
	public String downloadFileTransferManager(String sourcePath, String targetPath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFileTransferManager({},{})", sourcePath, targetPath);

		try {

			FileDownload download = transferManager.downloadFile(b -> b.destination(Paths.get(targetPath))
					.getObjectRequest(req -> req.bucket(cfg.getBucket()).key(sourcePath)));
			download.completionFuture().join();

			return targetPath;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw e;
		}
	}

	/**
	 * Sets bucket
	 * 
	 * @param bucket bucket
	 * @throws IOException
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
	 * Gets current bucket
	 * 
	 * @return bucket
	 */
	public String getBucket() {
		return cfg.getBucket();
	}

	/**
	 * Gets buckets
	 * 
	 * @return list of buckets
	 * @throws IOException
	 */
	public List<String> getBuckets() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getBuckets()");

		AtomicCommand<List<String>> bucketGetter = new S3AtomicBucketListGetter(s3ClientV2);

		return new DefaultRetryStrategy<List<String>>(bucketGetter, cfg.getMaxRequestAttempts(),
				cfg.getFileCheckWaitTime()).execute();
	}

	/**
	 * Checks if bucket exists
	 * 
	 * @param bucketName bucket name
	 * @return true if bucket exists
	 * @throws IOException
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
	 * Deletes bucket
	 * 
	 * @param bucketName bucket name
	 * @throws IOException
	 */
	public void deleteBucket(String bucketName) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteBucket({})", bucketName);

		deleteFiles();
		deleteEmptyBucket(bucketName);
	}

	/**
	 * Initializes credentials
	 * 
	 * @param s3AccessKey       s3 access key
	 * @param s3SecretAccessKey s3 secret access key
	 */
	private void initCredentials(String s3AccessKey, String s3SecretAccessKey) {

		credentials = AwsBasicCredentials.create(s3AccessKey, s3SecretAccessKey);

		credentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create(s3AccessKey, s3SecretAccessKey));
	}

	/**
	 * Initializes transfer manager for productive upload and download
	 * 
	 * @param region region
	 */
	private void initTransferManager(Region region) {

		transferManager = S3TransferManager.builder()
				.s3ClientConfiguration(cfg -> cfg.credentialsProvider(credentialsProvider).region(region)
						.targetThroughputInGbps(20.0).minimumPartSizeInBytes((long) (10 * 1024 * 1024)))
				.build();
	}

	/**
	 * Creates bucket
	 * 
	 * @param bucketName bucket name
	 * @return bucket name if bucket has been created
	 * @throws IOException
	 */
	private String createBucket(String bucketName) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> createBucket({})", bucketName);

		AtomicCommand<String> bucketCreator = new S3AtomicBucketCreator(s3ClientV2, cfg.getBucket());

		return new DefaultRetryStrategy<String>(bucketCreator, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime())
				.execute();
	}

	/**
	 * Deletes empty bucket
	 * 
	 * @param bucketName bucket name
	 * @return deleted bucket name
	 * @throws IOException
	 */
	private String deleteEmptyBucket(String bucketName) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteEmptyBucket({})", bucketName);

		AtomicCommand<String> bucketDeleter = new S3AtomicBucketDeleter(s3ClientV2, cfg.getBucket());

		return new DefaultRetryStrategy<String>(bucketDeleter, cfg.getMaxRequestAttempts(), cfg.getFileCheckWaitTime())
				.execute();
	}

	/**
	 * Converts deleted object file list to deleted file string list
	 * 
	 * @param files deleted object file list
	 * @return deleted file string list
	 */
	private List<String> toStringDeletedObjects(List<DeletedObject> files) {

		List<String> fileNames = new ArrayList<String>();

		for (DeletedObject f : files) {
			fileNames.add(f.key());
		}

		return fileNames;
	}
}
