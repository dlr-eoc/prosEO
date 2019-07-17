/**
 * ProductRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Product;

/**
 * @author thomas
 *
 */
@Repository
//public interface ProductRepository<T extends PersistentObject> extends CrudRepository<T, Long>, QuerydslPredicateExecutor<T> {
//
//}

public interface ProductRepository extends CrudRepository<Product, Long> {

}
