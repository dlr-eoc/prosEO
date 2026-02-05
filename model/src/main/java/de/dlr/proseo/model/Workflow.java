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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
public class Workflow extends OrderTemplateBase {
	
	/** The unique identifier of the workflow */
	// TODO Re-test column definition after migration to Spring Boot 3 / Hibernate 6 and remove if possible
	@Column(nullable = false, columnDefinition = "uuid")
	private UUID uuid;
	
	/**
	 * Textual description of the workflow, including details of the processor version and configuration applicable;
	 * default value is an empty string.
	 */
	private String description = "";
	
	/** Version number applicable to the workflow */
	@Column(name = "workflow_version", nullable = false) // For whatever reason the column is not found during index creation unless explicitly specified
	private String workflowVersion;
	
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
	
	/** Options, which can be set as "Dynamic Processing Parameters" in Job Orders generated from this workflow */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "workflow")
	private Set<WorkflowOption> workflowOptions = new HashSet<>();
	
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

	@Override
	public int hashCode() {
		return Objects.hash(uuid); // same UUID for different workflows extremely unlikely
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
		return "Workflow [ " + super.toString() + ", uuid=" + uuid + ", description=" + description 
				+ ", workflowVersion=" + workflowVersion 
				+ ", inputProductClass=" + (null == inputProductClass ? "null" : inputProductClass.getProductType())
				+ ", outputProductClass=" + (null == outputProductClass ? "null" : outputProductClass.getProductType())
				+ ", configuredProcessor=" + (null == configuredProcessor ? "null" : configuredProcessor.getIdentifier())
				+ ", workflowOptions=" + workflowOptions + "]";
	}

	
}
