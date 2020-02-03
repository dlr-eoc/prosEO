/**
 * AclSidRepository.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.usermgr.model.AclObjectIdentity;
import de.dlr.proseo.usermgr.model.AclSid;
import de.dlr.proseo.usermgr.model.User;

/**
 * Data Access Object for the ProcessingOrder class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface AclObjectRepository extends JpaRepository<AclObjectIdentity, Long> {

	/**
	 * Get the ACL object identity (domain object) with the given class and identity name
	 * 
	 * @param className the Java class name of the ACL object identity
	 * @param identity the name of the ACL object identity
	 * @return the unique ACL object identity identified by the given class and identity name
	 */
	public AclObjectIdentity findByObjectIdClassAndObjectIdIdentity(String className, String identity);
	
}
