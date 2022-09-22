/**
 * ProseoHttpTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.http;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.event.Level;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ProseoMessage;

/**
 * @author Katharina Bassler
 *
 */
public class ProseoHttpTest {

	ProseoLogger logger = new ProseoLogger(ProseoHttpTest.class);
	ProseoHttp http = new ProseoHttp(logger, HttpPrefix.MODEL);

	/**
	 * Test method for {@link de.dlr.proseo.logging.http.ProseoHttp#errorHeaders(java.lang.String)}.
	 */
	@Test
	public final void testErrorHeaders() {
		String result = http.errorHeaders(logger.log(TestMessage.ERROR_TEST, "test")).toString();
		String expected = "[Warning:\"199 proseo-model (E1) Insert: test\"]";
		assertEquals(expected,result);
		
		String resultNull = http.errorHeaders(null).toString();
		String expectedNull = "[Warning:\"199 proseo-model null\"]";
		assertEquals(expectedNull,resultNull);
	}

	/**
	 * Test method for {@link de.dlr.proseo.logging.http.ProseoHttp#extractProseoMessage(java.lang.String)}.
	 */
	@Test
	public final void testExtractProseoMessage() {
		String result = http.extractProseoMessage("[Warning:\"199 proseo-model (E1) Insert: test\"]");
		String expected = "(E1) Insert: test";
		assertEquals(expected,result);
		assertEquals(null, http.extractProseoMessage(null));
		assertEquals(null, http.extractProseoMessage("test"));
	}

}

enum TestMessage implements ProseoMessage {
	ERROR_TEST(1, Level.ERROR, "Insert: {0}", ""),

	;

	private final int code;
	private final Level level;
	private final String message;
	private final String description;

	private TestMessage(int code, Level level, String message, String description) {
		this.level = level;
		this.code = code;
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

}