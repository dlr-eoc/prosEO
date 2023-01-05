/**
 * PlannerMessageTest.java
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
public class PlannerMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.PlannerMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = PlannerMessage.ORDERS_RETRIEVED.getCode();
		int expected = 4000;
		assertEquals(expected, result);
		
		Set<Integer> codes = new HashSet<>();
		
		for (PlannerMessage m :	PlannerMessage.values()) {
			if (!codes.add(m.getCode()))
				throw new RuntimeException("No duplicate codes allowed, check " + m);
		}
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.PlannerMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = PlannerMessage.ORDERS_RETRIEVED.getLevel();
		Level expected = Level.INFO;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.PlannerMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = PlannerMessage.ORDERS_RETRIEVED.getMessage();
		String expected = "List of processing orders retrieved";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.PlannerMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = PlannerMessage.ORDERS_RETRIEVED.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.PlannerMessage#getSuccess()}.
	 */
	@Test
	public final void testGetSuccess() {
		boolean result = PlannerMessage.ORDERS_RETRIEVED.getSuccess();
		boolean expected = true;
		assertEquals(expected, result);
	}
}
