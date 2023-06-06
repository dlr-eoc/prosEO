/**
 * ProductArchiveRepository.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.ProductArchive;

/**
 * Data Access Object for the ProductArchive class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface ProductArchiveRepository extends JpaRepository<ProductArchive, Long> {
	
	/**
	 * Get the product archive with the given code
	 * 
	 * @param code the short code of the product archive (case insensitive)
	 * @return the unique product archive identified by the code
	 */
	@Query("select pa from ProductArchive pa where UPPER(pa.code) = UPPER(?1)")
	public ProductArchive findByCode(String code);
	
	
	/**
	 * Get the product archive with the given name
	 * 
	 * @param archiveName the name of the productArchive
	 * @return product archive identified by the name
	 */
	@Query("select pa from ProductArchive pa where UPPER(pa.name) = UPPER(?1)")
	public List<ProductArchive> findByName(String archiveName);

}
