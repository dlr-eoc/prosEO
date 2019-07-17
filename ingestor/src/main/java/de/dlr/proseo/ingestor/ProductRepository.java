/**
 * ProductRepository.java
 */
package de.dlr.proseo.ingestor;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

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
