/**
 * UIMessageTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.event.Level;

/**
 * @author Katharina Bassler
 *
 */
public class UIMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.UIMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = UIMessage.INVALID_COMMAND_NAME.getCode();
		int expected = 2800;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.UIMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = UIMessage.INVALID_COMMAND_NAME.getLevel();
		Level expected = Level.ERROR;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.UIMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = UIMessage.INVALID_COMMAND_NAME.getMessage();
		String expected = "Invalid command name {0}";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.UIMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = UIMessage.INVALID_COMMAND_NAME.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
