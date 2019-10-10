package de.dlr.proseo.samplewrap;

public class PushedProcessingOutput {
	private String id;
	private String fsType;
	private String path;
	private long revision;

	/**
	 * Gets the product identifier
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the product identifier
	 * 
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Gets the FS type
	 * 
	 * @return the Filesystem Type
	 */
	public String getFsType() {
		return fsType;
	}

	/**
	 * Sets the FS type
	 * 
	 * @return the Filesystem Type
	 */
	public void setFsType(String fsType) {
		this.fsType = fsType;
	}

	/**
	 * Gets the product Path
	 * 
	 * @param id the id to set
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the product Path
	 * 
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the revision
	 */
	public long getRevision() {
		return revision;
	}

	/**
	 * @param revision the revision to set
	 */
	public void setRevision(long revision) {
		this.revision = revision;
	}


}
