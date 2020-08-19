package de.dlr.proseo.storagemgr.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import com.amazonaws.services.s3.AmazonS3;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.fs.s3.S3Ops;
import software.amazon.awssdk.services.s3.S3Client;
import de.dlr.proseo.storagemgr.utils.StorageType;

/**
 * Proseo file representing POSIX file system
 * 
 * @author melchinger
 *
 */
public class ProseoFilePosix extends ProseoFile {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(ProseoFilePosix.class);
	
	/**
	 * Create a new posix file.
	 * 
	 * @param pathInfo The file path
	 * @param fullPath Use it as full path if true, otherwise use default bucket + path info
	 * @param cfg
	 */
	public ProseoFilePosix(String pathInfo, Boolean fullPath, StorageManagerConfiguration cfg) {
		this.cfg = cfg;
		String aPath = pathInfo.trim();
		this.pathInfo = aPath;
		while (aPath.startsWith("/")) {
			aPath = aPath.substring(1);			
		}
		if (fullPath) {
			String base = cfg.getPosixMountPoint();
			while (base.startsWith("/")) {
				base = base.substring(1);			
			}
			String baseWorker = cfg.getPosixWorkerMountPoint();
			while (baseWorker.startsWith("/")) {
				baseWorker = baseWorker.substring(1);			
			}
			if (aPath.startsWith(base)) {
				basePath = base;
				if (aPath.length() == base.length()) {
					relPath = "/";
				} else {
					relPath = aPath.substring(base.length() + 1);
				}
			} else if (aPath.startsWith(baseWorker)) {
				basePath = baseWorker;
				if (aPath.length() == baseWorker.length()) {
					relPath = "/";
				} else {
					relPath = aPath.substring(baseWorker.length() + 1);
				}
			} else {
				int pos = aPath.indexOf('/');
				if (pos >= 0) {
					basePath = aPath.substring(0, pos);
					relPath = aPath.substring(pos + 1);
				} else {
					basePath = "";
					relPath = aPath;
				}
			}
		} else {
			relPath = aPath;
			basePath = cfg.getPosixMountPoint().trim();
			while (basePath.startsWith("/")) {
				basePath = basePath.substring(1);			
			}				
		}
		buildFileName();
		pathInfo = getFullPath();	
		
		logger.trace("ProseoFilePosix created: {}", this);
	}

	/**
	 * Create a new posix file.
	 * 
	 * @param bucket The bucket
	 * @param pathInfo The relative path
	 * @param cfg
	 */
	public ProseoFilePosix(String bucket, String pathInfo, StorageManagerConfiguration cfg) {
		String aPath = pathInfo.trim();
		relPath = aPath;
		basePath = bucket.trim();
		while (basePath.startsWith("/")) {
			basePath = basePath.substring(1);			
		}
		buildFileName();
		pathInfo = getFullPath();						
		
		logger.trace("ProseoFilePosix created: {}", this);
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getFsType()
	 */
	@Override
	public StorageType getFsType() {
		return StorageType.POSIX;
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getFullPath()
	 */
	@Override
	public String getFullPath() {
		return "/" + getBasePath() + "/" + getRelPathAndFile();
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getDataAsInputStream()
	 */
	@Override
	public InputStream getDataAsInputStream() {
		try {
			return new FileInputStream(pathInfo);
		} catch (FileNotFoundException e) {
			logger.error("Job order file {} not found", pathInfo);
			return null;
		} 
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#writeBytes(byte[])
	 */
	@Override
	public Boolean writeBytes(byte[] bytes) throws IOException {
		if (bytes != null) {
			// create JOF file path if not exist
			File jofFile = new File(getFullPath());
			File jofFilePath = new File(jofFile.getParent());
			if (!jofFilePath.exists()) {
				jofFilePath.mkdirs();
			}
			FileOutputStream jofOut = new FileOutputStream(jofFile);
			jofOut.write(bytes);
			jofOut.close();
			logger.info("Bytes, written to {}", getFullPath());
			return true;
		}
		logger.warn("writeBytes, arument bytes not set");
		return false;
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#copyTo(de.dlr.proseo.storagemgr.utils.ProseoFile, java.lang.Boolean)
	 */
	@Override
	public ArrayList<String> copyTo(ProseoFile proFile, Boolean recursive) throws Exception {
		if (proFile == null) {
			logger.error("Illegal call of ProseoFilePosix::copyTo(ProseoFile, Boolean) with null argument");
			return null;
		}
		if (logger.isDebugEnabled()) logger.debug("Copying from {} to {}", this.getFullPath(), proFile.getFullPath());

		ArrayList<String> result = null;
		File srcFile = new File(this.getFullPath());
		switch (proFile.getFsType()) {
		case S3:// create internal buckets & prefixes if not exists..
			String targetPath = null;
			if (srcFile.isDirectory()) {
				StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),cfg.getS3DefaultBucket(),cfg.getS3Region());
				S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(), cfg.getS3Region());
				S3Ops.createFolder(s3, cfg.getS3DefaultBucket(), proFile.getRelPath());
				result = new ArrayList<String>();
				result.add(proFile.getFullPath());
				if (recursive) {
					targetPath = proFile.getRelPath();
				}
			} else {
				targetPath = proFile.getRelPath();
			}
			if (targetPath != null) {
				if (targetPath.endsWith("/")) {
					targetPath = targetPath.substring(0, targetPath.length() - 1);
				}
				StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),cfg.getS3DefaultBucket(),cfg.getS3Region());
				AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(), cfg.getS3Region());
				result = S3Ops.v1Upload(
						//the client
						s3, 
						// the local POSIX source file or directory
						this.getFullPath(), 
						// the storageId -> =BucketName
						cfg.getS3DefaultBucket(), 
						// the final prefix of the file or directory
						targetPath, 
						false
						);
			}
			break;
		case POSIX:
			result = new ArrayList<String>();
			ArrayList<File> files = new ArrayList<File>();
			if (srcFile.isDirectory()) {
				if (recursive) {
					File[] srcFiles = srcFile.listFiles();
					for (File f : srcFiles) {
						files.add(f);
					}
				} else {
					File targetFile = new File(proFile.getFullPath());
					if (this.isDirectory()) {
						if (!targetFile.exists()) {
							FileUtils.forceMkdir(targetFile);
						}
						result.add(proFile.getFullPath());		
					}
				}
			} else {
				if (srcFile.isFile()) {
					files.add(srcFile);
				} else {
					logger.error("Cannot find source file {}", srcFile);
				}
			}
			for (File f : files) {
				File targetFile = new File(proFile.getFullPath() + "/" + f.getName());
				if (targetFile.exists()) {
					result.add(targetFile.getPath());
				} else {
					FileUtils.copyFile(f, targetFile);
					if (targetFile.exists()) {
						targetFile.setWritable(true, false);
						result.add(targetFile.getPath());
					} else {
						logger.error("Cannot copy from source {} to target {}", f.getCanonicalPath(), targetFile.getCanonicalPath());
					}
				}
			}		
			break;
		case ALLUXIO:
			break;
		default:
			break;
		}
		if (logger.isDebugEnabled()) logger.debug("Files copied: {}", result);
		return result;
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#delete()
	 */
	@Override
	public ArrayList<String> delete() {
		ArrayList<String> result = new ArrayList<String>();
		File srcFile = new File(this.getFullPath());
		if (srcFile.isDirectory()) {
		    File[] files = srcFile.listFiles();
		    if(files!=null) {  
		        for(File f: files) {
		            if(f.isDirectory()) {
		    			try {
		    				FileUtils.deleteDirectory(f);
		    			} catch (IOException e) {
		    				e.printStackTrace();
		    			}
		            } else {
		                f.delete();
		            }
		        }
		    }
			result.add(getFullPath());
		} else {
			srcFile.delete();
			result.add(getFullPath());
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#list()
	 */
	@Override
	public ArrayList<ProseoFile> list() {
		ArrayList<ProseoFile> list = new ArrayList<ProseoFile>();
		File srcFile = new File(this.getFullPath());
		if (srcFile.isDirectory()) {
			Collection<File> files = FileUtils.listFilesAndDirs(srcFile, TrueFileFilter.INSTANCE , TrueFileFilter.INSTANCE );
			for (File file : files) {
				File tmpFile = new File(file.getAbsolutePath());
				if (tmpFile.isFile()) {
					list.add(new ProseoFilePosix(file.getAbsolutePath(), true, cfg));
				}
			}
		}
		return list;
	}
	
	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getFileSystemResource()
	 */
	@Override
	public FileSystemResource getFileSystemResource() {
		return new FileSystemResource(getFullPath());
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getLength()
	 */
	@Override
	public long getLength() {
		File f = new File(getFullPath());
		if (f.isFile()) {
			return f.length();
		} else {
			return 0;
		}
	}
}
