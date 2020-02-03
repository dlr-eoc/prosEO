/**
 * GroupRepository.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.dao;

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
public interface GroupRepository extends JpaRepository<User, Long> {

	/**
	 * Get the user group with the given group name
	 * 
	 * @param groupName the name of the user group
	 * @return the unique processing group identified by the given group name
	 */
	public User findByGroupName(String groupName);
	
}
