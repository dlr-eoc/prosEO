package de.dlr.proseo.storagemgr.cache;

import java.time.Instant;

/**
 * @author Denys Chaykovskiy
 *
 */
public class FileInfo {

	private Instant accessed;
	private long size;

	/**
	 * @param accessed
	 * @param size
	 */
	public FileInfo(Instant accessed, long size) {

		this.accessed = accessed;
		this.size = size;
	}

	/**
	 * @return the accessed
	 */
	public Instant getAccessed() {
		return accessed;
	}

	/**
	 * @param accessed the accessed to set
	 */
	public void setAccessed(Instant accessed) {
		this.accessed = accessed;
	}

	/**
	 * @return the size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 *
	 */
	@Override
	public String toString() {
		return "Accessed: '" + this.accessed + "', Size: '" + this.size;
	}

}
