package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.BucketsStorage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

/**
 * S3 Storage
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3Storage implements BucketsStorage {

	private S3DataAccessLayer s3DAL;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3Storage.class);

	public S3Storage(String s3AccessKey, String s3SecretAccessKey) {
		s3DAL = new S3DataAccessLayer(s3AccessKey, s3SecretAccessKey);
	}

	public S3Storage(String s3AccessKey, String s3SecretAccessKey, String s3Region, String s3EndPoint) {
		s3DAL = new S3DataAccessLayer(s3AccessKey, s3SecretAccessKey, s3Region, s3EndPoint);
	}

	@Override
	public String getBasePath() {
		return "";
	}

	@Override
	public void setBucket(String bucket) {
		s3DAL.setBucket(bucket);
	}

	@Override
	public String getBucket() {
		return s3DAL.getBucket();
	}

	@Override
	public boolean fileExists(StorageFile storageFile) {
		return s3DAL.fileExists(storageFile.getRelativePath());
	}

	@Override
	public List<StorageFile> getFiles() {

		List<String> files = s3DAL.getFiles();
		List<StorageFile> storageFiles = new ArrayList<StorageFile>();

		for (String filePath : files) {

			StorageFile storageFile = new S3StorageFile(s3DAL.getBucket(), filePath);
			storageFiles.add(storageFile);
		}

		return storageFiles;
	}

	@Override
	public void uploadFile(StorageFile sourceFile, StorageFile targetFile) throws IOException {

		String sourcePath = sourceFile.getFullPath();
		String targetPath = targetFile.getRelativePath();

		s3DAL.setBucket(targetFile.getBucket());

		try {
			s3DAL.uploadFile(sourcePath, targetPath);
		} catch (IOException e) {
			if (logger.isTraceEnabled())
				logger.warn("Cannot upload file from " + sourcePath + " to " + targetPath + " ", e.getMessage());
			throw e;
		}
	}

	@Override
	public void downloadFile(StorageFile sourceFile, StorageFile targetFile) throws IOException {

		String sourcePath = sourceFile.getRelativePath();
		String targetPath = targetFile.getFullPath();
		
		s3DAL.setBucket(sourceFile.getBucket());

		try {
			s3DAL.downloadFile(sourcePath, targetPath);		
		} catch (IOException e) {
			if (logger.isTraceEnabled())
				logger.warn("Cannot download file from " + sourcePath + " to " + targetPath + " ", e.getMessage());
			throw e;
		}
	}

	@Override
	public List<String> getBuckets() {
		return s3DAL.getBuckets();
	}

	@Override
	public boolean bucketExists(String bucketName) {
		return s3DAL.bucketExists(bucketName);
	}

	@Override
	public boolean deleteBucket(String bucketName) {
		s3DAL.deleteBucket(bucketName);

		// TODO: change DAL layer to boolean
		return true;
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.S3;
	}

	@Override
	public long getFileSize(StorageFile storageFile) {
		return s3DAL.getFileSize(storageFile.getRelativePath());
	}

	@Override
	public StorageFile getFile(String relativePath) {
		return new S3StorageFile(s3DAL.getBucket(), relativePath);
	}
}
