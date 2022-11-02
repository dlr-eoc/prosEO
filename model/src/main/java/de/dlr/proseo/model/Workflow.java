/**
 * Workflow.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

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
	@Index(unique = true, columnList = "name"),
	@Index(unique = true, columnList = "uuid") 
})
public class Workflow extends PersistentObject {

	/** The unique identifier of the workflow */
	@Column(nullable = false)
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
	private List<WorkflowOption> workflowOptions = new ArrayList<>();

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
	public List<WorkflowOption> getWorkflowOptions() {
		return workflowOptions;
	}

	/**
	 * Sets the available workflow options
	 * 
	 * @param workflowOptions the workflow options to set
	 */
	public void setWorkflowOptions(List<WorkflowOption> workflowOptions) {
		this.workflowOptions = workflowOptions;
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
				+ ", workflowOptions=" + workflowOptions + "]";
	}
	
}
