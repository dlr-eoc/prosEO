/**
 * OrderSource.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Possible sources for Processing Orders
 * 
 * @author Dr. Thomas Bassler
 */
public enum OrderSource {
	/** Order created from command-line interface */
	CLI, 
	/** Order created in prosEO GUI */
	GUI, 
	/** Order created from ODIP */
	ODIP, 
	/** Order source unknown (external call to order creation REST API) */
	OTHER
}
