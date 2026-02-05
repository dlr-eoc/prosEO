/**
 * DatatakeOrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.*;

import javax.persistence.Column;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Trigger to create a processing order for an expected datatake.
 * 
 * @since prosEO 2.1.0
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = { 
		@Index(unique = true, columnList = "mission_id,name")})
public class DatatakeOrderTrigger extends MissionPlanningOrderTrigger {

    /**
     * Type of datatake as annotated in Mission Planning files (details TBD)
     */
    private String datatakeType;
    
    /**
     * Start time of the last datatake, for which a processing order was generated
     */
	@Column(name = "last_datatake_start_time", columnDefinition = "TIMESTAMP(6)")
    private Instant lastDatatakeStartTime;
	
    /**
     * Set of keys for datatake parameters/attributes to copy as output parameters to the processing order
     */
	@ElementCollection
    private Set<String> parametersToCopy = new HashSet<>();

    /**
     * Gets the datatake type
     * 
     * @return the datatake type
     */
    public String getDatatakeType() {
        return this.datatakeType;
    }

    /**
     * Sets the datatake type
     * 
     * @param datatakeType the datatake type to set
     */
    public void setDatatakeType(String datatakeType) {
        this.datatakeType = datatakeType;
    }

    /**
     * Gets the last datatake start time
     * 
     * @return the last datatake start time
     */
    public Instant getLastDatatakeStartTime() {
        return this.lastDatatakeStartTime;
    }

    /**
     * Sets the last datatake start time
     * 
     * @param lastDatatakeStartTime the last datatake start time to set
     */
    public void setLastDatatakeStartTime(Instant lastDatatakeStartTime) {
        this.lastDatatakeStartTime = lastDatatakeStartTime;
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