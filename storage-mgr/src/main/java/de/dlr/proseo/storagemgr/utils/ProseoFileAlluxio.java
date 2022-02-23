package de.dlr.proseo.storagemgr.utils;

import java.io.InputStream;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;

/**
 * Proseo file representing alluxio.
 * 
 * !! NOT USED AND IMPLEMENTED !!
 * 
 * @author melchinger
 *
 */
public class ProseoFileAlluxio extends ProseoFile {

	private static Logger logger = LoggerFactory.getLogger(ProseoFileAlluxio.class);
	
	/**
	 * Creates a new Alluxio file
	 * 
	 * @param pathInfo Path information 
	 * @param fullPath if it is a full path
	 * @param cfg The Configuration of storage manager
	 */
	public ProseoFileAlluxio(String pathInfo, Boolean fullPath, StorageManagerConfiguration cfg) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> ProseoFileAlluxio({}, {}, {})", pathInfo, fullPath, cfg);

		this.cfg = cfg;
		String aPath = pathInfo.trim();
		this.pathInfo = aPath;
		if (fullPath) {
			if (aPath.startsWith("alluxio:/") || aPath.startsWith("alluxio:/")) {
				aPath = aPath.substring(9);			
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
			basePath = cfg.getAlluxioUnderFsS3Bucket();
		}
		pathInfo = getFullPath();			
		
		logger.trace("ProseoFileAlluxio created: {}", this.getFullPath());
	}

	/**
	 * Creates a new Alluxio file
	 * 
	 * @param bucket A bucket of the file 
	 * @param pathInfo Path Information 
	 * @param cfg The Configuration of Storage Manager
	 */
	public ProseoFileAlluxio(String bucket, String pathInfo, StorageManagerConfiguration cfg) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> ProseoFileAlluxio({}, {}, {})", bucket, pathInfo, cfg);
	
		String aPath = pathInfo.trim();
		relPath = aPath;
		basePath = bucket.trim();
		while (basePath.startsWith("/")) {
			basePath = basePath.substring(1);			
		}
		pathInfo = getFullPath();								
		
		logger.trace("ProseoFileAlluxio created: {}", this.getFullPath());
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getFSType()
	 */
	@Override
	public StorageType getFsType() {
		return StorageType.ALLUXIO;
	}
	
	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getFullPath()
	 */
	@Override
	public String getFullPath() {
		return "alluxio://" + getRelPathAndFile();
	}
	
	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getDataAsInputStream()
	 */
	@Override
	public InputStream getDataAsInputStream() {
		
		throw new UnsupportedOperationException("Not implemented yet");
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#copyTo()
	 */
	@Override
	public ArrayList<String> copyTo(ProseoFile proFile, Boolean recursive) throws Exception {
		
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#delete()
	 */
	@Override
	public ArrayList<String> delete() {
		
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#list()
	 */
	@Override
	public ArrayList<ProseoFile> list() {
		
		ArrayList<ProseoFile> list = new ArrayList<ProseoFile>();
		return list;
	}
	
	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#writeBytes()
	 */
	@Override
	public Boolean writeBytes(byte[] bytes) throws Exception {
		
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getFileSystemResource()
	 */
	@Override
	public FileSystemResource getFileSystemResource() {
		
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	/* (non-Javadoc)
	 * @see de.dlr.proseo.storagemgr.utils.ProseoFile#getLength()
	 */
	@Override
	public long getLength() {
		
		throw new UnsupportedOperationException("Not implemented yet");
	}
}
