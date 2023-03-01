package de.dlr.proseo.storagemgr.cache;

import java.time.Instant;

/**
 * File Info (last Accessed and file size) for File Cache with sorting and
 * search
 * 
 * @author Denys Chaykovskiy
 *
 */
public class FileInfo {

	/** last accessed time stamp */
	private Instant accessed;

	/** size of the file in bytes */
	private long size;

	/**
	 * File Info Constructor with accessed and size values
	 * 
	 * @param accessed Last accessed time stamp
	 * @param size     the size of file
	 */
	public FileInfo(Instant accessed, long size) {

		this.accessed = accessed;
		this.size = size;
	}

	/**
	 * Gets the last accessed
	 * 
	 * @return accessed last accessed time stamp
	 */
	public Instant getAccessed() {
		return accessed;
	}

	/**
	 * Sets the last accessed
	 * 
	 * @param accessed the accessed to set
	 */
	public void setAccessed(Instant accessed) {
		this.accessed = accessed;
	}

	/**
	 * Gets the file size in bytes
	 * 
	 * @return the size of the file
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Sets the file size in bytes
	 * 
	 * @param size the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * Shows string representation -> accessed + size
	 * 
	 * @return accessed + size converted in string
	 */
	@Override
	public String toString() {
		return "Accessed: '" + this.accessed + "', Size: '" + this.size;
	}
}
