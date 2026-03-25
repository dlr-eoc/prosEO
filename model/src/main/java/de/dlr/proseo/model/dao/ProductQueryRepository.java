/**
 * ProductQueryRepository.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.ProductQuery;

/**
 * Data Access Object for the ProductQuery class
 * 
 * @author Dr. Thomas Bassler
 *
 */
public interface ProductQueryRepository extends JpaRepository<ProductQuery, Long> {
	
	/**
	 * Get all unsatisfied product queries for a given product class
	 * 
	 * @param productClassId the database id of the product class
	 * @return a (possibly empty) list of unsatisfied product queries
	 */
	@Query("select pq from ProductQuery pq where pq.requestedProductClass.id = ?1 and pq.isSatisfied = false")
	public List<ProductQuery> findUnsatisfiedByProductClass(long productClassId);

}
