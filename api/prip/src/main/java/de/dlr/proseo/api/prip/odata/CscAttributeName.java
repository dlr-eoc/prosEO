/**
 * CscAttributeName.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.util.HashMap;
import java.util.Map;

/**
 * Allowed attribute names for a product (from the ESA S5P Product Attributes Mapping, issue 1.2, Jan 2021)
 * 
 * @author Dr. Thomas Bassler
 * 
 */
public enum CscAttributeName {
	
	// OGC JSON attributes
	
	/** Platform short name (fixed value 'SENTINEL-5P') */
	PLATFORM_SHORT_NAME("platformShortName"),
	/** Instrument short name */
	INSTRUMENT_SHORT_NAME("instrumentShortName"),
	/** Processing level */
	PROCESSING_LEVEL("processingLevel"),
	/** Product validity start time */
	BEGINNING_DATE_TIME("beginningDateTime"),
	/** Product validity end time */
	ENDING_DATE_TIME("endingDateTime"),
	/** Processing center */
	PROCESSING_CENTER("processingCenter"),
	/** Processing date (L1 and above only) */
	PROCESSING_DATE("processingDate"),
	/** Processor name */
	PROCESSOR_NAME("processorName"),
	/** Processor version */
	PROCESSOR_VERSION("processorVersion"),
	/** Orbit number */
	ORBIT_NUMBER("orbitNumber"),
	/** Product type */
	PRODUCT_TYPE("productType"),
	/** Geographic coordinates (String with blank-separated list of coordinate values; L1B only) */
	COORDINATES("coordinates"),
	/** Product identifier (L1 only) */
	PRODUCT_IDENTIFIER("identifier"),
	/** DOI  (L2 only) */
	PRODUCT_DOI("doi"),
	/** Parent product identifier (L1 and above only) */
	PARENT_IDENTIFIER("parentIdentifier"),
	/** Acquisition type (L1 only) */
	ACQUISITION_TYPE("acquisitionType"),
	/** Processing mode (L1 and above only) */
	PROCESSING_MODE("processingMode"),
	/** Quality status (L2 and above only) */
	QUALITY_STATUS("qualityStatus"),
	
	// Extended attributes (Copernicus Sentinel-5P only)
	/** Copernicus collection (baseline collection; L1 and above only) */
	BASELINE_COLLECTION("baselineCollection"),
	/** Product (file) class (L1 and above only) */
	PRODUCT_CLASS("productClass"),
	/** Product revision number (L1 and above only) */
	REVISION_NUMBER("revisionNumber")
	;
	
	/** The string value associated with this enum */
	private String value;
	
	/** A lookup table from string value to enum */
	private static Map<String, CscAttributeName> valueMap = new HashMap<>();
	
    /**
     * Populate the lookup table on loading time
     */
    static
    {
        for(CscAttributeName type: CscAttributeName.values())
        {
        	valueMap.put(type.getValue(), type);
        }
    }
  
    /**
     * Reverse lookup of enums from their value
     * 
     * @param value the value to look for
     * @return the enum associated with the value or null, if no such enum exists
     */
    public static CscAttributeName get(String value) 
    {
        return valueMap.get(value);
    }

    /**
     * Constructor with value string parameter
     * 
     * @param value the String value to associate with this enum
     */
    CscAttributeName(String value) {
		this.value = value;
	}
    
    /**
     * Returns the string value associated with this enum
     * 
     * @return a string value
     */
    public String getValue() {
    	return value;
    }
}
