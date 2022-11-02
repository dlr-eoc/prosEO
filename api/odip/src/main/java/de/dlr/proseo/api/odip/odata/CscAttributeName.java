/**
 * CscAttributeName.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.odata;

import java.util.HashMap;
import java.util.Map;

/**
 * Allowed attribute names for a product (from the ESA S1 Product Attributes Mapping, issue 1.6, Apr 2021, and
 * S5P Product Attributes Mapping, issue 1.2, Jan 2021)
 * 
 * @author Dr. Thomas Bassler
 * 
 */
public enum CscAttributeName {
	
	// OGC JSON attributes
	
	/** Platform short name (fixed value 'SENTINEL-5P') */
	PLATFORM_SHORT_NAME("platformShortName"),
	/** Platform serial identifier ('A', 'B' etc.) */
	PLATFORM_SERIAL("platformSerialIdentifier"),
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
	/** Geographic coordinates (String with blank-separated list of coordinate values) */
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
	/** Product (file) class */
	PRODUCT_CLASS("productClass"),
	
	// Extended attributes (Copernicus Sentinel-1 only)
	/** Start time from ascending node */
	START_ASCENDING("startTimeFromAscendingNode"),
	/** Completion time from ascending node */
	STOP_ASCENDING("completionTimeFromAscendingNode"),
	/** Operational mode ("IW", "EW" etc.) */
	OPERATIONAL_MODE("operationalMode"),
	/** Swath identifier (1 .. 6) */
	SWATH_IDENTIFIER("swathIdentifier"),
	/** Product consolidation ("SLICE", "FULL" etc.) */
	PRODUCT_CONSOLIDATION("productConsolidation"),
	/** Instrument configuration */
	INSTRUMENT_CONFIGURATION("instrumentConfigurationID"),
	/** Datatake ID */
	DATATAKE_ID("datatakeID"),
	/** Product composition (e. g. "slice") */
	PRODUCT_COMPOSITION("productComposition"),
	/** Slice product flag ("true", "false") */
	SLICE_PRODUCT_FLAG("sliceProductFlag"),
	/** Slice number */
	SLICE_NUMBER("sliceNumber"),
	/** Total number of slices in datatake */
	SLICE_TOTAL("totalSlices"),
	/** Segment start time */
	SEGMENT_START("segmentStartTime"),
	/** Production timeliness */
	TIMELINESS("timeliness"),
	/** Polarisation channels ("HH", "VV", "HV", "VH") */
	POLARISATION_CHANNELS("polarisationChannels"),
	/** Relative orbit number */
	RELATIVE_ORBIT("relativeOrbitNumber"),
	/** Cycle number */
	CYCLE("cycleNumber"),
	/** Orbit direction (e. g. "ASCENDING") */
	ORBIT_DIRECTION("orbitDirection"),
	
	// Extended attributes (Copernicus Sentinel-5P only)
	/** Copernicus collection (baseline collection; L1 and above only) */
	BASELINE_COLLECTION("baselineCollection"),
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
