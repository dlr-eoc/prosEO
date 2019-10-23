/**
 * JobOrderControllerImpl.java
 * 
 * (C) 2019 DLR
 */
package de.dlr.proseo.storagemgr.rest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.fs.s3.S3Ops;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.FsType;
import de.dlr.proseo.storagemgr.rest.model.Joborder;
import de.dlr.proseo.storagemgr.utils.StorageManagerUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services
 * required to manage Joborders
 * 
 * @author Hubert Asamer
 *
 */
@Component
public class JobOrderControllerImpl implements JoborderController{

	private static final Charset JOF_CHARSET = StandardCharsets.UTF_8;
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "4000 proseo-storage-mgr ";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final int MSG_ID_EXCEPTION_THROWN = 9001;
	private static Logger logger = LoggerFactory.getLogger(StorageControllerImpl.class);
	@Autowired
	StorageManagerConfiguration cfg = new StorageManagerConfiguration();

	/**
	 * Log an error and return the corresponding HTTP message header
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
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

	@Override
	public ResponseEntity<Joborder> createJoborder(@Valid Joborder joborder) {
		Joborder response = new Joborder();

		//create internal buckets, if not existing
		StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(), cfg.getJoborderBucket(),cfg.getS3Region());
		
		String separator = "/";
		// check if we have a Base64 encoded string & if we have valid XML
		if (!joborder.getJobOrderStringBase64()
				.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$")) {
			response.setUploaded(false);
			response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
			response.setPathInfo("n/a");
			response.setMessage("Attribute jobOrderStringBase64 is not Base64-encoded...");
			return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
		}

		S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
		String objKey = cfg.getJoborderPrefix() + separator + UUID.randomUUID().toString() + ".xml";
		try {
			String base64String = joborder.getJobOrderStringBase64();
			byte[] bytes = java.util.Base64.getDecoder().decode(base64String);
			if (!StorageManagerUtils.checkXml(StorageManagerUtils.inputStreamToString(new ByteArrayInputStream(bytes), JOF_CHARSET))) {
				response.setUploaded(false);
				response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
				response.setPathInfo("n/a");
				response.setMessage("XML Doc parsed from attribute jobOrderStringBase64 is not valid...");
				return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
			}
			InputStream fis = new ByteArrayInputStream(bytes);
			s3.putObject(PutObjectRequest.builder().bucket(cfg.getJoborderBucket()).key(objKey).build(),
					RequestBody.fromInputStream(fis, bytes.length));
			fis.close();
			s3.close();
		} catch (Exception e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		// now prepare response
		response.setUploaded(true);
		response.setFsType(FsType.S_3);
		response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
		response.setPathInfo("s3://" + cfg.getJoborderBucket() + separator+ objKey);
		logger.info("Received & Uploaded joborder-file: {}", response.getPathInfo());
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}


}
