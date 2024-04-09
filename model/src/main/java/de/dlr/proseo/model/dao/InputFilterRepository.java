/**
 * InputFilterRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import de.dlr.proseo.model.InputFilter;

/**
 * Data Access Object for the InputFilter class
 * 
 * @author Dr. Thomas Bassler
 *
 */
public interface InputFilterRepository extends JpaRepository<InputFilter, Long> {

}
