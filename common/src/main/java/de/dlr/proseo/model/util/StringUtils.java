/**
 * StringUtils.java
 * 
 * (C) 2023 DLR
 */
package de.dlr.proseo.model.util;

/**
 * String Utils
 * 
 * @deprecated as of prosEO 2.2.0: Use org.springframework.util.StringUtils instead
 * 
 * @author Denys Chaykovskiy
 */

@Deprecated
public class StringUtils {
		
	/**
	 * Checks if string null or empty
	 * 
	 * @param str String to check
	 * @return true if string null or empty
	 */
	public static boolean isNullOrEmpty(String str) {
		
		return str == null || str.isEmpty();
	}
	
	/**
	 * Checks if string null or blank
	 * 
	 * @param str String to check
	 * @return true if string null or blank
	 */
	public static boolean isNullOrBlank(String str) {
		
		return str == null || str.isBlank();
	}
}
