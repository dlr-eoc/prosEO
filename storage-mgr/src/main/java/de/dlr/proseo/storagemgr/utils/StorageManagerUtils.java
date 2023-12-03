package de.dlr.proseo.storagemgr.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.dlr.proseo.storagemgr.fs.s3.S3Ops;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * General utility methods 
 * 
 * @author melchinger
 *
 */
public class StorageManagerUtils {

	private static final String DIRECTORY_ROOT = "/";
	private static final String PROTOCOL_ALLUXIO = "alluxio://";
	private static final String PROTOCOL_S3 = "s3://";
	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(StorageManagerUtils.class);

	/**
	 * Creates an internal bucket for use by the Storage Manager itself; passes all exceptions on to the caller
	 * 
	 * @param s3AccessKey the access key for the client
	 * @param s3SecretAccesKey the secret access key for the client
	 * @param s3Endpoint the S3 endpoint to connect to
	 * @param bucketName the name of the bucket to create
	 * @param region the region, on which the client shall operate
	 * @return true, if the bucket exists or was created successfully, false otherwise
	 */
	public static Boolean createStorageManagerInternalS3Buckets(String s3AccessKey, String s3SecretAccesKey, String s3Endpoint,
			String bucketName, String region) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> createStorageManagerInternalS3Buckets(***, ***, {}, {}, {}", 
				s3Endpoint, bucketName, region);
		
		S3Client s3 = S3Ops.v2S3Client(s3AccessKey,  s3SecretAccesKey, s3Endpoint, region);
		ArrayList<String> buckets = S3Ops.listBuckets(s3);
		if (!buckets.contains(bucketName)) {
			String  bckt = S3Ops.createBucket(s3, bucketName, region);
			if (null == bckt) return false;
		}
		return true;
	}
	
	/**
	 * Check if the provided String represents a valid XML Document
	 * 
	 * @param xml the input String
	 * @return true, if the string is valid, false otherwise
	 */
	public static Boolean checkXml(String xml) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> checkXml(String)");
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			dBuilder.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(SAXParseException e) throws SAXException {
				}

				@Override
				public void fatalError(SAXParseException e) throws SAXException {
				}

				@Override
				public void error(SAXParseException e) throws SAXException {
				}
			});
			dBuilder.parse(new InputSource(new StringReader(xml)));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Converts an InputStrem to String
	 * 
	 * @param inputStream the input stream to read
	 * @param charset the charset of the string (defaults to UTF-8)
	 * @return String a string containing the data of the input stream
	 * @throws IOException if an I/O error occurs
	 */
	public static String inputStreamToString(InputStream inputStream, Charset charset) throws IOException {
		
		if (logger.isTraceEnabled()) logger.trace(">>> inputStreamToString(InputStream, Charset)");
		
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;

		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
		}
		inputStream.close();
		return stringBuilder.toString();
	}

	/**
	 * Get the storage type of a given path string
	 * 
	 * @param pathInfo the path to check
	 * @return the storage type indicated by this path
	 */
	public static StorageType getFsType(String pathInfo) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> getFsType({})", pathInfo);
		
		StorageType storageType = null;
		if (pathInfo != null) {
			// Find storage type
			if (pathInfo.toLowerCase().startsWith(PROTOCOL_S3)) { // upper case and lower case protocol name allowed!
				storageType = StorageType.S3;
			} else if (pathInfo.startsWith(PROTOCOL_ALLUXIO)) {
				storageType = StorageType.ALLUXIO;
			} else if (pathInfo.startsWith(DIRECTORY_ROOT)) {
				storageType = StorageType.POSIX;
			}
		}
		return storageType;
	}
	/**
	 * Remove storage dependent path information  
	 * 
	 * @param pathInfo the full path string
	 * @return the path string without the protocol part
	 */
	public static String getRelativePath(String pathInfo) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> getRelativePath({})", pathInfo);
		
		String relPath = null;
		if (pathInfo != null) {
			relPath = pathInfo.trim();
			// Remove protocol from path
			if (relPath.toLowerCase().startsWith(PROTOCOL_S3)) { // upper case and lower case protocol name allowed!
				relPath = DIRECTORY_ROOT + relPath.substring(PROTOCOL_S3.length());
			} else if (relPath.startsWith(PROTOCOL_ALLUXIO)) {
				relPath = DIRECTORY_ROOT + relPath.substring(PROTOCOL_ALLUXIO.length());
			}
			
		}
		return relPath;
	}
}
