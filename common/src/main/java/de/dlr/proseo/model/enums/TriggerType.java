/**
 * TriggerType.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Trigger types 
 * 
 * @author Ernst Melchinger
 * 
 */
public enum TriggerType {
	// Instance of CalendarOrderTrigger
	Calendar, 
	// Instance of DataDrivenOrderTrigger
	DataDriven, 
	// Instance of DatatakeOrderTrigger
	Datatake, 
	// Instance of OrbitOrderTrigger
	Orbit, 
	// Instance of TimeIntervalOrderTrigger
	TimeInterval
}
