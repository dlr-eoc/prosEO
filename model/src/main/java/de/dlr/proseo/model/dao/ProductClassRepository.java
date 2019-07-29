/**
 * ProductClassRepository.java
 */
package de.dlr.proseo.model.dao;

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

}
