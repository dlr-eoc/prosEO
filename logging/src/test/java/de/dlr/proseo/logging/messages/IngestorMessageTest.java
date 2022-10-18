/**
 * IngestorMessageTest.java
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
public class IngestorMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.IngestorMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = IngestorMessage.INVALID_FACILITY.getCode();
		int expected = 2018;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.IngestorMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = IngestorMessage.INVALID_FACILITY.getLevel();
		Level expected = Level.INFO;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.IngestorMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = IngestorMessage.INVALID_FACILITY.getMessage();
		String expected = "Invalid processing facility {0} for ingestion";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.IngestorMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = IngestorMessage.INVALID_FACILITY.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
