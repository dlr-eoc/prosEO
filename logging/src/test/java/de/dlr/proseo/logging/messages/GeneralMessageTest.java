/**
 * GeneralMessageTest.java
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
public class GeneralMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.GeneralMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = GeneralMessage.EXCEPTION_ENCOUNTERED.getCode();
		int expected = 9005;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.GeneralMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = GeneralMessage.EXCEPTION_ENCOUNTERED.getLevel();
		Level expected = Level.ERROR;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.GeneralMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = GeneralMessage.EXCEPTION_ENCOUNTERED.getMessage();
		String expected = "Exception encountered: {0}";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.GeneralMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = GeneralMessage.EXCEPTION_ENCOUNTERED.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.GeneralMessage#getSuccess()}.
	 */
	@Test
	public final void testGetSuccess() {
		boolean result = GeneralMessage.EXCEPTION_ENCOUNTERED.getSuccess();
		boolean expected = false;
		assertEquals(expected, result);
	}

}
