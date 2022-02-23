/**
 * ProductFile.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import de.dlr.proseo.model.enums.StorageType;

/**
 * The data file and pertinent auxiliary files for a product at a given processing facility. Each product has at most one
 * data file representation at each of the processing facilities.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = @Index(unique = true, columnList = "product_id, processing_facility_id"))
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
	@Enumerated(EnumType.STRING)
	private StorageType storageType;
	
	/** The size of the primary product file in bytes */
	private Long fileSize;
	
	/** Checksum value for the primary product file (computed by MD5 algorithm) */
	private String checksum;
	
	/** Checksum generation time */
	@Column(name = "checksum_time", columnDefinition = "TIMESTAMP(6)")
	private Instant checksumTime;
	
	/** The file name of the ZIP archive containing all product and auxiliary files intended for distribution */
	private String zipFileName;
	
	/** The file size of the ZIP archive in bytes */
	private Long zipFileSize;
	
	/** The MD5 checksum for the ZIP archive */
	private String zipChecksum;
	
	/** ZIP archive checksum generation time */
	@Column(name = "zip_checksum_time", columnDefinition = "TIMESTAMP(6)")
	private Instant zipChecksumTime;
	
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

	/**
	 * Gets the size in bytes of the primary product file
	 * 
	 * @return the file size
	 */
	public Long getFileSize() {
		return fileSize;
	}

	/**
	 * Sets the size in bytes of the primary product file
	 * 
	 * @param fileSize the file size to set
	 */
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	/**
	 * Gets the MD5 checksum for the primary product file
	 * 
	 * @return the checksum string
	 */
	public String getChecksum() {
		return checksum;
	}

	/**
	 * Sets the MD5 checksum for the primary product file
	 * 
	 * @param checksum the checksum to set
	 */
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	};

	/**
	 * Gets the computation time for the product file checksum
	 * 
	 * @return the checksum computation time
	 */
	public Instant getChecksumTime() {
		return checksumTime;
	}

	/**
	 * Sets the computation time for the product file checksum
	 * 
	 * @param checksumTime the checksumTime to set
	 */
	public void setChecksumTime(Instant checksumTime) {
		this.checksumTime = checksumTime;
	}

	/**
	 * Gets the name of the ZIP archive
	 * 
	 * @return the ZIP archive file name or null, if no ZIP archive exists
	 */
	public String getZipFileName() {
		return zipFileName;
	}

	/**
	 * Sets the name of the ZIP archive
	 * 
	 * @param zipFileName the ZIP archive file name to set (may be null)
	 */
	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}

	/**
	 * Gets the size of the ZIP archive in bytes
	 * 
	 * @return the ZIP archive file size or null, if no ZIP archive exists
	 */
	public Long getZipFileSize() {
		return zipFileSize;
	}

	/**
	 * Sets the size of the ZIP archive in bytes
	 * 
	 * @param zipFileSize the ZIP archive file size to set
	 */
	public void setZipFileSize(Long zipFileSize) {
		this.zipFileSize = zipFileSize;
	}

	/**
	 * Gets the MD5 checksum of the ZIP archive
	 * 
	 * @return the ZIP archive checksum or null, if no ZIP archive exists
	 */
	public String getZipChecksum() {
		return zipChecksum;
	}

	/**
	 * Sets the MD5 checksum of the ZIP archive
	 * 
	 * @param zipChecksum the ZIP archive checksum to set (may be null)
	 */
	public void setZipChecksum(String zipChecksum) {
		this.zipChecksum = zipChecksum;
	}

	/**
	 * Gets the computation time for the ZIP archive file checksum
	 * 
	 * @return the checksum computation time or null, if no ZIP archive exists
	 */
	public Instant getZipChecksumTime() {
		return zipChecksumTime;
	}

	/**
	 * Sets the computation time for the ZIP archive file checksum
	 * 
	 * @param zipChecksumTime the checksum computation time to set (may be null)
	 */
	public void setZipChecksumTime(Instant zipChecksumTime) {
		this.zipChecksumTime = zipChecksumTime;
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
	}

	@Override
	public String toString() {
		return "ProductFile [processingFacility=" + processingFacility + ", productFileName=" + productFileName + ", auxFileNames="
				+ auxFileNames + ", filePath=" + filePath + ", storageType=" + storageType + "]";
	}

}
