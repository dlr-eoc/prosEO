package de.dlr.proseo.storagemgr.version2;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Path Converter
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PathConverter {

	/** path */
	private String p;

	/** base pathes, which are used to make relative path */
	private List<String> basePaths = new ArrayList<>();

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

	private static String S3PREFIX = "s3://";
	private static String SLASH = "/";
	private static String DOUBLESLASH = "//";
	private static String BACKSLASH = "\\";

	/**
	 * no ":" is allowed in s3, that's why transformation of windows path to s3
	 * compatible path
	 */
	private static String WINPATH_IN_S3 = "-WIN-PATH-";

	/**
	 * Simple Constructor with path
	 * 
	 * @param path
	 */
	public PathConverter(String path) {

		p = path;
		init(path);
	}

	/**
	 * Constructor with 2 paths to merge
	 * 
	 * @param path1 begin of the path
	 * @param path2 end of the path
	 */
	public PathConverter(String path1, String path2) {

		p = Paths.get(path1, path2).toString();
		init(path2);
	}

	/**
	 * Constructor with 3 paths to merge
	 * 
	 * @param path1 begin of the path
	 * @param path2 middle of the path
	 * @param path3 end of the path
	 */
	public PathConverter(String path1, String path2, String path3) {

		p = Paths.get(path1, path2, path3).toString();
		init(path3);
	}

	/**
	 * Slash at the end will not be lost, replaces backslash and empty symbols
	 * 
	 * @param endPath
	 */
	private void init(String endPath) {
		p = p.replace(BACKSLASH, SLASH); // convertToSlash
		p = p.trim();

		if (p.endsWith(SLASH)) {
			return;
		}

		if (endPath.endsWith(SLASH)) { // do not forget directory at end
			p = p + SLASH;
		}
	}

	/**
	 * Constructor with path and base paths, which will be used to create relative
	 * path
	 * 
	 * @param path
	 * @param basePaths
	 */
	public PathConverter(String path, List<String> basePaths) {

		this(path);
		this.basePaths = basePaths;

		if (logger.isTraceEnabled())
			logger.trace(">>> PathConverter({}, {})", path, basePaths.size());
	}

	/**
	 * Copy object constructor
	 * 
	 * @param pathConverter
	 */
	public PathConverter(PathConverter pathConverter) {
		this(pathConverter.getPath());
		this.basePaths = pathConverter.basePaths;
	}

	/**
	 * Added base path to base path list, which are used for relative path
	 * 
	 * @param basePath
	 */
	public void addBasePath(String basePath) {
		String path = new PathConverter(basePath).removeLeftSlash().getPath();
		basePaths.add(path);
	}

	/**
	 * Gets path
	 * 
	 * @return path
	 */
	public String getPath() {
		return p;
	}

	/**
	 * Replaces backslashes with slashes
	 * 
	 * @return path with slashes only
	 */
	public PathConverter convertToSlash() {
		return new PathConverter(p.replace(BACKSLASH, SLASH), basePaths);
	}

	/**
	 * Gets first folder
	 * 
	 * @return path with first folder only
	 */
	public PathConverter getFirstFolder() {

		String path = new PathConverter(p).removeLeftSlash().getPath();

		File file = new File(path);

		if (file.getParent() == null) {
			return new PathConverter("", basePaths);
		}

		return new PathConverter(path.substring(0, path.indexOf(SLASH)), basePaths);
	}

	/**
	 * Removes first folder
	 * 
	 * @return path without first folder
	 */
	public PathConverter removeFirstFolder() {

		String path = new PathConverter(p).removeLeftSlash().getPath();

		File file = new File(path);

		if (file.getParent() == null) {
			return new PathConverter(path, basePaths);
		}

		return new PathConverter(path.substring(path.indexOf(SLASH) + 1), basePaths);
	}

	/**
	 * Checks if path is S3
	 * 
	 * @return true if s3 path
	 */
	public boolean isS3Path() {

		return p.startsWith(S3PREFIX) ? true : false;
	}

	/**
	 * Removes FS-Prefixes
	 * 
	 * @return path without FS prefixes
	 */
	public PathConverter removeFsPrefix() {

		if (p.startsWith(S3PREFIX)) {
			return new PathConverter(p.substring(S3PREFIX.length()), basePaths);
		}

		return new PathConverter(p, basePaths);
	}

	/**
	 * Removes base Path from path if base path is in the list.
	 * 
	 * @return path without base path
	 */
	public PathConverter removeBasePaths() {

		for (String basePath : basePaths) {

			if (p.startsWith(basePath)) {
				return new PathConverter(p.substring(basePath.length()), basePaths);
			}
		}

		return this;
	}

	/**
	 * Checks if path is part from one of base paths
	 * 
	 * @return true if path is part of one of base paths
	 */
	public boolean hasBasePaths() {

		for (String basePath : basePaths) {

			if (p.startsWith(basePath)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Removes bucket as a first folder from path
	 * 
	 * @return path without bucket
	 */
	public PathConverter removeBucket() {

		return new PathConverter(p.substring(p.indexOf(SLASH)), basePaths);
	}

	/**
	 * Removes left slash from path
	 * 
	 * @return path without left slash
	 */
	public PathConverter removeLeftSlash() {

		String path = p;

		while (path.startsWith(SLASH)) {
			path = path.substring(SLASH.length());
		}

		return new PathConverter(path, basePaths);
	}

	/**
	 * Adds slash at end of path
	 * 
	 * @return path with slash at the end
	 */
	public PathConverter addSlashAtEnd() {

		if (p.endsWith(SLASH))
			return this;

		if (p.endsWith(BACKSLASH))
			return this;

		if (p.contains(BACKSLASH))
			return new PathConverter(p + BACKSLASH, basePaths);

		return new PathConverter(p + SLASH, basePaths);
	}

	/**
	 * Fixes absolute path - 1) adds slash at end if directory 2) adds slash at
	 * begin if linux-path
	 * 
	 * @return fixed absolute path
	 */
	public PathConverter fixAbsolutePath() {

		PathConverter pathConverter = this;

		if (isDirectory())
			pathConverter = pathConverter.addSlashAtEnd();

		if (isLinuxPath())
			pathConverter = pathConverter.addSlashAtBegin();

		return pathConverter;
	}

	/**
	 * Checks if directory - if path ends with slash or backslash
	 * 
	 * @return true if path is directory
	 */
	public boolean isDirectory() {

		return (p.endsWith(SLASH) || p.endsWith(BACKSLASH)) ? true : false;
	}

	/**
	 * Adds slash at the beginning of the path
	 * 
	 * @return path with the slash at the beginning
	 */
	public PathConverter addSlashAtBegin() {

		if (p.startsWith(SLASH))
			return this;

		return new PathConverter(SLASH + p, basePaths);
	}

	/**
	 * Checks if path is windows, no s3 and has ":" symbol
	 * 
	 * @return true if path is windows, no s3 and has ":" symbol
	 */
	public boolean isWindowsPath() {

		if (isS3Path())
			return false;

		if (p.indexOf(':') >= 0)
			return true;

		return false;
	}

	/**
	 * Checks if path is linux, no windows and no s3 path
	 * 
	 * @return true if path is linux, no windows and no s3 path
	 */
	public boolean isLinuxPath() {

		if (isS3Path())
			return false;

		if (isWindowsPath())
			return false;

		return true;
	}

	/**
	 * Converts posix path (windows also) to compatible path for s3 storage
	 * 
	 * @return compatible path for s3 storage
	 */
	public PathConverter posixToS3Path() {

		// windows - replace ":"
		if (isWindowsPath()) {
			return new PathConverter(p.replace(":", WINPATH_IN_S3), basePaths);
		}

		// Linux - no action
		return this;
	}

	/**
	 * Converts s3 path to posix compatible path (windows also)
	 * 
	 * @return posix compatible path (windows also)
	 */
	public PathConverter s3ToPosixPath() {

		// windows - restore ":"
		if (isWinPathInS3()) {
			return new PathConverter(p.replace(WINPATH_IN_S3, ":"), basePaths);
		}

		// Linux - no action
		return this;
	}

	/**
	 * Checks if path is converted windows path for s3
	 * 
	 * @return true if path is converted windows path for s3
	 */
	public boolean isWinPathInS3() {

		return (p.indexOf(WINPATH_IN_S3) >= 0) ? true : false;
	}

	/**
	 * Removes double slash from path
	 * 
	 * @return path with slash, no double slash
	 */
	public PathConverter removeDoubleSlash() {

		return new PathConverter(p.replace(DOUBLESLASH, SLASH), basePaths);
	}

	/**
	 * Gets relative path from path using base path list
	 * 
	 * @return relative path from path using base path list
	 */
	public PathConverter getRelativePath() {

		PathConverter pathConverter = new PathConverter(this).removeFsPrefix().removeBasePaths();

		if (isS3Path()) {
			pathConverter = pathConverter.removeBucket();
		}

		return pathConverter.removeLeftSlash();
	}
	
	/**
	 * Adds s3 prefix to the path
	 * 
	 * @return s3 prefix + path
	 */
	public PathConverter addS3Prefix() {
		
		 String pathWithoutLeftSlash = new PathConverter(this).removeLeftSlash().getPath();
		 
		 return new PathConverter(S3PREFIX + pathWithoutLeftSlash, basePaths);
	}
}
