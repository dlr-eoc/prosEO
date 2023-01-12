package de.dlr.proseo.storagemgr.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.S3;
import de.dlr.proseo.storagemgr.rest.model.Posix;
import de.dlr.proseo.storagemgr.rest.model.Joborder;
import de.dlr.proseo.storagemgr.rest.model.RestInfo;



/**
 * Handle information about storage manager settings.
 * 
 * @author melchinger
 *
 */
@Component
public class InfoControllerImpl implements InfoController {

	@Autowired
	private StorageManagerConfiguration cfg;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(InfoControllerImpl.class);
	
	/**
	 * Set information with configuration settings.
	 * 
	 * @return a response entity with the requested information
	 */
	@Override
	public ResponseEntity<RestInfo> getRestInfo() {
		
		if (logger.isTraceEnabled()) logger.trace(">>> getRestInfo()");
		
		RestInfo response = new RestInfo();
		
		S3 s3 = new S3();
		s3.setS3EndPoint(cfg.getS3EndPoint());
		s3.setS3Region(cfg.getS3Region());
		s3.setS3MaxNumberOfBuckets(Long.valueOf(cfg.getS3MaxNumberOfBuckets()));
		s3.setS3DefaultBucket(cfg.getS3DefaultBucket());
		response.setS3(s3);
		

		Posix posix = new Posix();
		posix.setBackendPath(cfg.getPosixBackendPath());
		posix.setCachePath(cfg.getPosixCachePath());
		response.setPosix(posix);
		Joborder joborder = new Joborder();
		joborder.setBucket(cfg.getJoborderBucket());
		joborder.setPrefix(cfg.getJoborderPrefix());

		response.setJoborder(joborder);
		
		logger.log(StorageMgrMessage.REST_INFO_GOT);
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
