/**
 * ProseoLoggerTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.logger;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.event.Level;

import de.dlr.proseo.logging.messages.ProseoMessage;

/**
 * @author Katharina Bassler
 *
 */
public class ProseoLoggerTest {

	private final ProseoLogger logger = new ProseoLogger(ProseoLoggerTest.class);

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#log(de.dlr.proseo.logging.messages.ProseoMessage, java.lang.Object[])}.
	 */
	@Test
	public final void testLog() {
		String expected0 = "(E1) Insert: java.lang.Exception";
		String result0 = logger.log(TestMessage.ERROR_TEST, new Exception());
		assertEquals(expected0, result0);
		
		String expected1 = "(W2) Insert: test";
		String result1 = logger.log(TestMessage.WARN_TEST, "test");
		assertEquals(expected1, result1);
		
		String expected2 = "(I3) Insert: test, 1";
		String result2 = logger.log(TestMessage.INFO_TEST, "test", 1);
		assertEquals(expected2, result2);
		
		Assert.assertThrows("Please specify the type of the message.", 
				IllegalArgumentException.class, () -> {logger.log(null);});	
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#format(de.dlr.proseo.logging.messages.ProseoMessage, java.lang.Object[])}.
	 */
	@Test
	public final void testFormat() {
		String expected0 = "(E1) Insert: java.lang.Exception";
		String result0 = logger.log(TestMessage.ERROR_TEST, new Exception());
		assertEquals(expected0, result0);
		
		String expected1 = "(W2) Insert: test";
		String result1 = logger.log(TestMessage.WARN_TEST, "test");
		assertEquals(expected1, result1);
		
		String expected2 = "(I3) Insert: test, 1";
		String result2 = logger.log(TestMessage.INFO_TEST, "test", 1);
		assertEquals(expected2, result2);
		
		Assert.assertThrows("Please specify the type of the message.", 
				IllegalArgumentException.class, () -> {logger.log(null);});	
	}


	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#debug(java.lang.String)}.
	 */
	@Test
	public final void testDebugString() {
		logger.debug("test0");
		logger.debug(null);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#debug(java.lang.String, java.lang.Object[])}.
	 */
	@Test
	public final void testDebugStringObjectArray() {
		logger.debug("test1: {}", "arguments inserted");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#debug(java.lang.String, java.lang.Throwable)}.
	 */
	@Test
	public final void testDebugStringThrowable() {
		logger.debug("test2", new Exception());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#trace(java.lang.String)}.
	 */
	@Test
	public final void testTraceString() {
		logger.trace("test");
		logger.trace(null);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#trace(java.lang.String, java.lang.Object[])}.
	 */
	@Test
	public final void testTraceStringObjectArray() {
		logger.trace("test: {}", "arguments inserted");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#trace(java.lang.String, java.lang.Throwable)}.
	 */
	@Test
	public final void testTraceStringThrowable() {
		logger.trace("test", new Exception());
	}

	/**
	 * Test method for {@link de.dlr.proseo.logging.logger.ProseoLogger#getName()}.
	 */
	@Test
	public final void testGetName() {
		String expected = "de.dlr.proseo.logging.logger.ProseoLoggerTest";
		String result = logger.getName();
		assertEquals(expected, result);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#isTraceEnabled()}.
	 */
	@Test
	public final void testIsTraceEnabled() {
		logger.isTraceEnabled();
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#isDebugEnabled()}.
	 */
	@Test
	public final void testIsDebugEnabled() {
		logger.isDebugEnabled();
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#isInfoEnabled()}.
	 */
	@Test
	public final void testIsInfoEnabled() {
		logger.isInfoEnabled();
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#isWarnEnabled()}.
	 */
	@Test
	public final void testIsWarnEnabled() {
		logger.isWarnEnabled();
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.logger.ProseoLogger#isErrorEnabled()}.
	 */
	@Test
	public final void testIsErrorEnabled() {
		logger.isErrorEnabled();
	}

}

enum TestMessage implements ProseoMessage {
	ERROR_TEST(1, Level.ERROR, false, "Insert: {0}", ""),
	WARN_TEST(2, Level.WARN, true, "Insert: {0}", ""),
	INFO_TEST(3, Level.INFO, true, "Insert: {0}, {1}", "")

	;

	private final int code;
	private final Level level;
	private final boolean success;
	private final String message;
	private final String description;

	private TestMessage(int code, Level level, boolean success, String message, String description) {
		this.level = level;
		this.code = code;
		this.success = success;
		this.message = message;
		this.description = description;
	}

	public int getCode() {
		return code;
	}

	public Level getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public boolean getSuccess() {
		return success;
	}

}