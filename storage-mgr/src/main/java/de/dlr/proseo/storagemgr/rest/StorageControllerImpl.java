/**
 * StorageControllerImpl.java
 * 
 * (C) 2019 DLR
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.rest.model.Joborder;
import de.dlr.proseo.storagemgr.rest.model.ProductFS;
import de.dlr.proseo.storagemgr.rest.model.Storage;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services required to manage
 * any object storage, e. g. a storage based on the AWS S3 API
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
		
		// TODO Auto-generated method stub
		return null;
	}

}
