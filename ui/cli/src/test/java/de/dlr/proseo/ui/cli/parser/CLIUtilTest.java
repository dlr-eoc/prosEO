/**
 * CLIUtilTest.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import static org.junit.Assert.*;

import java.time.DateTimeException;

import java.time.Instant;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.ui.cli.CLIUtil;

/**
 * @author thomas
 *
 */
public class CLIUtilTest {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CLIUtilTest.class);
	
	/**
	 * Test method for {@link de.dlr.proseo.ui.cli.CLIUtil#parseObjectFile(java.io.File, java.lang.String, java.lang.Class)}.
	 */
	@Test
	public final void testParseObjectFile() {
		logger.info("Not yet implemented: testParseObjectFile"); // TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.ui.cli.CLIUtil#printObject(java.io.PrintStream, java.lang.Object, java.lang.String)}.
	 */
	@Test
	public final void testPrintObject() {
		logger.info("Not yet implemented: testPrintObject"); // TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.ui.cli.CLIUtil#setAttribute(java.lang.Object, java.lang.String)}.
	 */
	@Test
	public final void testSetAttribute() {
		logger.info("Not yet implemented: testSetAttribute"); // TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.ui.cli.CLIUtil#parseDateTime(java.lang.String)}.
	 */
	@Test
	public final void testParseDateTime() {
		assertEquals("Fail date only", Instant.parse("2020-04-08T00:00:00.0Z"), CLIUtil.parseDateTime("2020-04-08"));
		assertEquals("Fail date+TZ", Instant.parse("2020-04-07T22:00:00.0Z"), CLIUtil.parseDateTime("2020-04-08GMT+02"));
		assertEquals("Fail date+hour+min", Instant.parse("2020-04-08T11:30:00.0Z"), CLIUtil.parseDateTime("2020-04-08T11:30"));
		assertEquals("Fail date+hour+min+TZ", Instant.parse("2020-04-08T11:30:00.0Z"), CLIUtil.parseDateTime("2020-04-08T11:30Z"));
		assertEquals("Fail date+time", Instant.parse("2020-04-08T11:30:45.0Z"), CLIUtil.parseDateTime("2020-04-08T11:30:45"));
		assertEquals("Fail date+time+TZ", Instant.parse("2020-04-08T09:30:45.0Z"), CLIUtil.parseDateTime("2020-04-08T11:30:45+02:00"));
		assertEquals("Fail date+time+nano", Instant.parse("2020-04-08T11:30:45.123Z"), CLIUtil.parseDateTime("2020-04-08T11:30:45.123"));
		assertEquals("Fail date+time+nano+TZ", Instant.parse("2020-04-08T13:30:45.123456Z"), CLIUtil.parseDateTime("2020-04-08T11:30:45.123456-0200"));
		
		try {
			CLIUtil.parseDateTime("wrong");
			fail("No exception on 'wrong'");
		} catch (DateTimeException e) {
			// OK, expected
		}
		try {
			CLIUtil.parseDateTime("2020-13-01");
			fail("No exception on '2020-13-01'");
		} catch (DateTimeException e) {
			// OK, expected
		}
		try {
			CLIUtil.parseDateTime("2020-12-32");
			fail("No exception on '2020-12-32'");
		} catch (DateTimeException e) {
			// OK, expected
		}
		try {
			CLIUtil.parseDateTime("2020-12-31T25:00:00");
			fail("No exception on '2020-12-31T25:00:00'");
		} catch (DateTimeException e) {
			// OK, expected
		}
		try {
			CLIUtil.parseDateTime("2020-12-31T23:00:00X");
			fail("No exception on '2020-12-31T23:00:00X'");
		} catch (DateTimeException e) {
			// OK, expected
		}
	}

}
