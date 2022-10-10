/**
 * ProductClassMgrMessageTest.java
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
public class ProductClassMessageTest {

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ProductClassMgrMessage#getCode()}.
	 */
	@Test
	public final void testGetCode() {
		int result = ProductClassMgrMessage.CONCURRENT_UPDATE.getCode();
		int expected = 2103;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ProductClassMgrMessage#getLevel()}.
	 */
	@Test
	public final void testGetLevel() {
		Level result = ProductClassMgrMessage.CONCURRENT_UPDATE.getLevel();
		Level expected = Level.ERROR;
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ProductClassMgrMessage#getMessage()}.
	 */
	@Test
	public final void testGetMessage() {
		String result = ProductClassMgrMessage.CONCURRENT_UPDATE.getMessage();
		String expected = "The product class with ID {0} has been modified since retrieval by the client";
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.messages.ProductClassMgrMessage#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		String result = ProductClassMgrMessage.CONCURRENT_UPDATE.getDescription();
		String expected = "";
		assertEquals(expected, result);
	}

}
