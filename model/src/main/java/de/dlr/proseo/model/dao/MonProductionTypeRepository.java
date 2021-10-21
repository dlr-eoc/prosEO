package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonProductionType;

/**
 * Data Access Object for the MonProductionType class
 * 
 * @author Ernst Melchinger
 *
 */
@Repository
public interface MonProductionTypeRepository extends JpaRepository<MonProductionType, Long> {
	
}
