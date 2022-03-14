package de.dlr.proseo.storagemgr.version2;

import java.util.ArrayList;

/**
 * Path Converter
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PathConverter {

	private ArrayList<String> basePaths = new ArrayList<>();

	private String s3Prefix = "s3://";
	private String slash = "/";

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

	public boolean isS3Path(String path) {

		return path.startsWith(s3Prefix) ? true : false;
	}

	public String removeFsPrefix(String path) {

		if (path.startsWith(s3Prefix)) {
			return path.substring(s3Prefix.length());
		}

		return removeLeftSlash(path);
	}

	public String removeBasePath(String path) {

		for (String basePath : basePaths) {

			if (path.startsWith(basePath)) {
				return path.substring(basePath.length());
			}
		}

		return removeLeftSlash(path);
	}

	public String removeBucket(String path) {

		return path.substring(path.indexOf(slash));
	}

	public String removeLeftSlash(String path) {

		String p = path;

		while (p.startsWith(slash)) {
			p = path.substring(slash.length());
		}

		return p;
	}
}
