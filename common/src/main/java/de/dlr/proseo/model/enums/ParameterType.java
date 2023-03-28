package de.dlr.proseo.model.enums;

/**
 *  Enumeration of valid parameter types for mission-specific parameters
 */

public enum ParameterType {
	/** String parameter */
	STRING, 
	/** Boolean parameter, allowed values "true" and "false" */
	BOOLEAN, 
	/** Integer parameter */
	INTEGER, 
	/** Floating-point parameter with double precision */
	DOUBLE,
	/** ISO-formatted UTC-STS timestamps with microsecond fraction and without time zone */
	INSTANT
}
