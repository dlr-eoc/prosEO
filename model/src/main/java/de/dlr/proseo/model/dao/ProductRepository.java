/**
 * ProductRepository.java
 */
package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Product;

/**
 * Data Access Object for the Product class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	/**
	 * Get all products of a given mission and class with their orbit numbers in the given range
	 * 
	 * @param missionCode the mission code
	 * @param productType the prosEO product type
	 * @param orbitNumberFrom the first orbit number to include
	 * @param orbitNumberTo the last orbit number to include
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from Product p where p.productClass.mission.code = ?1 and p.productClass.productType = ?2"
			+ " and p.orbit.orbitNumber between ?3 and ?4")
	public List<Product> findByMissionCodeAndProductTypeAndOrbitNumberBetween(
			String missionCode, String productType, Integer orbitNumberFrom, Integer orbitNumberTo);

	/**
	 * Get all products of a given mission and class with their sensing start times in the given time interval
	 * 
	 * @param missionCode the mission code
	 * @param productType the prosEO product type
	 * @param sensingStartTimeFrom the earliest start time
	 * @param sensingStartTimeTo the latest stop time
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from Product p where p.productClass.mission.code = ?1 and p.productClass.productType = ?2"
			+ " and p.sensingStartTime between ?3 and ?4")
	public List<Product> findByMissionCodeAndProductTypeAndSensingStartTimeBetween(
			String missionCode, String productType, Instant sensingStartTimeFrom, Instant sensingStartTimeTo);

	/**
	 * Get all products of a given mission and class with their sensing start time before the end of a time interval
	 * and the sensing stop time after the beginning of that interval (including border values);
	 * this results in a check for intersection with the time interval, if latestSensingStartTime > earliestSensingStopTime,
	 * and a check for coverage of the time interval, if latestSensingStartTime < earliestSensingStopTime
	 * 
	 * @param missionCode the mission code
	 * @param productType the prosEO product type
	 * @param latestSensingStartTime the latest sensing start time
	 * @param earliestSensingStop the earliest sensing stop time
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from Product p where p.productClass.mission.code = ?1 and p.productClass.productType = ?2"
			+ " and p.sensingStartTime <= ?3 and p.sensingStopTime >= ?4")
	public List<Product> findByMissionCodeAndProductTypeAndSensingStartTimeLessAndSensingStopTimeGreater(
			String missionCode, String productType, Instant latestSensingStartTime, Instant earliestSensingStop);

	/**
	 * Get the product which is produced by a job step
	 * 
	 * @param jobStep the job step 
	 * @return the product produced by job step
	 */
	public Product findByJobStep(JobStep jobStep);
	
	/**
	 * Get the product with the given universally unique product identifier
	 * 
	 * @param uuid the product UUID
	 * @return the requested product
	 */
	public Product findByUuid(UUID uuid);

	@Query("select p from Product p where p.productClass.id = ?1 and p.configuredProcessor.id = ?2" 
			+ " and p.sensingStartTime = ?3 and p.sensingStopTime = ?4")
	public List<Product> findByProductClassAndConfiguredProcessorAndSensingStartTimeAndSensingStopTime(
			long productClassId, long configuredProcessorId, Instant sensingStartTime, Instant sensingStopTime);

}
