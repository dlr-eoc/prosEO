/**
 * ProductVisibility.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Visibility of products to external users (internally all products are visible at all times)
 * 
 * @author Dr. Thomas Bassler
 * 
 */
public enum ProductVisibility {
	/** Products of this class are not visible on external APIs */
	INTERNAL,
	/** Products of this class are only visible to specially authorized users on external APIs */
	RESTRICTED,
	/** Products of this class are visible to all users on external APIs */
	PUBLIC
}
