/**
 * ParameterRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.Parameter;

/**
 * Data Access Object for the Parameter class
 * 
 * @author melchinger
 *
 */
@Repository
public interface ParameterRepository  extends CrudRepository<Parameter, Long> {
	
	/**
	 * Get the parameters for product
	 * 
	 * @param product 
	 * @return list of parameters for product
	 */
	public List<Parameter> findByProduct(Product product);

}
