/**
 * ProseoUtilTest.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test class ProseoUtil
 *
 * @author Dr. Thomas Bassler
 *
 */
public class ProseoUtilTest {

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
    }

    @BeforeEach
    public void setUp() throws Exception {
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public final void testEscape() {
        final String TEST_STRING = "BACKSLASH \\ TAB \t BACKSPACE \b NEWLINE \n CARRIAGE_RETURN \r FORM_FEED \f QUOTE \' DOUBLE_QUOTE \"";
        final String TEST_RESULT = "BACKSLASH \\\\ TAB \\t BACKSPACE \\b NEWLINE \\n CARRIAGE_RETURN \\r FORM_FEED \\f QUOTE \\' DOUBLE_QUOTE \\\"";

        assertEquals(TEST_RESULT, ProseoUtil.escape(TEST_STRING), "Unexpected escape result:");
    }

    @Test
    public final void testExtractProseoMessage() {
        final String PROSEO_MESSAGE = "(E1234) Message text";
        final String VALID_HEADER = "199 proseo-util-test " + PROSEO_MESSAGE;
        final String INVALID_HEADER_1 = "199 proseo-util-test invalid message";
        final String INVALID_HEADER_2 = "invalid prefix " + PROSEO_MESSAGE;

        assertNull(ProseoUtil.extractProseoMessage(null), "Message not null:");
        assertEquals(PROSEO_MESSAGE, ProseoUtil.extractProseoMessage(VALID_HEADER), "Header not recognized:");
        assertEquals(INVALID_HEADER_1, ProseoUtil.extractProseoMessage(INVALID_HEADER_1), "Invalid header 1 not recognized");
        assertEquals(INVALID_HEADER_2, ProseoUtil.extractProseoMessage(INVALID_HEADER_2), "Invalid header 2 not recognized");
    }

}