package de.dlr.proseo.basewrap;

import java.io.File;

public class PushedProcessingOutput {
	private long id;
	private String fsType;
	private String path;
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return the fsType
	 */
	public String getFsType() {
		return fsType;
	}
	/**
	 * @param fsType the fsType to set
	 */
	public void setFsType(String fsType) {
		this.fsType = fsType;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		if (path != null) {
			this.path = path.trim();
		} else {
			this.path = path;
		}
	}

	/**
	 * @return file name part of path
	 */
	public String getFileName() {
		if (this.path == null) {
			return "";
		} else {
			File productFile = new File(this.path);
			return productFile.getName();
		}
	}
	
	/**
	 * Add fs type to path if not exist
	 * 
	 * @return normed path part
	 */
	public String getNormedPath() {
		if (this.path == null) {
			return "";
		} else {
			String aPath = this.path;			
			int last = aPath.lastIndexOf('/');
			if (last > 0) {
				aPath = aPath.substring(0, last);
			}
			String res = aPath;
			if (this.fsType.equalsIgnoreCase("s3")) {
				if (aPath.startsWith("/")) {
					res = "s3:/" + aPath;
				} else if (!aPath.startsWith("s3://")) {
					if (aPath.startsWith("s3:/")) {
						res = aPath;
						res = res.replaceAll("s3:/", "s3://");
					} else {
						res = "s3://" + aPath;
					}
				}
			} else if (this.fsType.equalsIgnoreCase("alluxio")) {
				if (aPath.startsWith("/")) {
					res = "alluxio:/" + aPath;
				} else if (!aPath.startsWith("alluxio://")) {
					res = "alluxio://" + aPath;
				}
			}
			return res;
		}
	}


}
