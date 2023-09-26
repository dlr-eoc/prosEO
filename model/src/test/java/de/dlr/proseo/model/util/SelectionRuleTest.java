/**
 * SelectionRuleTest.java
 * 
 * (C) 2016 - 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.SimpleSelectionRuleTest;

/**
 * Unit test class for SelectionRule using various valid and invalid rules and various sensing time intervals.
 * 
 * @author Thomas Bassler
 *
 */
public class SelectionRuleTest {
    private static final Long TEST_MISSION_ID = 1L;
	private static final String TEST_MISSION_CODE = "S5P";
	private static final String TEST_PRODUCT_TYPE = "AUX_CH4";
	private static final Long TEST_PRODUCT_CLASS_ID = 4711L;
	private static final String TEST_PRODUCT_TYPE_IR = "L1B_IR/category:UVN,revision:2.0";
	private static final Long TEST_PRODUCT_CLASS_IR_ID = 815L;
	private static final String TEST_PRODUCT_TYPE_ECMWF = "AUX_ECMWF48";
	private static final Long TEST_PRODUCT_CLASS_ECMWF_ID = 333L;
	private static String[] itemObjects = {
			TEST_PRODUCT_TYPE + " Item 0", TEST_PRODUCT_TYPE + " Item 1", TEST_PRODUCT_TYPE + " Item 2", TEST_PRODUCT_TYPE + " Item 3",
			TEST_PRODUCT_TYPE + " Item 4", TEST_PRODUCT_TYPE + " Item 5", TEST_PRODUCT_TYPE + " Item 6", TEST_PRODUCT_TYPE + " Item 7",
			TEST_PRODUCT_TYPE + " Item 8", TEST_PRODUCT_TYPE + " Item 9", TEST_PRODUCT_TYPE + " Item 10", TEST_PRODUCT_TYPE + " Item 11"
	};
	// TODO Add test cases for MINCOVER(n)
	private static String[] rules = {
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0 H, 0 H) MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValIntersect(0 H, 0 H) MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValidity MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(12 H, 1 D) MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0 H, 0 H) OPTIONAL",
			"FOR " + TEST_PRODUCT_TYPE_ECMWF + " SELECT ValIntersect(0 H, 0 H) OPTIONAL",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0 H, 0 H) OR LatestValidity OPTIONAL",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0 H, 0 H) OR LatestValIntersect(36 H, 24 H) OPTIONAL",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValidityClosest(1 H, 1 H)",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValidityClosest(1 H, 0)",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValCover(0, 0)",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ClosestStartValidity(1 H, 0)",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ClosestStopValidity(1 H, 0)",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestStartValidity MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestStopValidity MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersectWithoutDuplicates(0 H, 0 H) MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LastCreated"
	};
	/**
	 *  Selection intervals, for items see {@link SelectionRuleTest#createSelectionItems()}
	 */
	private static Instant[][] startStopTimes = {
			// Short interval within item 0, 1, 3
			{ Instant.parse("2016-11-02T00:00:00Z"), Instant.parse("2016-11-02T00:00:05Z") },
			// Long interval completely covering items 0 - 3
			{ Instant.parse("2016-11-01T00:00:00Z"), Instant.parse("2016-11-04T00:00:00Z") },
			// Interval reaching into items 0, 1 (item 3 excluded, is on interval boundary)
			{ Instant.parse("2016-11-01T00:00:00Z"), Instant.parse("2016-11-01T20:00:00Z") },
			// Interval stretching from item 2
			{ Instant.parse("2016-11-03T00:00:00Z"), Instant.parse("2016-11-04T00:00:00Z") },
			// Interval outside (and after) items 0 - 3
			{ Instant.parse("2016-11-04T00:00:00Z"), Instant.parse("2016-11-05T00:00:00Z") },
			// Interval inside items 0 and 1, but only partially covered by item 3
			{ Instant.parse("2016-11-01T19:00:00Z"), Instant.parse("2016-11-01T21:00:00Z") },
			// Interval within item 0, 1, 3 (centered between item 1 start and item 2 start)
			{ Instant.parse("2016-11-02T08:30:00Z"), Instant.parse("2016-11-02T09:30:00Z") }
	};
	private static String ERROR_RESULT = "ERROR";
	
	/* Test objects */
	private Mission mission = new Mission();
	private ProductClass productClassCH4 = new ProductClass();
	private ProductClass productClassIR = new ProductClass();
	private ProductClass productClassECMWF = new ProductClass();
	private ProductClass targetProductClass = productClassCH4;

	/** The logger for this class */
	private static final Logger logger = LoggerFactory.getLogger(SelectionRuleTest.class);
	
	/**
	 * Ensure that the time-based classes in Java handle leap seconds (e. g. 2015-06-30 23:59:60) in a graceful way
	 * (i. e. without breaking the surrounding code by exceptions and such).
	 * <p>
	 * In practice, Instant treats the leap second as the previous second, and Date treats it as the next second.
	 * None of the two actually honours it.
	 * 
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		final String leapSecondString = "2015-06-30T23:59:60Z";
		final String nextSecondString = "2015-07-01T00:00:00Z";
		final String previousSecondString = "2015-06-30T23:59:59Z";
		final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
		
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date noLeapSecondDate = sdf.parse(leapSecondString);
		assertEquals("SimpleDateFormat class is not expected preserve leap second", nextSecondString, sdf.format(noLeapSecondDate));
		
		Instant leapSecondInstant = Instant.parse(leapSecondString);
		assertEquals("Instant class should hide leap second", previousSecondString, leapSecondInstant.toString());
		
		assertEquals("Date and Instant classes should differ by one epoch second",
				noLeapSecondDate.getTime(), leapSecondInstant.toEpochMilli() + 1000);
		
		assertEquals("Unexpected Date difference between " + previousSecondString + " and " + nextSecondString, 1000,
				sdf.parse(nextSecondString).getTime() - sdf.parse(previousSecondString).getTime());
		
		assertEquals("Unexpected Instant difference between " + previousSecondString + " and " + nextSecondString, 1000,
				Instant.parse(nextSecondString).toEpochMilli() - Instant.parse(previousSecondString).toEpochMilli());
		
	}

//	/**
//	 * @throws java.lang.Exception if an error occurs
//	 */
//	@AfterClass
//	public static void tearDownAfterClass() throws Exception {
//	}
//
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@Before
	public void setUp() throws Exception {
		// Create required mission and product classes
		mission.setId(TEST_MISSION_ID);
		mission.setCode(TEST_MISSION_CODE);

		productClassCH4.setId(TEST_PRODUCT_CLASS_ID);
		productClassCH4.setProductType(TEST_PRODUCT_TYPE);
		productClassCH4.setMission(mission);

		productClassIR.setId(TEST_PRODUCT_CLASS_IR_ID);
		productClassIR.setProductType(TEST_PRODUCT_TYPE_IR.split("/")[0]);
		productClassIR.setMission(mission);
		
		productClassECMWF.setId(TEST_PRODUCT_CLASS_ECMWF_ID);
		productClassECMWF.setProductType(TEST_PRODUCT_TYPE_ECMWF);
		productClassECMWF.setMission(mission);
		
		mission.getProductClasses().addAll(Arrays.asList(productClassCH4, productClassIR, productClassECMWF));
		logger.debug("Set of product classes {} set up for mission {}", mission.getProductClasses(), mission.getCode());
	}

//	/**
//	 * @throws java.lang.Exception if an error occurs
//	 */
//	@After
//	public void tearDown() throws Exception {
//	}

	/**
	 * Test method for {@link de.dlr.proseo.model.util.SelectionRule#parseSelectionRule(ProductClass, java.lang.String)}.
	 */
	@Test
	public final void testParseSelectionRule() {
		System.out.println("\n*** Starting test for parseSelectionRule() ***");

		String[] legalRules = {
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0, 0) MINCOVER(50)",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0, 0) OR LatestValidity",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0, 0) OR LatestValidity OPTIONAL;"
						+ " for " + TEST_PRODUCT_TYPE_ECMWF + " select latestvalintersect ( 12 h , 24 h )",
				"for " + TEST_PRODUCT_TYPE + " seleCT latestvalintersect(1\tm,2\ns) MANDatory",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValidity OR LatestValIntersect(2 D, 123456789 D) OPTIONAL",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0, 1) OR ValIntersect(1, 0) MINCOVER ( 70 )",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1 H, 1 D); FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1 D, 1 H)",
				"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT LatestValidityClosest(2 d, 2 d)",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValCover(600 s, 10 m)",
				"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT ClosestStartValidity(8800 ms, 4400 ms)",
				"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT ClosestStopValidity(10 m, 10 m)",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestStartValidity",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestStopValidity",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersectWithoutDuplicates(10 m, 10 m)",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LastCreated"
		};
		String[] legalResults = {
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0 D, 0 D) MINCOVER(50)",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0 D, 0 D) OR LatestValidity MANDATORY",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0 D, 0 D) OR LatestValidity OPTIONAL;"
						+ " FOR " + TEST_PRODUCT_TYPE_ECMWF + " SELECT LatestValIntersect(12 H, 1 D) MANDATORY",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValIntersect(1 M, 2 S) MANDATORY",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValidity OR LatestValIntersect(2 D, 123456789 D) OPTIONAL",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1 D, 1 D) MINCOVER(70)",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1 D, 1 D) MANDATORY",
				"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT LatestValidityClosest(0 D, 0 D) MANDATORY", // normalized delta times
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValCover(10 M, 10 M) MANDATORY",
				"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT ClosestStartValidity(8800 MS, 4400 MS) MANDATORY", // normalized delta times
				"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT ClosestStopValidity(10 M, 10 M) MANDATORY", // normalized delta times
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestStartValidity MANDATORY",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestStopValidity MANDATORY",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersectWithoutDuplicates(10 M, 10 M) MANDATORY",
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LastCreated MANDATORY"
		};

		String[] illegalRules = {
				"SELECT ValIntersect(0, 0)",							// the auxiliary product type must be specified
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(-1, 0)",		// no negative values
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1h, 1h)",		// the time unit must be separated from the numerical value by white space
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0, 0);",		// the semicolon is a rule separator, not a rule terminator
				"x y z a b c",											// completely senseless, but a sufficient number of parts
				"FOR " + TEST_PRODUCT_TYPE + " säläct ValIntersect(0, 0)",		// SELECT misspelt
				"FOR " + TEST_PRODUCT_TYPE + " / code: UVN SELECT LatestValidity(0, 0)",		// Wrongly formatted product type (containing blanks)
				"FOR " + TEST_PRODUCT_TYPE + "/code=UVN,revision=07 SELECT LatestValidity",		// Wrongly formatted product type ("=" instead of ":")
				"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT WrongPolicy(0, 0)",			// Invalid policy name
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect",				// Policy parameter missing
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValIntersect",		// Policy parameter missing
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ClosestStartValidity",		// Policy parameter missing
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ClosestStopValidity",		// Policy parameter missing
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersectWithoutDuplicates",		// Policy parameter missing
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValidity(0, 0)",		// Superfluous policy parameter
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestStartValidity(0, 0)",		// Superfluous policy parameter
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestStopValidity(0, 0)",		// Superfluous policy parameter
				"FOR " + TEST_PRODUCT_TYPE + " SELECT LastCreated(0, 0)",		// Superfluous policy parameter
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0), 0",		// Invalid policy parameter syntax
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0, 0) MANDATORY OPTIONAL", // Too many options
				"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(0, 0) MINCOVER(200)", // Invalid coverage percentage
				"",														// empty input
				"FOR Unknown.Type SELECT LatestValidity"				// Unknown product type
		};
		// Parts of the error messages (just enough to determine, whether the correct message was returned)
		String[] illegalResults = {
				"simple rule must follow",								// RULE_SYNTAX_ERROR
				"Delta time must be",									// RULE_TIME_FORMAT_ERROR
				"Delta time must be",									// RULE_TIME_FORMAT_ERROR
				"rule string is empty",									// RULE_EMPTY_ERROR
				"Simple rule must start with",							// RULE_FOR_EXPECTED_ERROR
				"Expected 'SELECT' keyword",							// RULE_SELECT_EXPECTED_ERROR
				"Expected 'SELECT' keyword",							// RULE_SELECT_EXPECTED_ERROR
				"Product type contains malformed filter condition",		// RULE_MALFORMED_FILTER_ERROR
				"Allowed policies are",									// RULE_POLICY_INVALID_ERROR
				"delta times must be specified",						// RULE_POLICY_TIMES_ERROR
				"delta times must be specified",						// RULE_POLICY_TIMES_ERROR
				"delta times must be specified",						// RULE_POLICY_TIMES_ERROR
				"delta times must be specified",						// RULE_POLICY_TIMES_ERROR
				"delta times must be specified",						// RULE_POLICY_TIMES_ERROR
				"no delta times may be specified",						// RULE_POLICY_LATVAL_ERROR
				"no delta times may be specified",						// RULE_POLICY_LATVAL_ERROR
				"no delta times may be specified",						// RULE_POLICY_LATVAL_ERROR
				"no delta times may be specified",						// RULE_POLICY_LATVAL_ERROR
				"policy must follow",									// RULE_POLICY_SYNTAX_ERROR
				"Expected end of text or",								// RULE_POLICY_END_ERROR
				"Invalid minimum coverage",								// RULE_MINCOVER_ERROR
				"rule string is empty",									// RULE_EMPTY_ERROR
				"Source product class not found"						// RULE_SOURCE_PRODUCT_CLASS_NOT_FOUND
		};
		int[] illegalOffsets = { 0, 32, 32, 38, 0, 12, 12, 13, 44, 31, 37, 39, 38, 48, 33, 38, 37, 30, 32, 38, 47, 0, 4 };
		
		// Test legal rules
		for (int i = 0; i < legalRules.length; ++i) {
			try {
				SelectionRule selectionRule = SelectionRule.parseSelectionRule(targetProductClass, legalRules[i]);
				assertEquals(String.format("For legal rule %d the parse result is unexpected: ", i), legalResults[i], selectionRule.toString());
			} catch (ParseException e) {
				fail(String.format("No ParseException expected for legal rule #%d, error message is '%s', offset is %d",
						i, e.getMessage(), e.getErrorOffset()));
			}
		}
		
		// Test illegal rules
		for (int i = 0; i < illegalRules.length; ++i) {
			try {
				SelectionRule selectionRule = SelectionRule.parseSelectionRule(targetProductClass, illegalRules[i]);
				fail(String.format("ParseException expected for illegal rule #%d, parse result is '%s'",
						i, selectionRule.toString()));
			} catch (ParseException e) {
				assertTrue(
						String.format("For illegal rule %d the error message is unexpected: '%s' at offset %d", i, e.getMessage(), e.getErrorOffset()), 
						e.getMessage().contains(illegalResults[i]));
				assertEquals(String.format("For illegal rule %d the error position is unexpected: ", i), illegalOffsets[i], e.getErrorOffset());
			}
		}
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.util.SelectionRule#merge(SelectionRule)}.
	 */
	@Test
	public final void testMerge() {
		System.out.println("\n*** Starting test for merge() ***");
		
		// Special product classes required for this test
		for (int i = 1; i <= 4; ++i) {
			ProductClass pc = new ProductClass();
			pc.setProductType("p" + i);
			mission.getProductClasses().add(pc);
		}
		
		// ValIntersect + ValIntersect --> ValIntersect with maximum of delta times
		// LatestValidity + ValIntersect --> LatestValidity OR ValIntersect
		// LatestValIntersect OR ValIntersect + ValIntersect --> LatestValIntersect OR ValIntersect with maximum of delta times

		String rule1 = "FOR p1 SELECT ValIntersect(1, 0); FOR p2 SELECT LatestValidity OPTIONAL; FOR p3 SELECT LatestValIntersect(3, 3) OR ValIntersect(1, 1) OPTIONAL";
		String rule2 = "FOR p2 SELECT ValIntersect(0, 0) OR LatestValidity; FOR p3 SELECT LatestValIntersect(2, 4) OPTIONAL; FOR p4 SELECT ValIntersect(0, 0)";
		String result1 =
				"FOR p1 SELECT ValIntersect(1 D, 0 D) MANDATORY; " +
				"FOR p2 SELECT LatestValidity OR ValIntersect(0 D, 0 D) MANDATORY; " +
				"FOR p3 SELECT LatestValIntersect(3 D, 4 D) OR ValIntersect(1 D, 1 D) OPTIONAL; " +
				"FOR p4 SELECT ValIntersect(0 D, 0 D) MANDATORY";
		
		try {
			SelectionRule firstRule = SelectionRule.parseSelectionRule(targetProductClass, rule1);
			SelectionRule secondRule = SelectionRule.parseSelectionRule(targetProductClass, rule2);
			SelectionRule mergedRule = firstRule.merge(secondRule);
			assertEquals("Unexpected merge result (1): ", result1, mergedRule.toString());
		} catch (ParseException e) {
			fail(String.format("No ParseException expected for legal rule (1), error message is '%s', offset is %d",
					e.getMessage(), e.getErrorOffset()));
		}

		// LatestValCover + LatestValCover --> LatestValCover with maximum of delta times
		// LatestValidityClosest + LatestValidityClosest --> LatestValidityClosest with normalized (effectively equal) delta times
		// LatestValidityClosest + LatestValidity --> LatestValidityClosest OR LatestValidity
		// LatestValCover + LatestValIntersect --> LatestValCover OR LatestValIntersect
		
		String rule3 = "FOR p1 SELECT LatestValCover(1, 0); FOR p2 SELECT LatestValidityClosest(0, 1 h); FOR p3 SELECT LatestValidityClosest(1 h, 0); FOR p4 SELECT LatestValCover(1, 0)";
		String rule4 = "FOR p1 SELECT LatestValCover(0, 1); FOR p2 SELECT LatestValidityClosest(1 h, 2 h); FOR p3 SELECT LatestValidity; FOR p4 SELECT LatestValIntersect(0, 1)";
		String result2 =
				"FOR p1 SELECT LatestValCover(1 D, 1 D) MANDATORY; " +
				"FOR p2 SELECT LatestValidityClosest(0 D, 1 H) MANDATORY; " +
				"FOR p3 SELECT LatestValidityClosest(1 H, 0 D) OR LatestValidity MANDATORY; " +
				"FOR p4 SELECT LatestValCover(1 D, 0 D) OR LatestValIntersect(0 D, 1 D) MANDATORY";
		
		try {
			SelectionRule firstRule = SelectionRule.parseSelectionRule(targetProductClass, rule3);
			SelectionRule secondRule = SelectionRule.parseSelectionRule(targetProductClass, rule4);
			SelectionRule mergedRule = firstRule.merge(secondRule);
			assertEquals("Unexpected merge result (2): ", result2, mergedRule.toString());
		} catch (ParseException e) {
			fail(String.format("No ParseException expected for legal rule (2), error message is '%s', offset is %d",
					e.getMessage(), e.getErrorOffset()));
		} catch (IllegalArgumentException e) {
			fail("No IllegalArgumentException expected for legal merge");
		}
		
		// Illegal: Merging of 'LatestValidityClosest' rules with (effectively) different delta times
		String rule5 = "FOR p2 SELECT LatestValidityClosest(0, 1 h)";
		String rule6 = "FOR p2 SELECT LatestValidityClosest(1 h, 0)";
		try {
			SelectionRule firstRule = SelectionRule.parseSelectionRule(targetProductClass, rule5);
			SelectionRule secondRule = SelectionRule.parseSelectionRule(targetProductClass, rule6);
			firstRule.merge(secondRule);
			fail("Merging LatestValidityClosest with different delta times should have failed");
		} catch (ParseException e) {
			fail(String.format("No ParseException expected for legal rule (3), error message is '%s', offset is %d",
					e.getMessage(), e.getErrorOffset()));
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	/**
	 * Create a collection of SelectionItem objects for use in selection tests:
	 * <pre>
	 * Timeline:   earlier <-----------------------------------------> later
	 * Item 0:               VVVVVV                   G
	 * Item 1:                 VVVVVV                   G
	 * Item 2:                           VVVVV             G
	 * Item 3:                   VVVVVV             G
	 * </pre>
	 * (VVVVVV = validity period, G = generation time)
	 * 
	 * @return a collection of SelectionItem tests
	 */
	private Collection<SelectionItem> createSelectionItems() {
		List<SelectionItem> items = new ArrayList<SelectionItem>();
		
		// First item
		items.add(new SelectionItem(
				TEST_PRODUCT_TYPE,
				Instant.parse("2016-11-01T12:00:00Z"),
				Instant.parse("2016-11-02T12:00:00Z"),
				Instant.parse("2016-11-24T16:21:00Z"),
				itemObjects[0]
			));
		// Later validity period, but overlapping with item 0, later generation time
		items.add(new SelectionItem(
				TEST_PRODUCT_TYPE,
				Instant.parse("2016-11-01T18:00:00Z"),
				Instant.parse("2016-11-02T18:00:00Z"),
				Instant.parse("2016-11-24T16:22:00Z"),
				itemObjects[1]
			));
		// Later validity period, after items 0, 1 and 3, later generation time
		items.add(new SelectionItem(
				TEST_PRODUCT_TYPE,
				Instant.parse("2016-11-02T22:00:00Z"),
				Instant.parse("2016-11-03T22:00:00Z"),
				Instant.parse("2016-11-24T16:23:00Z"),
				itemObjects[2]
			));
		// Later validity period, but overlapping with item 0 and item 1, earlier generation time
		items.add(new SelectionItem(
				TEST_PRODUCT_TYPE,
				Instant.parse("2016-11-01T20:00:00Z"),
				Instant.parse("2016-11-02T20:00:00Z"),
				Instant.parse("2016-11-24T16:19:00Z"),
				itemObjects[3]
			));
		
		return items;
	}

	/**
	 * Create a collection of SelectionItem objects with different item types for use in selection tests
	 * 
	 * @return a collection of SelectionItem tests
	 */
	private Collection<SelectionItem> createInvalidSelectionItems() {
		List<SelectionItem> items = new ArrayList<SelectionItem>();
		
		// First item
		items.add(new SelectionItem(
				TEST_PRODUCT_TYPE,
				Instant.parse("2016-11-01T12:00:00Z"),
				Instant.parse("2016-11-02T12:00:00Z"),
				Instant.parse("2016-11-24T16:21:00Z"),
				itemObjects[0]
			));
		// Different product type
		items.add(new SelectionItem(
				"some other type",
				Instant.parse("2016-11-01T18:00:00Z"),
				Instant.parse("2016-11-02T18:00:00Z"),
				Instant.parse("2016-11-24T16:22:00Z"),
				itemObjects[1]
			));
		
		return items;
	}
	
	/**
	 * Concatenate strings with intervening commas (i. e. concatenate("a", "b") --> "a,b")
	 * 
	 * @param strings the strings to concatenate
	 * @return the concatenated string
	 */
	private String concatenate(Object... strings) {
		StringBuilder s = new StringBuilder();
		for (Object in: strings) {
			if (0 < s.length()) {
				s.append(',');
			}
			if (in instanceof String) {
				s.append((String) in);
			}
		}
		return s.toString();
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.util.SelectionRule#selectUniqueItem(String, Collection, Instant, Instant)}.
	 */
	@Test
	public final void testSelectUniqueItem() {
		System.out.println("\n*** Starting test for selectUniqueItem() ***");
		Collection<SelectionItem> items = createSelectionItems();
		
		// Outer array per rule, inner array per time interval
		String[][] expectedResults = {
				// ValIntersect
				{ ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  itemObjects[2],
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT },
				// LatestValIntersect
				{ itemObjects[1],
				  itemObjects[2],
				  itemObjects[1],
				  itemObjects[2],
				  ERROR_RESULT,
				  itemObjects[1],
				  itemObjects[1] },
				// LatestValidity
				{ itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2] },
				// ValIntersect(12 H, 1 D)
				{ ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  itemObjects[2],
				  ERROR_RESULT,
				  ERROR_RESULT},
				// ValIntersect OPTIONAL
				{ ERROR_RESULT,			// like rule 0
				  ERROR_RESULT,			// like rule 0
				  ERROR_RESULT,			// like rule 0
				  itemObjects[2],		// like rule 0
				  "",					// empty
				  ERROR_RESULT,
				  ERROR_RESULT },
				// ValIntersect OPTIONAL with different product type --> no applicable rule
				{ ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT },
				// ValIntersect OR LatestValidity OPTIONAL
				{ ERROR_RESULT,			// like rule 0
				  ERROR_RESULT,			// like rule 0
				  ERROR_RESULT,			// like rule 0
				  itemObjects[2],		// like rule 0
				  itemObjects[2],		// like rule 2
				  ERROR_RESULT,
				  ERROR_RESULT },
				// ValIntersect OR LatestValIntersect OPTIONAL
				{ ERROR_RESULT,			// like rule 0
				  ERROR_RESULT,			// like rule 0
				  ERROR_RESULT,			// like rule 0
				  itemObjects[2],		// like rule 0
				  itemObjects[2],		// newest of item 1-3
				  ERROR_RESULT,
				  ERROR_RESULT },
				// LatestValidityClosest symmetrical
				{ itemObjects[3],
				  itemObjects[2],
				  itemObjects[0],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[3],
				  itemObjects[2]	// specially constructed test case for LatestValidityClosest
				  },
				// LatestValidityClosest asymmetrical (1 h, 0)
				{ itemObjects[3],
				  itemObjects[2],
				  itemObjects[0],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[3],
				  itemObjects[3] },	// specially constructed test case for LatestValidityClosest
				// LatestValCover
				{ itemObjects[1],	// item 1 has most recent generation time
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  itemObjects[1],	// specially constructed test case for LatestValCover
				  itemObjects[1] },
				// ClosestStartValidity asymmetrical (1 h, 0)
				{ itemObjects[3],
				  itemObjects[0],
				  itemObjects[0],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[1],
				  itemObjects[3] },	// specially constructed test case for LatestValidityClosest
				// ClosestStopValidity asymmetrical (1 h, 0)
				{ itemObjects[0],
				  itemObjects[2],
				  itemObjects[0],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[0],
				  itemObjects[0] },	// specially constructed test case for LatestValidityClosest
				// LatestStartValidity
				{ itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2] },
				// LatestStopValidity
				{ itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2] },
				// ValIntersectWithoutDuplicates
				{ ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  itemObjects[2],
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT },
				// LastCreated
				{ itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2] }
		};

		// Perform tests on valid input
		for (int i = 0; i < rules.length; ++i) {
			for (int k = 0; k < startStopTimes.length; ++k) {
				try {
					System.out.println(String.format("Testing rule %d and time interval %d", i, k));
					String result = (String) SelectionRule.parseSelectionRule(targetProductClass, rules[i]).selectUniqueItem(
							TEST_PRODUCT_TYPE, items, startStopTimes[k][0], startStopTimes[k][1]);
					result = ( null == result ? "" : result);
					System.out.println(String.format("... result is: %s", result));
					assertEquals(String.format("Unexpected selection result for rule %d and time interval %d: ", i, k),
							expectedResults[i][k], result);
				} catch (NoSuchElementException e) {
					if (ERROR_RESULT.equals(expectedResults[i][k])) {
						System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
					}
				} catch (IllegalArgumentException | ParseException e) {
					fail(String.format("Test with rule %d and time interval %d throws exception %s", i, k, e.getMessage()));
				}
			}
		}
		
		// Test invalid input
		try {
			System.out.println("Testing invalid input");
			String result = (String) SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectUniqueItem(TEST_PRODUCT_TYPE,
					createInvalidSelectionItems(), startStopTimes[0][0], startStopTimes[0][1]);
			fail(String.format("Unexpected selection result for test with invalid input: %s", result));
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (NoSuchElementException | ParseException e) {
			fail(String.format("Test with invalid input throws unexpected exception %s", e.getMessage()));
		}
		
		try {
			System.out.println("Testing empty input");
			String result = (String) SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectUniqueItem(TEST_PRODUCT_TYPE,
					new ArrayList<SelectionItem>(), startStopTimes[0][0], startStopTimes[0][1]);
			fail(String.format("Unexpected selection result for test with empty input: %s", result));
		} catch (NoSuchElementException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (IllegalArgumentException | ParseException e) {
			fail(String.format("Test with empty input throws unexpected exception %s", e.getMessage()));
		}
		
		try {
			System.out.println("Testing null value for item collection");
			String result = (String) SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectUniqueItem(TEST_PRODUCT_TYPE,
					null, startStopTimes[0][0], startStopTimes[0][1]);
			fail(String.format("Unexpected selection result for test with null value for item collection: %s", result));
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (NoSuchElementException | ParseException e) {
			fail(String.format("Test with null value for item collection throws unexpected exception %s", e.getMessage()));
		}
		
		try {
			System.out.println("Testing null value for start time");
			String result = (String) SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectUniqueItem(TEST_PRODUCT_TYPE,
					items, null, startStopTimes[0][1]);
			fail(String.format("Unexpected selection result for test with null value for item collection: %s", result));
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (NoSuchElementException | ParseException e) {
			fail(String.format("Test with null value for item collection throws unexpected exception %s", e.getMessage()));
		}
		
		try {
			System.out.println("Testing null value for stop time");
			String result = (String) SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectUniqueItem(TEST_PRODUCT_TYPE,
					items, startStopTimes[0][0], null);
			fail(String.format("Unexpected selection result for test with null value for item collection: %s", result));
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (NoSuchElementException | ParseException e) {
			fail(String.format("Test with null value for item collection throws unexpected exception %s", e.getMessage()));
		}
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.util.SelectionRule#selectItems(String, Collection, Instant, Instant)}.
	 */
	@Test
	public final void testSelectItems() {
		System.out.println("\n*** Starting test for selectItems() ***");
		Collection<SelectionItem> items = createSelectionItems();
		
		// Outer array per rule, inner array per time interval
		String[][] expectedResults = {
				// ValIntersect
				{ concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),
				  concatenate(itemObjects[0], itemObjects[1], itemObjects[2], itemObjects[3]),
				  concatenate(itemObjects[0], itemObjects[1]                                ),
				  concatenate(                                itemObjects[2]                ),
				  ERROR_RESULT,
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]) },
				// LatestValIntersect
				{ itemObjects[1],
				  itemObjects[2],
				  itemObjects[1],
				  itemObjects[2],
				  ERROR_RESULT,
				  itemObjects[1],
				  itemObjects[1] },
				// LatestValidity
				{ itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2] },
				// ValIntersect(12 H, 1 D)
				{ concatenate(itemObjects[0], itemObjects[1], itemObjects[2], itemObjects[3]),
				  concatenate(itemObjects[0], itemObjects[1], itemObjects[2], itemObjects[3]),
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),
				  concatenate(                itemObjects[1], itemObjects[2], itemObjects[3]),
				  concatenate(                                itemObjects[2]                ),
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),
				  concatenate(itemObjects[0], itemObjects[1], itemObjects[2], itemObjects[3]) },
				// ValIntersect OPTIONAL
				{ concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),		// like rule 0
				  concatenate(itemObjects[0], itemObjects[1], itemObjects[2], itemObjects[3]),		// like rule 0
				  concatenate(itemObjects[0], itemObjects[1]                                ),		// like rule 0
				  concatenate(                                itemObjects[2]                ),		// like rule 0
				  "",																				// empty
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]) },
				// ValIntersect OPTIONAL with different product type --> no applicable rule (empty result)
				{ "",
				  "",
				  "",
				  "",
				  "",
				  "",
				  "" },
				// ValIntersect OR LatestValidity OPTIONAL
				{ concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),		// like rule 0
				  concatenate(itemObjects[0], itemObjects[1], itemObjects[2], itemObjects[3]),		// like rule 0
				  concatenate(itemObjects[0], itemObjects[1]                                ),		// like rule 0
				  concatenate(                                itemObjects[2]                ),		// like rule 0
				  concatenate(                                itemObjects[2]                ),		// like rule 2
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]) },
				// ValIntersect OR LatestValIntersect OPTIONAL
				{ concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),		// like rule 0
				  concatenate(itemObjects[0], itemObjects[1], itemObjects[2], itemObjects[3]),		// like rule 0
				  concatenate(itemObjects[0], itemObjects[1]                                ),		// like rule 0
				  concatenate(                                itemObjects[2]                ),		// like rule 0
				  concatenate(                                itemObjects[2]                ),		// newest of item 1-3
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),
				  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]) },
				// LatestValidityClosest symmetrical
				{ itemObjects[3],
				  itemObjects[2],
				  itemObjects[0],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[3],
				  itemObjects[2]	// specially constructed test case for LatestValidityClosest
				  },
				// LatestValidityClosest asymmetrical (1 h, 0)
				{ itemObjects[3],
				  itemObjects[2],
				  itemObjects[0],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[3],
				  itemObjects[3] },	// specially constructed test case for LatestValidityClosest
				// LatestValCover
				{ itemObjects[1],	// item 1 has most recent generation time
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  ERROR_RESULT,
				  itemObjects[1],	// specially constructed test case for LatestValCover
				  itemObjects[1] },

				// ClosestStartValidity asymmetrical (1 h, 0)
				{ itemObjects[3],
				  itemObjects[0],
				  itemObjects[0],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[1],
				  itemObjects[3] },	// specially constructed test case for LatestValidityClosest
				// ClosestStopValidity asymmetrical (1 h, 0)
				{ itemObjects[0],
				  itemObjects[2],
				  itemObjects[0],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[0],
				  itemObjects[0] },	// specially constructed test case for LatestValidityClosest
				// LatestStartValidity
				{ itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2] },
				// LatestStopValidity
				{ itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2] },
				// ValIntersectWithoutDuplicates
				{ concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),
					  concatenate(itemObjects[0], itemObjects[1], itemObjects[2], itemObjects[3]),
					  concatenate(itemObjects[0], itemObjects[1]                                ),
					  concatenate(                                itemObjects[2]                ),
					  ERROR_RESULT,
					  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]),
					  concatenate(itemObjects[0], itemObjects[1],                 itemObjects[3]) },
				// LastCreated
				{ itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2],
				  itemObjects[2] }
		};
		
		// Perform tests on valid input
		for (int i = 0; i < rules.length; ++i) {
			for (int k = 0; k < startStopTimes.length; ++k) {
				List<Object> result = null;
				try {
					System.out.println(String.format("Testing rule %d and time interval %d", i, k));
					result = SelectionRule.parseSelectionRule(targetProductClass, rules[i]).selectItems(
							TEST_PRODUCT_TYPE, items, startStopTimes[k][0], startStopTimes[k][1]);
					
					// Sequence of results is unpredictable: Based on Set! - Order result objects according to itemObjects sequence
					String resultString = null;
					if (null == result) {
						resultString = "";
					} else {
						Object[] resultObjects = new Object[result.size()];
						int resultIndex = 0;
						for (int itemIndex = 0; itemIndex < itemObjects.length; ++itemIndex) {
							for (Object resultObject : result) {
								if (itemObjects[itemIndex].equals(resultObject)) {
									resultObjects[resultIndex] = resultObject;
									++resultIndex;
									break;
								}
							}
						}
						resultString = concatenate(resultObjects);
					}
					
					System.out.println(String.format("... result is: %s", resultString));
					assertEquals(String.format("Unexpected selection result for rule %d and time interval %d: ", i, k),
							expectedResults[i][k], resultString);
				} catch (NoSuchElementException e) {
					if (ERROR_RESULT.equals(expectedResults[i][k])) {
						System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
					}
				} catch (IllegalArgumentException | ParseException e) {
					fail(String.format("Test with rule %d and time interval %d throws exception %s", i, k, e.getMessage()));
				}
			}
		}
		
		// Test invalid input
		try {
			System.out.println("Testing invalid input");
			List<Object> result = SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectItems(TEST_PRODUCT_TYPE,
					createInvalidSelectionItems(), startStopTimes[0][0], startStopTimes[0][1]);
			fail(String.format("Unexpected selection result for test with invalid input: %s", concatenate(result.toArray())));
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (NoSuchElementException | ParseException e) {
			fail(String.format("Test with invalid input throws unexpected exception %s", e.getMessage()));
		}
		
		try {
			System.out.println("Testing empty input");
			List<Object> result = SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectItems(TEST_PRODUCT_TYPE,
					new ArrayList<SelectionItem>(), startStopTimes[0][0], startStopTimes[0][1]);
			fail(String.format("Unexpected selection result for test with empty input: %s", concatenate(result.toArray())));
		} catch (NoSuchElementException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (IllegalArgumentException | ParseException e) {
			fail(String.format("Test with empty input throws unexpected exception %s", e.getMessage()));
		}
		
		try {
			System.out.println("Testing null value for item collection");
			List<Object> result = SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectItems(TEST_PRODUCT_TYPE,
					null, startStopTimes[0][0], startStopTimes[0][1]);
			fail(String.format("Unexpected selection result for test with null value for item collection: %s", concatenate(result.toArray())));
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (NoSuchElementException | ParseException e) {
			fail(String.format("Test with null value for item collection throws unexpected exception %s", e.getMessage()));
		}
		
		try {
			System.out.println("Testing null value for start time");
			List<Object> result = SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectItems(TEST_PRODUCT_TYPE,
					items, null, startStopTimes[0][1]);
			fail(String.format("Unexpected selection result for test with null value for item collection: %s", concatenate(result.toArray())));
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (NoSuchElementException | ParseException e) {
			fail(String.format("Test with null value for item collection throws unexpected exception %s", e.getMessage()));
		}
		
		try {
			System.out.println("Testing null value for stop time");
			List<Object> result = SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectItems(TEST_PRODUCT_TYPE,
					items, startStopTimes[0][0], null);
			fail(String.format("Unexpected selection result for test with null value for item collection: %s", concatenate(result.toArray())));
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("... expected exception thrown: %s", e.getMessage()));
		} catch (NoSuchElementException | ParseException e) {
			fail(String.format("Test with null value for item collection throws unexpected exception %s", e.getMessage()));
		}
		
		// Test special case with point-in-time item (start time == stop time)
		SelectionItem pointInTimeItem = new SelectionItem(
				TEST_PRODUCT_TYPE,
				Instant.parse("2016-11-02T00:00:00Z"),
				Instant.parse("2016-11-02T00:00:00Z"),
				Instant.parse("2016-11-24T16:21:00Z"),
				itemObjects[0]
			);
		try {
			List<Object> result = SelectionRule.parseSelectionRule(targetProductClass, rules[0]).selectItems(TEST_PRODUCT_TYPE,
					Arrays.asList(pointInTimeItem), startStopTimes[0][0], startStopTimes[0][0]);
			if (1 != result.size()) {
				fail(String.format("Unexpected selection result for test with point-in-time item: %s", concatenate(result.toArray())));
			}
		} catch (NoSuchElementException | IllegalArgumentException | ParseException e) {
			fail(String.format("Test with point-in-time item throws unexpected exception %s", e.getMessage()));
		}
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.model.util.SelectionRule#getSimpleRules()}.
	 */
	@Test
	public final void testGetSimpleRules() {
		System.out.println("\n*** Starting test for getSimpleRules() ***");
		
		try {
			SelectionRule rule = SelectionRule.parseSelectionRule(targetProductClass, rules[0]);
			List<SimpleSelectionRule> simpleRules = rule.getSimpleRules();
			assertNotNull("List of simple selection rules missing", simpleRules);
			assertEquals("Unexpected number of simple selection rules:", 1, simpleRules.size());
			SimpleSelectionRule simpleRule = simpleRules.get(0);
			assertEquals("Unexpected source product class:", TEST_PRODUCT_TYPE, simpleRule.getSourceProductClass().getProductType());
			assertEquals("Unexpected number of simple policies:", 1, simpleRule.getSimplePolicies().size());
		} catch (IllegalArgumentException | ParseException e) {
			fail(String.format("No exception expected for legal rule, error message is '%s'", e.getMessage()));
		}
		
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.util.SelectionRule#hasPolicyFor(String)}.
	 */
	@Test
	public final void testHasPolicyFor() {
		System.out.println("\n*** Starting test for hasPolicyFor() ***");
		
		try {
			SelectionRule rule = SelectionRule.parseSelectionRule(targetProductClass, rules[0]);
			String auxProductType = TEST_PRODUCT_TYPE;
			assertTrue(String.format("Rule %s should contain policy for %s", rules[0], auxProductType), 
					rule.hasPolicyFor(auxProductType));
			auxProductType = "garbage";
			assertFalse(String.format("Rule %s should not contain policy for %s", rules[0], auxProductType), 
					rule.hasPolicyFor(auxProductType));
		} catch (IllegalArgumentException | ParseException e) {
			fail(String.format("No exception expected for legal rule, error message is '%s'", e.getMessage()));
		}
		
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.util.SelectionRule#toString()}.
	 */
	@Test
	public final void testToString() {
		System.out.println("\n*** Starting test for toString() ***");
		try {
			SelectionRule selectionRule = SelectionRule.parseSelectionRule(
					targetProductClass, "FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1 H, 1 D); FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1 D, 1 H)");
			assertEquals("Unexpected string value for rule: ",
					"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1 D, 1 D) MANDATORY", selectionRule.toString());
		} catch (ParseException e) {
			fail(String.format("No ParseException expected for legal rule, error message is '%s', offset is %d",
					e.getMessage(), e.getErrorOffset()));
		}
	}

}
