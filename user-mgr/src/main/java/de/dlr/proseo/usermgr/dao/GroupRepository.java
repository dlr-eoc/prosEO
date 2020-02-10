/**
 * GroupRepository.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.usermgr.model.Group;

/**
 * Data Access Object for the ProcessingOrder class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

	/**
	 * Get the user group with the given group name
	 * 
	 * @param groupName the name of the user group
	 * @return the unique processing group identified by the given group name
	 */
	public Group findByGroupName(String groupName);
	
	/**
	 * Get all user groups having the given authority (as directly assigned authority)
	 * 
	 * @param authority the authority (name) to check for
	 * @return a list of user groups with the given authority
	 */
	@Query("select g from groups g join g.groupAuthorities ga where ga.authority = ?1")
	public List<Group> findByAuthority(String authority);
}
