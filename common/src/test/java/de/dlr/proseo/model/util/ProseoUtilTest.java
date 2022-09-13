package de.dlr.proseo.model.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProseoUtilTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testEscape() {
		final String TEST_STRING = "BACKSLASH \\ TAB \t BACKSPACE \b NEWLINE \n CARRIAGE_RETURN \r FORM_FEED \f QUOTE \' DOUBLE_QUOTE \"";
		final String TEST_RESULT = "BACKSLASH \\\\ TAB \\t BACKSPACE \\b NEWLINE \\n CARRIAGE_RETURN \\r FORM_FEED \\f QUOTE \\' DOUBLE_QUOTE \\\"";

		assertEquals("Unexpected escape result:", TEST_RESULT, ProseoUtil.escape(TEST_STRING));
	}

	@Test
	public final void testExtractProseoMessage() {
		final String PROSEO_MESSAGE = "(E1234) Message text";
		final String VALID_HEADER = "199 proseo-util-test " + PROSEO_MESSAGE;
		final String INVALID_HEADER_1 = "199 proseo-util-test invalid message";
		final String INVALID_HEADER_2 = "invalid prefix " + PROSEO_MESSAGE;

		assertNull("Message not null:", ProseoUtil.extractProseoMessage(null));
		assertEquals("Header not recognized:", PROSEO_MESSAGE, ProseoUtil.extractProseoMessage(VALID_HEADER));
		assertEquals("Invalid header 1 not recognized", INVALID_HEADER_1, ProseoUtil.extractProseoMessage(INVALID_HEADER_1));
		assertEquals("Invalid header 2 not recognized", INVALID_HEADER_2, ProseoUtil.extractProseoMessage(INVALID_HEADER_2));
	}

}
