/**
 * OrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
import java.util.Objects;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * Base class for all order generation triggers
 * 
 * @since prosEO 2.1.0
 * 
 * @author Dr. Thomas Bassler
 */
@MappedSuperclass
public abstract class OrderTrigger extends PersistentObject {
	
	/**
	 * The mission this trigger belongs to
	 */
	@ManyToOne
	private Mission mission;

    /**
     * An identifier for the trigger (unique within the mission)
     */
    private String name;
    
	/** Flag indicating whether this trigger is available for use (disabled triggers cannot be used for order generation) */
	private Boolean enabled;
	
    /**
     * The order template associated with this trigger
     */
	@ManyToOne
    private OrderTemplate orderTemplate;

    /**
     * Delay between order generation time (= trigger firing time) and earliest order execution (release) time; default 0
     */
    private Duration executionDelay = Duration.ZERO;
    
    /**
     * Priority to be set for generated processing orders; if set, overrides priority of associated workflow
     */
    private Integer priority;
    
	/**
	 * Gets the mission this trigger belongs to
	 * 
	 * @return the mission
	 */
	public Mission getMission() {
		return mission;
	}

	/**
	 * Sets the mission this trigger belongs to
	 * 
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}

    /**
     * Gets the trigger name
     * 
     * @return the trigger name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the trigger name
     * 
     * @param name the trigger name to set
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
	 * Gets the associated order template
	 * 
	 * @return the order template
	 */
	public OrderTemplate getOrderTemplate() {
		return orderTemplate;
	}

	/**
	 * Sets the associated order template
	 * 
	 * @param orderTemplate the order template to set
	 */
	public void setOrderTemplate(OrderTemplate orderTemplate) {
		this.orderTemplate = orderTemplate;
	}

    /**
     * Gets the processing order execution delay relative to the trigger firing time
     * 
     * @return the order execution delay
     */
    public Duration getExecutionDelay() {
        return this.executionDelay;
    }

    /**
     * Sets the processing order execution delay relative to the trigger firing time
     * 
     * @param executionDelay the order execution delay to set
     */
    public void setExecutionDelay(Duration executionDelay) {
        this.executionDelay = executionDelay;
    }

    /**
     * Gets the processing order execution priority
     * 
     * @return the order execution priority
     */
    public Integer getPriority() {
        return this.priority;
    }

    /**
     * Sets the processing order execution priority
     * 
     * @param priority the order execution priority to set
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

	@Override
	public int hashCode() {
		return Objects.hash(name); // same trigger name in different missions unlikely
	}

	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof OrderTrigger))
			return false;
		OrderTrigger other = (OrderTrigger) obj;
		return Objects.equals(name, other.getName()) && Objects.equals(mission, other.getMission());
	}

	@Override
	public String toString() {
		return "OrderTrigger [mission=" + (null == mission ? "null" : mission.getCode()) + ", name=" + name
				+ ", orderTemplate=" + (null == orderTemplate ? "null" : orderTemplate.getName()) 
				+ ", executionDelay=" + executionDelay + ", priority=" + priority + "]";
	}

}