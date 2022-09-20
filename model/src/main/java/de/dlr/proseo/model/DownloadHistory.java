/**
 * DownloadHistory.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

/**
 * History of product downloads; while originially bound to a ProductFile, the entry persists even after deletion
 * of the ProductFile to give a full history of the Product downloads
 * 
 * @author Dr. Thomas Bassler
 */
@Embeddable
public class DownloadHistory {

	/**
	 * The product file, which was downloaded (may be deleted during the lifetime of the history entry)
	 */
	@ManyToOne
	private ProductFile productFile;
	
	/**
	 * The user who initiated the download
	 */
	private String username;
	
	/**
	 * The file name of the downloaded ProductFile (persisted even after a possible deletion of the ProductFile)
	 */
	private String productFileName;
	
	/**
	 * The product file size in bytes (persisted from ProductFile)
	 */
	private Long productFileSize;
	
	/**
	 * The time of the download
	 */
	@Column(name = "date_time", columnDefinition = "TIMESTAMP(6)")
	private Instant dateTime;

	/**
	 * Gets the product file, which was downloaded
	 * 
	 * @return the download product file
	 */
	public ProductFile getProductFile() {
		return productFile;
	}

	/**
	 * Sets the product file, which was downloaded
	 * 
	 * @param productFile the download product file to set
	 */
	public void setProductFile(ProductFile productFile) {
		this.productFile = productFile;
	}

	/**
	 * Gets the user initiating the download
	 * 
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the user initiating the download
	 * 
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the file name of the download product
	 * 
	 * @return the product file name
	 */
	public String getProductFileName() {
		return productFileName;
	}

	/**
	 * Sets the file name of the download product
	 * 
	 * @param productFileName the product file name to set
	 */
	public void setProductFileName(String productFileName) {
		this.productFileName = productFileName;
	}

	/**
	 * Gets the download product file size in bytes
	 * 
	 * @return the product file size
	 */
	public Long getProductFileSize() {
		return productFileSize;
	}

	/**
	 * Sets the download product file size in bytes
	 * 
	 * @param productFileSize the product file size to set
	 */
	public void setProductFileSize(Long productFileSize) {
		this.productFileSize = productFileSize;
	}

	/**
	 * Gets the timestamp of the download
	 * 
	 * @return the download timestamp
	 */
	public Instant getDateTime() {
		return dateTime;
	}

	/**
	 * Sets the timestamp of the download
	 * 
	 * @param dateTime the timestamp to set
	 */
	public void setDateTime(Instant dateTime) {
		this.dateTime = dateTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dateTime, productFileName, productFileSize, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof DownloadHistory))
			return false;
		DownloadHistory other = (DownloadHistory) obj;
		return Objects.equals(dateTime, other.getDateTime()) && Objects.equals(productFileName, other.getProductFileName())
				&& Objects.equals(productFileSize, other.getProductFileSize()) && Objects.equals(username, other.getUsername());
	}

	@Override
	public String toString() {
		return "DownloadHistory [username=" + username + ", productFileName=" + productFileName + ", productFileSize="
				+ productFileSize + ", dateTime=" + dateTime + "]";
	}
	
}
