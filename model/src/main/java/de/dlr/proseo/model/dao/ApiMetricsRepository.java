package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.ApiMetrics;

/**
 * Data Access Object for the ApiMetrics class
 * 
 * @author Ernst Melchinger
 *
 */
public interface ApiMetricsRepository  extends JpaRepository<ApiMetrics, Long> {

	/**
	 * Get the latest entry by name
	 * 
	 * @return the latest entry by name
	 */
	@Query("select max(d.timestamp) from ApiMetrics d where name = ?1")
	public Instant findLastTimeStampByName(String name);
	
	/**
	 * Get the latest entry by name
	 * 
	 * @return the latest entry by name
	 */
	@Query("select d from ApiMetrics d where name = ?1 and timestamp = (select max(d.timestamp) from ApiMetrics d where name = ?1)")
	public ApiMetrics findLastEntryByName(String name);

	/**
	 * Get list of entries by name
	 * 
	 * @return the list of entries by name
	 */
	@Query("select d from ApiMetrics d where name = ?1")
	public List<ApiMetrics> findByName(String name);
	
	/**
	 * Get list of entries by name and timestamp
	 * 
	 * @return the list of entries by name and timestamp
	 */
	@Query("select d from ApiMetrics d where name = ?1 and timestamp = ?2")
	public List<ApiMetrics> findByNameAndTimeStamp(String name, Instant timestamp);
}
