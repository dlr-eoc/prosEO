/**
 * OrderMgrMessageTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

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
		
		Set<Integer> codes = new HashSet<>();
		
		for (OrderMgrMessage m : OrderMgrMessage.values()) {
			if (!codes.add(m.getCode()))
				throw new RuntimeException("No duplicate codes allowed, check " + m);
		}
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
