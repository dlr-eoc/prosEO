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

	private ArrayList<String> basePaths = new ArrayList<>();

	private static String S3PREFIX = "s3://";
	private static String SLASH = "/";
	private static String BACKSLASH = "\\"; 

	public void addBasePath(String basePath) {
		basePaths.add(removeLeftSlash(basePath.trim()));
	}

	public PathConverter() {
	}

	public String getRelativePath(String absolutePath) {

		String path = absolutePath.trim();

		path = removeFsPrefix(path);
		path = removeBasePath(path);

		if (isS3Path(absolutePath)) {
			path = removeBucket(path);
		}

		path = removeLeftSlash(path);

		return path;
	}
	
	public String getFirstFolder(String path) {
		
		String p = path.trim();
		
		p = removeLeftSlash(p);	
		File file = new File(p);
		
		if (file.getParent() == null) {
			return "";
		}
		
		return p.substring(0, p.indexOf(SLASH));
	}

	
	public String removeFirstFolder(String path) {
		
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
	
	public static String addSlashAtEnd(String path) { 
		
		if (path.contains(SLASH)) 
			return path + SLASH; 
		
		if (path.contains(BACKSLASH)) 
			return path + BACKSLASH;
		
		return path;
	}
}
