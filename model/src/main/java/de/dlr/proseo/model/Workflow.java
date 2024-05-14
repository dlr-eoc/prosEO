/**
 * Workflow.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
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

/**
 * A Workflow allows to select a suitable ConfiguredProcessor based on a given input product type
 * (and possibly also an output product type).
 * 
 * This class must not be confused with the generic concept of a "workflow" in the sense of chained processing steps,
 * since such a concept does not exist in prosEO. Its sole purpose is to support the implementation of ESA's
 * On-Demand Production Interface Delivery Point (ODPRIP) API.
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = {
	@Index(unique = true, columnList = "uuid"),
	@Index(unique = true, columnList = "mission_id,name,workflow_version")
})
public class Workflow extends PersistentObject {
	
	/** The mission this workflow belongs to */
	@ManyToOne
	private Mission mission;

	/** The unique identifier of the workflow */
	// TODO Re-test column definition after migration to Spring Boot 3 / Hibernate 6 and remove if possible
	@Column(nullable = false, columnDefinition = "uuid")
	private UUID uuid;
	
	/** A short name for the workflow */
	@Column(nullable = false)
	private String name;
	
	/**
	 * Textual description of the workflow, including details of the processor version and configuration applicable;
	 * default value is an empty string.
	 */
	private String description = "";
	
	/** Version number applicable to the workflow */
	@Column(name = "workflow_version", nullable = false) // For whatever reason the column is not found during index creation unless explicitly specified
	private String workflowVersion;
	
	/** Flag indicating whether this workflow is available for use (disabled workflows are not visible on the ODIP) */
	private Boolean enabled;
	
	/**
	 * The (primary) ProductClass used as input for the workflow (note that depending on the selection rules for the
	 * output ProductClass additional ProductClasses may be used as input, and it is not even guaranteed that the named
	 * input ProductClass will be used at all, although this is considered a misconfiguration)
	 */
	@ManyToOne
	private ProductClass inputProductClass;
	
	/** The ProductClass of the (main) output product */
	@ManyToOne
	private ProductClass outputProductClass;
	
	/** Configured processor implementing this workflow */
	@ManyToOne
	private ConfiguredProcessor configuredProcessor;
	
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
	
	/** Options, which can be set as "Dynamic Processing Parameters" in Job Orders generated from this workflow */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "workflow")
	private Set<WorkflowOption> workflowOptions = new HashSet<>();
	
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
	 * Gets the mission this workflow belongs to
	 * 
	 * @return the enclosing mission
	 */
	public Mission getMission() {
		return mission;
	}

	/**
	 * Sets the mission this workflow belongs to
	 * 
	 * @param mission the enclosing mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}

	/**
	 * Gets the workflow UUID
	 * 
	 * @return the UUID
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Sets the workflow UUID
	 * 
	 * @param uuid the UUID to set
	 */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * Gets the workflow name
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the workflow name
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the workflow description
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the workflow description
	 * 
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the workflow version
	 * 
	 * @return the workflow version
	 */
	public String getWorkflowVersion() {
		return workflowVersion;
	}

	/**
	 * Sets the workflow version
	 * 
	 * @param workflowVersion the workflow version to set
	 */
	public void setWorkflowVersion(String workflowVersion) {
		this.workflowVersion = workflowVersion;
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
	 * Gets the input product class
	 * 
	 * @return the input product class
	 */
	public ProductClass getInputProductClass() {
		return inputProductClass;
	}

	/**
	 * Sets the input product class
	 * 
	 * @param inputProductClass the input product class to set
	 */
	public void setInputProductClass(ProductClass inputProductClass) {
		this.inputProductClass = inputProductClass;
	}

	/**
	 * Gets the output product class
	 * 
	 * @return the output product class
	 */
	public ProductClass getOutputProductClass() {
		return outputProductClass;
	}

	/**
	 * Sets the output product class
	 * 
	 * @param outputProductClass the output product class to set
	 */
	public void setOutputProductClass(ProductClass outputProductClass) {
		this.outputProductClass = outputProductClass;
	}

	/**
	 * Gets the configured processor for this workflow
	 * 
	 * @return the configured processor
	 */
	public ConfiguredProcessor getConfiguredProcessor() {
		return configuredProcessor;
	}

	/**
	 * Sets the configured processor for this workflow
	 * 
	 * @param configuredProcessor the configured processor to set
	 */
	public void setConfiguredProcessor(ConfiguredProcessor configuredProcessor) {
		this.configuredProcessor = configuredProcessor;
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
	 * Gets the available workflow options
	 * 
	 * @return the workflow options
	 */
	public Set<WorkflowOption> getWorkflowOptions() {
		return workflowOptions;
	}

	/**
	 * Sets the available workflow options
	 * 
	 * @param workflowOptions the workflow options to set
	 */
	public void setWorkflowOptions(Set<WorkflowOption> workflowOptions) {
		this.workflowOptions = workflowOptions;
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
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(uuid);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Workflow))
			return false;
		Workflow other = (Workflow) obj;
		return Objects.equals(uuid, other.getUuid());
	}

	@Override
	public String toString() {
		return "Workflow [uuid=" + uuid + ", name=" + name + ", description=" + description + ", workflowVersion=" + workflowVersion
				+ ", inputProductClass=" + (null == inputProductClass ? "null" : inputProductClass.getProductType()) 
				+ ", outputProductClass=" + (null == outputProductClass ? "null" : outputProductClass.getProductType())
				+ ", configuredProcessor=" + (null == configuredProcessor ? "null" : configuredProcessor.getIdentifier())
				+ ", outputFileClass=" + outputFileClass + ", processingMode=" + processingMode + ", slicingType=" + slicingType
				+ ", sliceDuration=" + sliceDuration + ", sliceOverlap=" + sliceOverlap
				+ ", inputFilters=" + inputFilters + ", workflowOptions=" + workflowOptions 
				+ ", classOutputParameters=" + classOutputParameters + ", outputParameters=" + outputParameters
				+ "]";
	}
	
}
