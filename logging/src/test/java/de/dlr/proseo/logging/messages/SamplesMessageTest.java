/**
 * SamplesMessageTest.java
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
public class SamplesMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.SamplesMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = SamplesMessage.LEAVING_SAMPLE_PROCESSOR.getCode();
		int expected = 0;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.SamplesMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = SamplesMessage.LEAVING_SAMPLE_PROCESSOR.getLevel();
		Level expected = Level.ERROR;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.SamplesMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = SamplesMessage.LEAVING_SAMPLE_PROCESSOR.getMessage();
		String expected = "Leaving sample-processor with exit code {0} ({1})";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.SamplesMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = SamplesMessage.LEAVING_SAMPLE_PROCESSOR.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
