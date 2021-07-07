/**
 * Quota.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.util.Date;

import javax.persistence.Embeddable;

/**
 * Monthly data volume granted to and actually used by a user
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Embeddable
public class Quota {
	
	/** Monthly data volume allowance in GiB */
	private Integer assigned = 0;
	
	/**
	 * Data volume in GiB used in the last recorded calendar month
	 * (will be reset, when an access in another calendar month than recorded in "lastAccessDate" happens)
	 */
	private Integer used = 0;
	
	/** Date of last recorded access (determines calendar month, for which the "used" volume count is valid) */
	private Date lastAccessDate = new Date();

	/**
	 * Gets the assigned data volume in GiB
	 * 
	 * @return the assigned data volume
	 */
	public Integer getAssigned() {
		return assigned;
	}

	/**
	 * Sets the assigned data volume in GiB
	 * 
	 * @param assigned the assigned data volume to set
	 */
	public void setAssigned(Integer assigned) {
		this.assigned = assigned;
	}

	/**
	 * Gets the used data volume in GiB
	 * 
	 * @return the used data volume
	 */
	public Integer getUsed() {
		return used;
	}

	/**
	 * Sets the used data volume in GiB
	 * 
	 * @param used the used data volume to set
	 */
	public void setUsed(Integer used) {
		this.used = used;
	}

	/**
	 * Gets the date of the last recorded access
	 * 
	 * @return the last access date
	 */
	public Date getLastAccessDate() {
		return lastAccessDate;
	}

	/**
	 * Sets the date of the last recorded access
	 * 
	 * @param lastAccessDate the last access date to set
	 */
	public void setLastAccessDate(Date lastAccessDate) {
		this.lastAccessDate = lastAccessDate;
	}
	
}
