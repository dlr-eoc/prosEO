package de.dlr.proseo.storagemgr.rest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.StorageFile;

public class StorageFileConverter {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(StorageFileConverter.class);

	public static boolean isStringBase64(String stringBase64) {

		String stringBase64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";

		return stringBase64.matches(stringBase64Pattern) ? true : false;
	}

	public static String convertToString(StorageFile sourceFile) throws IOException {

		StorageFileConverter converter = new StorageFileConverter();
		FileInputStream stream = StorageFileConverter.convertToInputStream(sourceFile);
		return converter.convertToString(stream);
	}

	public static FileInputStream convertToInputStream(StorageFile sourceFile) throws FileNotFoundException {

		String fullpath = sourceFile.getFullPath();

		try {
			return new FileInputStream(fullpath);

		} catch (FileNotFoundException e) {
			logger.error("Requested POSIX file {} not found", fullpath);
			throw e;
		}
	}

	private String convertToString(FileInputStream stream) throws IOException {

		try {
			byte[] bytes = java.util.Base64.getEncoder().encode(stream.readAllBytes());
			stream.close();
			return new String(bytes);

		} catch (IOException e) {
			logger.error("Invalid job order stream");
			throw e;
		}
	}

	public static boolean isValidXml(String jobOrder64) {

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			dBuilder.parse(jobOrder64);
			return true;

		} catch (Exception e) {
			return false;
		}
	}
}
