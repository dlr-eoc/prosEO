/**
 * ProductFileRepository.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import de.dlr.proseo.model.ProductFile;

/**
 * Data Access Object for the ProductFile class
 * 
 * @author Dr. Thomas Bassler
 *
 */
public interface ProductFileRepository extends JpaRepository<ProductFile, Long> {

	/**
	 * Get all product files for a given product id
	 * 
	 * @param productId the database id of the product
	 * @return a (possibly empty) list of product files
	 */
	@NativeQuery("select pf from PRODUCT_FILE pf where pf.product_id = ?1")
	public List<ProductFile> findByProductId(long productId);

	/**
	 * Get all product files for a given processing facility id
	 * 
	 * @param facilityId the database id of the processing facility
	 * @return a (possibly empty) list of product files
	 */
	@NativeQuery("select pf from PRODUCT_FILE pf where pf.processing_facility_id = ?1")
	public List<ProductFile> findByProcessingFacilityId(long facilityId);

	/**
	 * Get all product files for a given file name
	 * 
	 * @param fileName the name of the product file
	 * @return a (possibly empty) list of product files
	 */
	@NativeQuery("select pf from PRODUCT_FILE pf where pf.product_file_name = ?1")
	public List<ProductFile> findByFileName(String fileName);

}
