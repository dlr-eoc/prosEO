/**
 * ProductRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Product;

/**
 * Data Access Object for the Product class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ProductRepository extends CrudRepository<Product, Long> {

}
