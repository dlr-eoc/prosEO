package de.dlr.proseo.storagemgr.fs.s3;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.io.File;
import java.util.List;
import java.util.ListIterator;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
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
 * S3 Operations for Storage Manager based on Amazon S3 SDK v2
 * 
 * @author Denys Chaykovskiy
 *
 */

@Component
public class ProseoS3 {

	// TODO: GLOBAL S3
	// TODO: improve create bucket loop and use it for all interface operations
	// (public methods)
	// TODO: maybe anonymous classes and command interface for loop calls
	// TODO: no exceptions for public calls? - we expect it
	// TODO: Unit-tests for all public methods
	// TODO: later - make interface for proseo file system s3 and posix
	// TODO: remove s3 v1 completely
	// TODO: link in pom.xml only used awssdk libraries, no v1

	/** Amount of retries and time interval */
	private static final int BUCKET_CREATION_RETRIES = 3;
	private static final int BUCKET_CREATION_RETRY_INTERVAL = 5000;

	// private static final int FILE_UPLOAD_RETRIES = 3;
	// private static final int FILE_DOWNLOAD_RETRIES = 3;

	/** S3 Client singleton */
	private static S3Client theS3Client;

	@Autowired
	private StorageManagerConfiguration cfg;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProseoS3.class);

	/**
	 * Instance of proseo s3
	 * 
	 * @return file cache singleton
	 */
	public static S3Client getInstance() {

		return theS3Client;
	}

	/**
	 * Initializes s3 client
	 */
	@PostConstruct
	private void init() {

		if (logger.isTraceEnabled())
			logger.trace(">>> init()");

		initializeS3Client();
	}

	private void initializeS3Client() {

		String s3AccessKey = ""; 
		String s3SecretAccessKey = ""; 
			
		// String s3AccessKey = cfg.getS3AccessKey(); 
		// String s3SecretAccessKey = cfg.getS3SecretAccessKey(); 
		// String s3Region = cfg.getS3Region();
		// String s3EndPoint = cfg.getS3EndPoint();

		AwsBasicCredentials s3Creds = AwsBasicCredentials.create(s3AccessKey, s3SecretAccessKey);

		// TODO: One builder for all like that
		// theS3Client = S3Client.builder().region(Region.of(s3Region))
		//      .endpointOverride(URI.create(s3EndPoint)).credentialsProvider(StaticCredentialsProvider.create(s3Creds)).build();

		// Amazon only, for tests 
		theS3Client = S3Client.builder().region(Region.EU_CENTRAL_1)
				.credentialsProvider(StaticCredentialsProvider.create(s3Creds)).build();

	}


	public ProseoS3() {
		initializeS3Client();
	}

	public Bucket getBucket(String bucketName) {

		if (bucketExists(bucketName)) {
			return findBucket(bucketName);
		}

		return createBucketWithRetries(bucketName);
	}

	/**
	 * List all buckets; passes all exceptions on to the caller
	 * 
	 * @param s3 the S3 client to use
	 * @return a list of buckets
	 */
	
	// TODO: Change try-catch behavior 
	public List<Bucket> getBuckets() {

		// if (logger.isTraceEnabled()) logger.trace(">>> listBuckets({})", s3);

		ListBucketsResponse listBucketsResponse = null;
		try {
			listBucketsResponse = theS3Client.listBuckets();
		} catch (Exception e) {
			// logger.error(e.getMessage());
			throw e;
		}

		return listBucketsResponse.buckets();
	}

	// TODO: Move show methods to tests
	public void showBuckets() {
		List<Bucket> buckets = getBuckets();

		System.out.println("FOUND: " + buckets.size() + " buckets");

		for (Bucket bucket : buckets) {
			System.out.println("BUCKET: " + bucket.name());
		}
	}

	public boolean bucketExists(String bucketName) {
		List<Bucket> buckets = getBuckets();

		for (Bucket bucket : buckets) {
			if (bucket.name().equals(bucketName)) {
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

	public boolean fileExists(String bucket, String filePath) {

		try {
			theS3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(filePath).build());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void deleteFile(String bucketName, String filePath) {
		DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucketName).key(filePath).build();

		theS3Client.deleteObject(request);
	}

	// TODO: Do we need async upload? add attempts
	public void uploadFileAsync(String bucketName, String filePath) {
		PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName).key(filePath).build();

		theS3Client.putObject(request, RequestBody.fromFile(new File(filePath)));
	}

	public void uploadFile(String fromFilePath, String toBucketName, String toFilePath) {

		PutObjectRequest request = PutObjectRequest.builder().bucket(toBucketName).key(toFilePath).build();

		theS3Client.putObject(request, RequestBody.fromFile(new File(fromFilePath)));

		S3Waiter waiter = theS3Client.waiter();
		HeadObjectRequest requestWait = HeadObjectRequest.builder().bucket(toBucketName).key(toFilePath).build();

		WaiterResponse<HeadObjectResponse> waiterResponse = waiter.waitUntilObjectExists(requestWait);

		waiterResponse.matched().response().ifPresent(System.out::println);

		System.out.println("File " + toFilePath + " was uploaded.");
	}

	public void downloadFile(String fromBucketName, String fromFilePath, String toFilePath) throws IOException {
		GetObjectRequest request = GetObjectRequest.builder().bucket(fromBucketName).key(fromFilePath).build();

		ResponseInputStream<GetObjectResponse> response = theS3Client.getObject(request);

		// String fileName = new File(toFilePath).getName();
		BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(toFilePath));

		byte[] buffer = new byte[4096];
		int bytesRead = -1;

		while ((bytesRead = response.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}

		response.close();
		outputStream.close();
	}

	// TODO: Maybe make List<ProseoFile> or similar
	public List<S3Object> getFiles(String bucketName) {
		ListObjectsRequest request = ListObjectsRequest.builder().bucket(bucketName).build();

		ListObjectsResponse response = theS3Client.listObjects(request);
		return response.contents();
	}

	public void showFiles(String bucketName) {
		ListIterator<S3Object> listIterator = getFiles(bucketName).listIterator();

		System.out.println("Objects in bucket: " + bucketName);

		while (listIterator.hasNext()) {
			S3Object object = listIterator.next();
			System.out.println(object.key() + " - " + object.size());
		}
	}

	private Bucket findBucket(String bucketName) {
		List<Bucket> buckets = getBuckets();
		Bucket desiredBucket = null;

		for (Bucket bucket : buckets) {
			if (bucket.name().equals(bucketName))
				desiredBucket = bucket;
		}

		if (desiredBucket == null) {
			throw new IllegalArgumentException("Cannot find Bucket: " + bucketName + " in findBucket()");
		}

		return desiredBucket;
	}

	private Bucket createBucketWithRetries(String bucketName) {
		boolean bucketCreated = false;

		for (int i = 1; i <= BUCKET_CREATION_RETRIES; i++) {
			if (createBucket(bucketName)) {
				bucketCreated = true;
				break;
			}

			// TODO: make a separate method?
			try {
				Thread.sleep(BUCKET_CREATION_RETRY_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		// TODO: Maybe do not throw exception, we expect it
		if (!bucketCreated) {
			throw new IllegalArgumentException(
					"Cannot create Bucket " + bucketName + " after " + BUCKET_CREATION_RETRIES + " attempts");
		}

		return findBucket(bucketName);
	}

	// Create a bucket by using a S3Waiter object
	private boolean createBucket(String bucketName) {

		try {
			S3Waiter s3Waiter = theS3Client.waiter();
			CreateBucketRequest bucketRequest = CreateBucketRequest.builder().bucket(bucketName).build();

			theS3Client.createBucket(bucketRequest);
			HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder().bucket(bucketName).build();

			// Wait until the bucket is created and print out the response
			WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
			waiterResponse.matched().response().ifPresent(System.out::println);
			System.out.println("Bucket " + bucketName + " has been created");

			return true;

		} catch (Exception e) {
			System.err.println(e.getMessage());
			// log somethere like logInfo
			return false;
		}
	}

	private void deleteEmptyBucket(String bucketName) {
		DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
		theS3Client.deleteBucket(deleteBucketRequest);
	}

}
