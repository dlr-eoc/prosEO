/**
 * MonServiceRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import de.dlr.proseo.model.MonService;

/**
 * Data Access Object for the MonService class
 * 
 * @author Ernst Melchinger
 *
 */
public interface MonServiceRepository extends JpaRepository<MonService, Long> {
	
	/**
	 * Get the mon service with the given name
	 * 
	 * @param name the name of the service
	 * @return the unique mon service identified by the name
	 */
	public MonService findByNameId(String name);

}
