/**
 * UserRepository.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.usermgr.model.User;

/**
 * Data Access Object for the ProcessingOrder class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * Get the user with the given user name
	 * 
	 * @param username the user name
	 * @return the unique processing user identified by the given user name
	 */
	public User findByUsername(String username);
	
	/**
	 * Get all user accounts whose expiration date is reached before the given limit date
	 * 
	 * @param expirationLimit limit date for account expirations
	 * @return a list of user accounts expired before the given limit
	 */
	public List<User> findByExpirationDateBefore(Date expirationLimit);
}
