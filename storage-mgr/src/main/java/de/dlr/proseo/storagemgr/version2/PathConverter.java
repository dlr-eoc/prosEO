package de.dlr.proseo.storagemgr.version2;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Path Converter
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PathConverter {

	// private String path; 
	private ArrayList<String> basePaths = new ArrayList<>();

	private static String S3PREFIX = "s3://";
	private static String SLASH = "/";
	private static String DOUBLESLASH = "//";
	private static String BACKSLASH = "\\"; 
	private static String WINPATH_IN_S3 = "-WIN-PATH-"; 

	public void addBasePath(String basePath) {
		basePaths.add(removeLeftSlash(basePath.trim()));
	}
	
	/*
	public String getPath() { 
		
		return path; 
	}
	*/

	public PathConverter() {
		
		//this.path = convertToSlash(path); 
	}
	
	public String convertToSlash(String backSlashPath) { 
	
		return backSlashPath.replace(BACKSLASH, SLASH);
	}

	public String getRelativePath(String absolutePath) {
		
		String path = absolutePath.trim();
		
		path = convertToSlash(path); 

		path = removeFsPrefix(path);
		path = removeBasePath(path);

		if (isS3Path(absolutePath)) {
			path = removeBucket(path);
		}

		path = removeLeftSlash(path);

		return path;
	}
	
	public String getFirstFolder(String path) {
		
		path = convertToSlash(path); 
		
		String p = path.trim();
		
		p = removeLeftSlash(p);	
		File file = new File(p);
		
		if (file.getParent() == null) {
			return "";
		}
		
		return p.substring(0, p.indexOf(SLASH));
	}

	
	public String removeFirstFolder(String path) {
		
		path = convertToSlash(path); 
		
		String p = path.trim();
		
		p = removeLeftSlash(p);	
		File file = new File(p);
		
		if (file.getParent() == null) {
			return p;
		} 
		
		return p.substring(p.indexOf(SLASH) + 1);
	}
	
	public boolean isS3Path(String path) {

		return path.startsWith(S3PREFIX) ? true : false;
	}

	public String removeFsPrefix(String path) {

		if (path.startsWith(S3PREFIX)) {
			return path.substring(S3PREFIX.length());
		}

		return removeLeftSlash(path);
	}

	public String removeBasePath(String path) {

		for (String basePath : basePaths) {

			if (path.startsWith(basePath)) {
				return path.substring(basePath.length());
			}
		}

		return removeFirstFolder(path);
	}

	public String removeBucket(String path) {

		return path.substring(path.indexOf(SLASH));
	}

	public String removeLeftSlash(String path) {

		String p = path;

		while (p.startsWith(SLASH)) {
			p = path.substring(SLASH.length());
		}

		return p;
	}
	
	public String addSlashAtEnd(String path) { 
		
		if (path.contains(SLASH)) 
			return path + SLASH; 
		
		if (path.contains(BACKSLASH)) 
			return path + BACKSLASH;
		
		return path;
	}

	public String verifyAbsolutePath(String path) {
		
		if (isDirectory(path)) path = addSlashAtEnd(path); 
		
		if (isLinuxPath(path)) path = addSlashAtBegin(path);
		
		return path;
	}
	
	private boolean isDirectory(String path) {
		
		return new File(path).isDirectory();
	}

	public String addSlashAtBegin(String path) {
		
		if (path.startsWith(SLASH)) return path; 
		
		return path = SLASH + path; 
		
	}

	public boolean isWindowsPath(String path) {
		
		if (isS3Path(path)) return false; 
		
		if (path.indexOf(':') >= 0) return true;
		
		return false;
	}
	
	public boolean isLinuxPath(String path) {
		
		if (isS3Path(path)) return false; 
		if (isWindowsPath(path)) return false; 
		
		return true; 
	}
	
	public String posixToS3Path(String path) {
		
		path = convertToSlash(path); 
		
		// windows - replace ":"
		if (isWindowsPath(path)) {
			
			return path.replace(":", WINPATH_IN_S3);
		}
		
		// Linux - no action
		return path; 
	}
	
	public String s3ToPosixPath(String path) {
		
		path = convertToSlash(path); 
		
		// windows - restore ":"
		if (isWinPathInS3(path)) {
			
			return path.replace(WINPATH_IN_S3, ":");
		}
		
		// Linux - no action
		return path; 
	}
	
	public boolean isWinPathInS3(String path) {
		
		return (path.indexOf(WINPATH_IN_S3) >= 0) ? true : false;
	}

	public String removeDoubleSlash(String targetSubDirPath) {
		
		return targetSubDirPath.replace(DOUBLESLASH, SLASH);
	}
}
