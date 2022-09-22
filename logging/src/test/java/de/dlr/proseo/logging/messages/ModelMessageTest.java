/**
 * ModelMessageTest.java
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
public class ModelMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ModelMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = ModelMessage.NO_ITEM_FOUND.getCode();
		int expected = 0;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ModelMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = ModelMessage.NO_ITEM_FOUND.getLevel();
		Level expected = Level.ERROR;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ModelMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = ModelMessage.NO_ITEM_FOUND.getMessage();
		String expected = "No item found or not enough time coverage for selection rule '{0}' and time interval ({1}, {2})";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ModelMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = ModelMessage.NO_ITEM_FOUND.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
