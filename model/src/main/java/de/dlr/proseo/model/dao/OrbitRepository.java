/**
 * OrbitRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Orbit;

/**
 * Data Access Object for the Orbit class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface OrbitRepository extends CrudRepository<Orbit, Long> {

}
