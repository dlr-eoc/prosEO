/**
 * StorageControllerImpl.java
 * 
 * (C) 2019 DLR
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3;

import de.dlr.proseo.model.fs.s3.AmazonS3URI;
import de.dlr.proseo.model.fs.s3.S3Ops;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.ProductFS;
import de.dlr.proseo.storagemgr.rest.model.SourceStorageType;
import de.dlr.proseo.storagemgr.rest.model.Storage;
import de.dlr.proseo.storagemgr.rest.model.StorageType;
import de.dlr.proseo.storagemgr.rest.model.TargetStorageType;
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
	private static final String HTTP_MSG_PREFIX = "4000 proseo-storage-mgr ";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final int MSG_ID_EXCEPTION_THROWN = 9001;
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
	public ResponseEntity<List<Storage>> getStoragesById(String id) {

		ArrayList<Storage> response = new ArrayList<Storage>();

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
							cfg.getAlluxioUnderFsS3BucketPrefix()
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
				Storage store = new Storage();
				store.setStorageType(StorageType.S_3);
				store.setId(id);
				store.setDescription("S3-Bucket s3://" + id + " @" + cfg.getS3EndPoint());
				response.add(store);
			} 
			if (id != null && alluxioStorages.contains(id) && !s3Storages.contains(id)) {
				logger.info("queryParam->yes, s3Id->no, alluxio->yes");
				Storage store = new Storage();
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
					Storage store = new Storage();
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
	public ResponseEntity<Storage> createStorage(@Valid Storage storage) {
		Storage response = new Storage();

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

		ArrayList<String[]> storages;
		try {
			storages = StorageManagerUtils
					.getAllStorages(cfg.getS3AccessKey(), 
							cfg.getS3SecretAccessKey(), 
							cfg.getS3EndPoint(), 
							cfg.getStorageIdPrefix(),
							cfg.getAlluxioUnderFsS3Bucket(), 
							cfg.getAlluxioUnderFsS3BucketPrefix()
							);
		} catch (Exception e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
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
	public ResponseEntity<ProductFS> createProductFS(String storageId, @Valid ProductFS productFS) {

		ProductFS response = new ProductFS();
		long regTimeStamp = System.currentTimeMillis()/1000;
		String separator = "/";

		// fetch all stoargeIDs
		ArrayList<String[]> storages;
		try {
			storages = StorageManagerUtils
					.getAllStorages(cfg.getS3AccessKey(), 
							cfg.getS3SecretAccessKey(), 
							cfg.getS3EndPoint(), 
							cfg.getStorageIdPrefix(),
							cfg.getAlluxioUnderFsS3Bucket(), 
							cfg.getAlluxioUnderFsS3BucketPrefix()
							);
		} catch (Exception e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		// create FS_TYPE specific lists
		ArrayList<String> s3Storages = new ArrayList<String>();
		ArrayList<String> alluxioStorages = new ArrayList<String>();
		// POSIX->n/a

		for (String[] entry : storages) {
			if (entry[1].equals(String.valueOf(StorageType.S_3))) {
				s3Storages.add(entry[0]);
			}
			if (entry[1].equals(String.valueOf(StorageType.ALLUXIO))) {
				alluxioStorages.add(entry[0]);
			}
		}

		//check FS_TYPE of Source
		SourceStorageType sourceStorageFsType = SourceStorageType.POSIX; // default...
		if (productFS.getSourceStorageType().equals(SourceStorageType.S_3)) sourceStorageFsType=SourceStorageType.S_3;
		if (productFS.getSourceStorageType().equals(SourceStorageType.POSIX)) sourceStorageFsType=SourceStorageType.POSIX;		

		//check FS_TYPE of referenced target storageID
		TargetStorageType targetStorageFsType =TargetStorageType.S_3; // default...
		if (s3Storages.contains(storageId)) targetStorageFsType=TargetStorageType.S_3;
		if (alluxioStorages.contains(storageId)) targetStorageFsType=TargetStorageType.ALLUXIO;


		// distinguish between requested target Storage FS_Type
		switch (targetStorageFsType) {

		case S_3:
			try {
				// check if storageID is present
				if(!s3Storages.contains(storageId)) {
					response.setTargetStorageType(productFS.getTargetStorageType());
					response.setMessage("StorageId does not exist...");
					return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
				}

				AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());

				/*
				 * prepare final file/directory prefix within storage S3-storage pattern:
				 * 
				 * s3://<storageId>/<productId>/<regTimeStamp>/...<file>
				 */				
				String pref = 
						productFS.getProductId()
						+separator+regTimeStamp;

				// distinguish between requested source Storage FS_Type
				switch(sourceStorageFsType) {
				case S_3:
					// initiate transfer from S3 filePath to S3
					AmazonS3URI s3uri = new AmazonS3URI(productFS.getSourceFilePath());
					String sourceBucket = s3uri.getBucket();
					String sourceKey = s3uri.getKey();
					if (null==sourceKey) sourceKey="";
					ArrayList<String> transferC = S3Ops.v1Copy(
							//the client
							s3, 
							// the source S3-Bucket
							sourceBucket, 
							// the source key
							sourceKey, 
							// the target s3-bucket (=storageId)
							storageId,
							// the final prefix including productId pattern of the file or directory
							pref
							);
					if (null==transferC) return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
					response.setProductId(productFS.getProductId());
					response.setTargetStorageId(storageId);
					response.setRegistered(true);
					response.setRegisteredFilePath("s3://"+storageId+separator+pref+separator+sourceKey);
					response.setSourceFilePath(productFS.getSourceFilePath());
					response.setSourceStorageType(sourceStorageFsType);
					response.setTargetStorageType(targetStorageFsType);
					response.setRegisteredFilesCount(Long.valueOf(transferC.size()));
					response.setRegisteredFilesList(transferC.toString());
					response.setDeleted(false);
					s3.shutdown();
					return new ResponseEntity<>(response, HttpStatus.CREATED);
				case POSIX:
					// initiate transfer from POSIX filePath to S3
					ArrayList<String> transferP = S3Ops.v1Upload(
							//the client
							s3, 
							// the local POSIX source file or directory
							productFS.getSourceFilePath(), 
							// the storageId -> =BucketName
							storageId, 
							// the final prefix of the file or directory
							pref, 
							false
							);
					if (null == transferP) return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
					response.setProductId(productFS.getProductId());
					response.setTargetStorageId(storageId);
					response.setRegistered(true);
					response.setRegisteredFilePath(pref+separator+productFS.getSourceFilePath());
					response.setSourceFilePath(productFS.getSourceFilePath());
					response.setSourceStorageType(sourceStorageFsType);
					response.setTargetStorageType(productFS.getTargetStorageType());
					response.setRegisteredFilesCount(Long.valueOf(transferP.size()));
					response.setRegisteredFilesList(transferP.toString());
					response.setDeleted(false);
					s3.shutdown();
					return new ResponseEntity<>(response, HttpStatus.CREATED);
				}


			} catch (Exception e) {
				return new ResponseEntity<>(
						errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
						HttpStatus.INTERNAL_SERVER_ERROR);
			}

		case ALLUXIO:
			try {
				// check if storageID is present
				if(!alluxioStorages.contains(storageId)) {
					response.setTargetStorageType(productFS.getTargetStorageType());
					response.setMessage("StorageId does not exist...");
					return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
				}

				AmazonS3 s3 = S3Ops.v1S3Client(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3EndPoint());

				/*
				 * prepare final file/directory prefix within storage 
				 * 
				 * ALLUXIO-storage pattern (UnderFS is S3):
				 * s3://<underFSBucket>/<underFSPrefix>/<storageId>/<productId>/<regTimeStamp>/...<file>
				 */		
				// prefix used for transfer to ALLUXIO-UnderFS
				String s3Pref = 
						cfg.getAlluxioUnderFsS3BucketPrefix()
						+separator+storageId
						+separator+productFS.getProductId()
						+separator+regTimeStamp;
				// prefix used for setRegisteredFilePath
				String alluxioPref = 
						storageId
						+separator+productFS.getProductId()
						+separator+regTimeStamp;

				switch(sourceStorageFsType) {
				case S_3:
					// initiate transfer from S3 filePath to ALLUXIO
					AmazonS3URI s3uri = new AmazonS3URI(productFS.getSourceFilePath());
					String sourceBucket = s3uri.getBucket();
					String sourceKey = s3uri.getKey();
					if (null==sourceKey) sourceKey="";
					ArrayList<String> transferC = S3Ops.v1Copy(
							//the client
							s3, 
							// the source s3-bucket
							sourceBucket,
							// the local S3 source file or directory
							sourceKey, 
							// target-bucket --> the ALLUXIO UnderFS Bucket
							cfg.getAlluxioUnderFsS3Bucket(),
							// the final prefix including productId pattern of the file or directory
							s3Pref
							);
					if (null==transferC) return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
					response.setProductId(productFS.getProductId());
					response.setTargetStorageId(storageId);
					response.setRegistered(true);
					response.setRegisteredFilePath(separator+alluxioPref+separator+sourceKey);
					response.setSourceFilePath(productFS.getSourceFilePath());
					response.setSourceStorageType(sourceStorageFsType);
					response.setTargetStorageType(targetStorageFsType);
					response.setRegisteredFilesCount(Long.valueOf(transferC.size()));
					response.setRegisteredFilesList(transferC.toString());
					response.setDeleted(false);
					s3.shutdown();
					return new ResponseEntity<>(response, HttpStatus.CREATED);
				case POSIX:
					// initiate transfer from POSIX filePath to ALLUXIO
					ArrayList<String> transferP = S3Ops.v1Upload(
							//the client
							s3, 
							// the local POSIX source file or directory
							productFS.getSourceFilePath(), 
							// the ALLUXIO UnderFS Bucket
							cfg.getAlluxioUnderFsS3Bucket(),
							// the final prefix including productId pattern of the file or directory
							s3Pref,
							false
							);
					if (null == transferP) return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
					response.setProductId(productFS.getProductId());
					response.setTargetStorageId(storageId);
					response.setRegistered(true);
					response.setRegisteredFilePath(separator+alluxioPref+separator+productFS.getSourceFilePath());
					response.setSourceFilePath(productFS.getSourceFilePath());
					response.setSourceStorageType(sourceStorageFsType);
					response.setTargetStorageType(productFS.getTargetStorageType());
					response.setRegisteredFilesCount(Long.valueOf(transferP.size()));
					response.setRegisteredFilesList(transferP.toString());
					response.setDeleted(false);
					s3.shutdown();
					return new ResponseEntity<>(response, HttpStatus.CREATED);
				}
			} catch (Exception e) {
				return new ResponseEntity<>(
						errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
						HttpStatus.INTERNAL_SERVER_ERROR);
			}

		case POSIX:
			return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
		}

		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<ProductFS> getProductFS(String storageId, String id) {
		ProductFS response = new ProductFS();
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<ProductFS> deleteProduct(String productId, String storageId) {
		ProductFS response = new ProductFS();
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
	}

}
