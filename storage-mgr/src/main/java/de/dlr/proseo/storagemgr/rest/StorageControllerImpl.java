/**
 * StorageControllerImpl.java
 * 
 * (C) 2019 DLR
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3;

import de.dlr.proseo.model.fs.s3.S3Ops;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.Joborder;
import de.dlr.proseo.storagemgr.rest.model.ProductFS;
import de.dlr.proseo.storagemgr.rest.model.Storage;
import de.dlr.proseo.storagemgr.rest.model.StorageType;
import de.dlr.proseo.storagemgr.utils.StorageManagerUtils;
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
	private static final String HTTP_MSG_PREFIX = "4000 proseo-storage-mgr ";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final int MSG_ID_EXCEPTION_THROWN = 9001;
	private static Logger logger = LoggerFactory.getLogger(StorageControllerImpl.class);
	private static final Charset JOF_CHARSET = StandardCharsets.UTF_8;
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
	public ResponseEntity<List<Storage>> getStoragesById(String id) {

		ArrayList<Storage> response = new ArrayList<Storage>();

		try {

			// fetch S3-buckets
			S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
			ArrayList<String> s3bckts = S3Ops.listBuckets(s3);

			// fetch Alluxio-Prefixes
			AmazonS3 s3_v1 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
			List<String> allxio = S3Ops.listKeysInBucket(s3_v1, cfg.getAlluxioUnderFsS3Bucket(), cfg.getAlluxioUnderFsS3BucketPrefix(),true);

			// global storages...
			ArrayList<String> storages = new ArrayList<String>(s3bckts);
			storages.addAll(allxio);
			// check if we have duplicate IDs
			Set<String> set = new HashSet<String>(storages);
			if(set.size() < storages.size()) {
				logger.error("TODO: there are duplicate IDs...");
			}

			if (id != null && s3bckts.contains(id) && !allxio.contains(id)) {
				Storage store = new Storage();
				store.setStorageType(StorageType.S_3);
				store.setId(id);
				store.setDescription("S3-Bucket s3://" + id + " @" + cfg.getS3EndPoint());
				response.add(store);
			} 
			if (id != null && allxio.contains(id) && !s3bckts.contains(id)) {
				Storage store = new Storage();
				store.setStorageType(StorageType.ALLUXIO);
				store.setId(id);
				store.setDescription("Alluxio Prefix alluxio://" + id);
				response.add(store);
			} 
			if (id != null && !allxio.contains(id) && !s3bckts.contains(id)) {
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			} 
			if (id==null) {
				for (String b : s3bckts) {
					Storage store = new Storage();
					store.setStorageType(StorageType.S_3);
					store.setId(b);
					store.setDescription("S3-Bucket s3://" + b + " @" + cfg.getS3EndPoint());
					response.add(store);
				}
				for (String b : allxio) {
					Storage store = new Storage();
					store.setStorageType(StorageType.ALLUXIO);
					store.setId(b);
					store.setDescription("Alluxio Prefix alluxio://" + b);
					response.add(store);
				}
			}
			s3.close();
			s3_v1.shutdown();
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<Storage> createStorage(@Valid Storage storage) {
		Storage response = new Storage();
		
		// fetch S3-buckets
		S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
		ArrayList<String> s3bckts = S3Ops.listBuckets(s3);

		// fetch Alluxio-Prefixes
		AmazonS3 s3_v1 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
		List<String> allxio = S3Ops.listKeysInBucket(s3_v1, cfg.getAlluxioUnderFsS3Bucket(), cfg.getAlluxioUnderFsS3BucketPrefix(),true);

		// global storages...
		ArrayList<String> storages = new ArrayList<String>(s3bckts);
		storages.addAll(allxio);
		// check if we have duplicate IDs
		Set<String> set = new HashSet<String>(storages);
		if(set.size() < storages.size() || storages.contains(storage.getId())) {
			response.setId(storage.getId());
			response.setStorageType(storage.getStorageType());
			response.setDescription("Storage with storageId "+storage.getId()+" already exists...");
			return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
		}

		if (storage.getStorageType() == StorageType.S_3) {
			try {
				if(s3bckts.size() > cfg.getS3MaxNumberOfBuckets()) {
					response.setId(storage.getId());
					response.setStorageType(storage.getStorageType());
					response.setDescription("Max number of S3-Buckets (="+cfg.getS3MaxNumberOfBuckets()+") is reached...");
					return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
				}				
				String  bckt = S3Ops.createBucket(s3, storage.getId());
				response.setId(bckt);
				response.setStorageType(StorageType.S_3);
				response.setDescription("created new S3-Bucket s3://"+bckt+" @"+cfg.getS3EndPoint());
				s3.close();
				s3_v1.shutdown();
				return new ResponseEntity<>(response, HttpStatus.CREATED);
			} catch (Exception e) {
				return new ResponseEntity<>(
						errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		if (storage.getStorageType() == StorageType.ALLUXIO) {
			try {
				if(allxio.size() > cfg.getAlluxioUnderFsMaxPrefixes()) {
					response.setId(storage.getId());
					response.setStorageType(storage.getStorageType());
					response.setDescription("Max number of Alluxio-UnderFS Prefixes (="+cfg.getAlluxioUnderFsMaxPrefixes()+") is reached...");
					return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
				}
				
				String key = storage.getId();
				String delimiter = "/";
				if (!key.endsWith(delimiter)) {
					key += delimiter;
				}
				String underFSPrefix = cfg.getAlluxioUnderFsS3BucketPrefix();
				if (!underFSPrefix.endsWith(delimiter)) {
					underFSPrefix += delimiter;
				}
				//Create a key - aka folder...
				S3Ops.createEmptyKey(s3, cfg.getAlluxioUnderFsS3Bucket(), underFSPrefix+key+"MANIFEST.txt", "prefix "+key+" created from storage-manager...");
				response.setId(storage.getId());
				response.setStorageType(StorageType.ALLUXIO);
				response.setDescription("created new Alluxio-Prefix alluxio//"+storage.getId()+" @"+cfg.getS3EndPoint());
				s3.close();
				s3_v1.shutdown();
				return new ResponseEntity<>(response, HttpStatus.CREATED);
			} catch (Exception e) {
				return new ResponseEntity<>(
						errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		response.setId(storage.getId());
		response.setStorageType(storage.getStorageType());
		response.setDescription("Storage Type "+storage.getStorageType()+" not implemented");
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
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
		String objKey = cfg.getS3JoborderPrefixKey() + "/" + UUID.randomUUID().toString() + ".xml";
		try {
			String base64String = joborder.getJobOrderStringBase64();
			byte[] bytes = java.util.Base64.getDecoder().decode(base64String);
			if (!StorageManagerUtils.checkXml(StorageManagerUtils.inputStreamToString(new ByteArrayInputStream(bytes), JOF_CHARSET))) {
				response.setUploaded(false);
				response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
				response.setStorageId(storageId);
				response.setPathInfo("n/a");
				response.setMessage("XML Doc parsed from attribute jobOrderStringBase64 is not valid...");
				return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
			}
			InputStream fis = new ByteArrayInputStream(bytes);
			s3.putObject(PutObjectRequest.builder().bucket(storageId).key(objKey).build(),
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
		response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
		response.setStorageId(storageId);
		response.setPathInfo("s3://" + storageId + objKey);
		logger.info("Received & Uploaded joborder-file: {}", response.getPathInfo());
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

}
