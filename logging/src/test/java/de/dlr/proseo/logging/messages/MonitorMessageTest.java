/**
 * MonitorMessageTest.java
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
public class MonitorMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.MonitorMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = MonitorMessage.ILLEGAL_CONFIG_VALUE.getCode();
		int expected = 0;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.MonitorMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = MonitorMessage.ILLEGAL_CONFIG_VALUE.getLevel();
		Level expected = Level.WARN;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.MonitorMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = MonitorMessage.ILLEGAL_CONFIG_VALUE.getMessage();
		String expected = "Illegal config value productAggregationStart: {0}";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.MonitorMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = MonitorMessage.ILLEGAL_CONFIG_VALUE.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
