/**
 * GroupMemberRepository.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.usermgr.model.GroupMember;

/**
 * Data Access Object for the GroupMember class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

}
