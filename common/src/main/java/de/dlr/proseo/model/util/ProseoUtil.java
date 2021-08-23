/**
 * ProseoUtil.java
 * 
 * @author Ernst Melchinger
 */

package de.dlr.proseo.model.util;


/**
 *  Class to hold general utility methods
 *
 */
public class ProseoUtil {
	/**
	 * escape()
	 *
	 * Escape a give String to make it safe to be printed or stored.
	 *
	 * @param s The input String.
	 * @return The output String.
	 **/
	public static String escape(String s){
	  return s.replace("\\", "\\\\")
	          .replace("\t", "\\t")
	          .replace("\b", "\\b")
	          .replace("\n", "\\n")
	          .replace("\r", "\\r")
	          .replace("\f", "\\f")
	          .replace("\'", "\\'")
	          .replace("\"", "\\\"");
	}
}
