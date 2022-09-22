/**
 * FacilityMgrMessageTest.java
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
public class FacilityMgrMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.FacilityMgrMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = FacilityMgrMessage.FACILITY_NOT_FOUND.getCode();
		int expected = 1014;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.FacilityMgrMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = FacilityMgrMessage.FACILITY_NOT_FOUND.getLevel();
		Level expected = Level.ERROR;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.FacilityMgrMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = FacilityMgrMessage.FACILITY_NOT_FOUND.getMessage();
		String expected = "No facility found for ID {0}";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.FacilityMgrMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = FacilityMgrMessage.FACILITY_NOT_FOUND.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
