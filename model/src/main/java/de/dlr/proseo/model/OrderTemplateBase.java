/**
 * OrderTemplateBase.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.dlr.proseo.model.enums.OrderSlicingType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * Abstract base class for classes serving as template for the generation of processing orders.
 * This base class defines common attributes for its subclasses.
 * 
 * @since prosEO 2.1.0
 * 
 * @author Dr. Thomas Bassler
 */
@MappedSuperclass
public abstract class OrderTemplateBase extends PersistentObject {
	
	/**
	 * The mission this template base belongs to
	 */
	@ManyToOne
	private Mission mission;

	/** A short name for the workflow */
	@Column(nullable = false)
	private String name;
	
	/** Flag indicating whether this template is available for use (disabled templates cannot be used for order generation) */
	private Boolean enabled;
	
	/**
	 * Priority of the ProcessingOrder (lower number means lower priority; value range 1..100 is defined for the ODIP,
	 * but other values are allowed outside On-Demand Production, including negative numbers). Default value is 50.
	 */
	private Integer priority = 50;
	
	/** The file class of the generated output products (from the list of allowed file classes agreed for the mission) */
	private String outputFileClass;
	
	/** The processing mode to run the processor(s) in (one of the modes specified for the mission) */
	private String processingMode;
	
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
	 * Overlap between adjacent time slices, half of the overlap is added at each end of the slice time interval
	 */
	private Duration sliceOverlap = Duration.ZERO;
	
	/**
	 * Filter conditions to apply to input products of a specific product class in addition to filter conditions contained
	 * in the applicable selection rule
	 */
	@ManyToMany
	private Map<ProductClass, InputFilter> inputFilters = new HashMap<>();
	
	/**
	 * Set of parameters to apply to a generated product of the referenced product class replacing the general output parameters
	 */
	@ManyToMany
	private Map<ProductClass, ClassOutputParameter> classOutputParameters = new HashMap<>();
	
	/**
	 * Parameters to set for the generated products
	 */
	@ElementCollection
	private Map<String, Parameter> outputParameters = new HashMap<>();
	
	/**
	 * Gets the mission this template base belongs to
	 * 
	 * @return the mission
	 */
	public Mission getMission() {
		return mission;
	}

	/**
	 * Sets the mission this template base belongs to
	 * 
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}

	/**
	 * Gets the template name
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the template name
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the status of the "enabled" flag
	 * 
	 * @return the enabled flag
	 */
	public Boolean getEnabled() {
		return enabled;
	}

	/**
	 * Indicates, whether the workflow is available for use
	 * 
	 * @return the enabled flag
	 */
	public Boolean isEnabled() {
		return getEnabled();
	}

	/**
	 * Sets the status of the "enabled" flag
	 * 
	 * @param enabled the status of the enabled flag to set
	 */
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Gets the priority value for scheduling
	 * 
	 * @return the priority
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * Sets the priority value for scheduling
	 * 
	 * @param priority the priority to set
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * Gets the output file class
	 * 
	 * @return the output file class
	 */
	public String getOutputFileClass() {
		return outputFileClass;
	}

	/**
	 * Sets the output file class
	 * 
	 * @param outputFileClass the output file class to set
	 */
	public void setOutputFileClass(String outputFileClass) {
		this.outputFileClass = outputFileClass;
	}

	/**
	 * Gets the processing mode
	 * 
	 * @return the processing mode
	 */
	public String getProcessingMode() {
		return processingMode;
	}

	/**
	 * Sets the processing mode
	 * 
	 * @param processingMode the processing mode to set
	 */
	public void setProcessingMode(String processingMode) {
		this.processingMode = processingMode;
	}

	/**
	 * Gets the order slicing type
	 * 
	 * @return the slicing type
	 */
	public OrderSlicingType getSlicingType() {
		return slicingType;
	}

	/**
	 * Sets the order slicing type
	 * 
	 * @param slicingType the slicing type to set
	 */
	public void setSlicingType(OrderSlicingType slicingType) {
		this.slicingType = slicingType;
	}

	/**
	 * Gets the slice duration for sliced orders
	 * 
	 * @return the slice duration
	 */
	public Duration getSliceDuration() {
		return sliceDuration;
	}

	/**
	 * Sets the slice duration for sliced orders
	 * 
	 * @param sliceDuration the slice duration to set
	 */
	public void setSliceDuration(Duration sliceDuration) {
		this.sliceDuration = sliceDuration;
	}

	/**
	 * Gets the overlap between adjacent slices
	 * 
	 * @return the slice overlap
	 */
	public Duration getSliceOverlap() {
		return sliceOverlap;
	}

	/**
	 * Sets the overlap between adjacent slices
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
	 * Gets the class-specific output parameters
	 * 
	 * @return the class-specific output parameters
	 */
	public Map<ProductClass, ClassOutputParameter> getClassOutputParameters() {
		return classOutputParameters;
	}

	/**
	 * Sets the class-specific output parameters
	 * 
	 * @param classOutputParameters the class-specific output parameters to set
	 */
	public void setClassOutputParameters(Map<ProductClass, ClassOutputParameter> classOutputParameters) {
		this.classOutputParameters = classOutputParameters;
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
	 * @param outputParameters the output parameters to set
	 */
	public void setOutputParameters(Map<String, Parameter> outputParameters) {
		this.outputParameters = outputParameters;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name); // same template name in different missions unlikely
	}

	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof OrderTemplateBase))
			return false;
		OrderTemplateBase other = (OrderTemplateBase) obj;
		return Objects.equals(name, other.getName()) && Objects.equals(mission, other.getMission());
	}

	@Override
	public String toString() {
		return "OrderTemplateBase [mission=" + mission + ", name=" + name + ", priority=" + priority 
				+ ", outputFileClass=" + outputFileClass + ", processingMode=" + processingMode 
				+ ", slicingType=" + slicingType + ", sliceDuration=" + sliceDuration + ", sliceOverlap=" + sliceOverlap 
				+ ", inputFilters=" + inputFilters + ", classOutputParameters=" + classOutputParameters 
				+ ", outputParameters=" + outputParameters + "]";
	}


}