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
import de.dlr.proseo.model.enums.StorageType;

/**
 * General utility methods 
 * 
 * @author melchinger
 *
 */
public class StorageManagerUtils {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(StorageManagerUtils.class);

	/**
	 * @param s3AccessKey
	 * @param s3SecretAccesKey
	 * @param s3Endpoint
	 * @param bucketName
	 * @return
	 */
	public static Boolean createStorageManagerInternalS3Buckets(String s3AccessKey, String s3SecretAccesKey, String s3Endpoint, String bucketName, String region) throws Exception {

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
	 * @return true/false
	 */
	public static Boolean checkXml(String xml) {

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
	 * @param inputStream
	 * @param charset the charset of the string (defaults to UTF-8)
	 * @return String
	 * @throws IOException
	 */
	public static String inputStreamToString(InputStream inputStream, Charset charset) throws IOException {

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
	 * Get StorageType of path
	 * 
	 * @param pathInfo
	 * @return StorageType
	 */
	public static StorageType getFsType(String pathInfo) {
		StorageType storageType = null;
		if (pathInfo != null) {
			// Find storage type
			if (pathInfo.startsWith("s3:") || pathInfo.startsWith("S3:")) {
				storageType = StorageType.S3;
			} else if (pathInfo.startsWith("alluxio:")) {
				storageType = StorageType.ALLUXIO;
			} else if (pathInfo.startsWith("/")) {
				storageType = StorageType.POSIX;
			}
		}
		return storageType;
	}
	/**
	 * Remove storage dependent path information  
	 * 
	 * @param pathInfo
	 * @return Relative path
	 */
	public static String getRelativePath(String pathInfo) {
		String relPath = null;
		if (pathInfo != null) {
			relPath = pathInfo.trim();
			// Find storage type
			if (relPath.startsWith("s3:/") || relPath.startsWith("S3:/")) {
				relPath = relPath.substring(4);
			} else if (relPath.startsWith("alluxio:/")) {
				relPath = relPath.substring(9);
			} else if (relPath.startsWith("/")) {
				relPath = relPath;
			}
			
		}
		return relPath;
	}
}
