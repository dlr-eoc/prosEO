package de.dlr.proseo.model;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import de.dlr.proseo.model.SimplePolicy.DeltaTime;

public class DeltaTimeTest {

	@Test
	public final void testDeltaTime() {
		DeltaTime dt = new DeltaTime();
		assertEquals("Unexpected default duration", 0L, dt.duration);
		assertEquals("Unexpected default unit", TimeUnit.DAYS, dt.unit);
	}

	@Test
	public final void testDeltaTimeLongTimeUnit() {
		DeltaTime dt = new DeltaTime(7, TimeUnit.MINUTES);
		assertEquals("Unexpected default duration", 7L, dt.duration);
		assertEquals("Unexpected default unit", TimeUnit.MINUTES, dt.unit);
	}

	@Test
	public final void testMerge() {
		DeltaTime dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		DeltaTime dt2 = new DeltaTime(63, TimeUnit.SECONDS);
		DeltaTime dt = dt1.merge(dt2);
		assertEquals("Unexpected merged duration (1)", 7L, dt.duration);
		assertEquals("Unexpected merged unit (1)", TimeUnit.MINUTES, dt.unit);

		dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		dt2 = new DeltaTime(1800, TimeUnit.SECONDS);
		dt = dt1.merge(dt2);
		assertEquals("Unexpected merged duration (2)", 30L, dt.duration);
		assertEquals("Unexpected merged unit (2)", TimeUnit.MINUTES, dt.unit);
	}

	@Test
	public final void testToSeconds() {
		DeltaTime dt = new DeltaTime(7, TimeUnit.MINUTES);
		assertEquals("Unexpected conversion to seconds (1)", 7L * 60, dt.toSeconds());
		dt = new DeltaTime(900, TimeUnit.MILLISECONDS);
		assertEquals("Unexpected conversion to seconds (2)", 1L, dt.toSeconds());
	}

	@Test
	public final void testToMilliseconds() {
		DeltaTime dt = new DeltaTime(7, TimeUnit.MINUTES);
		assertEquals("Unexpected conversion to milliseconds (1)", 7L * 60 * 1000, dt.toMilliseconds());
		dt = new DeltaTime(900, TimeUnit.MILLISECONDS);
		assertEquals("Unexpected conversion to milliseconds (2)", 900L, dt.toMilliseconds());
	}

	@Test
	public final void testNormalize() {
		DeltaTime dt = new DeltaTime(120000, TimeUnit.MILLISECONDS);
		dt.normalize();
		assertEquals("Unexpected normalized duration (1)", 2L, dt.duration);
		assertEquals("Unexpected normalized unit (1)", TimeUnit.MINUTES, dt.unit);
		dt = new DeltaTime(48, TimeUnit.HOURS);
		dt.normalize();
		assertEquals("Unexpected normalized duration (2)", 2L, dt.duration);
		assertEquals("Unexpected normalized unit (2)", TimeUnit.DAYS, dt.unit);
		dt = new DeltaTime(0, TimeUnit.MILLISECONDS);
		dt.normalize();
		assertEquals("Unexpected normalized duration (3)", 0L, dt.duration);
		assertEquals("Unexpected normalized unit (3)", TimeUnit.DAYS, dt.unit);
		dt = new DeltaTime(47, TimeUnit.HOURS);
		dt.normalize();
		assertEquals("Unexpected normalized duration (2)", 47L, dt.duration);
		assertEquals("Unexpected normalized unit (2)", TimeUnit.HOURS, dt.unit);
		dt = new DeltaTime(7, TimeUnit.DAYS);
		dt.normalize();
		assertEquals("Unexpected normalized duration (2)", 7L, dt.duration);
		assertEquals("Unexpected normalized unit (2)", TimeUnit.DAYS, dt.unit);
	}

	@Test
	public final void testToString() {
		DeltaTime dt = new DeltaTime(120, TimeUnit.MILLISECONDS);
		assertEquals("Unexpected delta time string (1)", "120 MS", dt.toString());
		dt = new DeltaTime(120, TimeUnit.SECONDS);
		assertEquals("Unexpected delta time string (2)", "120 S", dt.toString());
		dt = new DeltaTime(120, TimeUnit.MINUTES);
		assertEquals("Unexpected delta time string (2)", "120 M", dt.toString());
		dt = new DeltaTime(120, TimeUnit.HOURS);
		assertEquals("Unexpected delta time string (2)", "120 H", dt.toString());
		dt = new DeltaTime(120, TimeUnit.DAYS);
		assertEquals("Unexpected delta time string (2)", "120 D", dt.toString());
	}

	@Test
	public final void testEqualsObject() {
		DeltaTime dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		DeltaTime dt2 = new DeltaTime(7 * 60, TimeUnit.SECONDS);
		assertTrue("Unexpected inequality", dt1.equals(dt2));
		dt1 = new DeltaTime(123, TimeUnit.MILLISECONDS);
		dt2 = new DeltaTime(456, TimeUnit.MILLISECONDS);
		assertFalse("Unexpected equality", dt1.equals(dt2));
	}

	@Test
	public final void testCompareTo() {
		DeltaTime dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		DeltaTime dt2 = new DeltaTime(63, TimeUnit.SECONDS);
		assertEquals("Unexpected comparison result (1)", +1, dt1.compareTo(dt2));
		assertEquals("Unexpected comparison result (2)", -1, dt2.compareTo(dt1));
		dt1 = new DeltaTime(7, TimeUnit.MINUTES);
		dt2 = new DeltaTime(7 * 60, TimeUnit.SECONDS);
		assertEquals("Unexpected comparison result (3)", 0, dt1.compareTo(dt2));
	}

}
