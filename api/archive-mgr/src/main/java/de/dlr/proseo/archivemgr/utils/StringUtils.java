/**
 * StringUtils.java
 *
 * (C) 2023 DLR
 */
package de.dlr.proseo.archivemgr.utils;

/**
 * String Utils
 *
 * @author Denys Chaykovskiy
 */
public class StringUtils {

	/**
	 * Checks if two strings are equal
	 *
	 * @param str1 string 1
	 * @param str2 string 2
	 * @return true if both strings null, both empty or have same characters
	 */
	public static boolean equalStrings(String str1, String str2) {

		if (str1 == null && str2 == null) {
			return true;

		} else if (str1 == null || str2 == null) {
			return false;

		} else if (str1.isEmpty() && str2.isEmpty()) {
			return true;

		} else if (str1.equals(str2)) {
			return true;

		} else {
			return false;
		}
	}

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