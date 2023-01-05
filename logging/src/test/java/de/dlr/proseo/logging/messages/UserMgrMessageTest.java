/**
 * UserMgrMessageTest.java
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
public class UserMgrMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.UserMgrMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = UserMgrMessage.MISSION_MISSING.getCode();
		int expected = 6502;
		assertEquals(expected, result);
		
		Set<Integer> codes = new HashSet<>();
		
		for (UserMgrMessage m : UserMgrMessage.values()) {
			if (!codes.add(m.getCode()))
				throw new RuntimeException("No duplicate codes allowed, check " + m);
		}
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.UserMgrMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = UserMgrMessage.MISSION_MISSING.getLevel();
		Level expected = Level.ERROR;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.UserMgrMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = UserMgrMessage.MISSION_MISSING.getMessage();
		String expected = "Mission not set";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.UserMgrMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = UserMgrMessage.MISSION_MISSING.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
