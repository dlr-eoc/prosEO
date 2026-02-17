/**
 * DataDrivenOrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A trigger that fires upon ingestion of a product fulfilling certain criteria (match of product class, file class [if set] 
 * and/or processing mode [if set]); trigger events may be sent by the Ingestor or by Monitors for non-prosEO pickup points 
 * (local or remote)
 * 
 * @since prosEO 2.1.0
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = { 
		@Index(unique = true, columnList = "mission_id,name")})
public class DataDrivenOrderTrigger extends OrderTrigger {
	
	/** Product class of the input product */
	@ManyToOne
	private ProductClass inputProductClass;

	/** The file class of the input product (optional) */
	private String inputFileClass;
	
	/** The processing mode of the input product (optional) */
	private String inputProcessingMode;
	
    /**
     * Set of keys for the trigger product parameters, which shall be copied to the output product(s) of the generated 
     * processing order; a single entry "*" means "copy all parameters"
     */
	@ElementCollection
    private Set<String> parametersToCopy = new HashSet<>();

    /**
     * Gets the required product class for the input product
     * 
	 * @return the product class of the input product
	 */
	public ProductClass getInputProductClass() {
		return inputProductClass;
	}

	/**
     * Sets the required product class for the input product
     * 
	 * @param inputProductClass the product class of the input product to set
	 */
	public void setInputProductClass(ProductClass inputProductClass) {
		this.inputProductClass = inputProductClass;
	}

	/**
     * Gets the required file class for the input product
     * 
	 * @return the file class for the input product (may be null)
	 */
	public String getInputFileClass() {
		return inputFileClass;
	}

	/**
     * Sets the required file class for the input product
     * 
	 * @param inputFileClass the file class for the input product to set
	 */
	public void setInputFileClass(String inputFileClass) {
		this.inputFileClass = inputFileClass;
	}

	/**
     * Gets the required processing mode for the input product
     * 
	 * @return the processing mode for the input product (may be null)
	 */
	public String getInputProcessingMode() {
		return inputProcessingMode;
	}

	/**
     * Sets the required processing mode for the input product
     * 
	 * @param inputProcessingMode the processing mode for the input product to set
	 */
	public void setInputProcessingMode(String inputProcessingMode) {
		this.inputProcessingMode = inputProcessingMode;
	}

	/**
     * Gets the set of keys for the trigger product parameters
     * 
     * @return the list of parameter keys
     */
    public Set<String> getParametersToCopy() {
        return this.parametersToCopy;
    }

    /**
     * Sets the set of keys for the trigger product parameters
     * 
     * @param parametersToCopy the set of parameter keys to set
     */
    public void setParametersToCopy(Set<String> parametersToCopy) {
        this.parametersToCopy = parametersToCopy;
    }

}