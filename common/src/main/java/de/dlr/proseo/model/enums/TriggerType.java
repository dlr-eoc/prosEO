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
	// Instance of DataDrivenOrderTrigger
	DataDriven, 
	// Instance of TimeIntervalOrderTrigger
	TimeInterval, 
	// Instance of CalendarOrderTrigger
	Calendar, 
	// Instance of OrbitOrderTrigger
	Orbit, 
	// Instance of DatatakeOrderTrigger
	Datatake
}
