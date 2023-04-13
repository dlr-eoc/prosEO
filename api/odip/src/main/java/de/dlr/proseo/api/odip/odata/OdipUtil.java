/**
 * OdipUtil.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.odata;

import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Utility class to convert objects from prosEO database model to ODIP (OData) REST API
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class OdipUtil extends OdipUtilBase {
	/** A logger for this class */
	private ProseoLogger logger = new ProseoLogger(OdipUtil.class);

}
