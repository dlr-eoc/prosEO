/**
 * ProductQueryRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProductQuery;

/**
 * Data Access Object for the ProductQuery class
 * 
 * @author melchinger
 *
 */
@Repository
public interface ProductQueryRepository  extends CrudRepository<ProductQuery, Long> {
	
	/**
	 * Get the product queries to be satisfied to run job step
	 * 
	 * @param jobStep 
	 * @return list of product queries for job step
	 */
	public List<ProductQuery> findByJobStep(JobStep jobStep);

}
