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
import de.dlr.proseo.model.enums.ProductionType;

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
	 * Get all products of a given mission and class with their publication times in the given time interval
	 * 
	 * @param missionCode the mission code
	 * @param productType the prosEO product type
	 * @param publicationTimeFrom the earliest publication time
	 * @param publicationTimeTo the latest publication time
	 * @return a list of products satisfying the search criteria
	 */
	
	@Query("select p from Product p where p.productClass.mission.code = ?1 and p.productionType = ?2"
			+ " and p.publicationTime is not null and p.publicationTime between ?3 and ?4 and p.productClass.processingLevel is not null")
	public List<Product> findByMissionCodeAndProductionTypeAndPublicatedAndPublicationTimeBetween(
			String missionCode, ProductionType productionType, Instant publicationTimeFrom, Instant publicationTimeTo);
	/**
	 * Get all products of a given mission and class with their publication times in the given time interval
	 * 
	 * @param missionCode the mission code
	 * @param productType the prosEO product type
	 * @param generationTimeFrom the earliest generation time
	 * @param generationTimeTo the latest generation time
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from Product p where p.productClass.mission.code = ?1 and p.productionType = ?2"
			+ " and p.generationTime is not null and p.generationTime between ?3 and ?4")
	public List<Product> findByMissionCodeAndProductionTypeAndGeneratedAndGenerationTimeBetween(
			String missionCode, ProductionType productionType, Instant generationTimeFrom, Instant generationTimeTo);

	/**
	 * Get all products of a given mission and class with their sensing start time before the end of a time interval
	 * and the sensing stop time after the beginning of that interval (including border values);
	 * this results in a check for intersection with the time interval, if latestSensingStartTime after earliestSensingStopTime,
	 * and a check for coverage of the time interval, if latestSensingStartTime before earliestSensingStopTime
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

	/**
	 * Get a list of products of a given product class, configured process and sensing times
	 * 
	 * @param productClassId the ID of the product class
	 * @param configuredProcessorId the ID of the configured processor
	 * @param sensingStartTime the sensing start time
	 * @param sensingStopTime the sensing stop time
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from Product p where p.productClass.id = ?1 and p.configuredProcessor.id = ?2" 
			+ " and p.sensingStartTime = ?3 and p.sensingStopTime = ?4")
	public List<Product> findByProductClassAndConfiguredProcessorAndSensingStartTimeAndSensingStopTime(
			long productClassId, long configuredProcessorId, Instant sensingStartTime, Instant sensingStopTime);

	/**
	 * Get a list of products with eviction times in the past
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from Product p where p.evictionTime is not null and p.evictionTime < current_timestamp")
	public List<Product> findByEvictionTimeBeforeNow();

	/**
	 * Get a list of products with eviction times older than evictionTime
	 * 
	 * @param evictionTime the time to compare
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from Product p where p.evictionTime is not null and p.evictionTime < ?1 and p.productFile is not empty")
	public List<Product> findByEvictionTimeLessThan(Instant evictionTime);

}
