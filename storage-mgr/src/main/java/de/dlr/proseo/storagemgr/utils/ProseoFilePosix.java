package de.dlr.proseo.storagemgr.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.amazonaws.services.s3.AmazonS3;

import de.dlr.proseo.model.fs.s3.S3Ops;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.FsType;
import software.amazon.awssdk.services.s3.S3Client;

public class ProseoFilePosix extends ProseoFile {

	private static Logger logger = LoggerFactory.getLogger(ProseoFilePosix.class);
	
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
				relPath = aPath.substring(base.length() + 1);
			} else if (aPath.startsWith(baseWorker)) {
				basePath = baseWorker;
				relPath = aPath.substring(baseWorker.length() + 1);
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
	}

	public ProseoFilePosix(String bucket, String pathInfo, StorageManagerConfiguration cfg) {
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
		return FsType.POSIX;
	}

	@Override
	public String getFullPath() {
		return "/" + getBasePath() + "/" + getRelPathAndFile();
	}

	@Override
	public InputStream getDataAsInputStream() {
		try {
			return new FileInputStream(pathInfo);
		} catch (FileNotFoundException e) {
			logger.error("Job order file {} not found", pathInfo);
			return null;
		} 
	}

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

	@Override
	public ArrayList<String> copyTo(ProseoFile proFile, Boolean recursive) throws Exception {
		if (proFile != null) {
			ArrayList<String> result = null;
			File srcFile = new File(this.getFullPath());
			switch (proFile.getFsType()) {
			case S_3:// create internal buckets & prefixes if not exists..
				String targetPath = null;
				if (srcFile.isDirectory()) {
					StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),cfg.getS3DefaultBucket(),cfg.getS3Region());
					S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
					S3Ops.createFolder(s3, cfg.getS3DefaultBucket(), proFile.getRelPath());
					result = new ArrayList<String>();
					result.add(proFile.getFullPath());
					if (recursive) {
						targetPath = proFile.getRelPath();
						if (targetPath.endsWith("/")) {
							targetPath = targetPath.substring(0, targetPath.length() - 1);
						}
					}
				} else {
					targetPath = proFile.getRelPath();
				}
				if (targetPath != null) {
					StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),cfg.getS3DefaultBucket(),cfg.getS3Region());
					AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
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
						FileUtils.forceMkdir(new File(proFile.getFullPath()));
						result.add(proFile.getFullPath());
					}
				} else {
					if (srcFile.isFile()) {
						files.add(srcFile);
					}
				}
				for (File f : files) {
					File targetFile = new File(proFile.getFullPath() + "/" + srcFile.getName());
					FileUtils.copyFile(srcFile, targetFile);
					if (targetFile.exists()) {
						targetFile.setWritable(true, false);
						result.add(targetFile.getPath());
					}					
				}		
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
		File srcFile = new File(this.getFullPath());
		if (srcFile.isDirectory()) {
			try {
				FileUtils.deleteDirectory(srcFile);
				result.add(getFullPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			srcFile.delete();
			result.add(getFullPath());
		}
		return result;
	}

	@Override
	public FileSystemResource getFileSystemResource() {
		return new FileSystemResource(getFullPath());
	}

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
