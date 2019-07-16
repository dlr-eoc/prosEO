/**
 * BasicRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.PersistentObject;

/**
 * @author thomas
 *
 */
@Repository
//public interface BasicRepository<T extends PersistentObject> extends CrudRepository<T, Long>, QuerydslPredicateExecutor<T> {
//
//}

public interface BasicRepository<T extends PersistentObject> extends CrudRepository<T, Long> {

}
