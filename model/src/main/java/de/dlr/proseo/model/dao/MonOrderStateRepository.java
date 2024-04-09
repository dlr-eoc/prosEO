
/**
 * MonServiceStateRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import de.dlr.proseo.model.MonOrderState;

/**
 * Data Access Object for the MonOrderState class
 * 
 * @author Ernst Melchinger
 *
 */
public interface MonOrderStateRepository extends JpaRepository<MonOrderState, Long> {
	
	/**
	 * Get the mon service state with the given name
	 * 
	 * @param name the name of the service
	 * @return the unique mon service state identified by the name
	 */
	public MonOrderState findByName(String name);

}
