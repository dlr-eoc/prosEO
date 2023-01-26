/**
 * StorageControllerImpl.java
 * 
 * (C) 2019 DLR
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.Headers;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import de.dlr.proseo.storagemgr.utils.StorageType;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.RestProductFS;
import de.dlr.proseo.storagemgr.utils.ProseoFile;
import de.dlr.proseo.storagemgr.utils.StorageLogger;

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
public class ProductControllerImpl implements ProductController {

	/* Message IDs */
	private static final int MSG_ID_EXCEPTION_THROWN = 4001;
	private static final int MSG_ID_FILE_NOT_FOUND = 4002;
	private static final int MSG_ID_INVALID_PATH = 4003;
	private static final int MSG_ID_TOKEN_MISSING = 4004;
	private static final int MSG_ID_TOKEN_INVALID = 4005;
	private static final int MSG_ID_TOKEN_EXPIRED = 4006;
	private static final int MSG_ID_TOKEN_MISMATCH = 4007;
	private static final int MSG_ID_FILES_REGISTERED = 4008;
	private static final int MSG_ID_FILES_LISTED = 4009;
	private static final int MSG_ID_FILE_RETRIEVED = 4010;
	private static final int MSG_ID_FILE_DELETED = 4011;

	/* Message strings */
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final String MSG_FILE_NOT_FOUND = "(E%d) File %s not found";
	private static final String MSG_INVALID_PATH = "(E%d) Invalid path %s";
	private static final String MSG_TOKEN_MISSING = "(E%d) Authentication token missing";
	private static final String MSG_TOKEN_INVALID = "(E%d) Authentication token %s invalid (cause: %s)";
	private static final String MSG_TOKEN_EXPIRED = "(E%d) Authentication token expired at %s";
	private static final String MSG_TOKEN_MISMATCH = "(E%d) Authentication token not valid for file %s";
	private static final String MSG_FILES_REGISTERED = "(I%d) Files registered: %s";
	private static final String MSG_FILES_LISTED = "(I%d) Files listed: %s";
	private static final String MSG_FILE_RETRIEVED = "(I%d) File %s retrieved from byte %d to byte %d (%d bytes transferred)";
	private static final String MSG_FILE_DELETED = "(I%d) File %s deleted";

	/* Submessages for token evaluation */
	private static final String MSG_TOKEN_PAYLOAD_INVALID = "The payload of the JWT doesn't represent a valid JSON object and a JWT claims set";
	private static final String MSG_TOKEN_NOT_VERIFIABLE = "The JWS object couldn't be verified";
	private static final String MSG_TOKEN_STATE_INVALID = "The JWS object is not in a signed or verified state, actual state: ";
	private static final String MSG_TOKEN_VERIFICATION_FAILED = "Verification of the JWT failed";
	private static final String MSG_SECRET_TOO_SHORT = "Secret length is shorter than the minimum 256-bit requirement";
	private static final String MSG_TOKEN_NOT_PARSEABLE = "Token not parseable";

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-storage-mgr ";

	/** Logger for this class */
	private static Logger loggerLegacy = LoggerFactory.getLogger(ProductControllerImpl.class);
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PLANNER);

	/** Storage Manager configuration */
	@Autowired
	private StorageManagerConfiguration cfg;

	@Autowired
	private StorageProvider storageProvider;



	/**
	 * Copy a file from "ingest" file system to storage manager controlled prosEO
	 * cache. Source and target are defined in the restProductFS structure
	 * 
	 * @param restProductFS the ingest file information
	 * @return a response entity containing HTTP status CREATED and the ingest file
	 *         information updated with the file paths after ingestion on success,
	 *         or HTTP status INTERNAL_SERVER_ERROR and an error message
	 */
	@Override
	public ResponseEntity<RestProductFS> createRestProductFS(@Valid RestProductFS restProductFS) {

		if (logger.isTraceEnabled())
			logger.trace(">>> createRestProductFs({})",
					(null == restProductFS ? "MISSING" : restProductFS.getProductId()));

		if (storageProvider.isVersion2()) { // begin version 2 list upload source -> storage

			try {
				String hostName = getLocalHostName();
				String prefix = new PathConverter(restProductFS.getProductId()).addSlashAtEnd().getPath();
				List<String> allUploaded = new ArrayList<String>();

				StorageFile targetFolder = storageProvider.getStorageFile(prefix);

				for (String fileOrDir : restProductFS.getSourceFilePaths()) {

					StorageFile sourceFileOrDir = storageProvider.getAbsoluteFile(fileOrDir);
					List<String> uploaded = storageProvider.getStorage().upload(sourceFileOrDir, targetFolder);

					if (uploaded != null)
						allUploaded.addAll(uploaded);
				}

				allUploaded = storageProvider.getStorage().getAbsolutePath(allUploaded);

				RestProductFS response = setRestProductFS(restProductFS, targetFolder.getBasePath(), true,
						targetFolder.getFullPath() + "/", allUploaded, false,
						"registration executed on node " + hostName);

				// StorageLogger.logInfo(loggerLegacy, MSG_FILES_REGISTERED, MSG_ID_FILES_REGISTERED, allUploaded.toString());
				
				logger.log(StorageMgrMessage.PRODUCTS_UPLOADED_TO_STORAGE, Integer.toString(allUploaded.size()), allUploaded.toString());
				
				return new ResponseEntity<>(response, HttpStatus.CREATED);

			} catch (Exception e) {
				
				String msg = logger.log(StorageMgrMessage.INTERNAL_ERROR, e.getMessage());
								
				return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} // end version 2

		// get node name info...
		String hostName = null;
		try {
			InetAddress iAddress = InetAddress.getLocalHost();
			hostName = iAddress.getHostName();
		} catch (UnknownHostException e1) {
			hostName = "(UNKNOWN)";
		}

		RestProductFS response = new RestProductFS();

		loggerLegacy.info(restProductFS.toString());
		String pref = restProductFS.getProductId();
		if (!pref.endsWith("/")) {
			pref = pref + "/";
		}

		ArrayList<String> transferSum = new ArrayList<String>();

		ProseoFile targetFile = ProseoFile.fromType(StorageType.valueOf(restProductFS.getTargetStorageType()), pref,
				cfg);

		try {
			for (String fileOrDir : restProductFS.getSourceFilePaths()) {
				ProseoFile sourceFile = ProseoFile
						.fromTypeFullPath(StorageType.valueOf(restProductFS.getSourceStorageType()), fileOrDir, cfg);
				ArrayList<String> transfered = sourceFile.copyTo(targetFile, true);
				if (loggerLegacy.isDebugEnabled())
					loggerLegacy.debug("Files transferred: {}", transfered);
				if (transfered != null) {
					transferSum.addAll(transfered);
				}
			}
			setRestProductFS(response, restProductFS, targetFile.getBasePath(), true, targetFile.getFullPath() + "/",
					transferSum, false, "registration executed on node " + hostName);

			StorageLogger.logInfo(loggerLegacy, MSG_FILES_REGISTERED, MSG_ID_FILES_REGISTERED, transferSum.toString());

			return new ResponseEntity<>(response, HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * List the file/object contents of a repository.
	 * 
	 * @param storageType S2, POSIX or null
	 * @param prefix      Path information
	 * @return a response entity containing HTTP status OK or PARTIAL_CONTENT and
	 *         list of file (object) paths on success, or HTTP status
	 *         INTERNAL_SERVER_ERROR and an error message
	 */
	@Override
	public ResponseEntity<List<String>> getProductFiles(String storageType, String prefix) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getProductFiles({}, {})", storageType, prefix);

		if (storageProvider.isVersion2()) { // begin version 2 - get product files

			if (prefix == null)
				prefix = "";

			try {
				List<String> response;

				if (storageType == null) { // add both

					response = storageProvider.getStorage(de.dlr.proseo.storagemgr.version2.model.StorageType.S3)
							.getAbsoluteFiles(prefix);
					response = storageProvider.getStorage(de.dlr.proseo.storagemgr.version2.model.StorageType.S3)
							.addFSPrefix(response);

					response.addAll(
							storageProvider.getStorage(de.dlr.proseo.storagemgr.version2.model.StorageType.POSIX)
									.getAbsoluteFiles(prefix));
					response = storageProvider.getStorage(de.dlr.proseo.storagemgr.version2.model.StorageType.POSIX)
							.addFSPrefix(response);

				} else {

					response = storageProvider
							.getStorage(de.dlr.proseo.storagemgr.version2.model.StorageType.valueOf(storageType))
							.getAbsoluteFiles(prefix);
					response = storageProvider
							.getStorage(de.dlr.proseo.storagemgr.version2.model.StorageType.valueOf(storageType))
							.addFSPrefix(response);
				}
								
				logger.log(StorageMgrMessage.PRODUCT_FILES_LISTED, response.toString());

				return new ResponseEntity<>(response, HttpStatus.OK);

			} catch (Exception e) {
				
				String msg = logger.log(StorageMgrMessage.INTERNAL_ERROR, e.getMessage());

				return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} // end version 2

		List<StorageType> stl = new ArrayList<StorageType>();
		List<String> response = new ArrayList<String>();
		try {
			if (storageType == null) {
				stl.add(StorageType.S3);
				stl.add(StorageType.POSIX);
			} else {
				stl.add(StorageType.valueOf(storageType));
			}
			for (StorageType st : stl) {
				listProductFiles(st, prefix, response);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		StorageLogger.logInfo(loggerLegacy, MSG_FILES_LISTED, MSG_ID_FILES_LISTED, response.toString());

		return new ResponseEntity<>(response, HttpStatus.OK);
	}


	/**
	 * Retrieve the byte stream for download of a file object in repository.
	 * 
	 * @param pathInfo the file path as S3/ALLUXIO/POSIX string for download
	 * @param token    a JSON Web Token authenticating the download (obtained from
	 *                 Ingestor)
	 * @param fromByte The first byte of the data stream to download (default is
	 *                 file start, i.e. byte 0)
	 * @param toByte   The last byte of the data stream to download (default is file
	 *                 end, i.e. file size - 1)
	 * @return a response entity containing HTTP status OK or PARTIAL_CONTENT and
	 *         the byte stream on success, or HTTP status NOT_FOUND and an error
	 *         message, or HTTP status INTERNAL_SERVER_ERROR and an error message
	 */
	@Override
	public ResponseEntity<?> getObject(String pathInfo, String token, Long fromByte, Long toByte) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getObject({}, {}, {}, {})", pathInfo, token, fromByte, toByte);

		if (null == pathInfo) {
			String msg = logger.log(StorageMgrMessage.PATH_IS_NULL);
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.BAD_REQUEST);
		}
		
		if (null == token) {
			String msg = logger.log(StorageMgrMessage.TOKEN_MISSING);
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.UNAUTHORIZED);
		}
		
		// token check begin
		// Check authentication token
		JWTClaimsSet claimsSet = null;
		
		try {
			claimsSet = extractJwtClaimsSet(token);
			
		} catch (IllegalArgumentException e) {
			
			String msg = logger.log(StorageMgrMessage.TOKEN_INVALID, token, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.UNAUTHORIZED);
		}
		
		if ((new Date()).after(claimsSet.getExpirationTime())) {
			
			String msg = logger.log(StorageMgrMessage.TOKEN_EXPIRED, claimsSet.getExpirationTime().toString());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.UNAUTHORIZED);

		}
		// token check end
		
		if (storageProvider.isVersion2()) { // begin version 2 - storage file -> byte page

			try {	
				Storage storage = storageProvider.getStorage(pathInfo); 
				
				String relativePath = storage.getRelativePath(pathInfo);
				
				StorageFile sourceFile = storage.getStorageFile(relativePath);
				
				if (sourceFile == null) {
					
					String msg = logger.log(StorageMgrMessage.PATH_IS_NULL);
					return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.BAD_REQUEST);
				}
								
				// token check begin
				if (!sourceFile.getFileName().equals(claimsSet.getSubject())) {
					
					String msg = logger.log(StorageMgrMessage.TOKEN_MISMATCH, sourceFile.getFileName());
					return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.UNAUTHORIZED);
				}
				// token check end 
				
				InputStream stream = storage.getInputStream(sourceFile);
				if (stream == null) {
					
					String msg = logger.log(StorageMgrMessage.FILE_NOT_FOUND, pathInfo);
					return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.NOT_FOUND);					
				}

				HttpHeaders headers = getFilePage(sourceFile, stream, fromByte, toByte);
				HttpStatus status = getOkOrPartialStatus(fromByte, toByte);

				InputStreamResource fsr = new InputStreamResource(stream);
				if (fsr != null) {
					
					logger.log(StorageMgrMessage.PRODUCT_FILE_PARTIALLY_DOWNLOADED, pathInfo, Long.toString(fromByte), Long.toString(toByte), Long.toString(toByte - fromByte));
					return new ResponseEntity<>(fsr, headers, status);
				}

			} catch (Exception e) {
				
				String msg = logger.log(StorageMgrMessage.INTERNAL_ERROR, e.getMessage());
				return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} // end version 2

		// Download file

		try {
			ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
			if (sourceFile == null) {
				return new ResponseEntity<>(errorHeaders(MSG_INVALID_PATH, MSG_ID_INVALID_PATH, pathInfo),
						HttpStatus.BAD_REQUEST);
			}
			
			// token check begin
			if (!sourceFile.getFileName().equals(claimsSet.getSubject())) {
				return new ResponseEntity<>(
						errorHeaders(MSG_TOKEN_MISMATCH, MSG_ID_TOKEN_MISMATCH, sourceFile.getFileName()),
						HttpStatus.UNAUTHORIZED);
			}
			// token check end
			
			InputStream stream = sourceFile.getDataAsInputStream();
			if (stream == null) {
				return new ResponseEntity<>(errorHeaders(MSG_FILE_NOT_FOUND, MSG_ID_FILE_NOT_FOUND, pathInfo),
						HttpStatus.NOT_FOUND);
			}
			Long fileSize = sourceFile.getLength();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentDispositionFormData("attachment", sourceFile.getFileName());
			Long len = fileSize;
			long from = 0;
			long to = len - 1;
			HttpStatus status = HttpStatus.OK;
			if (fromByte != null || toByte != null) {
				if (fromByte != null) {
					from = fromByte;
					stream.skip(from);
				}
				if (toByte != null) {
					to = Math.min(toByte, len - 1);
				}
				len = to - from + 1;
				headers.add(Headers.CONTENT_RANGE, String.format("bytes %d-%d/%d", from, to, fileSize));
				status = HttpStatus.PARTIAL_CONTENT;
			}
			// TODO The media type is not fully correct: It's OK for "application/zip", but
			// e.g. for NetCDF (.nc) "application/netcdf" would be more appropriate
			// headers.setContentType(new MediaType("application",
			// sourceFile.getExtension()));
			// PRIP always returns "application/octet-stream" in the metadata, so probably
			// it's best to stay consistent here
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.setContentLength(len);
			InputStreamResource fsr = new InputStreamResource(stream);
			if (fsr != null) {
				StorageLogger.logInfo(loggerLegacy, MSG_FILE_RETRIEVED, MSG_ID_FILE_RETRIEVED, pathInfo, from, to, len);

				return new ResponseEntity<>(fsr, headers, status);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(errorHeaders(MSG_FILE_NOT_FOUND, MSG_ID_FILE_NOT_FOUND, pathInfo),
				HttpStatus.NOT_FOUND);
	}

	/**
	 * Delete object(s)
	 * 
	 * @param pathInfo path to the object or directory
	 * 
	 * @return a response entity containing HTTP status OK and the full metadata of
	 *         the deleted object, or HTTP status NOT_FOUND and an error message, or
	 *         HTTP status INTERNAL_SERVER_ERROR and an error message
	 */
	@Override
	public ResponseEntity<RestProductFS> deleteProductByPathInfo(String pathInfo) {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteProductByPathInfo({})", pathInfo);

		if (storageProvider.isVersion2()) { // begin version 2 - delete files in storage
			
			if (null == pathInfo)  {
				String msg = logger.log(StorageMgrMessage.PATH_IS_NULL);
				return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.NOT_FOUND);		
			}
			
			if (pathInfo == "") {			
				String msg = logger.log(StorageMgrMessage.INVALID_PATH, pathInfo);
				return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.NOT_FOUND);					
			}

			try {
				String storageType = storageProvider.getStorage().getStorageType().toString();

				String relativePath = storageProvider.getRelativePath(pathInfo);
				List<String> deletedFilesOrDir = storageProvider.getStorage().delete(relativePath);
				RestProductFS response = createRestProductFilesDeleted(deletedFilesOrDir, storageType);
				
				logger.log(StorageMgrMessage.PRODUCT_FILE_DELETED, pathInfo);
								
				return new ResponseEntity<>(response, HttpStatus.OK);

			} catch (Exception e) {
				
				String msg = logger.log(StorageMgrMessage.INTERNAL_ERROR, e.getMessage());
				return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} // end version 2

		RestProductFS response = new RestProductFS();
		if (pathInfo != null) {
			ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
			try {
				ArrayList<String> deleted = sourceFile.delete();
				if (deleted != null && !deleted.isEmpty()) {
					response.setProductId("");
					response.setDeleted(true);
					response.setRegistered(false);
					response.setSourceFilePaths(deleted);
					response.setSourceStorageType(sourceFile.getFsType().toString());

					StorageLogger.logInfo(loggerLegacy, MSG_FILE_DELETED, MSG_ID_FILE_DELETED, pathInfo);

					return new ResponseEntity<>(response, HttpStatus.OK);
				}
			} catch (Exception e) {
				return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
						e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<>(errorHeaders(MSG_FILE_NOT_FOUND, MSG_ID_FILE_NOT_FOUND, pathInfo),
				HttpStatus.NOT_FOUND);
	}
	
	/**
	 * Gets OK or PARTIAL_CONTENT status
	 * 
	 * @param fromByte from byte
	 * @param toByte to byte
	 * @return http status OK or PARTIAL_CONTENT
	 */
	private HttpStatus getOkOrPartialStatus(Long fromByte, Long toByte) {

		HttpStatus status;

		if (fromByte != null || toByte != null) {
			status = HttpStatus.PARTIAL_CONTENT;
		} else {
			status = HttpStatus.OK;
		}

		return status;
	}

	/**
	 * Gets file page
	 * 
	 * @param sourceFile source file 
	 * @param stream stream
	 * @param fromByte from byte
	 * @param toByte to byte
	 * @return http header
	 * @throws IOException
	 */
	private HttpHeaders getFilePage(StorageFile sourceFile, InputStream stream, Long fromByte, Long toByte)
			throws IOException {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> getFilePage({}, {}, {}, {})", sourceFile.getFullPath(), stream, fromByte, toByte);
		
		Storage storage = storageProvider.getStorage(sourceFile.getFullPath()); 	

		Long len = storage.getFileSize(sourceFile);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("attachment", sourceFile.getFileName());
		long from = 0;
		long to = len - 1;

		if (fromByte != null || toByte != null) {

			List<HttpRange> ranges = new ArrayList<HttpRange>();

			if (fromByte != null) {
				from = fromByte;
				stream.skip(from);
			}

			if (toByte != null) {
				to = Math.min(toByte, len - 1);
			}

			len = to - from + 1;
			HttpRange range = HttpRange.createByteRange(from, to);
			ranges.add(range);
			headers.setRange(ranges);
			headers.setContentType(new MediaType("multipart", "byteranges"));

		} else {
			headers.setContentType(new MediaType("application", sourceFile.getExtension()));
		}
		headers.setContentLength(len);

		return headers;
	}

	/**
	 * Set the members of RestProductFS response.
	 * 
	 * @param response           the ingest info structure to update
	 * @param restProductFS      the ingest info structure to copy product ID and
	 *                           source information from
	 * @param storageId          the ID of the storage used
	 * @param registered         true, if the requested files have been ingested,
	 *                           false otherwise
	 * @param registeredFilePath common path to the ingested files
	 * @param registeredFiles    file names after ingestion
	 * @param deleted            true, if the files were deleted, false otherwise
	 * @param msg                a response message text
	 * @return the updated response object
	 */
	private RestProductFS setRestProductFS(RestProductFS response, RestProductFS restProductFS, String storageId,
			Boolean registered, String registeredFilePath, List<String> registeredFiles, Boolean deleted, String msg) {

		if (logger.isTraceEnabled())
			logger.trace(">>> setRestProductFS({}, {}, {}, {}, {}, {}, {}, {})",
					(null == response ? "MISSING" : response.getProductId()),
					(null == restProductFS ? "MISSING" : restProductFS.getProductId()), storageId, registered,
					registeredFilePath, registeredFiles.size(), deleted, msg);

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
		if (logger.isDebugEnabled())
			logger.debug("Response created: {}", response);
		return response;
	}

	/**
	 * Check the given token for formal correctness and extract its JWT claims set
	 * 
	 * @param token the signed JSON Web Token to check
	 * @return the JWT claims set contained in the token
	 * @throws IllegalArgumentException if the token cannot be analyzed
	 */
	private JWTClaimsSet extractJwtClaimsSet(String token) throws IllegalArgumentException {

		if (logger.isTraceEnabled())
			logger.trace(">>> extractJwtClaimsSet({})", token);

		SignedJWT signedJWT = null;
		try {
			signedJWT = SignedJWT.parse(token);
			
		} catch (ParseException e) {
			
			String msg = logger.log(StorageMgrMessage.TOKEN_NOT_PARSEABLE, e.getMessage());
			throw new IllegalArgumentException(msg);
		}

		JWSVerifier verifier = null;
		try {
			verifier = new MACVerifier(cfg.getStorageManagerSecret());
			
		} catch (JOSEException e) {
			
			String msg = logger.log(StorageMgrMessage.SECRET_TOO_SHORT, e.getMessage());
			throw new IllegalArgumentException(msg);
		}

		try {
			if (!signedJWT.verify(verifier)) {
				String msg = logger.log(StorageMgrMessage.TOKEN_VERIFICATION_FAILED);
				throw new IllegalArgumentException(msg);
			}		
			
		} catch (IllegalStateException e) {
			
			String msg = logger.log(StorageMgrMessage.TOKEN_STATE_INVALID, signedJWT.getState(), e.getMessage());
			throw new IllegalArgumentException(msg);
			
		} catch (JOSEException e) {
			
			String msg = logger.log(StorageMgrMessage.TOKEN_NOT_VERIFIABLE, e.getMessage());
			throw new IllegalArgumentException(msg);
		}

		// Retrieve / verify the JWT claims according to the app requirements
		JWTClaimsSet claimsSet = null;
		try {
			claimsSet = signedJWT.getJWTClaimsSet();
			
		} catch (ParseException e) {
			
			String msg = logger.log(StorageMgrMessage.TOKEN_PAYLOAD_INVALID, e.getMessage());
			throw new IllegalArgumentException(msg);
		}
		return claimsSet;
	}

	/**
	 * Creates rest product files deleted
	 * 
	 * @param deletedFiles deleted files
	 * @param storageType storage type
	 * @return RestProductFS
	 */
	private RestProductFS createRestProductFilesDeleted(List<String> deletedFiles, String storageType) {

		RestProductFS response = new RestProductFS();

		response.setProductId("");
		response.setDeleted(true);
		response.setRegistered(false);
		response.setSourceFilePaths(deletedFiles);
		response.setSourceStorageType(storageType);

		return response;
	}

	/**
	 * List file objects of repository. Collect result in response.
	 * 
	 * @param st       the storage type
	 * @param prefix   relative path to list
	 * @param response the ingest information response to fill
	 */
	private void listProductFiles(StorageType st, String prefix, List<String> response) {

		if (logger.isTraceEnabled())
			logger.trace(">>> listProductFiles({}, {})", st, prefix, response.size());

		ProseoFile path = null;
		if (prefix == null) {
			path = ProseoFile.fromType(st, "", cfg);
		} else {
			path = ProseoFile.fromType(st, prefix + "/", cfg);
		}
		List<ProseoFile> files = path.list();
		for (ProseoFile f : files) {
			String fs = f.getFsType().toString() + "|" + f.getFullPath();
			response.add(fs);
		}
	}

	/**
	 * Set the members of RestProductFS response.
	 * 
	 * @param restProductFS      the ingest info structure to copy product ID and
	 *                           source information from
	 * @param storageId          the ID of the storage used
	 * @param registered         true, if the requested files have been ingested,
	 *                           false otherwise
	 * @param registeredFilePath common path to the ingested files
	 * @param registeredFiles    file names after ingestion
	 * @param deleted            true, if the files were deleted, false otherwise
	 * @param msg                a response message text
	 * @return the updated response object
	 */
	private RestProductFS setRestProductFS(RestProductFS restProductFS, String storageId, Boolean registered,
			String registeredFilePath, List<String> registeredFiles, Boolean deleted, String msg) {

		if (logger.isTraceEnabled())
			logger.trace(">>> setRestProductFS({}, {}, {}, {}, {}, {}, {})",
					(null == restProductFS ? "MISSING" : restProductFS.getProductId()), storageId, registered,
					registeredFilePath, registeredFiles.size(), deleted, msg);

		RestProductFS response = new RestProductFS();

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

		if (logger.isDebugEnabled())
			logger.debug("Response created: {}", response);

		return response;
	}
	
	/**
	 * Gets Local host name 
	 * 
	 * @return local host name
	 */
	private String getLocalHostName() {

		try {
			InetAddress iAddress = InetAddress.getLocalHost();
			return iAddress.getHostName();

		} catch (UnknownHostException e1) {
			return "(UNKNOWN)";
		}
	}
	
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

		if (loggerLegacy.isTraceEnabled())
			loggerLegacy.trace(">>> errorHeaders({}, {}, {})", messageFormat, messageId, "messageParameters");

		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		loggerLegacy.error(message);

		// Create an HTTP "Warning" header
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message);
		return responseHeaders;
	}
}
