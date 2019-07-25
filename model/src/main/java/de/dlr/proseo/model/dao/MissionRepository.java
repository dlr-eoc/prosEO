/**
 * MissionRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Mission;

/**
 * Data Access Object for the Mission class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface MissionRepository extends CrudRepository<Mission, Long> {

}
