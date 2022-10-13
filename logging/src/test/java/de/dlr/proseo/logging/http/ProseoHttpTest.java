/**
 * ProseoHttpTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.http;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.event.Level;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ProseoMessage;

/**
 * @author Katharina Bassler
 *
 */
public class ProseoHttpTest {

	ProseoLogger logger = new ProseoLogger(ProseoHttpTest.class);
	ProseoHttp http = new ProseoHttp(logger, HttpPrefix.UI);

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.http.ProseoHttp#errorHeaders(java.lang.String)}.
	 */
	@Test
	public final void testErrorHeaders() {
		String result = http.errorHeaders(logger.log(TestMessage.ERROR_TEST, "test")).toString();
		String expected = "[Warning:\"199 proseo-ui (E1) Insert: test\"]";
		assertEquals(expected, result);

		String resultNull = http.errorHeaders(null).toString();
		String expectedNull = "[Warning:\"199 proseo-ui null\"]";
		assertEquals(expectedNull, resultNull);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.http.ProseoHttp#extractProseoMessage(java.lang.String)}.
	 */
	@Test
	public final void testExtractProseoMessage() {
		String result = http.extractProseoMessage("[Warning:\"199 proseo-ui (E1) Insert: test\"]");
		String expected = "(E1) Insert: test";

		assertEquals(expected, result);
		assertEquals(null, http.extractProseoMessage(null));
		assertEquals(null, http.extractProseoMessage("test"));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.http.ProseoHttp#createMessageFromHeaders(java.lang.String)}.
	 */
	@Test
	public final void testCreateMessageFromHeaders() {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		HttpHeaders header = http.errorHeaders(logger.log(TestMessage.ERROR_TEST, "test"));
		HttpHeaders header1 = new HttpHeaders();
		header1.set(HttpHeaders.WARNING, "empty");

		String result = http.createMessageFromHeaders(status, header);
		String expected = "(E1) Insert: test";

		String result1 = http.createMessageFromHeaders(status, header1);
		String expected1 = "(E2811) Service request failed with status 500 (500 INTERNAL_SERVER_ERROR), cause: empty";
		
		assertEquals(expected, result);
		assertEquals(expected1, result1);
		assertEquals(null, http.createMessageFromHeaders(status, null));
		assertEquals(null, http.createMessageFromHeaders(null, header));
		assertEquals(null, http.createMessageFromHeaders(null, null));

	}

}

enum TestMessage implements ProseoMessage {
	ERROR_TEST(1, Level.ERROR, false, "Insert: {0}", ""),

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