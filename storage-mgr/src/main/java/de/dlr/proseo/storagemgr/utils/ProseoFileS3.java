package de.dlr.proseo.storagemgr.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import com.amazonaws.services.s3.AmazonS3;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.fs.s3.AmazonS3URI;
import de.dlr.proseo.storagemgr.fs.s3.S3Ops;
import de.dlr.proseo.storagemgr.rest.model.FsType;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ProseoFileS3 extends ProseoFile {
	
	private static Logger logger = LoggerFactory.getLogger(ProseoFileS3.class);

	public ProseoFileS3(String pathInfo, Boolean fullPath, StorageManagerConfiguration cfg) {
		this.cfg = cfg;
		String aPath = pathInfo.trim();
		this.pathInfo = aPath;
		if (fullPath) {
			if (aPath.startsWith("s3:/") || aPath.startsWith("S3:/")) {
				aPath = aPath.substring(4);			
			}
			while (aPath.startsWith("/")) {
				aPath = aPath.substring(1);			
			}
			int pos = aPath.indexOf('/');
			if (pos >= 0) {
				basePath = aPath.substring(0, pos);
				relPath = aPath.substring(pos + 1);
			} else {
				basePath = "";
				relPath = aPath;
			}
		} else {
			while (aPath.startsWith("/")) {
				aPath = aPath.substring(1);			
			}
			relPath = aPath;
			basePath = cfg.getS3DefaultBucket();
		}
		buildFileName();
		pathInfo = getFullPath();			
	}

	public ProseoFileS3(String bucket, String pathInfo, StorageManagerConfiguration cfg) {
		String aPath = pathInfo.trim();
		relPath = aPath;
		basePath = bucket.trim();
		while (basePath.startsWith("/")) {
			basePath = basePath.substring(1);			
		}
		buildFileName();
		pathInfo = getFullPath();						
		
	}
	
	@Override
	public FsType getFsType() {
		return FsType.S_3;
	}

	@Override
	public String getFullPath() {
		return "s3://" + getBasePath() + "/" + getRelPathAndFile();
	}

	@Override
	public InputStream getDataAsInputStream() {
		S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
		return S3Ops.v2FetchStream(s3, getFullPath());
	}

	@Override
	public Boolean writeBytes(byte[] bytes) throws Exception {
		if (bytes != null) {
			InputStream fis = new ByteArrayInputStream(bytes);
			// create internal buckets, if not existing
			StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(),
					cfg.getS3EndPoint(), getBasePath(), cfg.getS3Region());
			S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
			s3.putObject(PutObjectRequest.builder().bucket(getBasePath()).key(getRelPathAndFile()).build(),
					RequestBody.fromInputStream(fis, bytes.length));
			s3.close();
			fis.close();
			logger.info("Bytes, written to {}", getFullPath());
			return true;
		}
		logger.warn("writeBytes, arument bytes not set");
		return false;
	}

	@Override
	public ArrayList<String> copyTo(ProseoFile proFile, Boolean recursive) throws Exception {
		if (proFile != null) {
			ArrayList<String> result = null;
			AmazonS3URI s3uri = new AmazonS3URI(this.getFullPath());
			String sourceBucket = s3uri.getBucket();
			String sourceKey = s3uri.getKey();
			switch (proFile.getFsType()) {
			case S_3:
				// create internal buckets & prefixes if not exists..
				StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),cfg.getS3DefaultBucket(),cfg.getS3Region());
				AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
				result = S3Ops.v1Copy(
						//the client
						s3, 
						// the source S3-Bucket
						sourceBucket, 
						// the source key
						sourceKey, 
						// the target s3-bucket (=storageId)
						cfg.getS3DefaultBucket(),
						// the final prefix including productId pattern of the file or directory
						proFile.getRelPath()
						);
				break;
			case POSIX:
				S3Client s3c = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
				if (null==sourceKey) sourceKey="";
				result = new ArrayList<String>();
				// TODO recursive
				File targetFile = new File(proFile.getFullPath());
				if (this.isDirectory()) {
					if (!targetFile.exists()) {
						FileUtils.forceMkdir(targetFile);
					}
					result.add(proFile.getFullPath());					
				} else {
					if (targetFile.exists()) {
						result.add(proFile.getFullPath());
					} else {
						if (S3Ops.v2FetchFile(
								//the client
								s3c, 
								// the source S3-Bucket
								this.getFullPath(), 
								// the final prefix including productId pattern of the file or directory
								proFile.getFullPath()
								)) {
							targetFile.setWritable(true, false);
							result.add(proFile.getFullPath());
						}
					}
				};
				break;
			case ALLUXIO:
				break;
			default:
				break;
			}
			return result;
		}
		return null;
	}
	
	@Override
	public ArrayList<String> delete() {
		ArrayList<String> result = new ArrayList<String>();
		AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
		try {
			S3Ops.deleteDirectory(s3, getBasePath(), getRelPathAndFile());	
			result.add(getFullPath());		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	@Override
	public FileSystemResource getFileSystemResource() {
		return new FileSystemResource(getFullPath());
	}
	

	@Override
	public long getLength() {
		if (isDirectory()) {
			return 0;
		} else {
			AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
			return S3Ops.getLength(s3, getBasePath(), getRelPathAndFile());
		}
	}
}
