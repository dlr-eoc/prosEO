/**
 * TimeIntervalOrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A trigger that fires in certain time intervals.
 *
 * @since prosEO 2.1.0
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = { 
		@Index(unique = true, columnList = "mission_id,name")})
public class TimeIntervalOrderTrigger extends TimerOrderTrigger {

    /**
     * The interval between two firings of the trigger
     */
    private Duration triggerInterval;
    /**
     * The next date and time, at which the trigger is expected to fire (default setting is trigger creation time + triggerInterval)
     */
    private Instant nextTriggerTime;

    /**
     * Gets the interval between two firings of the trigger
     * 
     * @return the trigger interval
     */
    public Duration getTriggerInterval() {
        return this.triggerInterval;
    }

    /**
     * Sets the interval between two firings of the trigger
     * 
     * @param triggerInterval the trigger interval to set
     */
    public void setTriggerInterval(Duration triggerInterval) {
        this.triggerInterval = triggerInterval;
    }

    /**
     * Gets the next date and time, at which the trigger is expected to fire
     * 
     * @return the next trigger time
     */
    public Instant getNextTriggerTime() {
        return this.nextTriggerTime;
    }

    /**
     * Sets the next date and time, at which the trigger is expected to fire
     * 
     * @param nextTriggerTime the next trigger time to set
     */
    public void setNextTriggerTime(Instant nextTriggerTime) {
        this.nextTriggerTime = nextTriggerTime;
    }

}