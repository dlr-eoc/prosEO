/**
 * UserRole.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * prosEO User Roles (low-level roles, which can be bundled for user groups)
 * 
 * @author Dr. Thomas Bassler
 */
public enum UserRole {

	// General roles
	
	/** Root user, intended for mission creation/deletion and for creating the first user of a mission */
	ROOT,
	
	/** User with command line access */
	CLI_USER,
	
	/** User with GUI access */
	GUI_USER,
	
	/** User with PRIP API access */
	PRIP_USER,
	
	
	// User management roles
	
	/** A user role intended for managing users and groups and assigning authorities to them */
	USERMGR,
	
	
	// Mission management roles
	
	/** Read access to missions, spacecrafts and orbits */
	MISSION_READER,
	
	/** Update access to missions, spacecrafts and orbits */
	MISSION_MGR,
	
	
	// Product managment roles
	
	/** Read access to product classes and selection rules */
	PRODUCTCLASS_READER,
	
	/** Create, update and delete access to product classes and selection rules */
	PRODUCTCLASS_MGR,
	
	/** Query and download public products */
	PRODUCT_READER,
	
	/** Query and download public and restricted products */
	PRODUCT_READER_RESTRICTED,
	
	/** Query and download all products */
	PRODUCT_READER_ALL,
	
	/** Upload products from external source */
	PRODUCT_INGESTOR,
	
	/** Upload products from internal source */
	PRODUCT_GENERATOR,
	
	/** Update and delete products and product files */
	PRODUCT_MGR,
	
	
	// Processor management roles
	
	/** Read access to processor classes, processors, configurations, configured processors and any sub-objects of them */
	PROCESSOR_READER,
	
	/** Create, update and delete access to processor classes, processors and tasks */
	PROCESSORCLASS_MGR,
	
	/** Create, update and delete access to configurations and configured processors */
	CONFIGURATION_MGR,
	
	
	// Processing facility management roles
	
	/** Read access to processing facilities */
	FACILITY_READER,
	
	/** Create, update and delete access to processing facilities */
	FACILITY_MGR,
	
	/** Read access to facility monitoring data */
	FACILITY_MONITOR,
	
	
	// Order management roles
	
	/** Read access to processing order, jobs and job steps */
	ORDER_READER,
	
	/** Create, update, close and delete orders */
	ORDER_MGR,
	
	/** Approve orders */
	ORDER_APPROVER,
	
	/** Plan, release, suspend, cancel and retry orders, jobs and job steps */
	ORDER_PLANNER,
	
	/** Read access to order monitoring data */
	ORDER_MONITOR;
	
	/**
	 * Get the role name with "ROLE_" prefix as stored in the database
	 * 
	 * @return "ROLE_" + the role name
	 */
	public String asRoleString() { return "ROLE_" + this.toString(); }
}
