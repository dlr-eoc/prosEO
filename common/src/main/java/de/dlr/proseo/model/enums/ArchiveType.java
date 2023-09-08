/**
 * ArchiveType.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Interface protocol to use for a Product Archive
 * 
 * @author Dr. Thomas Bassler
 *
 */
public enum ArchiveType {
	/** Archive Interface Point (see "CSC – Long Term Archive Interface Control Document", ESA-EOPG-EOPGC-IF-2) */
	AIP,
	/** Auxiliary Data Interface Point (see "CSC – Auxiliary Data Interface Delivery Point Specification", ESA-EOPG-EOPGC-IF-10) */
	AUXIP,
	/** Precise Orbit Determination Interface Delivery Point (see AUXIP for reference) */
	PODIP,
	/** Production Service Interface Point (see "CSC – Production Interface Delivery Point Specification", ESA-EOPG-EOPGC-IF-3) */
	PRIP,
	/** Simple (attributes are not indexed in database) Archive Interface Point (see "CSC – Long Term Archive Interface Control Document", ESA-EOPG-EOPGC-IF-2) */
	SIMPLEAIP
}
