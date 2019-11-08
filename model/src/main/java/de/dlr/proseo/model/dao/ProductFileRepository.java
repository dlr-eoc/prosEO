/**
 * ProductFileRepository.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.ProductFile;

/**
 * Data Access Object for the ProductFile class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ProductFileRepository extends JpaRepository<ProductFile, Long> {
	
	/**
	 * Get all product files for a given product id
	 * 
	 * @param productId the database id of the product 
	 * @return a (possibly empty) list of product files
	 */
	@Query("select pf from ProductFile pf where product_id = ?1")
	public List<ProductFile> findByProductId(long productId);

}
