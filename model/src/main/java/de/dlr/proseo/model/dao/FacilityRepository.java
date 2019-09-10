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

	/**
	 * Get all processing facilities on a given host (string comparison only; no DNS resolution either forward or backward will be attempted)
	 * 
	 * @param hostname the host part of the URL for the processing facilities
	 * @return a list of processing facilities available on the given host
	 */
	@Query("select pf from ProcessingFacility pf where pf.url like 'http://?1%' or pf.url like 'https://?1%'")
	public List<ProcessingFacility> findByHostname(String hostname);
}
