/**
 * MD5Util.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.basewrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods to efficiently calculate an MD5 hash string from a file
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class MD5Util {

	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes();
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(MD5Util.class);

	/**
	 * Generate a string of hex digits (upper-case) representing the given byte array
	 * 
	 * @param bytes the byte array to convert
	 * @return a string with (upper-case) hex digits
	 */
	public static String bytesToHex(byte[] bytes) {
		if (logger.isTraceEnabled()) logger.trace(">>> bytesToHex(...)");

	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars, StandardCharsets.UTF_8);
	}
	
	/**
	 * Create an (upper-case) MD5 hash string for the given file
	 * 
	 * @param inputFile the file to digest
	 * @return an MD5 hash string
	 * @throws IOException if the given file cannot be read
	 */
	public static String md5Digest(File inputFile) throws IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> md5Digest({})", inputFile.getName());

		try (FileInputStream inputStream = new FileInputStream(inputFile)) {
			MessageDigest digest = MessageDigest.getInstance("MD5");

			byte[] bytesBuffer = new byte[1024 * 1024]; // Process 1 MB chunks
			int bytesRead = -1;

			while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
				digest.update(bytesBuffer, 0, bytesRead);
			}

			byte[] hashedBytes = digest.digest();

			return bytesToHex(hashedBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MD5 hash not implemented", e);
		}
	}

}
