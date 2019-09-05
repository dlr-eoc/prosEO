/**
 * ProductClassRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.ProductClass;

/**
 * Data Access Object for the ProductClassRepository class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ProductClassRepository extends CrudRepository<ProductClass, Long> {
	
	/**
	 * Find all product classes for a given mission code
	 * 
	 * @param missionCode the code of the mission
	 * @return a list of product classes
	 */
	@Query("select pc from ProductClass pc where pc.mission.code = ?1")
	public List<ProductClass> findByMissionCode(String missionCode);
	
	/**
	 * Find all product classes for a given product type (at most one per mission)
	 * 
	 * @param productType the (prosEO-internal) product type
	 * @return a list of product classes
	 */
	public List<ProductClass> findByProductType(String productType);
	
	/**
	 * Find all product classes for a given mission-defined type (at most one per mission)
	 * 
	 * @param missionType
	 * @return a list of product classes
	 */
	public List<ProductClass> findByMissionType(String missionType);
	
	/**
	 * Find a product class by mission code and product type
	 * 
	 * @param missionCode the code of the mission
	 * @param productType the (prosEO-internal) product type
	 * @return the unique product class identified by the mission code and product type
	 */
	@Query("select pc from ProductClass pc where pc.mission.code = ?1 and pc.productType = ?2")
	public ProductClass findByMissionCodeAndProductType(String missionCode, String productType);
	
	/**
	 * Find a product class by mission code and mission-defined type
	 * 
	 * @param missionCode the code of the mission
	 * @param missionType the mission-defined type of the product class
	 * @return the unique product class identified by the mission code and mission-defined type
	 */
	@Query("select pc from ProductClass pc where pc.mission.code = ?1 and pc.missionType = ?2")
	public ProductClass findByMissionCodeAndMissionType(String missionCode, String missionType);
}
