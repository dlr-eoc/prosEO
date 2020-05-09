/**
 * StorageControllerImpl.java
 * 
 * (C) 2019 DLR
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3;

import de.dlr.proseo.model.fs.s3.AmazonS3URI;
import de.dlr.proseo.model.fs.s3.S3Ops;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.FsType;
import de.dlr.proseo.storagemgr.rest.model.RestProductFS;
import de.dlr.proseo.storagemgr.rest.model.RestStorage;
import de.dlr.proseo.storagemgr.rest.model.SourceStorageType;
import de.dlr.proseo.storagemgr.rest.model.StorageType;
import de.dlr.proseo.storagemgr.utils.ProseoFile;
import de.dlr.proseo.storagemgr.utils.StorageManagerUtils;
import software.amazon.awssdk.services.s3.S3Client;


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
	private static final String HTTP_MSG_PREFIX = "199 proseo-storage-mgr ";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final int MSG_ID_EXCEPTION_THROWN = 4001;
	private static Logger logger = LoggerFactory.getLogger(StorageControllerImpl.class);

	@Autowired
	private StorageManagerConfiguration cfg;

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
	public ResponseEntity<List<RestStorage>> getRestStoragesById(String id) {

		ArrayList<RestStorage> response = new ArrayList<RestStorage>();

		try {

			// create internal buckets if not exists..
			StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),cfg.getAlluxioUnderFsS3Bucket(),cfg.getS3Region());

			// global storages...
			ArrayList<String[]> storages = StorageManagerUtils
					.getAllStorages(cfg.getS3AccessKey(), 
							cfg.getS3SecretAccessKey(), 
							cfg.getS3EndPoint(), 
							cfg.getStorageIdPrefix(),
							cfg.getAlluxioUnderFsS3Bucket(), 
							cfg.getAlluxioUnderFsS3BucketPrefix(),
							cfg.getPosixMountPoint()
							);

			ArrayList<String> s3Storages = new ArrayList<String>();
			ArrayList<String> alluxioStorages = new ArrayList<String>();

			for (String[] entry : storages) {
				if (entry[1].equals(String.valueOf(StorageType.S_3))) {
					s3Storages.add(entry[0]);
				}
				if (entry[1].equals(String.valueOf(StorageType.ALLUXIO))) {
					alluxioStorages.add(entry[0]);
				}
			}

			if (id != null && s3Storages.contains(id) && !alluxioStorages.contains(id)) {
				logger.info("queryParam->yes, s3Id->yes, alluxio->no");
				RestStorage store = new RestStorage();
				store.setStorageType(StorageType.S_3);
				store.setId(id);
				store.setDescription("S3-Bucket s3://" + id + " @" + cfg.getS3EndPoint());
				response.add(store);
			} 
			if (id != null && alluxioStorages.contains(id) && !s3Storages.contains(id)) {
				logger.info("queryParam->yes, s3Id->no, alluxio->yes");
				RestStorage store = new RestStorage();
				store.setStorageType(StorageType.ALLUXIO);
				store.setId(id);
				store.setDescription("Alluxio Prefix alluxio://" + id);
				response.add(store);
			} 
			if (id != null && !s3Storages.contains(id) && !alluxioStorages.contains(id)) {
				logger.info("queryParam->yes, s3Id->no, alluxio->no");
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			} 
			if (id==null) {
				for (String[] b : storages) {
					RestStorage store = new RestStorage();
					store.setStorageType(StorageType.fromValue(b[1]));
					store.setId(b[0]);
					store.setDescription(b[0]+b[1] + " @" + cfg.getS3EndPoint());
					response.add(store);
				}
			}

			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<RestStorage> createRestStorage(@Valid RestStorage storage) {
		RestStorage response = new RestStorage();

		// create internal buckets if not exists..
		try {
			StorageManagerUtils.createStorageManagerInternalS3Buckets(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint(),cfg.getAlluxioUnderFsS3Bucket(),cfg.getS3Region());
		} catch (Exception e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// check if storageId has no UpperCase letters
		if(!storage.getId().equals(storage.getId().toLowerCase())) {
			response.setId(storage.getId());
			response.setStorageType(storage.getStorageType());
			response.setDescription("StorageId must not have UpperCase letters...");
			return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
		}

		ArrayList<String> s3Storages = new ArrayList<String>();
		ArrayList<String> alluxioStorages = new ArrayList<String>();
		ArrayList<String> posixStorages = new ArrayList<String>();
		try {
			StorageManagerUtils.getAllStoragesInto(cfg, s3Storages, alluxioStorages, posixStorages);
		} catch (Exception e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (storage.getStorageType() == StorageType.S_3) {
			try {

				// check if we already have that ID
				if(s3Storages.contains(storage.getId()) || alluxioStorages.contains(storage.getId())) {
					response.setId(storage.getId());
					response.setStorageType(storage.getStorageType());
					response.setDescription("Storage with storageId "+storage.getId()+" already exists...");
					return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
				}
				if(s3Storages.size() > cfg.getS3MaxNumberOfBuckets()) {
					response.setId(storage.getId());
					response.setStorageType(storage.getStorageType());
					response.setDescription("Max number of S3-Buckets (="+cfg.getS3MaxNumberOfBuckets()+") is reached...");
					return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
				}		
				S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
				String  bckt = S3Ops.createBucket(s3, storage.getId(),cfg.getS3Region());
				if (null == bckt) return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
				response.setId(bckt);
				response.setStorageType(StorageType.S_3);
				response.setDescription("created new S3-Bucket s3://"+bckt+" @"+cfg.getS3EndPoint());
				s3.close();
				return new ResponseEntity<>(response, HttpStatus.CREATED);
			} catch (Exception e) {
				return new ResponseEntity<>(
						errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		if (storage.getStorageType() == StorageType.ALLUXIO) {
			try {
				if(alluxioStorages.contains(storage.getId()) || s3Storages.contains(storage.getId())) {
					response.setId(storage.getId());
					response.setStorageType(storage.getStorageType());
					response.setDescription("Storage with storageId "+storage.getId()+" already exists...");
					return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
				}
				if(alluxioStorages.size() > cfg.getAlluxioUnderFsMaxPrefixes()) {
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
				S3Client s3 = S3Ops.v2S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());
				S3Ops.createEmptyKey(s3, cfg.getAlluxioUnderFsS3Bucket(), underFSPrefix+key+"MANIFEST.txt", "prefix "+key+" created from storage-manager...");
				response.setId(storage.getId());
				response.setStorageType(StorageType.ALLUXIO);
				response.setDescription("created new Alluxio-Prefix alluxio//"+storage.getId()+" @"+cfg.getS3EndPoint());
				s3.close();
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
	public ResponseEntity<RestProductFS> createRestProductFS(@Valid RestProductFS restProductFS) {
		// get node name info...
		String hostName ="";
		try {
			InetAddress iAddress = InetAddress.getLocalHost();
			hostName = iAddress.getHostName();
		} catch (UnknownHostException e1) {

		}

		RestProductFS response = new RestProductFS();
		
		logger.info(restProductFS.toString());
		String pref = restProductFS.getProductId();
		
		ArrayList<String> transferSum = new ArrayList<String>();

		ProseoFile targetFile = ProseoFile.fromType(FsType.fromValue(restProductFS.getTargetStorageType().toString()), pref, cfg);

		try {
			for (String fileOrDir : restProductFS.getSourceFilePaths()) {
				ProseoFile sourceFile = ProseoFile.fromTypeFullPath(FsType.fromValue(restProductFS.getSourceStorageType().toString()), fileOrDir, cfg);
				ArrayList<String> transfered = sourceFile.copyTo(targetFile);
				if (transfered != null) {
					transferSum.addAll(transfered);
				}
			}
			setRestProductFS(response, restProductFS, cfg.getS3DefaultBucket(), true, targetFile.getFullPath() + "/",
							 transferSum, false, "registration executed on node "+hostName);
			return new ResponseEntity<>(response, HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<List<RestProductFS>> getRestProductFs(StorageType storageType, String storageId, Long id) {
		List<RestProductFS> response = new ArrayList<RestProductFS>();
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<RestProductFS> deleteProductByProductId(Long productId) {
		RestProductFS response = new RestProductFS();
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
	}

	
	private RestProductFS setRestProductFS(RestProductFS response,
			RestProductFS restProductFS,
			String storageId,
			Boolean registered,
			String registeredFilePath,
			List<String> registeredFiles,
			Boolean deleted,
			String msg
			) {
		if (response != null && restProductFS != null) {
			response.setProductId(restProductFS.getProductId());
			response.setTargetStorageId(storageId);
			response.setRegistered(registered);
			response.setRegisteredFilePath(registeredFilePath);
			response.setSourceFilePaths(restProductFS.getSourceFilePaths());
			response.setSourceStorageType(restProductFS.getSourceStorageType());
			response.setTargetStorageType(restProductFS.getTargetStorageType());
			response.setRegisteredFilesCount(Long.valueOf(registeredFiles.size()));
			response.setRegisteredFilesList(registeredFiles);
			response.setDeleted(deleted);
			response.setMessage(msg);
		}
		return response;
	}

	@Override
	public ResponseEntity<?> getObject(Long productId, Boolean zip, Long fromByte, Long toByte) {
		List<RestProductFS> response = new ArrayList<RestProductFS>();
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);		
	}

}
