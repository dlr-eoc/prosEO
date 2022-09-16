/**
 * ProseoMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * Interface for the enums collecting the services' messages.
 *
 * @author Katharina Bassler
 */
public interface ProseoMessage {

	/**
	 * Get the message's code.
	 * 
	 * @return The message code.
	 */
	public int getCode();

	/**
	 * Get the message's level.
	 * 
	 * @return The message level.
	 */
	public Level getLevel();

	/**
	 * Get the message.
	 * 
	 * @return The message.
	 */
	public String getMessage();

	/**
	 * Get a more detailed description of the message's purpose.
	 * 
	 * @return A description of the message.
	 */
	public String getDescription();
		
}
