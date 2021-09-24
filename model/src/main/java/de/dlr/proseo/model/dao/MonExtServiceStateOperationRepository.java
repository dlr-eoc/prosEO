/**
 * MonServiceStateOperationRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonExtServiceStateOperation;

/**
 * Data Access Object for the MonServiceStateOperation class
 * 
 * @author Ernst Melchinger
 *
 */
@Repository
public interface MonExtServiceStateOperationRepository extends JpaRepository<MonExtServiceStateOperation, Long> {
	
}
