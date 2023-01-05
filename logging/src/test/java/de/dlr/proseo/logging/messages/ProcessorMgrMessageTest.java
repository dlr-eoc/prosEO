/**
 * ProcessorMgrMessageTest.java
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
public class ProcessorMgrMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ProcessorMgrMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = ProcessorMgrMessage.CONCURRENT_UPDATE.getCode();
		int expected = 4501;
		assertEquals(expected, result);
		
		Set<Integer> codes = new HashSet<>();
		
		for (ProcessorMgrMessage m : ProcessorMgrMessage.values()) {
			if (!codes.add(m.getCode()))
				throw new RuntimeException("No duplicate codes allowed, check " + m);
		}
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ProcessorMgrMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = ProcessorMgrMessage.CONCURRENT_UPDATE.getLevel();
		Level expected = Level.ERROR;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ProcessorMgrMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = ProcessorMgrMessage.CONCURRENT_UPDATE.getMessage();
		String expected = "The configuration with ID {0} has been modified since retrieval by the client";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ProcessorMgrMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = ProcessorMgrMessage.CONCURRENT_UPDATE.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
