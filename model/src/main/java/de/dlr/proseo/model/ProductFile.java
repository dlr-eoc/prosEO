/**
 * ProductFile.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * The data file and pertinent auxiliary files for a product at a given processing facility. Each product has at most one
 * data file representation at each of the processing facilities.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = @Index(unique = true, columnList = "product_id, processing_facility"))
public class ProductFile extends PersistentObject {

	/** The product this data file belongs to */
	@ManyToOne
	private Product product;
	
	/** The processing facility, in which this data file is stored */
	@ManyToOne
	private ProcessingFacility processingFacility;
	
	/** The name of the data file */
	private String productFileName;
	
	/** The auxiliary files for this data file */
	@ElementCollection
	private Set<String> auxFileNames = new HashSet<>();
	
	/** The path to the product files (POSIX file path, S3 bucket etc.) */
	private String filePath;
	
	/** Type of the storage location */
	private StorageType storageType;
	
	/**
	 * The available storage types 
	 */
	public enum StorageType { S3, POSIX }

	/**
	 * Gets the associated product
	 * @return the product
	 */
	public Product getProduct() {
		return product;
	}

	/**
	 * Sets the associated product
	 * @param product the product to set
	 */
	public void setProduct(Product product) {
		this.product = product;
	}

	/**
	 * Gets the associated processing facility
	 * @return the processingFacility
	 */
	public ProcessingFacility getProcessingFacility() {
		return processingFacility;
	}

	/**
	 * Sets the associated processing facility
	 * @param processingFacility the processingFacility to set
	 */
	public void setProcessingFacility(ProcessingFacility processingFacility) {
		this.processingFacility = processingFacility;
	}

	/**
	 * Gets the name of the data file
	 * @return the productFileName
	 */
	public String getProductFileName() {
		return productFileName;
	}

	/**
	 * Sets the name of the data file
	 * @param productFileName the productFileName to set
	 */
	public void setProductFileName(String productFileName) {
		this.productFileName = productFileName;
	}

	/**
	 * Gets the set of auxiliary file names
	 * @return the auxFileNames
	 */
	public Set<String> getAuxFileNames() {
		return auxFileNames;
	}

	/**
	 * Sets the set of auxiliary file names
	 * @param auxFileNames the auxFileNames to set
	 */
	public void setAuxFileNames(Set<String> auxFileNames) {
		this.auxFileNames = auxFileNames;
	}

	/**
	 * Gets the file path for data file and auxiliary files
	 * @return the filePath
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Sets the file path for data file and auxiliary files (path name must be consistent with storage type)
	 * @param filePath the filePath to set
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * Gets the type of storage used at the processing facility
	 * @return the storageType
	 */
	public StorageType getStorageType() {
		return storageType;
	}

	/**
	 * Sets the type of storage used at the processing facility
	 * @param storageType the storageType to set
	 */
	public void setStorageType(StorageType storageType) {
		this.storageType = storageType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(processingFacility, product);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProductFile))
			return false;
		ProductFile other = (ProductFile) obj;
		return Objects.equals(processingFacility, other.processingFacility) && Objects.equals(product, other.product);
	};

}
