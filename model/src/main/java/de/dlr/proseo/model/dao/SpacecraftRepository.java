/**
 * SpacecraftRepository.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Spacecraft;

/**
 * Data Access Object for the Spacecraft class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface SpacecraftRepository extends CrudRepository<Spacecraft, Long> {

	/**
	 * Get the spacecraft with the given code
	 * 
	 * @param spacecraftCode the spacecraft code
	 * @return the unique spacecraft identified by the code
	 */
	public Spacecraft findByCode(String spacecraftCode);
}
