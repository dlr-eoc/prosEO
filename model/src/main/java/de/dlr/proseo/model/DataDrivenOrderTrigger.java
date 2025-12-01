/**
 * DataDrivenOrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Set;

import jakarta.persistence.Entity;

/**
 * A trigger that fires upon ingestion of a product fulfilling certain criteria (match of product class of associated workflow, optionally match of its file class and/or processing mode); trigger events may be sent by the Ingestor or by Monitors for non-prosEO pickup points (local or remote)
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
public class DataDrivenOrderTrigger extends OrderTrigger {

    /**
     * Set of keys for the trigger product parameters, which shall be copied to the output product(s) of the generated processing order; a single entry "*" means "copy all parameters"
     */
    private Set<String> parametersToCopy;

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