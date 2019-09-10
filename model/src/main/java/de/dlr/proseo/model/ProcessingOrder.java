/**
 * ProcessingOrder.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

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
@Table(indexes = { @Index(unique = true, columnList = "identifier"), @Index(unique = false, columnList = "execution_time") })
public class ProcessingOrder extends PersistentObject {

	/** Mission, to which this order belongs */
	@ManyToOne
	private Mission mission;
	
	/** User-defined order identifier */
	private String identifier;
	
	/** State of the processing order */
	private OrderState orderState;
	
	/** Expected execution time (optional, used for scheduling) */
	@Column(name = "execution_time", columnDefinition = "TIMESTAMP")
	private Instant executionTime;
	
	/**
	 * The start time of the time interval to process. If a range of orbit numbers is given, this time is set to the earliest
	 * start time of the selected orbits.
	 */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant startTime;
	
	/**
	 * The end time of the time interval to process. If a range of orbit numbers is given, this time is set to the latest
	 * stop time of the selected orbits.
	 */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant stopTime;
	
	/** A set of additional conditions to apply to selected products.
	 * Note: For Sentnel-5P at least the parameters "copernicusCollection", "fileClass" and "revision" are required. */
	@ElementCollection
	private Map<String, Parameter> filterConditions = new HashMap<>();
	
	/** A set of parameters to set for the generated products.
	 * Note: For Sentnel-5P at least the parameters "copernicusCollection", "fileClass" and "revision" are required.
	 */
	@ElementCollection
	private Map<String, Parameter> outputParameters = new HashMap<>();
	
	/** Set of requested product classes */
	@ManyToMany
	private Set<ProductClass> requestedProductClasses = new HashSet<>();
	
	/** The processing mode to run the processor(s) in (one of the modes specified for the mission) */
	private String processingMode;
	
	/** The processor configurations for processing the products */
	@ManyToMany
	private Set<ConfiguredProcessor> requestedConfiguredProcessors = new HashSet<>();
	
	/** The orbits, for which products are to be generated */
	@ManyToMany
	private List<Orbit> requestedOrbits = new ArrayList<>();
	
	/** The products, which will provided as input */
	@ManyToMany
	private Set<Product> promisedProducts = new HashSet<>();
	
	/** The processing jobs belonging to this order */	
	@OneToMany(mappedBy = "processingOrder")
	private Set<Job> jobs = new HashSet<>();
	
	/**
	 * Possible states for a processing order; recommended state transitions:
	 * <ol>
	 * <li>INITIAL -&gt; RELEASED: Customer approved order parameters and/or committed budget</li>
	 * <li>RELEASED -&gt; PLANNED: Jobs for the processing order have been generated</li>
	 * <li>PLANNED -&gt; RUNNING: The first jobs have started, further jobs can be started</li>
	 * <li>RUNNING -&gt; PLANNED: Order execution halted, no further jobs will be started (started jobs will be completed,
	 *     if they are not halted themselves)</li>
	 * <li>RUNNING -&gt; COMPLETED: All jobs have been completed successfully</li>
	 * <li>RUNNING -&gt; FAILED: All jobs have been completed, but at least one of them failed</li>
	 * <li>COMPLETED/FAILED -&gt; CLOSED: Delivery has been acknowledged by customer and/or order fee has been paid</li>
	 * </ol>
	 */
	public enum OrderState { INITIAL, RELEASED, PLANNED, RUNNING, COMPLETED, FAILED, CLOSED };

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
	 * Gets the state of the processing order
	 * 
	 * @return the orderState
	 */
	public OrderState getOrderState() {
		return orderState;
	}

	/**
	 * Sets the state of the processing order
	 * 
	 * @param orderState the orderState to set
	 */
	public void setOrderState(OrderState orderState) {
		this.orderState = orderState;
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
	 * Gets the (earliest) start time of the processing time interval
	 * 
	 * @return the startTime
	 */
	public Instant getStartTime() {
		return startTime;
	}

	/**
	 * Sets the (earliest) start time of the processing time interval
	 * 
	 * @param startTime the startTime to set
	 */
	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	/**
	 * Gets the (latest) stop time of the processing time interval
	 * 
	 * @return the stopTime
	 */
	public Instant getStopTime() {
		return stopTime;
	}

	/**
	 * Sets the (latest) stop time of the processing time interval
	 * 
	 * @param stopTime the stopTime to set
	 */
	public void setStopTime(Instant stopTime) {
		this.stopTime = stopTime;
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
	 * Gets the processing mode for the processors
	 * 
	 * @return the processingMode
	 */
	public String getProcessingMode() {
		return processingMode;
	}

	/**
	 * Sets the processing mode for the processors
	 * 
	 * @param processingMode the processingMode to set
	 */
	public void setProcessingMode(String processingMode) {
		this.processingMode = processingMode;
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
