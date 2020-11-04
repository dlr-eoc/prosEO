package de.dlr.proseo.storagemgr.fs.s3;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import alluxio.exception.FileAlreadyExistsException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author Hubert Asamer
 *
 */
public class S3Ops {

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3Ops.class);

	private static final Long MULTIPART_UPLOAD_PARTSIZE_BYTES = (long) (5 * 1024 * 1024);

	public static String createEmptyKey(S3Client s3, String bucketName, String key, String manifestMsg) {
		try {
			s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(),
					RequestBody.fromString(manifestMsg));
			return key;

		} catch (AwsServiceException | SdkClientException e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * List keys in bucket based on prefix
	 * 
	 * @param s3 the V1 S3 client to use
	 * @param bucketName the bucket name
	 * @param prefix the bucket prefix
	 * @return the keys contained in the bucket
	 */
	public static List<String> listObjectsInBucket(AmazonS3 s3, String bucketName, String prefix) {
		Boolean isTopLevel = false;
		String delimiter = "/";
		if (prefix == "" || prefix == "/") {
			isTopLevel = true;
		}
		if (!prefix.endsWith(delimiter)) {
			prefix += delimiter;
		}
		ListObjectsRequest listObjectsRequest = null;
		if (isTopLevel) {
			listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName);
		} else {
			listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix);
		}
		List<String> folderLike = new ArrayList<String>();
		ObjectListing objects = null;
		try {
			objects = s3.listObjects(listObjectsRequest);
		} catch (AmazonServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (com.amazonaws.SdkClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		for (S3ObjectSummary f : objects.getObjectSummaries()) {
			folderLike.add("s3://" + f.getBucketName() + "/" + f.getKey());
		}
		while (objects.isTruncated()) {
			try {
				objects = s3.listNextBatchOfObjects(objects);
			} catch (AmazonServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw e;
			} catch (com.amazonaws.SdkClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw e;
			}
			for (S3ObjectSummary f : objects.getObjectSummaries()) {
				folderLike.add("s3://" + f.getBucketName() + "/" + f.getKey());
			}
		}
		return folderLike;
	}

	/**
	 * List all buckets; passes all exceptions on to the caller
	 * 
	 * @param s3 the S3 client to use
	 * @return a list of buckets
	 */
	public static ArrayList<String> listBuckets(S3Client s3) {

			ArrayList<String> buckets = new ArrayList<String>();
			ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
			ListBucketsResponse listBucketsResponse = null;
			try {
				listBucketsResponse = s3.listBuckets(listBucketsRequest);
			} catch (Exception e) {
				logger.error(e.getMessage());
				throw e;
			}
			listBucketsResponse.buckets().stream().forEach(x -> buckets.add(x.name()));
			return buckets;
	}

	/**
	 * Creates a new S3 bucket
	 * 
	 * @param s3 the S3 client to use
	 * @param bucketName the name of the new bucket
	 * @param region the region, in which the bucket shall be stored
	 * @return the new bucket name or null, if the operation failed
	 */
	public static String createBucket(S3Client s3, String bucketName, String region) {

		try {
			CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket(bucketName)
					.createBucketConfiguration(CreateBucketConfiguration.builder().locationConstraint(region).build())
					.build();
			s3.createBucket(createBucketRequest);
			return createBucketRequest.bucket();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Create a base V2 S3 client
	 *
	 * @param s3AccessKey the access key for the client
	 * @param secretAccessKey the secret access key for the client
	 * @param s3Endpoint the S3 endpoint to connect to
	 * @param region the region, on which the client shall operate
	 * @return a configured S3 client or null, if an error occurred
	 */
	public static S3Client v2S3Client(String s3AccessKey, String secretAccessKey, String s3Endpoint, String region) {
		try {

			AwsBasicCredentials creds = AwsBasicCredentials.create(s3AccessKey, secretAccessKey);
			S3Client s3 = S3Client.builder().region(Region.of(region)).endpointOverride(URI.create(s3Endpoint))
					.credentialsProvider(StaticCredentialsProvider.create(creds)).build();
			return s3;
		} catch (software.amazon.awssdk.core.exception.SdkClientException e) {
			logger.error(e.getMessage());
			return null;
		} catch (java.lang.NullPointerException e1) {
			logger.error(e1.getMessage());
			return null;
		}
	}

	/**
	 * Create a base V1 S3 client
	 *
	 * @param s3AccessKey the access key for the client
	 * @param secretAccessKey the secret access key for the client
	 * @param s3Endpoint the S3 endpoint to connect to
	 * @param region the region, on which the client shall operate
	 * @return a configured S3 client or null, if an error occurred
	 */
	public static AmazonS3 v1S3Client(String s3AccessKey, String secretAccessKey, String s3Endpoint, String region) {
		try {

			BasicAWSCredentials awsCreds = new BasicAWSCredentials(s3AccessKey, secretAccessKey);
			ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setSignerOverride("AWSS3V4SignerType");
			AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
					.withEndpointConfiguration(
							new AwsClientBuilder.EndpointConfiguration(s3Endpoint, Region.of(region).id()))
					.withPathStyleAccessEnabled(true).withClientConfiguration(clientConfiguration)
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
			return amazonS3;
		} catch (AmazonServiceException e) {
			logger.error(e.getMessage());
			return null;
		} catch (AmazonClientException e) {
			logger.error(e.getMessage());
			return null;
		} catch (java.lang.NullPointerException e) {
			logger.error(e.getMessage());
			return null;
		}

	}

	/**
	 * Fetch file from S3 to local file
	 * 
	 * @param s3            a given instantiated S3Client
	 * @param s3Object      URI of S3-Object (e.g. s3://bucket/path/to/some/file)
	 * @param ContainerPath local target filePath
	 * @return true, if the operation succeeded, false otherwise
	 */
	public static Boolean v2FetchFile(S3Client s3, String s3Object, String ContainerPath) {

		try {
			File f = new File(ContainerPath);
			if (Files.exists(Paths.get(ContainerPath), LinkOption.NOFOLLOW_LINKS))
				f.delete();
			File subdirs = new File(FilenameUtils.getPrefix(ContainerPath) + FilenameUtils.getPath(ContainerPath));
			subdirs.mkdirs();

			AmazonS3URI s3uri = new AmazonS3URI(s3Object);
			s3.getObject(GetObjectRequest.builder().bucket(s3uri.getBucket()).key(s3uri.getKey()).build(),
					ResponseTransformer.toFile(Paths.get(ContainerPath)));
			logger.info("Copied " + s3Object + " to " + "file://" + ContainerPath);
			return true;
		} catch (software.amazon.awssdk.core.exception.SdkClientException e) {
			try {
				if (e.getCause().getCause().getCause().getClass().equals(FileAlreadyExistsException.class)) {
					return true;
				}
			} catch (Exception ee) {
				logger.error(ee.getMessage());
			}
			logger.error(e.getMessage());
			return false;
		} catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
			logger.error(s3Object + " --> " + e.getMessage());
			return false;
		} catch (software.amazon.awssdk.services.s3.model.NoSuchBucketException e) {
			logger.error(s3Object + " --> " + e.getMessage());
			return false;
		} catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
			logger.error(e.getMessage());
			return false;
		} catch (SecurityException e) {
			logger.error(e.getMessage());
			return false;
		}
	}

	/**
	 * Fetch file from S3 as input stream
	 * 
	 * @param s3            a given instantiated S3Client
	 * @param s3Object      URI of S3-Object (e.g. s3://bucket/path/to/some/file)
	 * @return the file content
	 */
	public static InputStream v2FetchStream(S3Client s3, String s3Object) {
		InputStream stream = null;
		try {
			AmazonS3URI s3uri = new AmazonS3URI(s3Object);
			stream = s3.getObject(GetObjectRequest.builder().bucket(s3uri.getBucket()).key(s3uri.getKey()).build(),
					ResponseTransformer.toInputStream());
		} catch (software.amazon.awssdk.core.exception.SdkClientException e) {
			logger.error(e.getMessage());
		} catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
			logger.error(s3Object + " --> " + e.getMessage());
		} catch (software.amazon.awssdk.services.s3.model.NoSuchBucketException e) {
			logger.error(s3Object + " --> " + e.getMessage());
		} catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
			logger.error(e.getMessage());
		} catch (SecurityException e) {
			logger.error(e.getMessage());
		}
		return stream;
	}

	/**
	 * Upload a directory to S3 using multipart uploads
	 * 
	 * @param v1S3Client  the S3 V1 client to use
	 * @param sourceDirPath    path to the directory to upload
	 * @param targetBucketName the name of the target bucket
	 * @param targetKeyPrefix  the key prefix to set for the target bucket
	 * @param recursive   true, if subdirectories shall be copied, too, false otherwise
	 * @param pause       (not used)
	 * @return a list of uploaded keys or null, if the operation failed
	 */
	public static ArrayList<String> v1UploadDir(AmazonS3 v1S3Client, String sourceDirPath, String targetBucketName, String targetKeyPrefix,
			boolean recursive, boolean pause) {
		ArrayList<String> response = new ArrayList<String>();
		TransferManager xfer_mgr = TransferManagerBuilder.standard()
				.withMultipartCopyPartSize(MULTIPART_UPLOAD_PARTSIZE_BYTES).withS3Client(v1S3Client).build();
		AmazonS3URI s3uri = new AmazonS3URI(targetBucketName);
		String bucket = s3uri.getBucket();
		try {
			MultipleFileUpload xfer = xfer_mgr.uploadDirectory(bucket, targetKeyPrefix, new File(sourceDirPath), recursive);
			// loop with Transfer.isDone()
			// or block with Transfer.waitForCompletion()

			V1XferMgrProgress.waitForCompletion(xfer);
			xfer_mgr.shutdownNow(false);

			// check files in s3 & add to response
			List<S3ObjectSummary> list = v1S3Client.listObjectsV2(bucket, targetKeyPrefix).getObjectSummaries();
			for (S3ObjectSummary o : list) {
				response.add(o.getKey());
			}
			logger.info("Copied dir://{} to {}/{}", sourceDirPath, targetBucketName, targetKeyPrefix);

			return response;
		} catch (AmazonServiceException e) {
			logger.error(e.getMessage());
			return null;
		} 
	}


	/**
	 * Upload file to S3 using multipart uploads
	 * 
	 * The source file is uploaded to a S3 storage. The target key in the storage is build as:
	 * s3://&lt;bucket name&gt;/&lt;target key prefix&gt;/&lt;source file name&gt;
	 * 
	 * @param v1S3Client  the S3 V1 client to use
	 * @param sourceFilePath The path of source file
	 * @param targetBucketName The S3 bucket to store the file
	 * @param targetKeyPrefix The key in the bucket to store the file
	 * @param pause       (not used)
	 * @return a single-element list of keys uploaded or null, if the operation failed
	 */
	public static ArrayList<String> v1UploadFile(AmazonS3 v1S3Client, String sourceFilePath, String targetBucketName, String targetKeyPrefix,
			boolean pause) {

		ArrayList<String> response = new ArrayList<String>();
		String targetKeyName = null;
		File f = new File(sourceFilePath);
		if (f != null && f.isFile()) {
			String fn = f.getName();
			if (targetKeyPrefix != null && !targetKeyPrefix.isEmpty()) {
				targetKeyName = targetKeyPrefix + (targetKeyPrefix.endsWith("/") ? "" : "/") + fn;
			} else {
				targetKeyName = fn;
			}
			AmazonS3URI s3uri = new AmazonS3URI(targetBucketName);
			String bucket = s3uri.getBucket();
			TransferManager xfer_mgr = TransferManagerBuilder.standard()
					.withMultipartCopyPartSize(MULTIPART_UPLOAD_PARTSIZE_BYTES).withS3Client(v1S3Client).build();
			try {
				// logger.info("before upload: " + bucket + ", " + key_name + ", " + f);
				Upload xfer = xfer_mgr.upload(bucket, targetKeyName, f);
				// loop with Transfer.isDone()
				// or block with Transfer.waitForCompletion()
				V1XferMgrProgress.waitForCompletion(xfer);
				// logger.info("after upload: " + bucket + ", " + key_name + ", " + f);
				String result = "s3://" + bucket + (targetKeyName.startsWith("/") ? "" : "/") + targetKeyName;
				response.add(result);
				logger.info("Copied file://{} to {}", sourceFilePath, result);
				xfer_mgr.shutdownNow(false);
				return response;
			} catch (AmazonServiceException e) {
				logger.error(e.getErrorMessage());
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Upload files or directories to S3 using Multipart-Uploads
	 * 
	 * @param v1S3Client  the S3 V1 client to use
	 * @param sourcePath    path to the file or directory to upload
	 * @param targetBucketName the name of the target bucket
	 * @param targetPathPrefix  the key prefix to set for the target bucket
	 * @param pause       (not used)
	 * @return a list of uploaded keys or null, if the operation failed
	 */
	public static ArrayList<String> v1Upload(AmazonS3 v1S3Client, String sourcePath, String targetBucketName, String targetPathPrefix,
			boolean pause) {

		String s3Prefix = "s3://";
		if (!targetBucketName.startsWith(s3Prefix)) {
			targetBucketName = s3Prefix + targetBucketName;
		}
		File f = new File(sourcePath);

		ArrayList<String> response;
		if (f.isFile()) {
			response = v1UploadFile(v1S3Client, sourcePath, targetBucketName, targetPathPrefix, false);
			if (null != response) {
				return response;
			}
		}
		if (f.isDirectory()) {
			response = v1UploadDir(v1S3Client, sourcePath, targetBucketName, targetPathPrefix, true, false);
			if (null != response) {
				return response;
			}
		}
		return null;
	}

	/**
	 * Copy objects between S3 buckets or inside S3 buckets
	 * 
	 * @param s3Client the S3 V1 client to use
	 * @param sourceBucketName the bucket to copy from
	 * @param sourceObjectKeyPrefix the object key to copy from
	 * @param destBucketName the bucket to copy to
	 * @param destObjectPrefix the object key to copy to 
	 * @return a list of copied keys or null, if the operation failed
	 */
	public static ArrayList<String> v1Copy(AmazonS3 s3Client, String sourceBucketName, String sourceObjectKeyPrefix,
			String destBucketName, String destObjectPrefix) {

		String separator = "/";
		
		if (destObjectPrefix.endsWith(separator)) {
			destObjectPrefix = destObjectPrefix.substring(0, destObjectPrefix.length() - 1);
		}
		
		ArrayList<String> response = new ArrayList<String>();
		TransferManager xfer_mgr = TransferManagerBuilder.standard()
				.withMultipartCopyPartSize(MULTIPART_UPLOAD_PARTSIZE_BYTES).withS3Client(s3Client).build();
		try {

			// list objects under sourceObjectKey (is 1:1 if single file, is 1:n if "folder")
			List<S3ObjectSummary> list = s3Client.listObjectsV2(sourceBucketName, sourceObjectKeyPrefix).getObjectSummaries();


			for (S3ObjectSummary o : list) {
				// destinationKey is built using this pattern: <destObjectPrefix>/<last elem of key>
				String key = o.getKey();
				Copy xfer = xfer_mgr.copy(sourceBucketName, key, destBucketName, destObjectPrefix+separator+key);
				V1XferMgrProgress.waitForCompletion(xfer);
				response.add("s3://" + destBucketName + "/" + destObjectPrefix + "/" + key);
				logger.info("Copied s3://{}/{} to s3://{}/{}/{}", sourceBucketName, key, destBucketName, destObjectPrefix,key);
			}


			xfer_mgr.shutdownNow(false);
			return response;
		} catch (AmazonServiceException e) {
			logger.error(e.getErrorMessage());
			return null;
		}

	}

	/**
	 * Create a folder-like object in repository. This method passes low-level S3 exceptions on.
	 * 
	 * @param client the S3 V2 client to use
	 * @param bucketName the name of the bucket to create the folder in
	 * @param folderName the name of the folder to create
	 * @throws SdkException Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for catch all scenarios.
     * @throws SdkClientException If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws S3Exception Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @throws AwsServiceException if an error in the S3 object storage service occurs
	 */
	public static void createFolder(S3Client client, String bucketName, String folderName) 
			throws SdkException, SdkClientException, S3Exception, AwsServiceException {
	    // create meta-data for your folder and set content-length to 0
		String key = folderName;
		if (!key.endsWith("/")) {
		    key += "/";
		}		
		PutObjectRequest putRequest = PutObjectRequest.builder()
		        .bucket(bucketName)
		        .key(key)
		        .build();
		try {
			client.putObject(putRequest, RequestBody.empty());
		} catch (S3Exception e) {
			logger.error(e.getMessage());
			throw e;
		} catch (AwsServiceException e) {
			logger.error(e.getMessage());
			throw e;
		} catch (SdkClientException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}

	/**
	 * Delete object(s) in repository.
	 * The prefix is either an object key or like a directory path 
	 * 
	 * @param client the S3 V1 client to use
	 * @param bucketName the name of the bucket to delete the object(s) in
	 * @param prefix the object prefix
	 */
	public static void deleteDirectory(AmazonS3 client, String bucketName, String prefix) {
	    try {
			ObjectListing objectList = client.listObjects(bucketName, prefix );
			List<S3ObjectSummary> objectSummeryList =  objectList.getObjectSummaries();
			while (objectList.isTruncated()) {
				objectList = client.listNextBatchOfObjects(objectList);
				objectSummeryList.addAll(objectList.getObjectSummaries());
			}
			
			String[] keysList = new String[ objectSummeryList.size() ];
			int count = 0;
			for( S3ObjectSummary summery : objectSummeryList ) {
			    keysList[count++] = summery.getKey();
			}
			DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest( bucketName ).withKeys( keysList );
			client.deleteObjects(deleteObjectsRequest);
		} catch (AmazonServiceException e) {
			logger.error(e.getMessage());
			throw e;
		} catch (com.amazonaws.SdkClientException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Get the object length (file size)
	 * 
	 * @param client the S3 V1 client to use
	 * @param bucketName the name of the bucket, in which the object is stored
	 * @param key the object key
	 * @return the length of the object or zero, if no object metadata could be retrieved
	 */
	public static long getLength(AmazonS3 client, String bucketName, String key) {
		ObjectMetadata md;
		try {
			md = client.getObjectMetadata(bucketName, key);
		} catch (AmazonServiceException e) {
			logger.error(e.getMessage());
			throw e;
		} catch (com.amazonaws.SdkClientException e) {
			logger.error(e.getMessage());
			throw e;
		}
		if (md != null) {
			return md.getContentLength();
		}
		return 0;
	}

}
