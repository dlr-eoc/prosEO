package de.dlr.proseo.storagemgr.utils;

import java.io.InputStream;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.FsType;

public class ProseoFileAlluxio extends ProseoFile {

	private static Logger logger = LoggerFactory.getLogger(ProseoFileAlluxio.class);
	
	public ProseoFileAlluxio(String pathInfo, Boolean fullPath, StorageManagerConfiguration cfg) {
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
	}

	public ProseoFileAlluxio(String bucket, String pathInfo, StorageManagerConfiguration cfg) {
		String aPath = pathInfo.trim();
		relPath = aPath;
		basePath = bucket.trim();
		while (basePath.startsWith("/")) {
			basePath = basePath.substring(1);			
		}
		pathInfo = getFullPath();								
	}

	@Override
	public FsType getFsType() {
		return FsType.ALLUXIO;
	}

	@Override
	public String getFullPath() {
		return "alluxio://" + getBasePath() + "/" + getRelPath();
	}

	@Override
	public InputStream getDataAsInputStream() {
		return null;
	}

	@Override
	public ArrayList<String> copyTo(ProseoFile proFile, Boolean recursive) throws Exception {
		return null;
	}

	@Override
	public Boolean writeBytes(byte[] bytes) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
