/**
 * CscAttributeName.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.odata;

import java.util.HashMap;
import java.util.Map;

/**
 * Allowed attribute names for a product (from the ESA S1 Product Attributes Mapping, issue 1.6, Apr 2021, and S5P Product
 * Attributes Mapping, issue 1.2, Jan 2021)
 *
 * @author Dr. Thomas Bassler
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
	/** JobStatus value */
	STATUS("Status"),
	/** Text message providing additional information on the returned status */
	STATUS_MESSAGE("StatusMessage"),
	/** Actual size in bytes (B) of the output Product composing the Order */
	ORDER_OUTPUT_SIZE("OrderOutputSize"),
	/** Date and time at which the ProductionOrder was received by the ODPRIP */
	SUBMISSION_DATE("SubmissionDate"),
	/** Estimated date and time when the product will be available for download from the ODPRIP */
	ESTIMATED_DATE("EstimatedDate"),
	/** Date and time when the product was available for download from the ODPRIP */
	COMPLETED_DATE("CompletedDate"),
	/** Date when the Product related to the order will be removed from the ODPRIP */
	EVICTION_DATE("EvictionDate"),
	/** Priority of the ProductionOrder. It is an integer from 1-100, default 50. */
	PRIORITY("Priority"),
	/** Complex type used to unambiguously identify the input product */
	INPUT_PRODUCT_REFERENCE("InputProductReference"),
	/**
	 * The Id is a local unique identifier for the Workflow instance within the ODPRIP which is applicable to the ProductionOrder
	 */
	WORKFLOW_ID("WorkflowId"),
	/** Short name of the workflow */
	WORKFLOW_NAME("WorkflowName"),
	/** Selection of applicable options from the Workflow */
	WORK_FLOW_OPTIONS("WorkflowOptions"),
	/** URI used by the ODPRIP for product download readiness notifications, */
	NOTIFICATION_ENDPOINT("NotificationEndpoint"),
	/** The username associated with the EndPoint URI provided */
	NOTIFICATION_EPUSERNAME("NotificationEpUsername"),
	/** The password associated with the EndPoint URI provided */
	NOTIFICATION_EPPASSWORD("NotificationEpPassword"),
	/** Product (file) class */
	PRODUCT_CLASS("ProductClass"),
	/** Output product (file) class */
	OUTPUT_PRODUCT_CLASS("OutputProductType"),
	/** Input product (file) class */
	INPUT_PRODUCT_CLASS("InputProductType"),
	/** configured processor */
	CONFIGURED_PROCESSOR("ConfiguredProcessor"),
	/** processing mode */
	PROCESSING_MODE("ProcessingMode"),
	/** output file class */
	OUTPUT_FILE_CLASS("OutputFileClass"),
	/** description */
	DESCRIPTION("Description"),
	/** description */
	WORKFLOW_VERSION("WorkflowVersion"),

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
	REVISION_NUMBER("revisionNumber");

	/** The string value associated with this enum */
	private String value;

	/** A lookup table from string value to enum */
	private static Map<String, CscAttributeName> valueMap = new HashMap<>();

	/**
	 * Populate the lookup table on loading time
	 */
	static {
		for (CscAttributeName type : CscAttributeName.values()) {
			valueMap.put(type.getValue(), type);
		}
	}

	/**
	 * Reverse lookup of enums from their value
	 *
	 * @param value the value to look for
	 * @return the enum associated with the value or null, if no such enum exists
	 */
	public static CscAttributeName get(String value) {
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