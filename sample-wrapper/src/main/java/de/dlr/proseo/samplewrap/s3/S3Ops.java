package de.dlr.proseo.samplewrap.s3;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;


public class S3Ops {

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3Ops.class);

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
		logger.info("directory: " + dir_path + (recursive ?
				" (recursive)" : "") + (pause ? " (pause)" : ""));

		TransferManager xfer_mgr = TransferManagerBuilder
				.standard()
				.withS3Client(v1S3Client)
				.build();
		try {
			MultipleFileUpload xfer = xfer_mgr.uploadDirectory(bucket_name,
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

		// snippet-end:[s3.java1.s3_xfer_mgr_upload.single]
	}



}
