/**
 * CLIUtil.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * Utility methods for command interpretation
 * 
 * @author Dr. Thomas Bassler
 */
public class CLIUtil {

	/** YAML file format */
	public static final String FILE_FORMAT_YAML = "YAML";
	/** JSON file format */
	public static final String FILE_FORMAT_JSON = "JSON";
	/** XML file format */
	public static final String FILE_FORMAT_XML = "XML";
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(CLIUtil.class);
	
	/**
	 * Helper class to return username and password from a method
	 */
	public static class Credentials {
		public String username;
		public String password;
	}
	
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
		case FILE_FORMAT_JSON:
			mapper = new ObjectMapper();
			break;
		case FILE_FORMAT_XML:
			mapper = new XmlMapper();
			break;
		case FILE_FORMAT_YAML:
			mapper = new ObjectMapper(new YAMLFactory());
			break;
		default:
			String message = logger.log(UIMessage.INVALID_FILE_TYPE, fileFormat);
			throw new IllegalArgumentException(message);
		}
		
		try {
			return mapper.readValue(objectFile, clazz);
		} catch (JsonParseException e) {
			String message = logger.log(UIMessage.INVALID_FILE_SYNTAX, objectFile.toString(), fileFormat, e.getMessage());
			throw new IllegalArgumentException(message, e);
		} catch (JsonMappingException e) {
			String message = logger.log(UIMessage.INVALID_FILE_STRUCTURE, fileFormat, objectFile.toString(), e.getMessage());
			throw new IllegalArgumentException(message, e);
		} catch (IOException e) {
			String message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			throw new IOException(message, e);
		}
	}
	
	/**
	 * Print the given object to the given output stream according to the requested file format; if the object is a list or set
	 * of size 1, then the single element of the collection is printed, not the list/set itself
	 * 
	 * @param out the output stream to print to
	 * @param object the object to print
	 * @param fileFormat the file format requested (one of JSON, XML, YAML)
	 * @throws IllegalArgumentException if the file format is not one of the above, or if a formatting error occurs during printing
	 * @throws IOException if an I/O error occurs during printing
	 */
	@SuppressWarnings("rawtypes")
	public static void printObject(PrintStream out, Object object, String fileFormat) throws IllegalArgumentException, IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> printObject({}, object, {})", out, object, fileFormat);
		
		ObjectMapper mapper = null;
		switch(fileFormat.toUpperCase()) {
		case FILE_FORMAT_JSON:
			mapper = new ObjectMapper();
			break;
		case FILE_FORMAT_XML:
			mapper = (new XmlMapper()).enable(ToXmlGenerator.Feature.WRITE_XML_1_1);
			break;
		case FILE_FORMAT_YAML:
			mapper = new ObjectMapper(new YAMLFactory());
			break;
		default:
			String message = logger.log(UIMessage.INVALID_FILE_TYPE, fileFormat);
			throw new IllegalArgumentException(message);
		}
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		
		try {
			Object objectToPrint = object;
			if (object instanceof List && 1 == ((List) object).size()) {
				objectToPrint = ((List) object).get(0);
			} else if (object instanceof Set && 1 == ((Set) object).size()) {
				objectToPrint = ((Set) object).iterator().next();
			}
			out.println(mapper.writeValueAsString(objectToPrint));
		} catch (JsonGenerationException e) {
			String message = logger.log(UIMessage.GENERATION_EXCEPTION, object, fileFormat, e.getMessage());
			throw new IllegalArgumentException(message, e);
		} catch (JsonMappingException e) {
			String message = logger.log(UIMessage.MAPPING_EXCEPTION, object, fileFormat, e.getMessage());
			throw new IllegalArgumentException(message, e);
		} catch (IOException e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Set an object attribute from an "attribute=value" parameter using reflection. Supported attribute types are
	 * String, Date, Long and List&lt;String&gt;.
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
			String message = logger.log(UIMessage.INVALID_ATTRIBUTE_NAME, paramParts[0]);
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
				attributeField.set(restObject, Date.from(parseDateTime(paramParts[1])));
			} else if (Instant.class.isAssignableFrom(attributeField.getType())) {
				// Parse attribute value as Instant of the form "yyyy-MM-dd'T'HH:mm:ss"
				attributeField.set(restObject, parseDateTime(paramParts[1]));
			} else if (Boolean.class.isAssignableFrom(attributeField.getType())) {
				// Parse attribute value as Boolean (true/false)
				attributeField.set(restObject, Boolean.parseBoolean(paramParts[1]));
			} else if (String.class.isAssignableFrom(attributeField.getType())) {
				// Use attribute value as is
				attributeField.set(restObject, paramParts[1]);
			} else {
				// Attribute type not supported
				String message = logger.log(UIMessage.INVALID_ATTRIBUTE_TYPE, paramParts[0], attributeField.getType().toString());
				if (null != System.console()) System.err.println(message);
				throw new ClassCastException(message);
			}
		} catch (ClassCastException e) {
			// Already formatted, rethrow
			throw e;
		} catch (IllegalArgumentException | DateTimeException e) {
			String message = logger.log(UIMessage.INVALID_ATTRIBUTE_TYPE, paramParts[0], attributeField.getType().toString());
			if (null != System.console()) System.err.println(message);
			throw new ClassCastException(message);
		} catch (Exception e) {
			String message = logger.log(UIMessage.REFLECTION_EXCEPTION, paramParts[0], e.getMessage());
			if (null != System.console()) System.err.println(message);
			throw new RuntimeException(message, e);
		}
	}
	
	/**
	 * Parse a date and time string in the format "yyyy-MM-dd'T'HH:mm:ss.SSSSSS[zZX]", whereby all of the following variants 
	 * are allowed:
	 * <ul>
	 *   <li>yyyy-MM-dd</li>
	 *   <li>yyyy-MM-ddTHH:mm</li>
	 *   <li>yyyy-MM-ddTHH:mm:ss</li>
	 *   <li>yyyy-MM-ddTHH:mm:ss.S[SSSSS]</li>
	 *   <li>any of the above plus a time zone in general, RFC 822 or ISO 8601 format</li>
	 * </ul>
	 * Missing parts are set to zero, a missing time zone is set to UTC.
	 * 
	 * @param dateTime the date and time string to parse
	 * @return the parsed point in time
	 * @throws DateTimeException if the given string cannot be parsed according to the format given above
	 */
	public static Instant parseDateTime(String dateTime) throws DateTimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> parseDateTime({})", dateTime);
		
		Instant result;
		try {
			result = Instant.from(OrbitTimeFormatter.parse(dateTime));
		} catch (DateTimeParseException e) {
			throw new DateTimeException(e.getMessage(), e);
		}
		
		if (logger.isTraceEnabled()) logger.trace(String.format("... converted input string %s to Instant %s", dateTime, result.toString()));
		
		return result;
	}
	
	/**
	 * Read the user credentials from a file consisting of one or two lines, the first line containing the username (without mission
	 * prefix) and the second line the password.
	 * The file will only be read, if it is only readable by the current system user (as far as warranted by the operating system).
	 * 
	 * @param filePathString path to the file containing the credentials
	 * @return a Credentials object with username and password set from the file
	 * @throws SecurityException if the file denoted by the file path does not meet the security criteria
	 * @throws FileNotFoundException if the file denoted by the file path does not exist
	 * @throws IOException if the file is not readable
	 */
	public static Credentials readIdentFile(String filePathString) throws SecurityException, FileNotFoundException, IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> readIdentFile({})", filePathString);

		Credentials credentials = new Credentials();
		
		try {
			// Check file permissions
			Path filePath = Path.of(filePathString);
			if (Files.isReadable(filePath)) {
				Set<PosixFilePermission> permissions;
				try {
					permissions = Files.getPosixFilePermissions(filePath);
				} catch (UnsupportedOperationException e) {
					// On file systems not supporting POSIX permissions (e. g. Windows FAT) we shrug and just log a warning
					String message = logger.log(UIMessage.WARN_CREDENTIALS_INSECURE, filePathString);
					permissions = null;
				}
				if (null != permissions && 
						(permissions.contains(PosixFilePermission.GROUP_READ) 
					  || permissions.contains(PosixFilePermission.OTHERS_READ))) {
					String message = logger.log(UIMessage.CREDENTIALS_INSECURE, filePathString);
					System.err.println(message);
					throw new SecurityException(message);
				}
			} else {
				String message = logger.log(UIMessage.CREDENTIALS_NOT_FOUND, filePathString);
				System.err.println(message);
				throw new FileNotFoundException(message);
			}
			
			// Read the credentials from the file
			BufferedReader credentialFile = new BufferedReader(new FileReader(filePathString));
			credentials.username = credentialFile.readLine();
			if (null == credentials.username || credentials.username.isBlank()) {
				String message = logger.log(UIMessage.INVALID_IDENT_FILE, filePathString);
				System.err.println(message);
				credentialFile.close();
				throw new SecurityException(message);
			}
			credentials.password = credentialFile.readLine();
			credentialFile.close();
			if (null == credentials.password || credentials.password.isBlank()) {
				String message = logger.log(UIMessage.INVALID_IDENT_FILE, filePathString);
				System.err.println(message);
				throw new SecurityException(message);
			}
		} catch (IOException e) {
			if (e instanceof FileNotFoundException) throw e;
			
			String message = logger.log(UIMessage.CREDENTIALS_NOT_READABLE, filePathString, e.getMessage());
			System.err.println(message);
			throw new IOException(message, e);
		}
		
		logger.trace("<<< readIdentFile()");
		return credentials;
	}

}
