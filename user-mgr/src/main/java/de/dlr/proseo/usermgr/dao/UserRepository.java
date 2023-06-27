/**
 * UserRepository.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.usermgr.model.User;

/**
 * Data Access Object for the User class
 *
 * @author Dr. Thomas Bassler *
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

	/**
	 * Get the user with the given user name
	 *
	 * @param username the user name
	 * @return the unique processing user identified by the given user name
	 */
	public User findByUsername(String username);

	/**
	 * Get all users for the given mission
	 *
	 * @param missionCode the code of the mission
	 * @return a list of users with user names starting with the mission code
	 */
	@Query("select u from users u where u.username like concat(?1, '-%')")
	public List<User> findByMissionCode(String missionCode);

	/**
	 * Get all user accounts whose expiration date is reached before the given limit
	 * date
	 *
	 * @param expirationLimit limit date for account expirations
	 * @return a list of user accounts expired before the given limit
	 */
	public List<User> findByExpirationDateBefore(Date expirationLimit);

	/**
	 * Get all user accounts having the given authority (as directly assigned
	 * authority)
	 *
	 * @param authority the authority (name) to check for
	 * @return a list of user accounts with the given authority
	 */
	@Query("select u from users u join u.authorities a where a.authority = ?1")
	public List<User> findByAuthority(String authority);

}
