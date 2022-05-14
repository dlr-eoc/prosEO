package de.dlr.proseo.storagemgr.version2;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Path Converter
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PathConverter {

	private String p;
	private List<String> basePaths = new ArrayList<>();

	private static String S3PREFIX = "s3://";
	private static String SLASH = "/";
	private static String DOUBLESLASH = "//";
	private static String BACKSLASH = "\\";
	private static String WINPATH_IN_S3 = "-WIN-PATH-";

	public PathConverter(String path) {
		p = path.replace(BACKSLASH, SLASH); // convertToSlash
		p = p.trim();
	}

	public PathConverter(String path, List<String> basePaths) {
		this(path);
		this.basePaths = basePaths;
	}

	public PathConverter(PathConverter pathConverter) {
		this(pathConverter.getPath());
		this.basePaths = pathConverter.basePaths;
	}

	public void addBasePath(String basePath) {
		String path = new PathConverter(basePath).removeLeftSlash().getPath();
		basePaths.add(path);
	}

	public String getPath() {
		return p;
	}

	public PathConverter convertToSlash() {
		return new PathConverter(p.replace(BACKSLASH, SLASH), basePaths);
	}

	public PathConverter getFirstFolder() {

		String path = new PathConverter(p).removeLeftSlash().getPath();

		File file = new File(path);

		if (file.getParent() == null) {
			return new PathConverter("", basePaths);
		}

		return new PathConverter(path.substring(0, path.indexOf(SLASH)), basePaths);
	}

	public PathConverter removeFirstFolder() {

		String path = new PathConverter(p).removeLeftSlash().getPath();

		File file = new File(path);

		if (file.getParent() == null) {
			return new PathConverter(path, basePaths);
		}

		return new PathConverter(path.substring(path.indexOf(SLASH) + 1), basePaths);
	}

	public boolean isS3Path() {

		return p.startsWith(S3PREFIX) ? true : false;
	}

	public PathConverter removeFsPrefix() {

		if (p.startsWith(S3PREFIX)) {
			return new PathConverter(p.substring(S3PREFIX.length()), basePaths);
		}

		return new PathConverter(p, basePaths);
	}

	// removes base Path from path if base path is in the list. If not, deletes
	// first folder as base path
	public PathConverter removeBasePath() {

		for (String basePath : basePaths) {

			if (p.startsWith(basePath)) {
				return new PathConverter(p.substring(basePath.length()), basePaths);
			}
		}

		return this;
	}

	public PathConverter removeBucket() {

		return new PathConverter(p.substring(p.indexOf(SLASH)), basePaths);
	}

	public PathConverter removeLeftSlash() {

		String path = p;

		while (path.startsWith(SLASH)) {
			path = path.substring(SLASH.length());
		}

		return new PathConverter(path, basePaths);
	}

	public PathConverter addSlashAtEnd() {

		if (p.endsWith(SLASH))
			return this;
		
		if (p.endsWith(BACKSLASH))
			return this;
		
		if (p.contains(SLASH))
			return new PathConverter(p + SLASH, basePaths);

		if (p.contains(BACKSLASH))
			return new PathConverter(p + BACKSLASH, basePaths);

		return this;
	}

	public PathConverter verifyAbsolutePath() {

		PathConverter pathConverter = this;

		if (isDirectory())
			pathConverter.addSlashAtEnd();

		if (isLinuxPath())
			pathConverter.addSlashAtBegin();

		return pathConverter;
	}

	public boolean isDirectory() {

		return (p.endsWith(SLASH) || p.endsWith(BACKSLASH)) ? true : false; 
	}

	public PathConverter addSlashAtBegin() {

		if (p.startsWith(SLASH))
			return this;

		return new PathConverter(SLASH + p, basePaths);
	}

	public boolean isWindowsPath() {

		if (isS3Path())
			return false;

		if (p.indexOf(':') >= 0)
			return true;

		return false;
	}

	public boolean isLinuxPath() {

		if (isS3Path())
			return false;

		if (isWindowsPath())
			return false;

		return true;
	}

	public PathConverter posixToS3Path() {

		// windows - replace ":"
		if (isWindowsPath()) {
			return new PathConverter(p.replace(":", WINPATH_IN_S3), basePaths);
		}

		// Linux - no action
		return this;
	}

	public PathConverter s3ToPosixPath() {

		// windows - restore ":"
		if (isWinPathInS3()) {
			return new PathConverter(p.replace(WINPATH_IN_S3, ":"), basePaths);
		}

		// Linux - no action
		return this;
	}

	public boolean isWinPathInS3() {

		return (p.indexOf(WINPATH_IN_S3) >= 0) ? true : false;
	}

	public PathConverter removeDoubleSlash() {

		return new PathConverter(p.replace(DOUBLESLASH, SLASH), basePaths);
	}

	public PathConverter getRelativePath() {

		PathConverter pathConverter = new PathConverter(this).removeFsPrefix().removeBasePath();

		if (isS3Path()) {
			pathConverter = pathConverter.removeBucket();
		}

		return pathConverter.removeLeftSlash();
	}
}
