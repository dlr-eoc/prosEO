package de.dlr.proseo.storagemgr.fs.s3;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.io.File;
import java.util.List;
import java.util.ListIterator;


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


public class ProseoS3 {

	// TODO: GLOBAL S3
	// TODO: improve create bucket loop and use it for all interface operations (public methods)
	// TODO: maybe anonymous classes and command interface for loop calls 
	// TODO: no exceptions for public calls? - we expect it
	// TODO: Unit-tests for all public methods 
	// TODO: later - make interface for proseo file system s3 and posix
	// TODO: remove s3 v1 completely 
	// TODO: link in pom.xml only used awssdk libraries, no v1 
		
	private static final int BUCKET_CREATION_RETRIES = 3;
	private static final int BUCKET_CREATION_RETRY_INTERVAL = 5000;
	
	// private static final int FILE_UPLOAD_RETRIES = 3;
	// private static final int FILE_DOWNLOAD_RETRIES = 3;

	private S3Client s3Client;

	public static void start() throws IOException
	{
		// TODO: move to tests and split 
		// TODO: make tests platform-independent /../
		
		String testBucket = "created-bucket-upload-aaaaffgkjk";
		String testFile = "E:\\s3testfolder\\s3testfile.txt";
		String testDownloadFile = "E:\\s3testfolder\\download\\s3testfile.txt";
		
		ProseoS3 s3 = new ProseoS3();
		
		s3.showBuckets();
		
		// create bucket
		s3.getBucket(testBucket); 
		s3.showBuckets();
		
		System.out.println(); 
		
		// upload file
		s3.uploadFile(testFile, testBucket, testFile);
		s3.showFiles(testBucket);
		
		System.out.println(); 
		
		// download file 
		s3.downloadFile(testBucket, testFile, testDownloadFile);
		
		System.out.println(); 
		
		// delete file 
		s3.deleteFile(testBucket, testFile);
		s3.showFiles(testBucket);
		
		System.out.println(); 
		
		// delete bucket 
		s3.deleteBucket(testBucket); 
	
		// show buckets 
		s3.showBuckets();
			
		System.out.println("OK - SUCCESS"); 
		
	}

	private void initializeS3Client() {
		
		// TODO: Read from cfg (make component) 
	
		String s3AccessKey = "";
		String secretAccessKey = "";
		
		
		
		String region = "eu-north-1"; 
		String s3EndPoint = "ec2.eu-north-1.amazonaws.com";
		
		AwsBasicCredentials creds = AwsBasicCredentials.create(s3AccessKey, secretAccessKey);
		
		System.out.println("URI: " + URI.create(s3EndPoint).toString());
		System.out.println("Region: " + Region.of(region).toString()); 
		
		
		// TODO: One builder
		// All 
		// s3Client = S3Client.builder().region(Region.of(region)).endpointOverride(URI.create(s3EndPoint)).credentialsProvider(StaticCredentialsProvider.create(creds)).build();
		
		// Amazon only, for tests  
		s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).credentialsProvider(StaticCredentialsProvider.create(creds)).build();

		
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

	
	public List<Bucket> getBuckets() {

		// if (logger.isTraceEnabled()) logger.trace(">>> listBuckets({})", s3);

		ListBucketsResponse listBucketsResponse = null;
		try {
			listBucketsResponse = s3Client.listBuckets();
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
	
	
	public void deleteFile(String bucketName, String filePath) 
	{
       DeleteObjectRequest request = DeleteObjectRequest.builder()
                           .bucket(bucketName)
                           .key(filePath)
                           .build();
        
       s3Client.deleteObject(request);
	}

	// TODO: Do we need async upload? add attempts
	public void uploadFileAsync(String bucketName, String filePath) {
		PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName).key(filePath).build();

		s3Client.putObject(request, RequestBody.fromFile(new File(filePath)));
	}

	
	public void uploadFile(String fromFilePath, String toBucketName, String toFilePath) 
	{

		PutObjectRequest request = PutObjectRequest.builder().bucket(toBucketName).key(toFilePath).build();

		s3Client.putObject(request, RequestBody.fromFile(new File(fromFilePath)));

		S3Waiter waiter = s3Client.waiter();
		HeadObjectRequest requestWait = HeadObjectRequest.builder().bucket(toBucketName).key(toFilePath).build();

		WaiterResponse<HeadObjectResponse> waiterResponse = waiter.waitUntilObjectExists(requestWait);

		waiterResponse.matched().response().ifPresent(System.out::println);

		System.out.println("File " + toFilePath + " was uploaded.");
	}
	
	public void downloadFile(String fromBucketName, String fromFilePath, String toFilePath) throws IOException
	{
		GetObjectRequest request = GetObjectRequest.builder()
		                    .bucket(fromBucketName)
		                    .key(fromFilePath)
		                    .build();
		 
		ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
		 
		//	String fileName = new File(toFilePath).getName();
		BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(toFilePath));
		 
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		 
		while ((bytesRead = response.read(buffer)) !=  -1) {
		    outputStream.write(buffer, 0, bytesRead);
		}
		                     
		response.close();
		outputStream.close();
	}
	
	
	
	// TODO: Maybe make List<ProseoFile> or similar
	public List<S3Object> getFiles(String bucketName) 
	{
		ListObjectsRequest request = ListObjectsRequest.builder().bucket(bucketName).build();
        
        ListObjectsResponse response = s3Client.listObjects(request);
        return response.contents();        
	}
	
	public void showFiles(String bucketName) 
	{
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
			// log somethere like logInfo 
			return false;
		}
	}

	

	private void deleteEmptyBucket(String bucketName) {
		DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
		s3Client.deleteBucket(deleteBucketRequest);
	}

}

