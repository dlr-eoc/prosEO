/**
 * ConfiguredProcessorRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.ConfiguredProcessor;

/**
 * Data Access Object for the ConfiguredProcessor class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ConfiguredProcessorRepository extends CrudRepository<ConfiguredProcessor, Long> {

}
