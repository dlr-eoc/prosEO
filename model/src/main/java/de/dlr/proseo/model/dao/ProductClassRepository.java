/**
 * ProductClassRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.ProductClass;

/**
 * Data Access Object for the ProductClassRepository class
 * 
 * @author Dr. Thomas Bassler
 *
 */
public interface ProductClassRepository extends JpaRepository<ProductClass, Long> {
	
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
	 * Find a product class by mission code and product type
	 * 
	 * @param missionCode the code of the mission
	 * @param productType the (prosEO-internal) product type
	 * @return the unique product class identified by the mission code and product type
	 */
	@Query("select pc from ProductClass pc where pc.mission.code = ?1 and pc.productType = ?2")
	public ProductClass findByMissionCodeAndProductType(String missionCode, String productType);
}
