package de.dlr.proseo.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import de.dlr.proseo.model.SimplePolicy.DeltaTime;

public class DeltaTimeTest {

	@Test
	public final void testDeltaTime() {
		DeltaTime dt = new DeltaTime();
		assertEquals(0L, dt.duration, "Unexpected default duration");
		assertEquals(TimeUnit.DAYS, dt.unit, "Unexpected default unit");
	}

	@Test
	public final void testDeltaTimeLongTimeUnit() {
		DeltaTime dt = new DeltaTime(7, TimeUnit.MINUTES);
		assertEquals(7L, dt.duration, "Unexpected default duration");
		assertEquals(TimeUnit.MINUTES, dt.unit, "Unexpected default unit");
	}

	@Test
	public final void testMerge() {
		DeltaTime dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		DeltaTime dt2 = new DeltaTime(63, TimeUnit.SECONDS);
		DeltaTime dt = dt1.merge(dt2);
		assertEquals(7L, dt.duration, "Unexpected merged duration (1)");
		assertEquals(TimeUnit.MINUTES, dt.unit, "Unexpected merged unit (1)");

		dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		dt2 = new DeltaTime(1800, TimeUnit.SECONDS);
		dt = dt1.merge(dt2);
		assertEquals(30L, dt.duration, "Unexpected merged duration (2)");
		assertEquals(TimeUnit.MINUTES, dt.unit, "Unexpected merged unit (2)");
	}

	@Test
	public final void testToSeconds() {
		DeltaTime dt = new DeltaTime(7, TimeUnit.MINUTES);
		assertEquals(7L * 60, dt.toSeconds(), "Unexpected conversion to seconds (1)");
		dt = new DeltaTime(900, TimeUnit.MILLISECONDS);
		assertEquals(1L, dt.toSeconds(), "Unexpected conversion to seconds (2)");
	}

	@Test
	public final void testToMilliseconds() {
		DeltaTime dt = new DeltaTime(7, TimeUnit.MINUTES);
		assertEquals(7L * 60 * 1000, dt.toMilliseconds(), "Unexpected conversion to milliseconds (1)");
		dt = new DeltaTime(900, TimeUnit.MILLISECONDS);
		assertEquals(900L, dt.toMilliseconds(), "Unexpected conversion to milliseconds (2)");
	}

	@Test
	public final void testNormalize() {
		DeltaTime dt = new DeltaTime(120000, TimeUnit.MILLISECONDS);
		dt.normalize();
		assertEquals(2L, dt.duration, "Unexpected normalized duration (1)");
		assertEquals(TimeUnit.MINUTES, dt.unit, "Unexpected normalized unit (1)");
		dt = new DeltaTime(48, TimeUnit.HOURS);
		dt.normalize();
		assertEquals(2L, dt.duration, "Unexpected normalized duration (2)");
		assertEquals(TimeUnit.DAYS, dt.unit, "Unexpected normalized unit (2)");
		dt = new DeltaTime(0, TimeUnit.MILLISECONDS);
		dt.normalize();
		assertEquals(0L, dt.duration, "Unexpected normalized duration (3)");
		assertEquals(TimeUnit.DAYS, dt.unit, "Unexpected normalized unit (3)");
		dt = new DeltaTime(47, TimeUnit.HOURS);
		dt.normalize();
		assertEquals(47L, dt.duration, "Unexpected normalized duration (2)");
		assertEquals(TimeUnit.HOURS, dt.unit, "Unexpected normalized unit (2)");
		dt = new DeltaTime(7, TimeUnit.DAYS);
		dt.normalize();
		assertEquals(7L, dt.duration, "Unexpected normalized duration (2)");
		assertEquals(TimeUnit.DAYS, dt.unit, "Unexpected normalized unit (2)");
	}

	@Test
	public final void testToString() {
		DeltaTime dt = new DeltaTime(120, TimeUnit.MILLISECONDS);
		assertEquals("120 MS", dt.toString(), "Unexpected delta time string (1)");
		dt = new DeltaTime(120, TimeUnit.SECONDS);
		assertEquals("120 S", dt.toString(), "Unexpected delta time string (2)");
		dt = new DeltaTime(120, TimeUnit.MINUTES);
		assertEquals("120 M", dt.toString(), "Unexpected delta time string (3)");
		dt = new DeltaTime(120, TimeUnit.HOURS);
		assertEquals("120 H", dt.toString(), "Unexpected delta time string (4)");
		dt = new DeltaTime(120, TimeUnit.DAYS);
		assertEquals("120 D", dt.toString(), "Unexpected delta time string (5)");
	}

	@Test
	public final void testEqualsObject() {
		DeltaTime dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		DeltaTime dt2 = new DeltaTime(7 * 60, TimeUnit.SECONDS);
		assertTrue(dt1.equals(dt2), "Unexpected inequality");
		dt1 = new DeltaTime(123, TimeUnit.MILLISECONDS);
		dt2 = new DeltaTime(456, TimeUnit.MILLISECONDS);
		assertFalse(dt1.equals(dt2), "Unexpected equality");
	}

	@Test
	public final void testCompareTo() {
		DeltaTime dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		DeltaTime dt2 = new DeltaTime(63, TimeUnit.SECONDS);
		assertEquals(+1, dt1.compareTo(dt2), "Unexpected comparison result (1)");
		assertEquals(-1, dt2.compareTo(dt1), "Unexpected comparison result (2)");
		dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		dt2 = new DeltaTime(7 * 60, TimeUnit.SECONDS);
		assertEquals(0, dt1.compareTo(dt2), "Unexpected comparison result (3)");
	}

}
