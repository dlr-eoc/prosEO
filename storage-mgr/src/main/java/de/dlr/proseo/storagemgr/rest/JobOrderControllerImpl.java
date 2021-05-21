/**
 * JobOrderControllerImpl.java
 * 
 * (C) 2019 DLR
 */
package de.dlr.proseo.storagemgr.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.utils.StorageType;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.RestJoborder;
import de.dlr.proseo.storagemgr.utils.ProseoFile;
import de.dlr.proseo.storagemgr.utils.StorageManagerUtils;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services
 * required to manage Joborders
 * 
 * @author Hubert Asamer
 *
 */
@Component
public class JobOrderControllerImpl implements JoborderController {

	private static final Charset JOF_CHARSET = StandardCharsets.UTF_8;
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "4000 proseo-storage-mgr ";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final int MSG_ID_EXCEPTION_THROWN = 9001;
	private static Logger logger = LoggerFactory.getLogger(JoborderController.class);
	@Autowired
	private StorageManagerConfiguration cfg;

	/**
	 * Log an error and return the corresponding HTTP message header
	 * 
	 * @param messageFormat     the message text with parameter placeholders in
	 *                          String.format() style
	 * @param messageId         a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the
	 *                          message format)
	 * @return an HttpHeaders object with a formatted error message
	 */
	private HttpHeaders errorHeaders(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);

		// Create an HTTP "Warning" header
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message);
		return responseHeaders;
	}

	/**
	 * Create a job order file with generic name out of base64 string contained in RestJoborder. Store it into FS defined by fsType.
	 * 
	 * @param joborder Job order information
	 * @return ResponseEntity as RestJoborder
	 */
	@Override
	public ResponseEntity<RestJoborder> createRestJoborder(@Valid RestJoborder joborder) {
		RestJoborder response = new RestJoborder();
		String separator = "/";
		try {			
			// check if we have a Base64 encoded string & if we have valid XML
			if (!joborder.getJobOrderStringBase64()
					.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$")) {
				response.setUploaded(false);
				response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
				response.setPathInfo("n/a");
				response.setMessage("Attribute jobOrderStringBase64 is not Base64-encoded...");
				return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
			}

			DateTime tstamp=DateTime.now(DateTimeZone.UTC);
			String objKey = 
					cfg.getJoborderPrefix()
					+ separator
					+ tstamp.getYear()
					+ separator 
					+ tstamp.getMonthOfYear()
					+ separator 
					+ tstamp.getDayOfMonth()
					+ separator 
					+ tstamp.getHourOfDay()
					+ separator 
					+ UUID.randomUUID().toString() 
					+ ".xml";

			String base64String = joborder.getJobOrderStringBase64();
			byte[] bytes = java.util.Base64.getDecoder().decode(base64String);
			if (!StorageManagerUtils
					.checkXml(StorageManagerUtils.inputStreamToString(new ByteArrayInputStream(bytes), JOF_CHARSET))) {
				response.setUploaded(false);
				response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
				response.setPathInfo("n/a");
				response.setMessage("XML Doc parsed from attribute jobOrderStringBase64 is not valid...");
				return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
			}
			ProseoFile proFile = ProseoFile.fromType(StorageType.valueOf(joborder.getFsType()), objKey, cfg);
			if (proFile != null) {
				if (proFile.writeBytes(bytes)) {
					response.setFsType(proFile.getFsType().toString());
					response.setPathInfo(proFile.getFullPath());
					response.setUploaded(true);
					response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
					logger.info("Received & Uploaded joborder-file: {}", response.getPathInfo());
					return new ResponseEntity<>(response, HttpStatus.CREATED);
				}
			}
		} catch (Exception e) {
			return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}		
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * Retrieve contents of file as base64 string.
	 * 
	 * @param pathInfo the path to the file to retrieve
	 * @return Base64 coded String
	 */
	@Override
	public ResponseEntity<String> getObjectByPathInfo(String pathInfo) {
		String response = "";
		if (pathInfo != null) {
			ProseoFile proFile = ProseoFile.fromPathInfo(pathInfo, cfg);
			// Find storage type
			if (proFile == null || proFile.getFsType() == StorageType.ALLUXIO) {
				logger.warn("Invalid storage type for path: {}", pathInfo);
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
			InputStream jofStream = proFile.getDataAsInputStream();
			if (jofStream != null) {
				byte[] bytes = null;
				try {
					bytes = java.util.Base64.getEncoder().encode(jofStream.readAllBytes());
				} catch (IOException e) {
					logger.error("Invalid job order stream");
					return new ResponseEntity<>(
							errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
							HttpStatus.INTERNAL_SERVER_ERROR);
				}
				response = new String(bytes);
				try {
					jofStream.close();
				} catch (IOException e) {
					logger.warn("Failed to close input stream of " + pathInfo + " | " + e.getMessage());
				}
				return new ResponseEntity<>(response, HttpStatus.OK);
			}				
		} 
		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}
}
