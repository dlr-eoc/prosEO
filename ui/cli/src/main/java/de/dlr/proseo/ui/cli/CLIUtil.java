/**
 * CLIUtil.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.Date;
import java.util.List;


/**
 * Utility methods for command interpretation
 * 
 * @author Dr. Thomas Bassler
 */
public class CLIUtil {

	/* Message ID constants */
	private static final int MSG_ID_INVALID_FILE_TYPE = 2990;
	private static final int MSG_ID_INVALID_FILE_STRUCTURE = 2991;
	private static final int MSG_ID_INVALID_FILE_SYNTAX = 2992;
	private static final int MSG_ID_INVALID_ATTRIBUTE_NAME = 2993;
	private static final int MSG_ID_INVALID_ATTRIBUTE_TYPE = 2994;
	private static final int MSG_ID_REFLECTION_EXCEPTION = 2995;
	
	/* Message string constants */
	private static final String MSG_INVALID_FILE_TYPE = "(E%d) Invalid order file type %s";
	private static final String MSG_INVALID_FILE_STRUCTURE = "(E%d) %s content of order file %s invalid for order generation (cause: %s)";
	private static final String MSG_INVALID_FILE_SYNTAX = "(E%d) Order file %s contains invalid %s content (cause: %s)";
	private static final String MSG_INVALID_ATTRIBUTE_NAME = "(E%d) Invalid attribute name %s";
	private static final String MSG_INVALID_ATTRIBUTE_TYPE = "(E%d) Attribute %s cannot be converted to type %s";
	private static final String MSG_REFLECTION_EXCEPTION = "(E%d) Reflection exception setting attribute %s (cause: %s)";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CLIUtil.class);
	
	/**
	 * Read the description for an object of the given type from a file in Json, XML or Yaml format
	 * @param <T> the type parameter
	 * @param objectFile the file to read
	 * @param fileFormat the file format (one of { JSON, XML, YAML })
	 * @param clazz the class object for type T
	 * @return a deserialized object of type T
	 * @throws IllegalArgumentException if the file format is not valid or the object file does not conform to the file format
	 *             or the object file cannot be deserialized into an object of type T
	 * @throws IOException if an error occurs while reading the object file
	 */
	public static <T> T parseObjectFile(File objectFile, String fileFormat, Class<T> clazz)
			throws IllegalArgumentException, IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> parseObjectFile({}, {}, {})", objectFile, fileFormat, clazz);
		
		ObjectMapper mapper = null;
		switch(fileFormat.toUpperCase()) {
		case "JSON":
			mapper = new ObjectMapper();
			break;
		case "XML":
			mapper = new XmlMapper();
			break;
		case "YAML":
			mapper = new ObjectMapper(new YAMLFactory());
			break;
		default:
			String message = String.format(MSG_INVALID_FILE_TYPE, MSG_ID_INVALID_FILE_TYPE, fileFormat);
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		
		try {
			return mapper.readValue(objectFile, clazz);
		} catch (JsonParseException e) {
			String message = String.format(MSG_INVALID_FILE_SYNTAX, MSG_ID_INVALID_FILE_SYNTAX,
					objectFile.toString(), fileFormat, e.getMessage());
			logger.error(message);
			throw new IllegalArgumentException(message, e);
		} catch (JsonMappingException e) {
			String message = String.format(MSG_INVALID_FILE_STRUCTURE, MSG_ID_INVALID_FILE_STRUCTURE,
					fileFormat, objectFile.toString(), e.getMessage());
			logger.error(message);
			throw new IllegalArgumentException(message, e);
		} catch (IOException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Set an object attribute from an "attribute=value" parameter using reflection. Supported attribute types are
	 * String, Date, Long and List<String>.
	 * 
	 * @param restObject the object to set the attribute in
	 * @param attributeParameter a string of the form "attribute=value", where "value" may be a comma-separated string list
	 * @throws IllegalArgumentException if the given attribute name does not exist or is not accessible in the given object
	 * @throws ClassCastException if the attribute type is not supported (i. e. not listed above)
	 */
	public static void setAttribute(Object restObject, String attributeParameter) throws IllegalArgumentException, ClassCastException {
		if (logger.isTraceEnabled()) logger.trace(">>> setAttribute({}, {})", restObject.getClass().toString(), attributeParameter);
		
		String[] paramParts = attributeParameter.split("=");
		Field attributeField = null;
		try {
			attributeField = restObject.getClass().getDeclaredField(paramParts[0]);
		} catch (Exception e) {
			String message = (String.format(MSG_INVALID_ATTRIBUTE_NAME, MSG_ID_INVALID_ATTRIBUTE_NAME, paramParts[0]));
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		try {
			attributeField.setAccessible(true);
			if (List.class.isAssignableFrom(attributeField.getType())) {
				// Parse attribute as multi-valued String list
				attributeField.set(restObject, Arrays.asList(paramParts[1].split(",")));
			} else if (Number.class.isAssignableFrom(attributeField.getType())) {
				// Parse attribute value as Long (there are no floating point attributes in RestOrder)
				attributeField.set(restObject, Long.parseLong(paramParts[1]));
			} else if (Date.class.isAssignableFrom(attributeField.getType())) {
				// Parse attribute value as Date of the form "yyyy-MM-dd'T'HH:mm:ss"
				attributeField.set(restObject, Date.from(Instant.parse(paramParts[1] + "Z"))); // no timezone expected
			} else if (Instant.class.isAssignableFrom(attributeField.getType())) {
				// Parse attribute value as Instant of the form "yyyy-MM-dd'T'HH:mm:ss"
				attributeField.set(restObject, Instant.parse(paramParts[1] + "Z")); // no timezone expected
			} else if (String.class.isAssignableFrom(attributeField.getType())) {
				// Use attribute value as is
				attributeField.set(restObject, paramParts[1]);
			} else {
				// Attribute type not supported
				String message = String.format(MSG_INVALID_ATTRIBUTE_TYPE, MSG_ID_INVALID_ATTRIBUTE_TYPE,
						paramParts[0], attributeField.getType().toString());
				logger.error(message);
				throw new ClassCastException(message);
			}
		} catch (Exception e) {
			String message = String.format(MSG_REFLECTION_EXCEPTION, MSG_ID_REFLECTION_EXCEPTION,
					paramParts[0], e.getMessage());
			logger.error(message);
			throw new RuntimeException(message, e);
		}
	}
	
}
