package de.dlr.proseo.storagemgr.version2.s3;

import java.util.ArrayList;
import java.util.List;

import de.dlr.proseo.storagemgr.version2.model.BucketsStorage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

/**
 * S3 Storage
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3Storage implements BucketsStorage {

	private S3DataAccessLayer s3DAL;

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
	public boolean uploadFile(StorageFile sourceFile, StorageFile storageFile) {
		s3DAL.setBucket(storageFile.getBucket());
		s3DAL.uploadFile(sourceFile.getRelativePath(), storageFile.getRelativePath());
		
		// TODO: change DAL layer to boolean
		return true; 
	}

	@Override
	public boolean downloadFile(StorageFile storageFile, StorageFile targetFile) {
		
		s3DAL.setBucket(storageFile.getBucket());
		return s3DAL.downloadFile(storageFile.getRelativePath(), targetFile.getRelativePath()); 		
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
}
