/**
 * MonOrderProgressRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonOrderProgress;

/**
 * Data Access Object for the MonOrderProgress class
 * 
 * @author Ernst Melchinger
 *
 */
@Repository
public interface MonOrderProgressRepository extends JpaRepository<MonOrderProgress, Long> {
	
}
