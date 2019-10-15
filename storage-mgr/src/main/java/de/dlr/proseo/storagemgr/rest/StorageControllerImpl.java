/**
 * StorageControllerImpl.java
 * 
 * (C) 2019 DLR
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import de.dlr.proseo.model.fs.s3.S3Ops;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.Joborder;
import de.dlr.proseo.storagemgr.rest.model.ProductFS;
import de.dlr.proseo.storagemgr.rest.model.Storage;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services
 * required to manage any object storage, e. g. a storage based on the AWS S3
 * API
 * 
 * @author Hubert Asamer
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class StorageControllerImpl implements StorageController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-order-mgr ";
	private static Logger logger = LoggerFactory.getLogger(StorageControllerImpl.class);
	private static final Charset JOF_CHARSET = StandardCharsets.UTF_8;
	@Autowired
	StorageManagerConfiguration cfg = new StorageManagerConfiguration();

	private Boolean checkXml(String xml) {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			dBuilder.parse(xml);
			return true;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			logger.error(e.getMessage());
			return false;
		}
	}

	private String convert(InputStream inputStream, Charset charset) throws IOException {

		StringBuilder stringBuilder = new StringBuilder();
		String line = null;

		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
		}

		logger.info(stringBuilder.toString());
		return stringBuilder.toString();
	}

	@Override
	public ResponseEntity<List<Storage>> getStoragesById(String id) {
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "GET for id %s not implemented (%d)", id, 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Storage> createStorage(@Valid Storage storage) {
		// TODO Auto-generated method stub
		String message = String.format(MSG_PREFIX + "POST not implemented (%d)", 2001);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<ProductFS> createProductFS(String storageId, @Valid ProductFS productFS) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<ProductFS> deleteProduct(String productId, String storageId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<Joborder> createJoborder(String storageId, @Valid Joborder joborder) {
		Joborder response = new Joborder();

		// check if we have a Base64 encoded string & if we have valid XML

		if (!joborder.getJobOrderStringBase64()
				.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$")) {
			response.setUploaded(false);
			response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
			response.setStorageId(storageId);
			response.setPathInfo("n/a");
			response.setMessage("Attribute jobOrderStringBase64 is not Base64-encoded...");
			return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
		}

		S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
		String objKey = "/joborders/" + UUID.randomUUID().toString() + ".xml";
		try {
			String base64String = joborder.getJobOrderStringBase64();
			byte[] bytes = java.util.Base64.getDecoder().decode(base64String);
			InputStream fis = new ByteArrayInputStream(bytes);

			
			if (!checkXml(convert(fis, JOF_CHARSET))) {
				response.setUploaded(false);
				response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
				response.setStorageId(storageId);
				response.setPathInfo("n/a");
				response.setMessage("XML Doc parsed from attribute jobOrderStringBase64 is not valid...");
				return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
			}

			s3.putObject(PutObjectRequest.builder().bucket(storageId).key(objKey).build(),
					RequestBody.fromInputStream(fis, bytes.length));
		} catch (Exception e) {
			logger.error(e.getMessage());
			response.setUploaded(false);
			response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
			response.setStorageId(storageId);
			response.setPathInfo("n/a");
			response.setMessage(e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		// now prepare response
		response.setUploaded(true);
		response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
		response.setStorageId(storageId);
		response.setPathInfo("s3://" + storageId + objKey);
		logger.info("Received & Uploaded joborder-file: {}", response.getPathInfo());
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

}
