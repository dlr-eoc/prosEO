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

import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.enums.StorageType;

/**
 * A processing facility for running prosEO jobs and storing prosEO product files,  e. g. at a cloud service provider.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = { @Index(unique = true, columnList = "name") })
public class ProcessingFacility extends PersistentObject {
	
	/* Message strings */
	private static final String MSG_ILLEGAL_STATE_TRANSITION = "Illegal facility state transition from %s to %s";

	/** The facility name (unique key) */
	@Column(nullable = false)
	private String name;
	
	/** A short description of the processing facility */
	private String description;
	
	/** The run state the facility currently is in */
	@Enumerated(EnumType.STRING)
	private FacilityState facilityState;
	
	/** The URL to access this facility's processing engine (Kubernetes instance) */
	private String processingEngineUrl;
	
	/** Authentication token for connecting to this facility's processing engine (Kubernetes instance) */
	@org.hibernate.annotations.Type(type = "materialized_clob")
	private String processingEngineToken;
	
	/**
	 * Maximum number of jobs, which may on average be scheduled per processing node on this processing facility
	 * (on a Kubernetes cluster with n worker nodes the Production Planner will never schedule more than
	 * n * maxJobsPerNode job steps).
	 */
	private Integer maxJobsPerNode = 1;
	
	/** The URL to access this facility's storage manager from the control instance */
	private String storageManagerUrl;
	
	/** The URL to access this facility's storage manager from an external client (via PRIP API) */
	private String externalStorageManagerUrl;
	
	/**
	 * URL of the locally accessible Storage Manager instance on a specific processing node (to be used by the Processing Engine).
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
	 * Gets the run state of the processing facility
	 * @return the facility state
	 */
	public FacilityState getFacilityState() {
		return facilityState;
	}

	/**
	 * Sets the run state of the processing facility
	 * @param facilityState the facility state to set
	 * @throws IllegalStateException if the intended facility state transition is illegal
	 */
	public void setFacilityState(FacilityState facilityState) throws IllegalStateException {
		if (null == this.facilityState || this.facilityState.equals(facilityState) || this.facilityState.isLegalTransition(facilityState)) {
			this.facilityState = facilityState;
		} else {
			throw new IllegalStateException(String.format(MSG_ILLEGAL_STATE_TRANSITION,
					this.facilityState.toString(), facilityState.toString()));
		}
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
	 * Gets the authentication token for the processing engine
	 * 
	 * @return the processing engine token
	 */
	public String getProcessingEngineToken() {
		return processingEngineToken;
	}

	/**
	 * Sets the authentication token for the processing engine
	 * 
	 * @param processingEngineToken the processing engine token to set
	 */
	public void setProcessingEngineToken(String processingEngineToken) {
		this.processingEngineToken = processingEngineToken;
	}

	/**
	 * Gets the maximum number of jobs schedulable per worker node
	 * 
	 * @return the maximum number of jobs per node
	 */
	public Integer getMaxJobsPerNode() {
		return maxJobsPerNode;
	}

	/**
	 * Sets the maximum number of jobs schedulable per worker node
	 * 
	 * @param maxJobsPerNode the maximum number of jobs per node to set
	 */
	public void setMaxJobsPerNode(Integer maxJobsPerNode) {
		this.maxJobsPerNode = maxJobsPerNode;
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
	 * Gets the URL of the facility's storage manager for external clients
	 * 
	 * @return the externalStorageManagerUrl
	 */
	public String getExternalStorageManagerUrl() {
		return externalStorageManagerUrl;
	}

	/**
	 * Sets the URL of the facility's storage manager for external clients
	 * 
	 * @param externalStorageManagerUrl the externalStorageManagerUrl to set
	 */
	public void setExternalStorageManagerUrl(String externalStorageManagerUrl) {
		this.externalStorageManagerUrl = externalStorageManagerUrl;
	}

	/**
	 * Gets the URL of the facility's storage manager for local worker nodes
	 * 
	 * @return the localStorageManagerUrl
	 */
	public String getLocalStorageManagerUrl() {
		return localStorageManagerUrl;
	}

	/**
	 * Sets the URL of the facility's storage manager for local worker nodes
	 * 
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
		if (super.equals(obj))
			return true;
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
