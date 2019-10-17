package de.dlr.proseo.model.fs.s3;

import java.io.File;
import java.net.URI;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
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


public class S3Ops {

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3Ops.class);

	private static final  Region S3_DEFAULT_REGION = Region.EU_CENTRAL_1;

	
	public static String createEmptyKey(S3Client s3, String bucketName, String key, String manifestMsg) {
		try {
			s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(key)
					.build(),
					RequestBody.fromString(manifestMsg));
			return key;
			
		} catch (AwsServiceException | SdkClientException e) {
			logger.error(e.getMessage());
			return null;
		}
	}
	
	/**
	 * list Keys in Bucket based on prefix
	 * 
	 * @param s3
	 * @param bucketName
	 * @param prefix
	 * @return List<String> the keys
	 */
	public static List<String> listKeysInBucket(AmazonS3 s3, String bucketName, String prefix, Boolean likeSimpleFolders) {
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
			listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withDelimiter(delimiter);
		} else {
			listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix)
					.withDelimiter(delimiter);
		}
		ObjectListing objects = s3.listObjects(listObjectsRequest);

		if (likeSimpleFolders) {
			List<String> folderLike = new ArrayList<String>();
			for (String f : objects.getCommonPrefixes()) {
				folderLike.add(f.replace(prefix, "").replace(delimiter, ""));
			}
			return folderLike;
		}
		return objects.getCommonPrefixes();
	}
	
	/**
	 * list all Buckets
	 * 
	 * @param s3
	 * @return ArrayList<String> buckets
	 */
	public static ArrayList<String> listBuckets(S3Client s3) {
		try {
			ArrayList<String> buckets = new ArrayList<String>();
			ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
			ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);
			listBucketsResponse.buckets().stream().forEach(x -> buckets.add(x.name()));
			return buckets;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Creates a new S3-Bucket
	 * 
	 * @param s3
	 * @return String the new bucket name
	 */
	public static String createBucket(S3Client s3, String bucketName) {


		try {
			CreateBucketRequest createBucketRequest = CreateBucketRequest
					.builder()
					.bucket(bucketName)
					.createBucketConfiguration(CreateBucketConfiguration.builder()
							.locationConstraint(S3_DEFAULT_REGION.id())
							.build())
					.build();
			s3.createBucket(createBucketRequest);
			return createBucketRequest.bucket();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/** 
	 * return Base V2 S3-Client
	 *
	 * @param s3AccessKey
	 * @param secretAccessKey
	 * @param s3Endpoint
	 * @return S3Client
	 */
	public static S3Client v2S3Client(String s3AccessKey, String secretAccessKey, String s3Endpoint) {
		try {

			AwsBasicCredentials creds = AwsBasicCredentials.create( s3AccessKey,secretAccessKey);
			S3Client s3 = S3Client.builder()
					.region(S3_DEFAULT_REGION)
					.endpointOverride(URI.create(s3Endpoint))
					.credentialsProvider(StaticCredentialsProvider.create(creds))
					.build();
			return s3;
		} catch(software.amazon.awssdk.core.exception.SdkClientException e) {
			logger.error(e.getMessage());
			return null;
		} catch (java.lang.NullPointerException e1) {
			logger.error(e1.getMessage());
			return null;
		}
	}

	/** 
	 * return Base V1 S3-Client
     *
	 * @param s3AccessKey
	 * @param secretAccessKey
	 * @param s3Endpoint
	 * @return AmazonS3
	 */
	public static AmazonS3 v1S3Client(String s3AccessKey, String secretAccessKey, String s3Endpoint) {
		try {

			BasicAWSCredentials awsCreds = new BasicAWSCredentials(s3AccessKey,secretAccessKey);
			ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setSignerOverride("AWSS3V4SignerType");
			AmazonS3 amazonS3 = AmazonS3ClientBuilder
					.standard()
					.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, S3_DEFAULT_REGION.id()))
					.withPathStyleAccessEnabled(true)
					.withClientConfiguration(clientConfiguration)
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
					.build();
			return amazonS3;
		}	catch (AmazonServiceException e) {
			logger.error(e.getMessage());
			return null;
		}  catch(AmazonClientException e) {
			logger.error(e.getMessage());
			return null;
		} catch (java.lang.NullPointerException e) {
			logger.error(e.getMessage());
			return null;
		}

	}


	/**
	 * fetch file from S3 to local file
	 * 
	 * @param s3 a given instantiated S3Client
	 * @param s3Object URI of S3-Object (e.g. s3://bucket/path/to/some/file)
	 * @param ContainerPath local target filePath
	 * @return
	 */
	public static Boolean v2FetchFile(S3Client s3, String s3Object, String ContainerPath) {

		try {
			File f = new File(ContainerPath);
			if (Files.exists(Paths.get(ContainerPath), LinkOption.NOFOLLOW_LINKS)) f.delete();
			File subdirs = new File(FilenameUtils.getPath(ContainerPath));
			subdirs.mkdirs();

			AmazonS3URI s3uri = new AmazonS3URI(s3Object);
			s3.getObject(GetObjectRequest.builder()
					.bucket(s3uri.getBucket())
					.key(s3uri.getKey())
					.build(),
					ResponseTransformer.toFile(Paths.get(ContainerPath)));
			logger.info("Copied " + s3Object + " to " + "file://" + ContainerPath);
			return true;
		} catch (software.amazon.awssdk.core.exception.SdkClientException e) {
			logger.error(e.getMessage());
			return false;
		} catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
			logger.error(s3Object+" --> "+e.getMessage());
			return false;
		} catch (software.amazon.awssdk.services.s3.model.NoSuchBucketException e) {
			logger.error(s3Object+" --> "+e.getMessage());
			return false;
		} catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
			logger.error(e.getMessage());
			return false;
		}  catch (SecurityException e) {
			logger.error(e.getMessage());
			return false;
		}
	}


	/**
	 * Upload Dir to S3 using Multipart-Uploads
	 * 
	 * @param v1S3Client AmazonS3 v1 client
	 * @param dir_path String
	 * @param bucket_name Bucket
	 * @param key_prefix String
	 * @param recursive Boolean
	 * @param pause Boolean
	 * @return True/False
	 */
	public static Boolean v1UploadDir(AmazonS3 v1S3Client, String dir_path, String bucket_name,
			String key_prefix, boolean recursive, boolean pause) {
		TransferManager xfer_mgr = TransferManagerBuilder
				.standard()
				.withS3Client(v1S3Client)
				.build();
		AmazonS3URI s3uri = new AmazonS3URI(bucket_name);
		String bucket = s3uri.getBucket();
		try {
			MultipleFileUpload xfer = xfer_mgr.uploadDirectory(bucket,
					key_prefix, new File(dir_path), recursive);
			// loop with Transfer.isDone()
			// or block with Transfer.waitForCompletion()
			V1XferMgrProgress.waitForCompletion(xfer);
			xfer_mgr.shutdownNow(false);
			return true;
		} catch (AmazonServiceException e) {
			logger.error(e.getErrorMessage());
			return false;
		}
	}

	/**
	 * Upload FileList to S3 using Multipart-Uploads
	 * 
	 * @param v1S3Client AmazonS3 v1 client
	 * @param file_paths String[]
	 * @param bucket_name Bucket
	 * @param key_prefix String
	 * @param pause Boolean
	 * @return True/False
	 */
	public static Boolean v1UploadFileList(AmazonS3 v1S3Client, String[] file_paths, String bucket_name,
			String key_prefix, boolean pause) {
		logger.info("file list: " + Arrays.toString(file_paths) +
				(pause ? " (pause)" : ""));
		// convert the file paths to a list of File objects (required by the
		// uploadFileList method)
		ArrayList<File> files = new ArrayList<File>();
		for (String path : file_paths) {
			files.add(new File(path));
		}

		TransferManager xfer_mgr = TransferManagerBuilder
				.standard()
				.withS3Client(v1S3Client)
				.build();
		try {
			MultipleFileUpload xfer = xfer_mgr.uploadFileList(bucket_name,
					key_prefix, new File("."), files);
			// loop with Transfer.isDone()
			// or block with Transfer.waitForCompletion()
			V1XferMgrProgress.waitForCompletion(xfer);
			xfer_mgr.shutdownNow(false);
			return true;
		} catch (AmazonServiceException e) {
			logger.error(e.getErrorMessage());
			return false;
		}
	}

	/**
	 * Upload File to S3 using Multipart-Uploads
	 * 
	 * @param v1S3Client AmazonS3 v1 client
	 * @param file_path String
	 * @param bucket_name String
	 * @param key_prefix String
	 * @param pause Boolean
	 * @return True/False
	 */
	public static Boolean v1UploadFile(AmazonS3 v1S3Client, String file_path, String bucket_name,
			String key_prefix, boolean pause) {

		String key_name = null;
		if (key_prefix != null) {
			key_name = key_prefix + '/' + file_path;
		} else {
			key_name = file_path;
		}
		AmazonS3URI s3uri = new AmazonS3URI(bucket_name);
		String bucket = s3uri.getBucket();
		File f = new File(file_path);
		TransferManager xfer_mgr = TransferManagerBuilder
				.standard()
				.withS3Client(v1S3Client)
				.build();
		try {
			Upload xfer = xfer_mgr.upload(bucket, key_name, f);
			// loop with Transfer.isDone()
			//  or block with Transfer.waitForCompletion()
			V1XferMgrProgress.waitForCompletion(xfer);
			xfer_mgr.shutdownNow(false);
			return true;
		} catch (AmazonServiceException e) {
			logger.error(e.getErrorMessage());
			return false;
		}
	}

	/**
	 * Upload Files or Dirs to S3 using Multipart-Uploads
	 * 
	 * @param v1S3Client AmazonS3 v1 client
	 * @param file_path String
	 * @param bucket_name String
	 * @param key_prefix String
	 * @param pause Boolean
	 * @return True/False
	 */
	public static Boolean v1Upload(AmazonS3 v1S3Client, String file_path, String bucket_name,
			String key_prefix, boolean pause) {

		File f = new File(file_path);

		if (f.isFile()) {
			if(v1UploadFile(v1S3Client,file_path,bucket_name,key_prefix,false)) {
				logger.info("Copied file://{} to {}",file_path, bucket_name+File.separator+key_prefix+File.separator+file_path);
				return true;
			}
		}
		if (f.isDirectory()) {
			if(v1UploadDir(v1S3Client,file_path,bucket_name,key_prefix,true,false)) {
				logger.info("Copied dir://{} to {}/{}",file_path,bucket_name,key_prefix);
				return true;
			}
		}
		return false;
	}
}
