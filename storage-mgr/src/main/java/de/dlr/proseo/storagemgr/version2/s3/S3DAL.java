package de.dlr.proseo.storagemgr.version2.s3;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.core.ResponseInputStream;

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

	/** S3 Client */
	private S3Client s3Client;

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
	 */
	public S3DAL(S3Configuration cfg) {

		this.cfg = cfg;

		if (cfg.isDefaultRegion()) {
			initS3Client();
		} else {
			initS3ClientWithRegion();
		}
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
	 * Initialization of s3 client
	 */
	public void initS3Client() {

		Region s3Region = Region.EU_CENTRAL_1;

		initCredentials(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey());
		initTransferManager(s3Region);

		s3Client = S3Client.builder().region(s3Region)
				.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();

		setBucket(cfg.getBucket());
	}

	/**
	 * Initialization of s3 client with region
	 */
	public void initS3ClientWithRegion() {

		initCredentials(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey());
		initTransferManager(Region.of(cfg.getS3Region()));

		s3Client = S3Client.builder().region(Region.of(cfg.getS3Region()))
				.endpointOverride(URI.create(cfg.getS3EndPoint()))
				.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();

		setBucket(cfg.getBucket());
	}

	/**
	 * Gets all files from current bucket
	 *
	 * @return list of all files
	 */
	public List<String> getFiles() {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles()");

		ListObjectsRequest request = ListObjectsRequest.builder().bucket(cfg.getBucket()).build();

		ListObjectsResponse response = s3Client.listObjects(request);
		return toStringFiles(response.contents());
	}

	/**
	 * Gets files which match path (prefix)
	 * 
	 * @param path path (prefix)
	 * @return list if files
	 */
	public List<String> getFiles(String folder) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", folder);

		ListObjectsRequest request = ListObjectsRequest.builder().bucket(cfg.getBucket()).prefix(folder).build();

		ListObjectsResponse response = s3Client.listObjects(request);
		return toStringFiles(response.contents());
	}

	/**
	 * Checks if file exists in storage
	 * 
	 * @param filePath file path
	 * @return
	 */
	public boolean fileExists(String filePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> fileExists({},{})", filePath);

		try {
			s3Client.headObject(HeadObjectRequest.builder().bucket(cfg.getBucket()).key(filePath).build());
			return true;
		} catch (Exception e) {
			return false;
		}
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
	 */
	public long getFileSize(String filePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFileSize({})", filePath);

		HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(cfg.getBucket()).key(filePath).build();
		HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

		return headObjectResponse.contentLength();
	}

	/**
	 * Gets file content
	 * 
	 * @param filePath file path
	 * @return file content
	 */
	public String getFileContent(String filePath) throws IOException {

		GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(cfg.getBucket()).key(filePath).build();

		ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);

		// InputStream stream = new
		// ByteArrayInputStream(responseInputStream.readAllBytes());

		String content = new String(responseInputStream.readAllBytes(), StandardCharsets.UTF_8);

		System.out.println("Content :" + content);

		return content;
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

		AtomicCommand fileUploader = new S3AtomicFileUploader(s3Client, cfg.getBucket(), sourceFile, targetFileOrDir);
		try {
			return new S3DefaultRetryStrategy(fileUploader, cfg.getMaxUploadAttempts(), cfg.getFileCheckWaitTime())
					.execute();
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			throw e;
		}
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

		AtomicCommand fileDownloader = new S3AtomicFileDownloader(s3Client, cfg.getBucket(), sourceFile,
				targetFileOrDir);
		try {
			return new S3DefaultRetryStrategy(fileDownloader, cfg.getMaxUploadAttempts(), cfg.getFileCheckWaitTime())
					.execute();
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			throw e;
		}
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
	 * @throws IOException if file or directorz cannot be downloaded
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
	 */
	public List<String> delete(String fileOrDir) {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", fileOrDir);

		return deleteS3Data(s3Client, cfg.getBucket(), fileOrDir);
	}

	/**
	 * Deletes file
	 * 
	 * @param filepath file path
	 * @return deleted file path
	 */
	public String deleteFile(String filepath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", filepath);

		ArrayList<ObjectIdentifier> toDelete = new ArrayList<ObjectIdentifier>();
		toDelete.add(ObjectIdentifier.builder().key(filepath).build());

		try {
			DeleteObjectsRequest dor = DeleteObjectsRequest.builder().bucket(cfg.getBucket())
					.delete(Delete.builder().objects(toDelete).build()).build();
			s3Client.deleteObjects(dor);
			System.out.println("Successfully deleted object " + filepath);

			DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(dor);
			return toStringDeletedObject(deleteResponse.deleted());

		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Deletes files
	 * 
	 * @return deleted file path list
	 */
	public List<String> deleteFiles() {

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

			DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(multiObjectDeleteRequest);
			return toStringDeletedObjects(deleteResponse.deleted());

		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Uploads file or directory with file transfer manager
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
	 * Downloads file or directory with file transfer manager
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
	 */
	public void setBucket(String bucket) {

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
	 */
	public List<String> getBuckets() {

		try {
			ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
			return toStringBuckets(listBucketsResponse.buckets());
		} catch (Exception e) {
			System.out.println("Cannot get s3 buckets " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Checks if bucket exists
	 * 
	 * @param bucketName bucket name
	 * @return true if bucket exists
	 */
	public boolean bucketExists(String bucketName) {

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
	 */
	public void deleteBucket(String bucketName) {

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
	 * Deletes S3 file or directory which matches prefix
	 * 
	 * @param s3Client s3 client
	 * @param bucket   bucket
	 * @param prefix   prefix
	 * @return deleted file path list
	 */
	private List<String> deleteS3Data(S3Client s3Client, String bucket, String prefix) {

		ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
		ListObjectsV2Iterable list = s3Client.listObjectsV2Paginator(request);

		List<ObjectIdentifier> objectIdentifiers = list.stream().flatMap(r -> r.contents().stream())
				.map(o -> ObjectIdentifier.builder().key(o.key()).build()).collect(Collectors.toList());

		if (objectIdentifiers.isEmpty())
			return new ArrayList<String>();
		DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder().bucket(bucket)
				.delete(Delete.builder().objects(objectIdentifiers).build()).build();

		DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(deleteObjectsRequest);
		return toStringDeletedObjects(deleteResponse.deleted());
	}

	/**
	 * Creates bucket
	 * 
	 * @param bucketName bucket name
	 * @return true if bucket has been created
	 */
	private boolean createBucket(String bucketName) {

		if (logger.isTraceEnabled())
			logger.trace(">>> createBucket({})", bucketName);

		try {
			S3Waiter s3Waiter = s3Client.waiter();
			CreateBucketRequest bucketRequest = CreateBucketRequest.builder().bucket(bucketName).build();

			s3Client.createBucket(bucketRequest);
			HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder().bucket(bucketName).build();

			// Wait until the bucket is created and print out the response
			WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
			waiterResponse.matched().response().ifPresent(System.out::println);
			System.out.println("Bucket " + bucketName + " has been created");

			return true;

		} catch (Exception e) {
			System.err.println(e.getMessage());
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Deletes empty bucket
	 * 
	 * @param bucketName bucket name
	 */
	private void deleteEmptyBucket(String bucketName) {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteEmptyBucket({})", bucketName);

		DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
		s3Client.deleteBucket(deleteBucketRequest);
	}

	/**
	 * Converts bucket object list in bucket string list
	 * 
	 * @param buckets bucket object list
	 * @return bucket string list
	 */
	private List<String> toStringBuckets(List<Bucket> buckets) {

		List<String> bucketNames = new ArrayList<String>();

		for (Bucket bucket : buckets) {
			bucketNames.add(bucket.name());
		}

		return bucketNames;
	}

	/**
	 * Converts s3 object file list to file string list
	 * 
	 * @param files s3 object file list
	 * @return file string list
	 */
	private List<String> toStringFiles(List<S3Object> files) {

		List<String> fileNames = new ArrayList<String>();

		for (S3Object f : files) {
			fileNames.add(f.key());
		}

		return fileNames;
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

	/**
	 * Converts deleted object file to deleted file string
	 * 
	 * @param files files
	 * @return deleted storage file path
	 */
	private String toStringDeletedObject(List<DeletedObject> files) {

		List<String> fileNames = new ArrayList<String>();

		for (DeletedObject f : files) {
			fileNames.add(f.key());
		}

		if (fileNames.size() > 1) {
			System.out.println("Expected 1 s3 object to delete. Deleted:" + fileNames.size());
		}

		return fileNames.get(0);
	}
}
