/**
 * StorageMgrMessageTest.java
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
public class StorageMgrMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.StorageMgrMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = StorageMgrMessage.TEST_TO_BE_REPLACED.getCode();
		int expected = 5500;
		assertEquals(expected, result);
		
		Set<Integer> codes = new HashSet<>();
		
		for (StorageMgrMessage m : StorageMgrMessage.values()) {
			if (!codes.add(m.getCode()))
				throw new RuntimeException("No duplicate codes allowed, check " + m);
		}
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.StorageMgrMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = StorageMgrMessage.TEST_TO_BE_REPLACED.getLevel();
		Level expected = Level.WARN;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.StorageMgrMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = StorageMgrMessage.TEST_TO_BE_REPLACED.getMessage();
		String expected = "to be replaced";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.StorageMgrMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = StorageMgrMessage.TEST_TO_BE_REPLACED.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
