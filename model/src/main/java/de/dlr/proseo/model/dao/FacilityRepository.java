/**
 * FacilityRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.ProcessingFacility;

/**
 * Data Access Object for the ProcessingFacility class
 * 
 * @author Dr. Thomas Bassler
 *
 */
public interface FacilityRepository extends JpaRepository<ProcessingFacility, Long> {
	
	/**
	 * Get the processing facility with the given name
	 * 
	 * @param facilityName the name of the processing facility
	 * @return the unique processing facility identified by the name
	 */
	@Query("select pf from ProcessingFacility pf where UPPER(pf.name) = UPPER(?1)")
	public ProcessingFacility findByName(String facilityName);

}
