/**
 * TransferObject.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.basemon;

import java.time.Instant;

/**
 * Interface for all objects to be transferred via a monitor derived from BaseMonitor
 *
 * @author Dr. Thomas Bassler
 */
public interface TransferObject {

	/**
	 * Gets the unique identifier of the transfer object for use in the transfer history
	 *
	 * @return the transfer object identifier
	 */
	public String getIdentifier();

	/**
	 * Gets the reference time of this object for use in the transfer history
	 *
	 * @return the transfer object reference time
	 */
	public Instant getReferenceTime();

}