/**
 * ProductFileRepository.java
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;

/**
 * Data Access Object for the ProductFile class
 * 
 * @author melchinger
 *
 */
@Repository
public interface ProductFileRepository  extends CrudRepository<ProductFile, Long> {
	
	/**
	 * Get the product files for product
	 * 
	 * @param product 
	 * @return list of product files for product
	 */
	public List<ProductFile> findByProduct(Product product);

}
