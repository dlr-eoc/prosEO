/**
 * MissionPlanningOrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;

import jakarta.persistence.Entity;

/**
 * Base class for triggers which fire upon reaching start time of a mission planning object plus a given deltaTime (may be negative, which means that the event fires before the start time of the object)
 * @author Dr. Thomas Bassler
 */
@Entity
public abstract class MissionPlanningOrderTrigger extends OrderTrigger {

    /**
     * Time offset for the trigger to fire, relative to the mission planning object's start time (may be negative, which means that the event fires before the start time of the object)
     */
    private Duration deltaTime = Duration.ZERO;

    /**
     * Gets the time offset for the trigger to fire
     * 
     * @return the time offset relative to the mission planning object's start time
     */
    public Duration getDeltaTime() {
        return this.deltaTime;
    }

    /**
     * Sets the time offset for the trigger to fire
     * 
     * @param deltaTime the time offset relative to the mission planning object's start time to set
     */
    public void setDeltaTime(Duration deltaTime) {
        this.deltaTime = deltaTime;
    }

}