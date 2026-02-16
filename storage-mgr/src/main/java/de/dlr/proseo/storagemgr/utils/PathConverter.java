/**
 * PathConverter.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.utils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.storagemgr.model.StorageType;

/**
 * A set of methods to convert and manipulate file paths based on different
 * conventions and requirements. It handles Windows paths, Linux paths, and S3
 * paths, and offers functionality to work with base paths and relative paths.
 *
 * @author Denys Chaykovskiy
 */
public class PathConverter {

	/** The path */
	private String pathToConvert;

	/** The base paths used to create relative paths */
	private List<String> basePaths = new ArrayList<>();

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(PathConverter.class);

	private static String S3PREFIX = "s3://";
	private static String SLASH = "/";
	private static String DOUBLESLASH = "//";
	private static String BACKSLASH = "\\";

	/**
	 * The prefix used to convert Windows paths to S3-compatible paths (necessary as
	 * the ":" character is not allowed in S3 paths)
	 */
	private static String WINPATH_IN_S3 = "-WIN-PATH-";

	/**
	 * Simple constructor with a path
	 *
	 * @param path The path
	 */
	public PathConverter(String path) {
		pathToConvert = path;
		init(path);
	}

	/**
	 * Constructor to merge two paths
	 *
	 * @param beginPath The beginning of the path
	 * @param endPath The end of the path
	 */
	public PathConverter(String beginPath, String endPath) {
		pathToConvert = Paths.get(beginPath, endPath).toString();
		init(endPath);
	}

	/**
	 * Constructor to merge three paths
	 *
	 * @param beginPath The beginning of the path
	 * @param middlePath The middle of the path
	 * @param endPath The end of the path
	 */
	public PathConverter(String beginPath, String middlePath, String endPath) {
		pathToConvert = Paths.get(beginPath, middlePath, endPath).toString();
		init(endPath);
	}

	/**
	 * Initializes the path by replacing backslashes with slashes and ensuring a
	 * trailing slash for directories.
	 *
	 * @param endPath The last part of the path
	 */
	private void init(String endPath) {
		pathToConvert = pathToConvert.replace(BACKSLASH, SLASH); // Convert backslashes to slashes
		pathToConvert = pathToConvert.trim();

		if (pathToConvert.endsWith(SLASH)) {
			return;
		}

		if (endPath.endsWith(SLASH)) { // Ensure directory path ends with a slash
			pathToConvert = pathToConvert + SLASH;
		}
	}

	/**
	 * Constructor with path and base paths, which will be used to create relative
	 * path
	 *
	 * @param path      The path
	 * @param basePaths The base paths used for relative paths
	 */
	public PathConverter(String path, List<String> basePaths) {
		this(path);

		if (logger.isTraceEnabled())
			logger.trace(">>> PathConverter({}, String[{}])", path, basePaths.size());

		this.basePaths = new ArrayList<>();
		for (String basePath: basePaths) {
			this.basePaths.add(basePath.replace(BACKSLASH, SLASH));
		}
	}

	/**
	 * Copy constructor
	 *
	 * @param pathConverter The PathConverter object to be copied
	 */
	public PathConverter(PathConverter pathConverter) {
		this(pathConverter.getPath());
		this.basePaths = pathConverter.basePaths;
	}

	/**
	 * Gets the current path.
	 *
	 * @return The path
	 */
	public String getPath() {
		return pathToConvert;
	}

	/**
	 * Converts backslashes to slashes in the path.
	 *
	 * @return A new PathConverter object with the converted path
	 */
	public PathConverter convertToSlash() {
		return new PathConverter(pathToConvert.replace(BACKSLASH, SLASH), basePaths);
	}

	/**
	 * Gets the first folder from the path.
	 *
	 * @return A new PathConverter object representing the first folder
	 */
	public PathConverter getFirstFolder() {
		String path = new PathConverter(pathToConvert).removeLeftSlash().getPath();
		File file = new File(path);

		if (file.getParent() == null) {
			return new PathConverter("", basePaths);
		}

		return new PathConverter(path.substring(0, path.indexOf(SLASH)), basePaths);
	}

	/**
	 * Removes the first folder from the path.
	 *
	 * @return A new PathConverter object without the first folder
	 */
	public PathConverter removeFirstFolder() {
		String path = new PathConverter(pathToConvert).removeLeftSlash().getPath();
		File file = new File(path);

		if (file.getParent() == null) {
			return new PathConverter(path, basePaths);
		}

		return new PathConverter(path.substring(path.indexOf(SLASH) + 1), basePaths);
	}

	/**
	 * Checks if the path is an S3 path.
	 *
	 * @return true if the path is an S3 path, false otherwise
	 */
	public boolean isS3Path() {
		return pathToConvert.startsWith(S3PREFIX);
	}

	/**
	 * Removes the file system prefixes from the path.
	 *
	 * @return A new PathConverter object without the file system prefixes
	 */
	public PathConverter removeFsPrefix() {
		if (pathToConvert.startsWith(S3PREFIX)) {
			return new PathConverter(pathToConvert.substring(S3PREFIX.length()), basePaths);
		}

		return new PathConverter(pathToConvert, basePaths);
	}

	/**
	 * Removes the base paths from the path.
	 *
	 * @return A new PathConverter object without the base paths
	 */
	public PathConverter removeBasePaths() {
		for (String basePath : basePaths) {
			if (pathToConvert.startsWith(basePath)) {
				return new PathConverter(pathToConvert.substring(basePath.length()), basePaths);
			}
		}

		return this;
	}

	/**
	 * Checks if the path has any base paths.
	 *
	 * @return true if the path has any base paths, false otherwise
	 */
	public boolean hasBasePaths() {
		for (String basePath : basePaths) {
			if (pathToConvert.startsWith(basePath)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Removes the bucket as the first folder from the path.
	 *
	 * @return A new PathConverter object without the bucket as the first folder
	 */
	public PathConverter removeBucket() {
		return new PathConverter(pathToConvert.substring(pathToConvert.indexOf(SLASH)), basePaths);
	}

	/**
	 * Removes the leading slash from the path.
	 *
	 * @return A new PathConverter object without the leading slash
	 */
	public PathConverter removeLeftSlash() {
		String path = pathToConvert;

		while (path.startsWith(SLASH)) {
			path = path.substring(SLASH.length());
		}

		return new PathConverter(path, basePaths);
	}

	/**
	 * Checks if the path starts with a slash (absolute path).
	 *
	 * @return true if the path starts with a slash, false otherwise
	 */
	public boolean startsWithSlash() {
		return pathToConvert.startsWith(SLASH);
	}

	/**
	 * Adds a slash at the end of the path.
	 *
	 * @return A new PathConverter object with a slash at the end of the path
	 */
	public PathConverter addSlashAtEnd() {
		if (pathToConvert.endsWith(SLASH) || pathToConvert.endsWith(BACKSLASH))
			return this;

		if (pathToConvert.contains(BACKSLASH))
			return new PathConverter(pathToConvert + BACKSLASH, basePaths);

		return new PathConverter(pathToConvert + SLASH, basePaths);
	}

	/**
	 * Fixes the absolute path by adding a slash at the end for directories and a
	 * slash at the beginning for Linux paths.
	 *
	 * @return A new PathConverter object with the fixed absolute path
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
	 * Checks if the path is a directory.
	 *
	 * @return true if the path is a directory, false otherwise
	 */
	public boolean isDirectory() {
		return (pathToConvert.endsWith(SLASH) || pathToConvert.endsWith(BACKSLASH));
	}

	/**
	 * Adds a slash at the beginning of the path.
	 *
	 * @return A new PathConverter object with a slash at the beginning of the path
	 */
	public PathConverter addSlashAtBegin() {
		if (pathToConvert.startsWith(SLASH))
			return this;

		return new PathConverter(SLASH + pathToConvert, basePaths);
	}

	/**
	 * Checks if the path is a Windows path.
	 *
	 * @return true if the path is a Windows path, false otherwise
	 */
	public boolean isWindowsPath() {
		if (isS3Path())
			return false;

		if ((pathToConvert.indexOf(':') == 1) && Character.isLetter(pathToConvert.charAt(0))) // "c:"
			return true;

		// TODO: Remove after integration, only for compatibility with v1, path like
		// "/c:"
		if ((pathToConvert.indexOf(':') == 2) && Character.isLetter(pathToConvert.charAt(1)) && pathToConvert.startsWith("/"))
			return true;

		// TODO: Remove after integration, only for compatibility with v1, path like
		// "POSIX|/c:"
		if ((pathToConvert.indexOf(':') == 8) && Character.isLetter(pathToConvert.charAt(7)) && pathToConvert.startsWith("POSIX|/"))
			return true;

		return false;
	}

	/**
	 * Checks if the path is a Linux path.
	 *
	 * @return true if the path is a Linux path, false otherwise
	 */
	public boolean isLinuxPath() {
		if (isS3Path() || isWindowsPath())
			return false;

		return true;
	}

	/**
	 * Converts the path to a compatible S3 path by replacing ":" in Windows paths.
	 *
	 * @return A new PathConverter object with the S3-compatible path
	 */
	public PathConverter posixToS3Path() {
		if (isWindowsPath()) {
			return new PathConverter(pathToConvert.replace(":", WINPATH_IN_S3), basePaths);
		}

		return this;
	}

	/**
	 * Converts the S3 path to a compatible POSIX path by restoring ":" in Windows
	 * paths.
	 *
	 * @return A new PathConverter object with the POSIX-compatible path
	 */
	public PathConverter s3ToPosixPath() {
		if (pathToConvert.indexOf(WINPATH_IN_S3) >= 0) {
			return new PathConverter(pathToConvert.replace(WINPATH_IN_S3, ":"), basePaths);
		}

		return this;
	}

	/**
	 * Gets the relative path from the path using the base path list.
	 *
	 * @return The relative path from the path using the base path list
	 */
	public PathConverter getRelativePath() {
		PathConverter pathConverter = new PathConverter(this).removeFsPrefix().removeBasePaths();

		if (isS3Path()) {
			pathConverter = pathConverter.removeBucket();
		}

		return pathConverter.removeLeftSlash();
	}

	/**
	 * Adds the S3 prefix to the path.
	 *
	 * @return The path with the S3 prefix
	 */
	public PathConverter addS3Prefix() {
		String pathWithoutLeftSlash = new PathConverter(this).removeLeftSlash().getPath();

		return new PathConverter(S3PREFIX + pathWithoutLeftSlash, basePaths);
	}

	/**
	 * Gets the file name from the path.
	 *
	 * @return The file name
	 */
	public String getFileName() {
		return new File(pathToConvert).getName();
	}

	/**
	 * Gets the storage type (S3 or POSIX) from the path.
	 *
	 * @return The storage type
	 */
	public StorageType getStorageType() {
		if (isS3Path()) {
			return StorageType.S3;
		} else {
			return StorageType.POSIX;
		}
	}

	/**
	 * Normalizes the Windows path by removing the leading slash.
	 *
	 * @return The normalized Windows path
	 */
	public PathConverter normalizeWindowsPath() {
		if (isWindowsPath() && pathToConvert.startsWith(SLASH)) {
			pathToConvert = pathToConvert.substring(1); // "/c:/blabla" => "c:/blabla"
		}

		// TODO: Remove after integration, only for compatibility with the existing code
		if (isWindowsPath() && pathToConvert.startsWith("POSIX|/")) {
			int i = 6;
			pathToConvert = pathToConvert.substring(0, i) + pathToConvert.substring(i + 1); // "POSIX|/c:/" => "POSIX|c:/"
		}

		return new PathConverter(pathToConvert, basePaths);
	}
}
