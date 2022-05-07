package de.dlr.proseo.storagemgr.version2.posix;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.utils.ProseoFile;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

/**
 * Posix Storage File 
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PosixStorageFile implements StorageFile {

	// TODO: Make verifications for pathes

	// "/" + basepath + bucket (optional) + relativePath (with fileName)
	private String basePath;
	private String bucket;
	private String relativePath;

	/**
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(PosixStorageFile.class);

	// no bucket
	public PosixStorageFile(String basePath, String relativePath) {
		this(basePath, StorageFile.NO_BUCKET, relativePath);
	}

	// default or another bucket
	public PosixStorageFile(String basePath, String bucket, String relativePath) {

		this.basePath = verifyBasePath(basePath);
		this.bucket = verifyBucket(bucket);
		this.relativePath = verifyRelativePath(relativePath);
	}
	
	public PosixStorageFile(StorageFile storageFile) { 
		this(storageFile.getBasePath(), storageFile.getBucket(), storageFile.getRelativePath() );
	}
	
	@Override
	public String getFullPath() {
		
		try {
			String path=  new PathConverter().convertToSlash(Paths.get(basePath, bucket, relativePath).toString());
			
			path = new PathConverter().verifyAbsolutePath(path);
			
			return path;
		}
		catch (Exception e) {
			
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public String getBasePath() {
		return basePath;
	}

	@Override
	public String getBucket() {
		return bucket;
	}

	@Override
	public String getRelativePath() {
		return relativePath;
	}

	@Override
	public String getFileName() {
		return new File(relativePath).getName();
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.POSIX;
	}

	private String verifyBasePath(String basePath) {
		//if (!basePath.startsWith("/"))
		//	basePath = "/" + basePath;

		return basePath;
	}

	private String verifyBucket(String bucket) {
		return bucket;
	}

	private String verifyRelativePath(String relativePath) {
		return relativePath;
	}

	@Override
	public String getExtension() {

		if (logger.isTraceEnabled())
			logger.trace(">>> getExtension()");

		return FilenameUtils.getExtension(relativePath);
	}
	
	@Override
	public boolean isDirectory() { 
		
		return (relativePath.endsWith("/") || relativePath.endsWith("\\"))  ? true : false; 		
	}
	

	@Override
	public void setBasePath(String basePath) {
		this.basePath = basePath; 
	}

	@Override
	public void setBucket(String bucket) {
		this.bucket = bucket; 
		
	}

	@Override
	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath; 
	}
}
