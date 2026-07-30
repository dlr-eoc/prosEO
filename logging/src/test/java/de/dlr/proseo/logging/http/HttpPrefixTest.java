/**
 * HttpPrefixTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.http;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author Katharina Bassler
 *
 */
public class HttpPrefixTest {

	/**
	 * Test method for {@link de.dlr.proseo.logging.http.HttpPrefix#getPrefix()}.
	 */
	@Test
	public final void testGetPrefix() {
		String result = HttpPrefix.PLANNER.getPrefix();
		String expected = "199 proseo-planner ";
		assertEquals(expected, result);
	}

}
