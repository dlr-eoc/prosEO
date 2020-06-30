/**
 * ProcessingOrder.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ProductionType;

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
@Table(indexes = {
	@Index(unique = true, columnList = "identifier"),
	@Index(unique = true, columnList = "uuid"),
	@Index(unique = false, columnList = "execution_time") 
})
public class ProcessingOrder extends PersistentObject {

	private static final String MSG_SLICING_DURATION_NOT_ALLOWED = "Setting of slicing duration not allowed for slicing type ";

	/** Mission, to which this order belongs */
	@ManyToOne
	private Mission mission;
	
	/** User-defined order identifier */
	@Column(nullable = false)
	private String identifier;
	
	/** The universally unique identifier (UUID) for this order */
	@Column(nullable = false)
	private UUID uuid;
	
	/** State of the processing order */
	@Enumerated(EnumType.STRING)
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
	
	/**
	 * Method for slicing the orbit time interval into jobs for product generation (default "ORBIT")
	 */
	@Enumerated(EnumType.STRING)
	private OrderSlicingType slicingType = OrderSlicingType.ORBIT;
	
	/**
	 * Duration of a time slice for slicing type TIME_SLICE
	 */
	private Duration sliceDuration = null;
	
	/**
	 * Overlap between adjacent time slices, half of the overlap is added at each end of the slice time interval except at the 
	 * beginning of the first and the end of the last time interval.
	 */
	private Duration sliceOverlap = Duration.ZERO;
	
	/**
	 * Filter conditions to apply to input products of a specific product class in addition to filter conditions contained
	 * in the applicable selection rule
	 */
	@ManyToMany
	private Map<ProductClass, InputFilter> inputFilters = new HashMap<>();
	
	/**
	 * Product classes for output products with optional set of parameters to apply to the generated product
	 */
	@ManyToMany
	private Map<ProductClass, ParameterizedOutput> parameterizedOutputs = new HashMap<>();
	
	/** Set of product classes provided as input data (processing job steps must not be generated) */
	@ManyToMany
	private Set<ProductClass> inputProductClasses = new HashSet<>();
	
	/** The file class of the generated output products (from the list of allowed file classes agreed for the mission) */
	private String outputFileClass;
	
	/** The processing mode to run the processor(s) in (one of the modes specified for the mission) */
	private String processingMode;
	
	/** Production type context, in which the order is running */
	@Enumerated(EnumType.STRING)
	private ProductionType productionType = ProductionType.ON_DEMAND_DEFAULT;
	
	/** The processor configurations for processing the products */
	@ManyToMany
	private Set<ConfiguredProcessor> requestedConfiguredProcessors = new HashSet<>();
	
	/** The orbits, for which products are to be generated */
	@ManyToMany
	private List<Orbit> requestedOrbits = new ArrayList<>();
	
	/** The processing jobs belonging to this order */	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "processingOrder")
	private Set<Job> jobs = new HashSet<>();
	
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
	 * Gets the universally unique identifier (UUID)
	 * 
	 * @return the UUID
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Sets the universally unique identifier (UUID)
	 * 
	 * @param uuid the UUID to set
	 */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
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
	 * Gets the method for partitioning the orbit time interval
	 * 
	 * @return the slicingType the order slicing type
	 */
	public OrderSlicingType getSlicingType() {
		return slicingType;
	}

	/**
	 * Sets the method for partitioning the orbit time interval (if the slicing type is TIME_SLICE and the slice duration
	 * has not yet been set, then the slice duration will be set to a default value of one day; for other slicing types
	 * the current slice duration will be deleted)
	 * 
	 * @param slicingType the slicing type to set
	 */
	public void setSlicingType(OrderSlicingType slicingType) {
		this.slicingType = slicingType;
		if (OrderSlicingType.TIME_SLICE.equals(slicingType)) {
			if (null == sliceDuration) {
				sliceDuration = Duration.of(1, ChronoUnit.DAYS);
			}
		} else {
			sliceDuration = null;
		}
	}

	/**
	 * Gets the duration for a single slice
	 * 
	 * @return the slice duration for slicing type TIME_SLICE, null otherwise
	 */
	public Duration getSliceDuration() {
		return sliceDuration;
	}

	/**
	 * Sets the duration for a single slice (for slicing type TIME_SLICE only)
	 * 
	 * @param sliceDuration the sliceDuration to set
	 */
	public void setSliceDuration(Duration sliceDuration) {
		if (OrderSlicingType.TIME_SLICE.equals(slicingType)) {
			this.sliceDuration = sliceDuration;
		} else {
			throw new IllegalStateException(MSG_SLICING_DURATION_NOT_ALLOWED + slicingType);
		}
	}

	/**
	 * Gets the overlap time between slices
	 * 
	 * @return the slice overlap
	 */
	public Duration getSliceOverlap() {
		return sliceOverlap;
	}

	/**
	 * Sets the overlap time between slices
	 * 
	 * @param sliceOverlap the slice overlap to set
	 */
	public void setSliceOverlap(Duration sliceOverlap) {
		this.sliceOverlap = sliceOverlap;
	}

	/**
	 * Gets the input filters
	 * 
	 * @return the input filters
	 */
	public Map<ProductClass, InputFilter> getInputFilters() {
		return inputFilters;
	}

	/**
	 * Sets the input filters
	 * 
	 * @param inputFilters the input filters to set
	 */
	public void setInputFilters(Map<ProductClass, InputFilter> inputFilters) {
		this.inputFilters = inputFilters;
	}

	/**
	 * Gets the parameterized outputs
	 * 
	 * @return the parameterized outputs
	 */
	public Map<ProductClass, ParameterizedOutput> getParameterizedOutputs() {
		return parameterizedOutputs;
	}

	/**
	 * Sets the parameterized outputs
	 * 
	 * @param parameterizedOutputs the parameterized outputs to set
	 */
	public void setParameterizedOutputs(Map<ProductClass, ParameterizedOutput> parameterizedOutputs) {
		this.parameterizedOutputs = parameterizedOutputs;
	}

	/**
	 * Gets the input product classes provided to the order
	 * 
	 * @return the input product classes
	 */
	public Set<ProductClass> getInputProductClasses() {
		return inputProductClasses;
	}

	/**
	 * Sets the input product classes provided to the order
	 * 
	 * @param inputProductClasses the input product classes to set
	 */
	public void setInputProductClasses(Set<ProductClass> inputProductClasses) {
		this.inputProductClasses = inputProductClasses;
	}

	/**
	 * Gets the file class of the output products
	 * 
	 * @return the outputFileClass
	 */
	public String getOutputFileClass() {
		return outputFileClass;
	}

	/**
	 * Sets the file class for the output products
	 * 
	 * @param outputFileClass the outputFileClass to set
	 */
	public void setOutputFileClass(String outputFileClass) {
		this.outputFileClass = outputFileClass;
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
	 * Gets the production type context
	 * 
	 * @return the production type
	 */
	public ProductionType getProductionType() {
		return productionType;
	}

	/**
	 * Sets the production type context
	 * 
	 * @param productionType the production type to set
	 */
	public void setProductionType(ProductionType productionType) {
		this.productionType = productionType;
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

	@Override
	public String toString() {
		return "ProcessingOrder [mission=" + (null == mission ? "null" : mission.getCode()) + ", identifier=" + identifier 
				+ ", orderState=" + orderState + ", executionTime=" + executionTime
				+ ", startTime=" + startTime + ", stopTime=" + stopTime + ", slicingType=" + slicingType + ", sliceDuration="
				+ sliceDuration + ", inputFilters=" + inputFilters + ", parameterizedOutputs=" + parameterizedOutputs
				+ ", processingMode=" + processingMode + "]";
	}
}
