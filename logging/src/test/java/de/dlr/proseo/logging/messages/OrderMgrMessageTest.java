/**
 * OrderMgrMessageTest.java
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
public class OrderMgrMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.OrderMgrMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = OrderMgrMessage.JOB_NOT_EXIST.getCode();
		int expected = 3516;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.OrderMgrMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = OrderMgrMessage.JOB_NOT_EXIST.getLevel();
		Level expected = Level.ERROR;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.OrderMgrMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = OrderMgrMessage.JOB_NOT_EXIST.getMessage();
		String expected = "Job {0} does not exist";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.OrderMgrMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = OrderMgrMessage.JOB_NOT_EXIST.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.OrderMgrMessage#getSuccess()}.
	 */
	@Test
	public final void testGetSuccess() {
		boolean result = OrderMgrMessage.JOB_NOT_EXIST.getSuccess();
		boolean expected = false;
		assertEquals(expected, result);
	}
}
