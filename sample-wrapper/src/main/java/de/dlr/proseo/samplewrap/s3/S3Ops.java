package de.dlr.proseo.samplewrap.s3;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public static Boolean fetch(S3Client s3, String s3Object, String ContainerPath) {
		
		try {
			File f = new File(ContainerPath);
			if (Files.exists(Paths.get(ContainerPath), LinkOption.NOFOLLOW_LINKS)) f.delete();
			else f.mkdirs();
			
			AmazonS3URI s3uri = new AmazonS3URI(s3Object);
			s3.getObject(GetObjectRequest.builder()
					.bucket(s3uri.getBucket())
					.key(s3uri.getKey())
					.build(),
					ResponseTransformer.toFile(Paths.get(ContainerPath)));
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
		
		return true;
	}

}
