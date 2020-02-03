/**
 * AclSidRepository.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.usermgr.model.AclSid;
import de.dlr.proseo.usermgr.model.User;

/**
 * Data Access Object for the ProcessingOrder class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface AclSidRepository extends JpaRepository<AclSid, Long> {

	/**
	 * Get the security identities with the given sid name
	 * 
	 * @param sid the name of the security identity
	 * @return the unique ACL security identity identified by the given name
	 */
	public AclSid findBySid(String sid);
	
}
