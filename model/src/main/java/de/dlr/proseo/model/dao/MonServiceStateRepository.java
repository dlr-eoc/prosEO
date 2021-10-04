/**
 * MonServiceStateRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonServiceState;

/**
 * Data Access Object for the MonService class
 * 
 * @author Ernst Melchinger
 *
 */
@Repository
public interface MonServiceStateRepository extends JpaRepository<MonServiceState, Long> {
	
	/**
	 * Get the mon service state with the given name
	 * 
	 * @param name the name of the service
	 * @return the unique mon service state identified by the name
	 */
	public MonServiceState findByName(String name);

}
