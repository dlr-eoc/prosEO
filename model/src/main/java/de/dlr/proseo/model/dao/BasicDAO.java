/**
 * BasicDAO.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.PersistentObject;

/**
 * Generic Data Access Object class for prosEO model classes. This class provides methods for creating, reading, updating and deleting
 * objects through Hibernate.
 * 
 * @author Dr. Thomas Bassler
 */
public class BasicDAO<T extends PersistentObject> {
	private static final String DELETE_FOR_INEXISTENT_OBJECT = "Delete for non-existent object with id {} attempted";

	/**
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(BasicDAO.class);
	
	/**
	 * The entity manager to use for this DAO (note that transaction handling is left with the calling service)
	 */
	private EntityManager em;
	/**
	 * The class for which this DAO is instantiated
	 */
	private Class<T> type;
	
	/**
	 * DAO constructor taking the entity manager as argument
	 * @param em the entity manager to set for this DAO
	 */
	public BasicDAO(EntityManager em, Class<T> type) {
		super();
		this.em = em;
		this.type = type;
	}
	
	/**
	 * Creates a given prosEO object in the database.
	 * @param newObject the object to store in the database
	 * @return the id assigned to the object
	 * @throws EntityExistsException if a user object with the same unique id exists in the database
	 */
	public synchronized long create(T newObject) throws EntityExistsException {
		if (logger.isTraceEnabled()) logger.trace("... entering create()");
		
		// Create the new user
		em.persist(newObject);

		return newObject.getId();
	}
	
	/**
	 * Gets the user with the given unique id. This method does not guarantee consistency of the persistent data source.
	 *
	 * @param id the unique id of the user
	 * @return the user with this unique id or null, if no such object exists
	 */
	public synchronized T get(long id) {
		if (logger.isTraceEnabled()) logger.trace("... entering get({})", id);
		
		T foundObject = em.find(type, id);

		return foundObject;
	}

	/**
	 * Updates a prosEO object in the database
	 * @param updateObject the object to update
	 */
	public synchronized void update(T updateObject) {
		if (logger.isTraceEnabled()) logger.trace("... entering update({})", updateObject.getId());

		em.persist(updateObject);
	}
	
	/**
	 * Deletes the prosEO object with the given unique id. If no such entry exists, nothing happens (but a warning is logged).
	 * @param id the id of the object to delete
	 */
	public synchronized void delete(long id) {
		if (logger.isTraceEnabled()) logger.trace("... entering delete({})", id);
		
		T deleteObject = em.find(type, id);

		if (null == deleteObject) {
			logger.warn(String.format(DELETE_FOR_INEXISTENT_OBJECT, id));
			return;
		}
		em.remove(deleteObject);
	}
}
