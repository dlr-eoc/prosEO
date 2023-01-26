/**
 * GeoToolsMessageTest.java
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
public class GeotoolsMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.GeotoolsMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = GeotoolsMessage.SHAPE_FILE_INITIALIZED.getCode();
		int expected = 1500;
		assertEquals(expected, result);
		
		Set<Integer> codes = new HashSet<>();
		
		for (GeotoolsMessage m : GeotoolsMessage.values()) {
			if (!codes.add(m.getCode()))
				throw new RuntimeException("No duplicate codes allowed, check " + m);
		}
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.GeotoolsMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = GeotoolsMessage.SHAPE_FILE_INITIALIZED.getLevel();
		Level expected = Level.INFO;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.GeotoolsMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = GeotoolsMessage.SHAPE_FILE_INITIALIZED.getMessage();
		String expected = "Shape file {0} for type {1} initialized";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.GeotoolsMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = GeotoolsMessage.SHAPE_FILE_INITIALIZED.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
