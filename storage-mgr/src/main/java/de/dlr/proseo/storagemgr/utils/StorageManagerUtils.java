package de.dlr.proseo.storagemgr.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import java.util.List;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.amazonaws.services.s3.AmazonS3;

import de.dlr.proseo.model.fs.s3.S3Ops;
import de.dlr.proseo.storagemgr.rest.model.StorageType;
import software.amazon.awssdk.services.s3.S3Client;

public class StorageManagerUtils {


	/**
	 * @param s3AccessKey
	 * @param s3SecretAccesKey
	 * @param s3Endpoint
	 * @param bucketName
	 * @return
	 */
	public static Boolean createStorageManagerInternalS3Buckets(String s3AccessKey, String s3SecretAccesKey, String s3Endpoint, String bucketName, String region) throws Exception {

		S3Client s3 = S3Ops.v2S3Client(s3AccessKey,  s3SecretAccesKey, s3Endpoint);
		ArrayList<String> buckets = S3Ops.listBuckets(s3);
		if (!buckets.contains(bucketName)) {
			String  bckt = S3Ops.createBucket(s3, bucketName, region);
			if (null == bckt) return false;
		}
		return true;
	}

	/**
	 * List all available storages
	 * 
	 * @return ArrayList<String> of storageIds
	 */
	/**
	 * @param s3AccessKey
	 * @param s3SecretAccessKey
	 * @param s3Endpoint
	 * @param globalStorageIdPrefix
	 * @param alluxioUnderFsBucket
	 * @param alluxioUnderFsS3BucketPrefix
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String[]> getAllStorages(String s3AccessKey, String s3SecretAccessKey, String s3Endpoint, String globalStorageIdPrefix, String alluxioUnderFsBucket, String alluxioUnderFsS3BucketPrefix) throws Exception{
		// global storages...
		ArrayList<String[]> storages = new ArrayList<String[]>();

		// fetch S3-buckets
		S3Client s3 = S3Ops.v2S3Client(s3AccessKey, s3SecretAccessKey,s3Endpoint);
		ArrayList<String> s3bckts = S3Ops.listBuckets(s3);

		for (String b : s3bckts) {
			if(b.startsWith(globalStorageIdPrefix)) {
				String[] s = new String[2];
				s[0]=b;
				s[1]=String.valueOf(StorageType.S_3);
				storages.add(s);
			}
		}

		// fetch Alluxio-Prefixes
		AmazonS3 s3_v1 = S3Ops.v1S3Client(s3AccessKey, s3SecretAccessKey,s3Endpoint);
		List<String> allxio = S3Ops.listKeysInBucket(s3_v1, alluxioUnderFsBucket,alluxioUnderFsS3BucketPrefix,true);

		for (String p : allxio) {
			if(p.startsWith(globalStorageIdPrefix)) {
				String[] s = new String[2];
				s[0]=p;
				s[1]=String.valueOf(StorageType.ALLUXIO);
				storages.add(s);
			}
		}
		s3.close();
		s3_v1.shutdown();
		return storages;
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
}
