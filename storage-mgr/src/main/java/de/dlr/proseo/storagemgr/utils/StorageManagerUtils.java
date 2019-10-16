package de.dlr.proseo.storagemgr.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class StorageManagerUtils {

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
