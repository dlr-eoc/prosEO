/**
 * StringUtils.java
 *
 * (C) 2024 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.utils;

import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * A utility class for common string operations. 
 *
 * @author Denys Chaykovskiy
 */
public class StringUtils {

	/** the full path to file */
	private String str;

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(FileUtils.class);

	/**
	 * Constructor sets the string
	 *
	 * @param str String
	 */
	public StringUtils(String str) {

		this.str = str;
	}
	
	/**
	 * Returns the first maxLength characters of the string. If the string shorter than maxLength, return the whole string
	 * 
	 * @param maxLength
	 * @return
	 */
	public String getMaxSubstring(int maxLength) {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> getMaxSubstring({})", maxLength);
		
	    return str.length() <= maxLength ? str : str.substring(0, maxLength);
	}
}