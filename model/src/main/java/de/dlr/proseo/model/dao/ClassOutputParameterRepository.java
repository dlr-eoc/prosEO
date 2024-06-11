/**
 * ClassOutputParameterRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import de.dlr.proseo.model.ClassOutputParameter;

/**
 * Data Access Object for the Task class
 * 
 * @author Dr. Thomas Bassler
 *
 */
public interface ClassOutputParameterRepository extends JpaRepository<ClassOutputParameter, Long> {

}
