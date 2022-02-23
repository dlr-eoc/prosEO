/**
 * ProductionType.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Production context for the generation of a product (from the ESA PRIP API specification, ESA-EOPG-EOPGC-IF-3, issue 1.4, sec. 3.2)
 * 
 * @author Dr. Thomas Bassler
 * 
 */
public enum ProductionType {
	/** Standard systematic production has been applied. */
	SYSTEMATIC("systematic_production"),
	/** The production is the result of an on-demand processing request with default workflow options applied. */
	ON_DEMAND_DEFAULT("on-demand default"),
	/** The production is the result of an on-demand processing request with non-default workflow options applied. */
	ON_DEMAND_NON_DEFAULT("on-demand non-default");
	
	/** The string value associated with this enum */
	private String value;
	
	/** A lookup table from string value to enum */
	private static Map<String, ProductionType> valueMap = new HashMap<>();
	
    /**
     * Populate the lookup table on loading time
     */
    static
    {
        for(ProductionType type: ProductionType.values())
        {
        	valueMap.put(type.getValue(), type);
        }
    }
  
    /**
     * Reverse lookup of enums from their value
     * 
     * @param value the value to look for
     * @return the enum associated with the value
     */
    public static ProductionType get(String value) 
    {
        return valueMap.get(value);
    }

    /**
     * Constructor with value string parameter
     * 
     * @param value the String value to associate with this enum
     */
    ProductionType(String value) {
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
