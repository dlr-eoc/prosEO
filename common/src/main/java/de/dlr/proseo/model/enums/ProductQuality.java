/**
 * ProductQuality.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Quality annotation for products (determined by processor configuration)
 * 
 * @author Dr. Thomas Bassler
 * 
 */
public enum ProductQuality {
	/** Products with nominal (production, operational) quality */
	NOMINAL,
	/** Products created with an experimental configuration, possibly not suitable for general use */
	EXPERIMENTAL,
	/** Products generated for test use only, usually not suited for general use */
	TEST,
	/** Products with nominal (production, operational) quality, generated from systematic processing */
	SYSTEMATIC
}
