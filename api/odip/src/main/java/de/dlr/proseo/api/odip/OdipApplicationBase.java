/**
 * OdipApplicationBase.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip;

import de.dlr.proseo.api.odip.odata.OdipUtilBase;

/**
 * Base class for the ODIP application.
 * 
 * @author Ernst Melchinger
 */
public class OdipApplicationBase {

	/** The instance of the ODIP application. */
	public static OdipApplicationBase application = null;

	/** The utility instance for ODIP operations. */
	public static OdipUtilBase util = null;

}