/**
 * CalendarOrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A cron-style trigger, which is tied to specific times of day/week/month.
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = { 
		@Index(unique = true, columnList = "mission_id,name")})
public class CalendarOrderTrigger extends TimerOrderTrigger {

    /**
     * A cron-style expression to schedule trigger firing
     */
    private String cronExpression;

    /**
     * Gets the cron expression for trigger scheduling
     * 
     * @return the cron expression
     */
    public String getCronExpression() {
        return this.cronExpression;
    }

    /**
     * Sets the cron expression for trigger scheduling
     * 
     * @param cronExpression the cron expression to set
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

}