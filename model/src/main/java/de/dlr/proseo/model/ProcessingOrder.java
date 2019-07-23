/**
 * ProcessingOrder.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import de.dlr.proseo.model.Product.Parameter;

/**
 * A customer order to process a specific set of ProductClasses for a specific period of time using a specific set of
 * ConfiguredProcessors. An order may have properties like a product quality indicator (test vs operational), specific product
 * delivery endpoints, specific (potentially mission-dependent) product generation attributes (e. g. a Copernicus collection
 * number) etc.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class ProcessingOrder extends PersistentObject {

	/** Mission, to which this order belongs */
	@ManyToOne
	private Mission mission;
	
	/** User-defined order identifier */
	private String identifier;
	
	/** Expected execution time (optional, used for scheduling) */
	private Instant executionTime;
	
	/** A set of additional conditions to apply to selected products.
	 * Note: For Sentnel-5P at least the parameters "copernicusCollection", "fileClass" and "revision" are required. */
	@ElementCollection
	private Map<String, Parameter> filterConditions;
	
	/** A set of parameters to set for the generated products.
	 * Note: For Sentnel-5P at least the parameters "copernicusCollection", "fileClass" and "revision" are required.
	 */
	@ElementCollection
	private Map<String, Parameter> outputParameters;
	
	/** Set of requested product classes */
	@ManyToMany
	private Set<ProductClass> requestedProductClasses;
	
	/** The processor configurations for processing the products */
	@ManyToMany
	private Set<ConfiguredProcessor> requestedConfiguredProcessors;
	
	/** The orbits, for which products are to be generated */
	@ManyToMany
	private List<Orbit> requestedOrbits;
	
	/** The products, which will provided as input */
	@ManyToMany
	private Set<Product> promisedProducts;
	
	/** The processing jobs belonging to this order */	
	@OneToMany(mappedBy = "processingOrder")
	private Set<Job> jobs;

	/**
	 * Gets the owning mission
	 * 
	 * @return the mission
	 */
	public Mission getMission() {
		return mission;
	}

	/**
	 * Sets the owning mission
	 * 
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}

	/**
	 * Gets the user-defined identifier
	 * 
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the user-defined identifier
	 * 
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Gets the scheduled execution time (if any)
	 * 
	 * @return the executionTime (may be null)
	 */
	public Instant getExecutionTime() {
		return executionTime;
	}

	/**
	 * Sets the scheduled execution time
	 * 
	 * @param executionTime the executionTime to set (a null value removes an existing execution time)
	 */
	public void setExecutionTime(Instant executionTime) {
		this.executionTime = executionTime;
	}

	/**
	 * Gets the filter conditions
	 * 
	 * @return the filterConditions
	 */
	public Map<String, Parameter> getFilterConditions() {
		return filterConditions;
	}

	/**
	 * Sets the filter conditions
	 * 
	 * @param filterConditions the filterConditions to set
	 */
	public void setFilterConditions(Map<String, Parameter> filterConditions) {
		this.filterConditions = filterConditions;
	}

	/**
	 * Gets the output parameters
	 * 
	 * @return the outputParameters
	 */
	public Map<String, Parameter> getOutputParameters() {
		return outputParameters;
	}

	/**
	 * Sets the output parameters
	 * 
	 * @param outputParameters the outputParameters to set
	 */
	public void setOutputParameters(Map<String, Parameter> outputParameters) {
		this.outputParameters = outputParameters;
	}

	/**
	 * Gets the requested product classes
	 * 
	 * @return the requestedProductClasses
	 */
	public Set<ProductClass> getRequestedProductClasses() {
		return requestedProductClasses;
	}

	/**
	 * Sets the requested product classes
	 * 
	 * @param requestedProductClasses the requestedProductClasses to set
	 */
	public void setRequestedProductClasses(Set<ProductClass> requestedProductClasses) {
		this.requestedProductClasses = requestedProductClasses;
	}

	/**
	 * Gets the requested configured processors
	 * 
	 * @return the requestedConfiguredProcessors
	 */
	public Set<ConfiguredProcessor> getRequestedConfiguredProcessors() {
		return requestedConfiguredProcessors;
	}

	/**
	 * Sets the requested configured processors
	 * 
	 * @param requestedConfiguredProcessors the requestedConfiguredProcessors to set
	 */
	public void setRequestedConfiguredProcessors(Set<ConfiguredProcessor> requestedConfiguredProcessors) {
		this.requestedConfiguredProcessors = requestedConfiguredProcessors;
	}

	/**
	 * Gets the requested orbits
	 * 
	 * @return the requestedOrbits
	 */
	public List<Orbit> getRequestedOrbits() {
		return requestedOrbits;
	}

	/**
	 * Sets the requested orbits
	 * 
	 * @param requestedOrbits the requestedOrbits to set
	 */
	public void setRequestedOrbits(List<Orbit> requestedOrbits) {
		this.requestedOrbits = requestedOrbits;
	}

	/**
	 * Gets the promised products
	 * 
	 * @return the promisedProducts
	 */
	public Set<Product> getPromisedProducts() {
		return promisedProducts;
	}

	/**
	 * Sets the promised products
	 * 
	 * @param promisedProducts the promisedProducts to set
	 */
	public void setPromisedProducts(Set<Product> promisedProducts) {
		this.promisedProducts = promisedProducts;
	}

	/**
	 * Gets the processing jobs
	 * 
	 * @return the jobs
	 */
	public Set<Job> getJobs() {
		return jobs;
	}

	/**
	 * Sets the processing jobs
	 * 
	 * @param jobs the jobs to set
	 */
	public void setJobs(Set<Job> jobs) {
		this.jobs = jobs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(identifier, mission);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProcessingOrder))
			return false;
		ProcessingOrder other = (ProcessingOrder) obj;
		return Objects.equals(identifier, other.identifier) && Objects.equals(mission, other.mission);
	}
}
