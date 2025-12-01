/**
 * TimerOrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import jakarta.persistence.MappedSuperclass;

/**
 * Base class for triggers which fire upon reaching a certain point in time
 * @author Dr. Thomas Bassler
 */
@MappedSuperclass
public abstract class TimerOrderTrigger extends OrderTrigger {
}