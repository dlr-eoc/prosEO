package de.dlr.proseo.storagemgr.version2.s3;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;

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

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3DataAccessLayer.class);

	public S3DataAccessLayer(String s3AccessKey, String s3SecretAccessKey) {
		AwsBasicCredentials s3Creds = AwsBasicCredentials.create(s3AccessKey, s3SecretAccessKey);

		s3Client = S3Client.builder().region(Region.EU_CENTRAL_1)
				.credentialsProvider(StaticCredentialsProvider.create(s3Creds)).build();
	}

	public S3DataAccessLayer(String s3AccessKey, String s3SecretAccessKey, String s3Region, String s3EndPoint) {
		AwsBasicCredentials s3Creds = AwsBasicCredentials.create(s3AccessKey, s3SecretAccessKey);

		s3Client = S3Client.builder().region(Region.of(s3Region)).endpointOverride(URI.create(s3EndPoint))
				.credentialsProvider(StaticCredentialsProvider.create(s3Creds)).build();

	}

	// high-level function
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

		// TODO: Delete all files from bucket
		// deleteObjectsInBucket(bucketName);

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

	public void deleteFile(String filePath) {
		DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucket).key(filePath).build();

		s3Client.deleteObject(request);
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
