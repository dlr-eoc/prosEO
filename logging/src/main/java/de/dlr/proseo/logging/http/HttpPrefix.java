/**
 * HttpPrefix.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.http;


/**
 * @author Katharina Bassler
 *
 */
public enum HttpPrefix {
	
	AUXIP_MONITOR 	("199 proseo-auxip-monitor "),
	CADIP_MONITOR 	("199 proseo-cadip-monitor "),
	EDIP_MONITOR 	("199 proseo-edip-monitor "),
	FACILTY_MGR 	("199 proseo-facmgr "),
	MODEL			("199 proseo-model "),
	INGESTOR		("199 proseo-ingestor "),
	ORDER_MGR		("199 proseo-order-mgr "),
	PLANNER			("199 proseo-planner "),
	PROCESSOR_MGR	("199 proseo-processor-mgr "),
	PRODUCTCLASS_MGR("199 proseo-productclass-mgr "),
	SAMPLES			("199 proseo-samples "),
	STORAGE_MGR		("199 proseo-storage-mgr "),
	UI				("199 proseo-ui "),
	USER_MGR		("199 proseo-user-mgr "),
	XBIP_MONITOR 	("199 proseo-xbip-monitor "),
	NOTIFICATION	("199 proseo-notification "),
	AIP_CLIENT		("199 proseo-aip-client "),

	;

	private String prefix;

	private HttpPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}
}
