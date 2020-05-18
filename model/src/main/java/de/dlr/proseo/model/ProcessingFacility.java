/**
 * ProcessingFacility.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.Table;

import de.dlr.proseo.model.ProductFile.StorageType;

/**
 * A processing facility for running prosEO jobs and storing prosEO product files,  e. g. at a cloud service provider.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = { @Index(unique = true, columnList = "name") })
public class ProcessingFacility extends PersistentObject {

	/** The facility name (unique key) */
	@Column(nullable = false)
	private String name;
	
	/** A short description of the processing facility */
	private String description;
	
	/** The URL to access this facility's processing engine (Kubernetes instance) */
	private String processingEngineUrl;
	
	/** User name for connecting to this facility's processing engine (Kubernetes instance) */
	private String processingEngineUser;
	
	/** Password for connecting to this facility's processing engine (Kubernetes instance) */
	private String processingEnginePassword;
	
	/** The URL to access this facility's storage manager */
	private String storageManagerUrl;
	
	/**
	 * URL of the locally accessible Storage Manager instance on a specific processing node (to be used by the Processing Engine).
	 * This URL shall contain the string "%NODE_IP%", which will be replaced by the actual node IP of the Kubernetes worker node.
	 */
	private String localStorageManagerUrl;
	
	/** User name for connecting to the Storage Manager (locally and from external services) */
	private String storageManagerUser;
	
	/** Password for connecting to the Storage Manager (locally and from external services) */
	private String storageManagerPassword;

	/** The default storage type (S3, POSIX, ...) to use in this facility. */
	@Enumerated(EnumType.STRING)
	private StorageType defaultStorageType;
	
	/**
	 * Gets the name of the processing facility
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the processing facility
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the description of the processing facility
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of the processing facility
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the URL of the facility's processing engine (Kubernetes instance)
	 * 
	 * @return the URL of the processing engine
	 */
	public String getProcessingEngineUrl() {
		return processingEngineUrl;
	}

	/**
	 * Sets the URL of the facility's processing engine (Kubernetes instance)
	 * 
	 * @param processingEngineUrl the URL of the processing engine to set
	 */
	public void setProcessingEngineUrl(String processingEngineUrl) {
		this.processingEngineUrl = processingEngineUrl;
	}

	/**
	 * Gets the user name for the processing engine
	 * 
	 * @return the processing engine user
	 */
	public String getProcessingEngineUser() {
		return processingEngineUser;
	}

	/**
	 * Sets the user name for the processing engine
	 * 
	 * @param processingEngineUser the processing engine user to set
	 */
	public void setProcessingEngineUser(String processingEngineUser) {
		this.processingEngineUser = processingEngineUser;
	}

	/**
	 * Gets the password for the processing engine
	 * 
	 * @return the processing engine password
	 */
	public String getProcessingEnginePassword() {
		return processingEnginePassword;
	}

	/**
	 * Sets the password for the processing engine
	 * 
	 * @param processingEnginePassword the processing engine password to set
	 */
	public void setProcessingEnginePassword(String processingEnginePassword) {
		this.processingEnginePassword = processingEnginePassword;
	}

	/**
	 * Gets the URL of the facility's storage manager
	 * 
	 * @return the URL of the storage manager
	 */
	public String getStorageManagerUrl() {
		return storageManagerUrl;
	}

	/**
	 * Sets the URL of the facility's storage manager
	 * 
	 * @param storageManagerUrl the URL of the storage manager to set
	 */
	public void setStorageManagerUrl(String storageManagerUrl) {
		this.storageManagerUrl = storageManagerUrl;
	}

	/**
	 * @return the localStorageManagerUrl
	 */
	public String getLocalStorageManagerUrl() {
		return localStorageManagerUrl;
	}

	/**
	 * @param localStorageManagerUrl the localStorageManagerUrl to set
	 */
	public void setLocalStorageManagerUrl(String localStorageManagerUrl) {
		this.localStorageManagerUrl = localStorageManagerUrl;
	}

	/**
	 * @return the storageManagerUser
	 */
	public String getStorageManagerUser() {
		return storageManagerUser;
	}

	/**
	 * @param storageManagerUser the storageManagerUser to set
	 */
	public void setStorageManagerUser(String storageManagerUser) {
		this.storageManagerUser = storageManagerUser;
	}

	/**
	 * @return the storageManagerPassword
	 */
	public String getStorageManagerPassword() {
		return storageManagerPassword;
	}

	/**
	 * @param storageManagerPassword the storageManagerPassword to set
	 */
	public void setStorageManagerPassword(String storageManagerPassword) {
		this.storageManagerPassword = storageManagerPassword;
	}

	/**
	 * Gets the default storage type of the facility
	 * 
	 * @return the default storage type
	 */
	public StorageType getDefaultStorageType() {
		return defaultStorageType;
	}

	/**
	 * Sets the default storage type of the facility
	 * 
	 * @param defaultStorageType the default storage type to set
	 */
	public void setDefaultStorageType(StorageType defaultStorageType) {
		this.defaultStorageType = defaultStorageType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(name);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProcessingFacility))
			return false;
		ProcessingFacility other = (ProcessingFacility) obj;
		return Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "ProcessingFacility [name=" + name + ", description=" + description + ", processingEngineUrl=" + processingEngineUrl
				+ ", storageManagerUrl=" + storageManagerUrl + "]";
	}
	
}
