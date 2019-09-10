/**
 * FacilityRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.ProcessingFacility;

/**
 * Data Access Object for the ProcessingFacility class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface FacilityRepository extends CrudRepository<ProcessingFacility, Long> {
	
	/**
	 * Get the processing facility with the given name
	 * 
	 * @param facilityName the name of the processing facility
	 * @return the unique processing facility identified by the name
	 */
	public ProcessingFacility findByName(String facilityName);

}
