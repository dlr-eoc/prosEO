package de.dlr.proseo.storagemgr.version2.s3;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.transfer.s3.S3ClientConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.CompletedDownload;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.Download;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.FileUpload;
import software.amazon.awssdk.transfer.s3.Upload;
import software.amazon.awssdk.transfer.s3.UploadRequest;

/**
 * S3 Data Access Layer based on Amazon S3 SDK v2
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3DataAccessLayer {

	/** S3 Client */
	private S3Client s3Client;

	private String bucket;

	private AwsBasicCredentials credentials;
	private AwsCredentialsProvider credentialsProvider;
	private S3TransferManager transferManager;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3DataAccessLayer.class);

	public S3DataAccessLayer(String s3AccessKey, String s3SecretAccessKey) {

		Region s3Region = Region.EU_CENTRAL_1;

		initCredentials(s3AccessKey, s3SecretAccessKey);
		initTransferManager(s3Region);

		s3Client = S3Client.builder().region(s3Region)
				.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
	}

	public S3DataAccessLayer(String s3AccessKey, String s3SecretAccessKey, String s3Region, String s3EndPoint) {

		initCredentials(s3AccessKey, s3SecretAccessKey);

		s3Client = S3Client.builder().region(Region.of(s3Region)).endpointOverride(URI.create(s3EndPoint))
				.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
	}

	private void initCredentials(String s3AccessKey, String s3SecretAccessKey) {

		credentials = AwsBasicCredentials.create(s3AccessKey, s3SecretAccessKey);

		credentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create(s3AccessKey, s3SecretAccessKey));
	}

	private void initTransferManager(Region region) {

		transferManager = S3TransferManager.builder()
				.s3ClientConfiguration(cfg -> cfg.credentialsProvider(credentialsProvider).region(region)
						.targetThroughputInGbps(20.0).minimumPartSizeInBytes((long) (10 * 1024 * 1024)))
				.build();
	}

	public void setBucket(String bucket) {

		if (!bucketExists(bucket)) {
			createBucket(bucket);
		}

		this.bucket = bucket;
	}

	public String getBucket() {
		return bucket;
	}

	public List<String> getBuckets() {
		try {
			ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
			return toStringBuckets(listBucketsResponse.buckets());
		} catch (Exception e) {
			System.out.println("Cannot get s3 buckets " + e.getMessage());
			throw e;
		}
	}

	public boolean bucketExists(String bucketName) {
		List<String> buckets = getBuckets();

		for (String bucket : buckets) {
			if (bucket.equals(bucketName)) {
				return true;
			}
		}

		return false;
	}

	public void deleteBucket(String bucketName) {

		deleteFiles();
		deleteEmptyBucket(bucketName);
	}

	public boolean fileExists(String filePath) {
		try {
			s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(filePath).build());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public String deleteFile(String filePath) {

		ArrayList<ObjectIdentifier> toDelete = new ArrayList<ObjectIdentifier>();
		toDelete.add(ObjectIdentifier.builder().key(filePath).build());

		try {
			DeleteObjectsRequest dor = DeleteObjectsRequest.builder().bucket(bucket)
					.delete(Delete.builder().objects(toDelete).build()).build();
			s3Client.deleteObjects(dor);
			System.out.println("Successfully deleted object " + filePath);

		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
			e.printStackTrace();
			throw e;
		}

		return filePath;
	}

	public void deleteFiles() {

		for (String file : getFiles()) {
			deleteFile(file);
		}
	}

	public List<String> deleteFiles(List<String> toDeleteList) {

		ArrayList<ObjectIdentifier> keys = new ArrayList<>();
		ObjectIdentifier objectId = null;

		for (String toDelete : toDeleteList) {

			objectId = ObjectIdentifier.builder().key(toDelete).build();
			keys.add(objectId);
		}

		Delete del = Delete.builder().objects(keys).build();

		try {
			DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder().bucket(bucket).delete(del)
					.build();
			s3Client.deleteObjects(multiObjectDeleteRequest);
			System.out.println("Multiple objects are deleted!");

		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
			e.printStackTrace();
			throw e;
		}

		return toDeleteList;
	}

	public void deleteFolder(String prefix) {

		ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
		ListObjectsV2Iterable list = s3Client.listObjectsV2Paginator(request);

		List<ObjectIdentifier> objectIdentifiers = list.stream().flatMap(r -> r.contents().stream())
				.map(o -> ObjectIdentifier.builder().key(o.key()).build()).collect(Collectors.toList());

		if (objectIdentifiers.isEmpty())
			return;
		DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder().bucket(bucket)
				.delete(Delete.builder().objects(objectIdentifiers).build()).build();

		try {
			s3Client.deleteObjects(deleteObjectsRequest);
			System.out.println("Folder/prefix deleted successfully " + prefix);

		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
			e.printStackTrace();
			throw e;
		}
	}

	public String uploadFile(String sourcePath, String targetPath) throws IOException {

		try {
			PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(targetPath).build();

			s3Client.putObject(request, RequestBody.fromFile(new File(sourcePath)));

			S3Waiter waiter = s3Client.waiter();
			HeadObjectRequest requestWait = HeadObjectRequest.builder().bucket(bucket).key(targetPath).build();

			WaiterResponse<HeadObjectResponse> waiterResponse = waiter.waitUntilObjectExists(requestWait);
			waiterResponse.matched().response().ifPresent(System.out::println);

			System.out.println("File " + targetPath + " was uploaded.");

			return targetPath;

		} catch (Exception e) {
			logger.error(e.getMessage());
			throw e;
		}
	}

	public String uploadFileTransferManager(String sourcePath, String targetPath) throws IOException {

		try {
			FileUpload upload = transferManager.uploadFile(
					b -> b.source(Paths.get(sourcePath)).putObjectRequest(req -> req.bucket(bucket).key(targetPath)));

			upload.completionFuture().join();

			return targetPath;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw e;
		}
	}
	

	public String downloadFileTransferManager(String sourcePath, String targetPath) throws IOException {

		try {

			FileDownload download = transferManager.downloadFile(b -> b.destination(Paths.get(targetPath))
					.getObjectRequest(req -> req.bucket(bucket).key(sourcePath)));
			download.completionFuture().join();

			return targetPath;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw e;
		}
	}

	public String downloadFile(String sourcePath, String targetPath) throws IOException {

		GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(sourcePath).build();
		ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
		BufferedOutputStream outputStream;

		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(targetPath));
			byte[] buffer = new byte[4096];
			int bytesRead = -1;

			while ((bytesRead = response.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			response.close();
			outputStream.close();

			return targetPath;

		} catch (IOException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}

	public List<String> getFiles() {
		ListObjectsRequest request = ListObjectsRequest.builder().bucket(bucket).build();

		ListObjectsResponse response = s3Client.listObjects(request);
		return toStringFiles(response.contents());
	}

	public List<String> getFiles(String folder) {

		ListObjectsRequest request = ListObjectsRequest.builder().bucket(bucket).prefix(folder).build();

		ListObjectsResponse response = s3Client.listObjects(request);
		return toStringFiles(response.contents());
	}

	public long getFileSize(String filePath) {

		HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucket).key(filePath).build();
		HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

		return headObjectResponse.contentLength();
	}

	private boolean createBucket(String bucketName) {

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
			return false;
		}
	}

	private void deleteEmptyBucket(String bucketName) {
		DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
		s3Client.deleteBucket(deleteBucketRequest);
	}

	private List<String> toStringBuckets(List<Bucket> buckets) {

		List<String> bucketNames = new ArrayList<String>();

		for (Bucket bucket : buckets) {
			bucketNames.add(bucket.name());
		}

		return bucketNames;
	}

	private List<String> toStringFiles(List<S3Object> files) {

		List<String> fileNames = new ArrayList<String>();

		for (S3Object f : files) {
			fileNames.add(f.key());
		}

		return fileNames;
	}

	/*
	 * // TODO: Delete? Do we need async upload? add attempts private void
	 * uploadFileAsync(String bucketName, String filePath) { PutObjectRequest
	 * request =
	 * PutObjectRequest.builder().bucket(bucketName).key(filePath).build();
	 * 
	 * theS3Client.putObject(request, RequestBody.fromFile(new File(filePath))); }
	 */

}
