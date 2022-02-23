/**
 * JobOrderVersion.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Variants of Job Order file specifications: Over time the specification for Job Order files evolves, but processors may rely on
 * older versions of the specification. Also, branches (non-linear evolution of the specification) have been observed.
 * By choosing one of the enumeration values the syntax for the generation of Job Order files from the Job Order classes can be
 * configured.
 * 
 * @author Thomas Bassler
 *
 */
public enum JobOrderVersion {
	/**
	 * Syntax according to "Generic IPF Interface Specifications", MMFI-GSEG-EOPG-TN-07-0003, issue 1.8;
	 * identical to "GMES Generic PDGS-IPF Interface Specifications", GMES-GSEG-EOPG-TN-09-0016, issue 1.0
	 */
	MMFI_1_8,
	/**
	 * Syntax according to "GMES Generic PDGS-IPF Interface Specifications", GMES-GSEG-EOPG-TN-09-0016, issue 1.1 
	 */
	GMES_1_1
}
