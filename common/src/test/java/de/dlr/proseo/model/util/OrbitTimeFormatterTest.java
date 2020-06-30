/**
 * OrbitTimeFormatterTest.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.Test;

/**
 * Test class for OrbitTimeFormatter
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class OrbitTimeFormatterTest {

	/**
	 * Test method for {@link de.dlr.proseo.model.util.OrbitTimeFormatter#format(java.time.temporal.TemporalAccessor)}.
	 */
	@Test
	public final void testFormat() {
		Instant testInstant = Instant.parse("2020-03-23T15:46:17.123456Z");
		
		String formattedInstant = OrbitTimeFormatter.format(testInstant);
		
		assertEquals("Unexpected time format:", "2020-03-23T15:46:17.123456", formattedInstant);
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.util.OrbitTimeFormatter#parse(java.lang.String)}.
	 */
	@Test
	public final void testParse() {
		String inputNoTimezone = "2020-03-23T15:46:17.123456";
		
		Instant testInstantNoTimezone = Instant.parse(inputNoTimezone + "Z");
		
		assertEquals("Parsing without timezone failed:", testInstantNoTimezone, Instant.from(OrbitTimeFormatter.parse(inputNoTimezone)));
		
	}

}
