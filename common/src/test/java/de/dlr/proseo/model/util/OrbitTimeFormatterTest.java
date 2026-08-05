/**
 * OrbitTimeFormatterTest.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

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

        assertEquals("2020-03-23T15:46:17.123456", formattedInstant, "Unexpected time format:");
    }

    /**
     * Test method for {@link de.dlr.proseo.model.util.OrbitTimeFormatter#parse(java.lang.String)}.
     */
    @Test
    public final void testParse() {
        String inputNoTimezone = "2020-03-23T15:46:17.123456";

        Instant testInstantNoTimezone = Instant.parse(inputNoTimezone + "Z");

        assertEquals(testInstantNoTimezone, Instant.from(OrbitTimeFormatter.parse(inputNoTimezone)), "Parsing without timezone failed:");

        inputNoTimezone = "2020-03-23T15:46:17";

        testInstantNoTimezone = Instant.parse(inputNoTimezone + "Z");

        assertEquals(testInstantNoTimezone, Instant.from(OrbitTimeFormatter.parse(inputNoTimezone)), "Parsing without timezone and fraction of seconds failed:");

        inputNoTimezone = "2020-03-23T15:46:17.456";

        testInstantNoTimezone = Instant.parse(inputNoTimezone + "Z");

        assertEquals(testInstantNoTimezone, Instant.from(OrbitTimeFormatter.parse(inputNoTimezone)), "Parsing without timezone and with milliseconds failed:");
        // assertEquals("Parsing without timezone and with milliseconds failed:", testInstantNoTimezone, Instant.from(OrbitTimeFormatter.parse(inputNoTimezone + "G")));

        // --- Test invalid inputs ---
        String invalidInput = "something weird";
        try {
            OrbitTimeFormatter.parse(invalidInput);
            fail("DateTimeParseException expected on input '" + invalidInput + "'");
        } catch (DateTimeParseException e) {
            assertEquals("Cannot parse date/time string " + invalidInput + " at index 0", e.getMessage(), "Unexpected error message");
        }

        invalidInput = "2020-03-23T15:16GGG";
        try {
            OrbitTimeFormatter.parse(invalidInput);
            fail("DateTimeParseException expected on input '" + invalidInput + "'");
        } catch (DateTimeParseException e) {
            assertEquals("Cannot parse date/time string " + invalidInput + " at index " + invalidInput.indexOf('G'), e.getMessage(), "Unexpected error message");
        }
    }

}