/**
 * ProductArchiveManagerApplication.java
 *
 * (C) 2023 DLR
 */
package de.dlr.proseo.archivemgr.rest;

import java.util.ConcurrentModificationException;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.archivemgr.rest.model.RestProductArchive;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Spring MVC controller for the prosEO Product Archive Manager; implements the services required to manage product archive
 * endpoints
 *
 * @author Denys Chaykovskiy
 */
@Component
public class ProductArchiveControllerImpl implements ArchiveController {

	private static ProseoLogger logger = new ProseoLogger(ProductArchiveControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ARCHIVE_MGR);

	/** Product archive manager */
	@Autowired
	private ProductArchiveManager productArchiveManager;

	/**
	 * Create a product archive from the given Json object
	 *
	 * @param archive the Json object to create the product archive from
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the product archive after persistence
	 *         (with ID and version for all contained objects) or HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestProductArchive> createArchive(RestProductArchive archive) {

		if (logger.isTraceEnabled())
			logger.trace(">>> createArchive({})", (null == archive ? "MISSING" : archive.getName()));

		try {
			return new ResponseEntity<>(productArchiveManager.createArchive(archive), HttpStatus.CREATED);

		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * List of all product archives with no search criteria
	 *
	 * @param code 			the product archive code
	 * @param name 			the product archive name
	 * @param archiveType	the product archive type
	 * @param recordFrom    first record of filtered and ordered result to return
	 * @param recordTo      last record of filtered and ordered result to return
	 * @return a response entity with either a list of product archives and HTTP status OK or an error message and an HTTP status
	 *         indicating failure
	 */
	@Override
	public ResponseEntity<List<RestProductArchive>> getArchives(String code, String name, String archiveType, Integer recordFrom, Integer recordTo) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getArchives({}, {}, {}, {}, {})", code, name, archiveType, recordFrom, recordTo);

		try {
			return new ResponseEntity<>(productArchiveManager.getArchives(code, name, archiveType, recordFrom, recordTo), HttpStatus.OK);

		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Find the product archive with the given ID
	 *
	 * @param id the ID to look for
	 * @return a response entity corresponding to the found product archive and HTTP status "OK" or an error message and HTTP status
	 *         "NOT_FOUND", if no product archive with the given ID exists
	 */
	@Override
	public ResponseEntity<RestProductArchive> getArchiveById(Long id) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getArchiveById({})", id);

		try {
			return new ResponseEntity<>(productArchiveManager.getArchiveById(id), HttpStatus.OK);

		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);

		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Delete a product archive by ID
	 *
	 * @param id the ID of the product archive to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, "BAD_REQUEST", if the archive still
	 *         has stored products, TODO: Check dependencies "NOT_FOUND", if the archive did not exist, or "NOT_MODIFIED", if the
	 *         deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteArchiveById(Long id) {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteArchiveById({})", id);

		try {
			productArchiveManager.deleteArchiveById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);

		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);

		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);

		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Update the product archive with the given ID with the attribute values of the given Json object.
	 *
	 * @param id          the ID of the product archive to update
	 * @param restArchive a Json object containing the modified (and unmodified) attributes
	 * @return a response containing HTTP status "OK" and a Json object corresponding to the product archive after modification
	 *         (with ID and version for all contained objects) or HTTP status "NOT_MODIFIED" and the unchanged product archive, if
	 *         no attributes were actually changed, or HTTP status "NOT_FOUND" and an error message, if no product archive with the
	 *         given ID exists
	 */
	@Override
	public ResponseEntity<RestProductArchive> modifyArchive(Long id, RestProductArchive restArchive) {

		if (logger.isTraceEnabled())
			logger.trace(">>> modifyArchive({})", id);

		try {
			RestProductArchive changedArchive = productArchiveManager.modifyArchive(id, restArchive);
			HttpStatus httpStatus = (restArchive.getVersion() == changedArchive.getVersion() ? HttpStatus.NOT_MODIFIED
					: HttpStatus.OK);
			return new ResponseEntity<>(changedArchive, httpStatus);

		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);

		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);

		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

	/**
	 * Count the product archives matching the specified name, archive type.
	 *
	 * @param name         the product archive name
	 * @param archiveType      the product archive type
	 * @return the number of matching archives as a String (may be zero) or HTTP status "FORBIDDEN" and an error message */
	@Override
	public ResponseEntity<String> countArchives(String name, String archiveType) {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> countArchives({}, {})", name, archiveType);

		try {
			return new ResponseEntity<>(productArchiveManager.countArchives(name, archiveType), HttpStatus.OK);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}