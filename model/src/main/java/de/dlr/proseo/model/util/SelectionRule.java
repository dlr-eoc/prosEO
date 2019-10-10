/**
 * SelectionRule.java
 * 
 * (C) 2016 - 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import java.sql.Date;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimplePolicy;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.Parameter.ParameterType;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.SimplePolicy.DeltaTime;

/**
 * This class allows to describe temporal selection policies for item types, to manipulate selection policies (combining and
 * chaining), to select valid items from a container of items, and to generate product query objects from a selection rule (the
 * result of the product query can then be fed into the item selection method to filter the actually valid items).
 * <p>
 * A selection rule can be described in text form according to the following grammar (words in single quotes denote language
 * key words, angled brackets denote optional elements, an ellipsis denotes zero or more repetitions of the element preceding it,
 * a vertical bar denotes choice between two or more elements, all other punctuation characters are to be taken literally):
 * 
 * <pre> rule ::= simple_rule [ ; rule ]
 * simple_rule ::= 'FOR' source_product_type 'SELECT' policy [ 'OPTIONAL' | 'MANDATORY' | 'MINCOVER' ( digit ... ) ]
 * policy ::= simple_policy [ 'OR' policy ]
 * simple_policy ::= policy_name [ ( delta_time , delta_time ) ]
 * policy_name ::= 'ValCover' | 'LatestValCover' | 'ValIntersect' | 'latestValIntersect' | 'LatestValidityClosest' |
 *                 'BestCenteredCover' | 'LatestValCoverClosest' | 'LargestOverlap' | 'LargestOverlap85' | 
 *                 'LatestValidity' | 'LatestValCoverNewestValidity'
 * source_product_type ::= product_type[/parameter_key:parameter_value[,parameter_key:parameter_value] ...] 
 * delta_time ::= digit ... [ 'd' | 'h' | 'm' | 's' ]
 * digit ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9</pre>
 * 
 * Key words are not case sensitive, this includes the names of selection policies. Punctuation characters need not (but may) be
 * enclosed by white space.
 * <p>
 * If for a simple rule neither OPTIONAL nor MINCOVER or MANDATORY is specified, MANDATORY is the default. If for a delta time, neither 'd'
 * (days), 'h' (hours), 'm' (minutes) nor 's' (seconds) is specified, 'd' (days) is the default.
 * <p>
 * A simple rule marked as OPTIONAL is fulfilled even if no items satisfy the rule. For a simple rule marked MANDATORY at
 * least one item must fulfil the rule criteria. If the rule is marked as MINCOVER(n), the set of items fulfilling the rule
 * criteria must as a whole cover at least n % of the validity interval given as selection parameters. 
 * MINCOVER implies MANDATORY (even MINCOVER(0), in which case it is acceptable, if all items are outside of the validity interval).
 * <p>
 * The 'OR' operator is a short-circuited operator, i. e. if during the selection of items the first simple policy yields
 * a non-empty result set, this result set is returned, and only if the result set is empty, the policy following the 'OR'
 * is evaluated.
 * <p>
 * Multiple occurrences of 'LatestValidityClosest' for the same item type (product type) in one set of selection rules
 * are only allowed, if all occurrences have the same delta times. (This policy actually refers to a point in time and 
 * not to a time interval, therefore a merge can only be done between policies referring to the same point in time.)
 * <p>
 * A 'source_product_type' consists of prosEO product (class) type and optionally parameter values to filter
 * items within the collection. Parameter keys and values are separated from the product type by a slash ('/'), and
 * are separated from each other by commas. Parameter key and value for each entry are separated by colons (':').
 * There must not be any white space between any of the elements of the product type. Note that for the purpose of
 * the selection, source product types with the same product type name, but different parameters are considered different
 * source product types. The parameter keys and values are only evaluated for the generation of product query objects. 
 * <p>
 * The following are examples for legal rules:
 * <ul>
 *   <li>FOR AUX_CH4 SELECT ValIntersect(0, 0)</li>
 *   <li>FOR AUX_CH4 SELECT ValIntersect(0, 0) OR LatestValidity</li>
 *   <li>FOR AUX_CH4 SELECT ValIntersect(0, 0) OR LatestValidity OPTIONAL; for AUX_ECMWF-FC48 select latestvalintersect ( 12 h , 24 h )</li>
 * </ul>
 * Note that in the last example, the name of the product type is in upper case, although the keywords are not. Product type
 * names are case sensitive!
 * <p>
 * The following rule examples are not valid:
 * <ul>
 *   <li>SELECT ValIntersect(0, 0) <i>(the auxiliary product type must be specified)></i></li>
 *   <li>FOR AUX_CH4 SELECT ValIntersect(-1, 0) <i>(no negative values)</i></li>
 *   <li>FOR AUX_CH4 SELECT ValIntersect(1h, 1h) <i>(the time unit must be separated from the numerical value by white space)</i></li>
 *   <li>FOR AUX_CH4 SELECT ValIntersect(0, 0); <i>(the semicolon is a rule separator, not a rule terminator)</i></li>
 * </ul>
 * 
 * Allowed selection policies are described in ESA's "Generic IPF Interface Specification" (MMFI-GSEG-EOPG-TN-07-0003, issue 1.8),
 * applicable product types are mission-specific and defined by the available product classes (attribute productType) for
 * the respective mission.
 * <p>
 * @author Dr. Thomas Bassler
 *
 */
public class SelectionRule {
	
	/* Selection rule syntax elements */
	public static final String RULE_SEPARATOR = ";";
	public static final String RULE_FOR = "FOR";
	public static final String RULE_SELECT = "SELECT";
	public static final String RULE_OPTIONAL = "OPTIONAL";
	public static final String RULE_MANDATORY = "MANDATORY";
	public static final String RULE_MINCOVER = "MINCOVER";
	public static final Pattern RULE_MINCOVER_PATTERN = Pattern.compile("^" + RULE_MINCOVER + "\\s*\\(\\s*(\\d+)\\s*\\)$");
	public static final String RULE_POLICY_OR = "OR";
	public static final String RULE_PAREN_OPEN = "(";
	public static final String RULE_PAREN_CLOSE = ")";
	public static final String RULE_COMMA = ",";
	public static final String RULE_POLICY_VALCOV = "VALCOVER";
	public static final String RULE_POLICY_LATVALCOV = "LATESTVALCOVER";
	public static final String RULE_POLICY_VALINT = "VALINTERSECT";
	public static final String RULE_POLICY_LATVALINT = "LATESTVALINTERSECT";
	public static final String RULE_POLICY_LATVALCLO = "LATESTVALIDITYCLOSEST";
	public static final String RULE_POLICY_BESTCENT = "BESTCENTEREDCOVER";
	public static final String RULE_POLICY_LATCOVCLO = "LATESTVALCOVERCLOSEST";
	public static final String RULE_POLICY_OVERLAP = "LARGESTOVERLAP";
	public static final String RULE_POLICY_OVERLAP85 = "LARGESTOVERLAP85";
	public static final String RULE_POLICY_LATVALCOVNEW = "LATESTVALCOVERNEWESTVALIDITY";
	public static final String RULE_POLICY_LATVAL = "LATESTVALIDITY";
	public static final String RULE_DELTA_DAYS = "D";
	public static final String RULE_DELTA_HOURS = "H";
	public static final String RULE_DELTA_MINS = "M";
	public static final String RULE_DELTA_SECS = "S";
	
	/* Messages for parsing errors */
	private static final String RULE_EMPTY_ERROR = "Syntax error: The rule string is empty.";
	private static final String RULE_SYNTAX_ERROR = "Syntax error: A simple rule must follow the pattern '" + RULE_FOR
			+ "' product_type '" + RULE_SELECT + "' policy [ '" + RULE_OPTIONAL + "' | '" + RULE_MANDATORY + "' | '" + RULE_MINCOVER + "'(n) ], found: ";
	private static final String RULE_OPTMANCOVPART_ERROR = "Only one of [ '" + RULE_OPTIONAL + "' | '" + RULE_MANDATORY + "' | '" + RULE_MINCOVER + "'(n) ] allowed, found: ";
	private static final String RULE_MINCOVER_ERROR = "Invalid minimum coverage percentage (allowed values 0 .. 100), found: ";
	private static final String RULE_FOR_EXPECTED_ERROR = "Syntax error: Simple rule must start with '" + RULE_FOR + "' keyword, found: ";
	private static final String RULE_SELECT_EXPECTED_ERROR = "Syntax error: Expected '" + RULE_SELECT + "' keyword, found: ";
	private static final String RULE_POLICY_INVALID_ERROR = "Syntax error: Allowed policies are '" + RULE_POLICY_VALINT
			+ "', '" + RULE_POLICY_LATVALINT + "', '" + RULE_POLICY_LATVAL + "', '" + RULE_POLICY_LATVALCLO
			+ "' and '" + RULE_POLICY_LATVALCOV + "', found: ";
	private static final String RULE_POLICY_TIMES_ERROR = "Error: For policies '" + RULE_POLICY_VALINT + "', '"
			+ RULE_POLICY_LATVALINT + "', '" + RULE_POLICY_LATVALCLO + "' and '"
			+ RULE_POLICY_LATVALCOV + "' delta times must be specified as parameter, found: ";
	private static final String RULE_POLICY_LATVAL_ERROR = "Error: For the policy '" + RULE_POLICY_LATVAL
			+ "' no delta times may be specified, found: ";
	private static final String RULE_POLICY_SYNTAX_ERROR = "Syntax error: A policy must follow the pattern policy_name [ " +
			RULE_PAREN_OPEN + " delta_time " + RULE_COMMA + " delta_time " + RULE_PAREN_CLOSE + " ], found: ";
	private static final String RULE_POLICY_END_ERROR = "Syntax error: Expected end of text or one of {'" + RULE_POLICY_OR + "', '"
			+ RULE_OPTIONAL + "', '" + RULE_MANDATORY + "', '" + RULE_MINCOVER + "', '" + RULE_SEPARATOR + "'}, found: ";
	private static final String RULE_TIME_FORMAT_ERROR = "Syntax error: Delta time must be of the form digit ... [ '"
			+ RULE_DELTA_DAYS + "' | '" + RULE_DELTA_HOURS + "' | '" + RULE_DELTA_MINS + "' | '" + RULE_DELTA_SECS + "'], found: ";
	private static final String RULE_MALFORMED_PRODUCT_TYPE_ERROR = "Product type contains more than one slash ('/'): ";
	private static final String RULE_MISSING_FILTER_ERROR = "Product type with slash ('/') does not contain filter conditions, found: ";
	private static final String RULE_EMPTY_FILTER_ERROR = "Product type contains empty filter condition, found: ";
	private static final String RULE_MALFORMED_FILTER_ERROR = "Product type contains malformed filter condition, found: ";
	private static final String RULE_SOURCE_PRODUCT_CLASS_NOT_FOUND = "Source product class not found: ";
	
	private static final String MSG_NO_UNIQUE_ITEM_FOUND = "Multiple items found for selection rule: ";
	private static final String MSG_MISSING_ARGUMENTS = "Null arguments not allowed";
	private static final String MSG_START_OR_STOP_TIME_NULL = "Start and stop times must not be null";
	private static final String MSG_NULL_SELECTION_RULE = "Null value for selectionRuleString not allowed!";
	private static final String MSG_NULL_TARGET_PRODUCT_CLASS = "Null value for targetProductClass not allowed!";
	private static final String MSG_WRONG_ITEM_CLASS = "Unexpected item class ";
	
	/** The logger for this class */
	private static final Logger logger = LoggerFactory.getLogger(SelectionRule.class);
	
	/** The simple selection rules making up this rule, mapped to (aux) product types */
	private Map<String, SimpleSelectionRule> simpleRules = new LinkedHashMap<String, SimpleSelectionRule>();
		
	/**
	 * This nested class describes a selectable item consisting of an item type (auxiliary product type), a validity start
	 * time, a validity end time, and the original object for which the selection shall be effected. As this is only a
	 * support structure, all instance variables are publicly accessible.
	 */
	public static class SelectionItem {
		/** The product type of the item */
		public String itemType;
		/** The start time of the item validity period */
		public Instant startTime;
		/** The end time of the item validity period */
		public Instant stopTime;
		/** The generation time of the item */
		public Instant generationTime;
		/** The original object belonging to the item */
		public Object itemObject;
		
		/**
		 * Default constructor
		 */
		public SelectionItem() {}
		
		/**
		 * Convenience constructor to create a SelectionItem with all attributes set at once.
		 * 
		 * @param itemType the item product type to set
		 * @param startTime the start time of the item validity period
		 * @param stopTime the end time of the item validity period
		 * @param generationTime the generation time of the item
		 * @param itemObject the original object belonging to the item
		 */
		public SelectionItem(String itemType, Instant startTime, Instant stopTime, Instant generationTime, Object itemObject) {
			this.itemType = itemType;
			this.startTime = startTime;
			this.stopTime = stopTime;
			this.generationTime = generationTime;
			this.itemObject = itemObject;
		}
		
		/**
		 * Convenience method to set the validity start time from a Unix time value (milliseconds since 1970-01-01T00:00:00 UTC)
		 * 
		 * @param time the time in milliseconds to set the start time from
		 */
		public void setStartTime(long time) {
			startTime = Instant.ofEpochMilli(time);
		}
		
		/**
		 * Convenience method to set the validity start time from a Java date object
		 * 
		 * @param date the date object to set the start time from
		 */
		public void setStartTime(Date date) {
			setStartTime(date.getTime());
		}

		/**
		 * Convenience method to set the validity end time from a Unix time value (milliseconds since 1970-01-01T00:00:00 UTC)
		 * 
		 * @param time the time in milliseconds to set the end time from
		 */
		public void setStopTime(long time) {
			stopTime = Instant.ofEpochMilli(time);
		}
		
		/**
		 * Convenience method to set the validity end time from a Java date object
		 * 
		 * @param date the date object to set the end time from
		 */
		public void setStopTime(Date date) {
			setStopTime(date.getTime());
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "{ type: " + itemType + ", start: " + startTime + ", stop: " + stopTime + ", generated: " 
					+ generationTime + ", object: " + itemObject + " }";
		}

		@Override
		public int hashCode() {
			return Objects.hash(generationTime, itemObject, itemType, startTime, stopTime);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof SelectionItem))
				return false;
			SelectionItem other = (SelectionItem) obj;
			return Objects.equals(generationTime, other.generationTime) && Objects.equals(itemObject, other.itemObject)
					&& Objects.equals(itemType, other.itemType) && Objects.equals(startTime, other.startTime)
					&& Objects.equals(stopTime, other.stopTime);
		}
	}
	
	/**
	 * Gets all simple selection rules contained in the selection rule
	 * 
	 * @return a list of simple selection rules
	 */
	public List<SimpleSelectionRule> getSimpleRules() {
		List<SimpleSelectionRule> newSimpleRules = new ArrayList<>();
		newSimpleRules.addAll(simpleRules.values());
		return newSimpleRules;
	}
	
	/**
	 * Syntactical analysis of a simple rule: FOR product_type SELECT policy [ OPTIONAL | MANDATORY | MINCOVER(n) ]
	 * (see {@link SelectionRule grammar definition})
	 * @param targetProductClass the product class, which requires source products defined by this rule
	 * @param simpleRuleString the text string containing the simple rule to analyse (without preceding white space)
	 * @param offset the character offset within the complete rule text
	 * 
	 * @return a SimpleSelectionRule object created from the text contents
	 * @throws ParseException if the simple rule text contains syntactical or semantical errors
	 */
	private static SimpleSelectionRule parseSimpleRule(ProductClass targetProductClass, String simpleRuleString, int offset) throws ParseException {
		logger.debug("... parsing simple rule '" + simpleRuleString + "' at offset " + offset);
		
		// Check simple rule syntax
		String[] ruleParts = simpleRuleString.split("\\s+", 4);
		if (4 > ruleParts.length) {
			throw new ParseException(RULE_SYNTAX_ERROR + simpleRuleString, offset);
		}
		if (!RULE_FOR.equals(ruleParts[0].toUpperCase())) {
			throw new ParseException(RULE_FOR_EXPECTED_ERROR + ruleParts[0], offset);
		}
		logger.debug("... source product type found: " + ruleParts[1]);
		if (!RULE_SELECT.equals(ruleParts[2].toUpperCase())) {
			throw new ParseException(RULE_SELECT_EXPECTED_ERROR + ruleParts[2], offset + simpleRuleString.indexOf(ruleParts[2]));
		}
		
		// Create simple rule from its policies
		SimpleSelectionRule simpleRule = parsePolicy(ruleParts[3], offset + simpleRuleString.indexOf(ruleParts[3]));
		simpleRule.setTargetProductClass(targetProductClass);
		
		// Parse product type
		simpleRule.setFilteredSourceProductType(ruleParts[1]);
		String[] productTypeParts = ruleParts[1].split("/", 2);
		if (2 < productTypeParts.length) {
			throw new ParseException(RULE_MALFORMED_PRODUCT_TYPE_ERROR + ruleParts[1],
					simpleRuleString.indexOf(productTypeParts[2], offset) - 1);			
		}
		
		// Find product type in mission
		for (ProductClass missionProductClass: targetProductClass.getMission().getProductClasses()) {
			if (productTypeParts[0].equals(missionProductClass.getProductType())) {
				simpleRule.setSourceProductClass(missionProductClass);
				break;
			}
		}
		if (null == simpleRule.getSourceProductClass()) {
			throw new ParseException(RULE_SOURCE_PRODUCT_CLASS_NOT_FOUND + ruleParts[1],
					simpleRuleString.indexOf(ruleParts[1], offset));
		}
		
		// Parse filter conditions
		if (2 == productTypeParts.length) {
			if (0 == productTypeParts[1].length()) {
				throw new ParseException(RULE_MISSING_FILTER_ERROR + ruleParts[1],
						simpleRuleString.indexOf('/', offset) + 1);			
			}
			for (String filterConditionString: productTypeParts[1].split(",")) {
				if (0 == filterConditionString.length()) {
					throw new ParseException(RULE_EMPTY_FILTER_ERROR + ruleParts[1],
							simpleRuleString.indexOf('/', offset) + 1);			
				}
				String[] filterConditionParts = filterConditionString.split(":");
				if (2 != filterConditionParts.length) {
					throw new ParseException(RULE_MALFORMED_FILTER_ERROR + ruleParts[1],
							simpleRuleString.indexOf(filterConditionString, offset) + 1);			
				}
				simpleRule.getFilterConditions().put(filterConditionParts[0], 
						(new Parameter()).init(ParameterType.STRING, filterConditionParts[1]));
			}
		}
		
		return simpleRule;
	}
	
	/**
	 * Syntactical analysis of a policy: simple_policy [ OR policy ]
	 * (see {@link SelectionRule grammar definition})
	 * 
	 * @param policyString the text string containing the policy to analyse (without preceding white space)
	 * @param offset the character offset within the complete rule text
	 * @return a SimpleSelectionRule object created from the text contents
	 * @throws ParseException if the policy text contains syntactical or semantical errors
	 */
	private static SimpleSelectionRule parsePolicy(String policyString, int offset) throws ParseException {
		SimpleSelectionRule simpleRule = new SimpleSelectionRule();
		
		logger.debug("... parsing policy '" + policyString + "' at offset " + offset);
		
		int optionalPos = policyString.toUpperCase().lastIndexOf(RULE_OPTIONAL);
		int mandatoryPos = policyString.toUpperCase().lastIndexOf(RULE_MANDATORY);
		int mincoverPos = policyString.toUpperCase().lastIndexOf(RULE_MINCOVER);
		// Find the position of any of the above three (if more than one, it's a syntax error anyway)
		int simplePolicyListEnd = (optionalPos > mandatoryPos ? optionalPos : ( mandatoryPos > mincoverPos ? mandatoryPos : mincoverPos));
		if (-1 == simplePolicyListEnd) {
			simplePolicyListEnd = policyString.length();
		}
		String simplePolicyList = policyString.substring(0, simplePolicyListEnd);
		String[] simplePolicies = simplePolicyList.toUpperCase().split("\\s+" + RULE_POLICY_OR + "\\s+");
		if (0 == simplePolicies.length) {
			throw new ParseException(RULE_SYNTAX_ERROR + "<empty policy>", offset);
		}
		for (int i = 0; i < simplePolicies.length; ++i) {
			SimplePolicy simplePolicy = parseSimplePolicy(simplePolicies[i], offset + policyString.toUpperCase().indexOf(simplePolicies[i]));
			boolean found = false;
			for (int k = 0; k < simpleRule.getSimplePolicies().size(); ++k) {
				if (simplePolicy.getPolicyType().equals(simpleRule.getSimplePolicies().get(k).getPolicyType())) {
					logger.debug("... merging policy " + simpleRule.getSimplePolicies().get(k) + " with " + simplePolicy);
					simpleRule.getSimplePolicies().set(k, simplePolicy.merge(simpleRule.getSimplePolicies().get(k)));
					logger.debug("... replaced policy: '" + simpleRule.getSimplePolicies().get(k) + "'");
					found = true;
					break;
				}
			}
			if (!found) {
				logger.debug("... adding policy '" + simplePolicy + "'");
				simpleRule.getSimplePolicies().add(simplePolicy);
			}
		}
		
		// Default is mandatory
		simpleRule.setIsMandatory(true);			
		// Check that the final part indeed only contains (at most) one option
		if (simplePolicyListEnd < policyString.length()) {
			String optManCovPart = policyString.toUpperCase().substring(simplePolicyListEnd).trim();
			if (-1 < optionalPos) {
				if (RULE_OPTIONAL.equals(optManCovPart)) {
					simpleRule.setIsMandatory(false);
				} else {
					throw new ParseException(RULE_OPTMANCOVPART_ERROR + optManCovPart, offset + optionalPos);
				}
			}
			if (-1 < mandatoryPos) {
				if (!RULE_MANDATORY.equals(optManCovPart)) {
					throw new ParseException(RULE_OPTMANCOVPART_ERROR + optManCovPart, offset + mandatoryPos);
				}
			}
			if (mincoverPos > -1) {
				Matcher mincoverMatcher = RULE_MINCOVER_PATTERN.matcher(optManCovPart);
				if (mincoverMatcher.matches()) {
					Short minimumCoverage = Short.parseShort(mincoverMatcher.group(1));
					if (0 > minimumCoverage || 100 < minimumCoverage) {
						throw new ParseException(RULE_MINCOVER_ERROR + minimumCoverage, offset + policyString.lastIndexOf(mincoverMatcher.group(1)));
					}
					simpleRule.setMinimumCoverage(minimumCoverage);
				} else {
					throw new ParseException(RULE_OPTMANCOVPART_ERROR + optManCovPart, offset + mincoverPos);
				}
			}
		}
		
		logger.debug("... policy is mandatory: " + simpleRule.getIsMandatory());
		return simpleRule;
	}
	
	/**
	 * Syntactical analysis of a simple policy: policy_name [ ( delta_time , delta_time ) ]
	 * (see {@link SelectionRule grammar definition})
	 * <p>
	 * Note: For the "LatestValidityClosest" policy the delta times will be normalized, so that the centre of the interval
	 * is preserved, but at least one of the delta times will be zero. If the delta times amount to the same time
	 * period, the time unit will be preserved, otherwise the time unit of the non-zero delta time will be set to minutes.
	 * This improves the probability of successfully merging "LatestValidityClosest" policies.
	 * 
	 * @param simplePolicyString the text string containing the simple policy to analyse (without preceding white space)
	 * @param offset the character offset within the complete rule text
	 * @return a SimplePolicy object created from the text contents
	 * @throws ParseException if the simple policy text contains syntactical or semantical errors
	 */
	private static SimplePolicy parseSimplePolicy(String simplePolicyString, int offset) throws ParseException {
		SimplePolicy simplePolicy = new SimplePolicy();

		logger.debug("... parsing simple policy '" + simplePolicyString + "' at offset " + offset);
		
		// Separate policy name from (optional) policy parameters
		String[] policyParts = simplePolicyString.split("\\s*\\" + RULE_PAREN_OPEN + "\\s*");
		switch(policyParts[0].trim()) {
			case RULE_POLICY_VALINT:
				simplePolicy.setPolicyType(SimplePolicy.PolicyType.ValIntersect);
				break;
			case RULE_POLICY_LATVALINT:
				simplePolicy.setPolicyType(SimplePolicy.PolicyType.LatestValIntersect);
				break;
			case RULE_POLICY_LATVAL:
				simplePolicy.setPolicyType(SimplePolicy.PolicyType.LatestValidity);
				break;
			case RULE_POLICY_LATVALCLO:
				simplePolicy.setPolicyType(SimplePolicy.PolicyType.LatestValidityClosest);
				break;
			case RULE_POLICY_LATVALCOV:
				simplePolicy.setPolicyType(SimplePolicy.PolicyType.LatestValCover);
				break;
			default:
				throw new ParseException(RULE_POLICY_INVALID_ERROR + policyParts[0], offset);
		}
		logger.debug("... policy name found: " + policyParts[0]);
		
		// Analyse policy parameters, if present
		if (2 > policyParts.length) {
			if (RULE_POLICY_VALINT.equals(policyParts[0])    || RULE_POLICY_LATVALINT.equals(policyParts[0])
			||  RULE_POLICY_LATVALCLO.equals(policyParts[0]) || RULE_POLICY_LATVALCOV.equals(policyParts[0])) {
				throw new ParseException(RULE_POLICY_TIMES_ERROR + "<none>", offset + simplePolicyString.length());
			}
			// otherwise OK: LatestValidity has no parameters
		} else {
			if (RULE_POLICY_LATVAL.equals(policyParts[0])) {
				int parenPos = simplePolicyString.indexOf(RULE_PAREN_OPEN);
				throw new ParseException(RULE_POLICY_LATVAL_ERROR + simplePolicyString.substring(parenPos), offset + parenPos);
			}
			// Determine and analyse substrings for delta time parameters
			int commaPos = policyParts[1].indexOf(RULE_COMMA);
			int parenPos = policyParts[1].indexOf(RULE_PAREN_CLOSE);
			if (-1 == commaPos || -1 == parenPos || commaPos > parenPos) {
				throw new ParseException(RULE_POLICY_SYNTAX_ERROR + policyParts[1], offset + simplePolicyString.indexOf(policyParts[1]));
			}
			String deltaTimeT0 = policyParts[1].substring(0, commaPos).trim();
			String deltaTimeT1 = policyParts[1].substring(commaPos + 1, parenPos).trim();
			simplePolicy.setDeltaTimeT0(parseDeltaTime(deltaTimeT0, offset + simplePolicyString.indexOf(policyParts[1])));
			simplePolicy.setDeltaTimeT1(parseDeltaTime(deltaTimeT1,
					offset + simplePolicyString.indexOf(policyParts[1]) + commaPos + 1
					+ simplePolicyString.substring(simplePolicyString.indexOf(policyParts[1]) + commaPos + 1).indexOf(deltaTimeT1)));
			// Normalize delta times for LatestValidityClosest, so that at least one of the delta times is zero
			if (SimplePolicy.PolicyType.LatestValidityClosest.equals(simplePolicy.getPolicyType())) {
				long diffSeconds = simplePolicy.getDeltaTimeT1().toSeconds() - simplePolicy.getDeltaTimeT0().toSeconds();
				if (0 < diffSeconds) {
					simplePolicy.getDeltaTimeT0().duration = 0;
					simplePolicy.getDeltaTimeT1().duration = diffSeconds / 60;
					simplePolicy.getDeltaTimeT1().unit = TimeUnit.MINUTES;
				} else if (0 == diffSeconds) {
					simplePolicy.getDeltaTimeT0().duration = 0;
					simplePolicy.getDeltaTimeT1().duration = 0;					
				} else {
					simplePolicy.getDeltaTimeT0().duration = - diffSeconds / 60;
					simplePolicy.getDeltaTimeT0().unit = TimeUnit.MINUTES;
					simplePolicy.getDeltaTimeT1().duration = 0;
				}
				logger.debug(String.format("... delta times %s/%s normalized to %s/%s", deltaTimeT0, deltaTimeT1,
						simplePolicy.getDeltaTimeT0().toString(), simplePolicy.getDeltaTimeT1().toString()));
			}
			// Check whether there is any unexpected trailing text
			String unexpectedRemainder = policyParts[1].substring(parenPos + 1).trim();
			if (0 < unexpectedRemainder.length()) {
				throw new ParseException(RULE_POLICY_END_ERROR + unexpectedRemainder,
						offset + simplePolicyString.indexOf(policyParts[1]) + policyParts[1].indexOf(unexpectedRemainder));
			}
		}
		return simplePolicy;
	}
	
	/**
	 * Syntactical analysis of a delta time: digit ... [ d | h | m | s ]]
	 * (see {@link SelectionRule grammar definition})
	 * 
	 * @param deltaTimeString the text string containing the delta time to analyse (without preceding or trailing white space)
	 * @param offset the character offset within the complete rule text
	 * @return a DeltaTime object created from the text contents
	 * @throws ParseException if the delta time text contains syntactical or semantical errors
	 */
	private static DeltaTime parseDeltaTime(String deltaTimeString, int offset) throws ParseException {
		DeltaTime deltaTime = new DeltaTime();
		
		String patternString = "(\\d+)\\s*(?:\\s("
				+ RULE_DELTA_DAYS + "|" + RULE_DELTA_HOURS + "|" + RULE_DELTA_MINS + "|" + RULE_DELTA_SECS
				+ "))?";
		logger.debug(String.format("... parsing delta time '%s' against pattern '%s'", deltaTimeString, patternString));
		Pattern deltaTimePattern = Pattern.compile(patternString);
		Matcher deltaTimeMatcher = deltaTimePattern.matcher(deltaTimeString);
		if (deltaTimeMatcher.matches()) {
			deltaTime.duration = Integer.parseInt(deltaTimeMatcher.group(1));
			if (null == deltaTimeMatcher.group(2)) {
				deltaTime.unit = TimeUnit.DAYS;
			} else {
				switch(deltaTimeMatcher.group(2)) {
				case RULE_DELTA_HOURS:
					deltaTime.unit = TimeUnit.HOURS;
					break;
				case RULE_DELTA_MINS:
					deltaTime.unit = TimeUnit.MINUTES;
					break;
				case RULE_DELTA_SECS:
					deltaTime.unit = TimeUnit.SECONDS;
					break;
				case RULE_DELTA_DAYS:
				default:
					deltaTime.unit = TimeUnit.DAYS;
				}
			}
			logger.debug("... found delta time with duration '" + deltaTime.duration + "' and unit '" + deltaTime.unit + "'");
		} else {
			throw new ParseException(RULE_TIME_FORMAT_ERROR + deltaTimeString, offset);
		}
		return deltaTime;
	}
	
	/**
	 * Parse the given string as a selection rule (see {@link SelectionRule the grammar definition in the class comment}).
	 * Returns a new instance of SelectionRule.
	 * @param targetProductClass the product class, which requires this rule for processing
	 * @param selectionRuleString the string containing the selection rule
	 * 
	 * @return a new SelectionRule object
	 * @throws ParseException if the string does not conform to the grammar given in the class comment
	 * @throws IllegalArgumentException if selectionRuleString is null
	 */
	public static SelectionRule parseSelectionRule(ProductClass targetProductClass, String selectionRuleString) throws ParseException, IllegalArgumentException {
		if (null == targetProductClass) {
			throw new IllegalArgumentException(MSG_NULL_TARGET_PRODUCT_CLASS);
		}
		if (null == selectionRuleString) {
			throw new IllegalArgumentException(MSG_NULL_SELECTION_RULE);
		}
		
		SelectionRule selectionRule = new SelectionRule();
		String[] simpleRuleStrings = selectionRuleString.trim().split("\\s*" + RULE_SEPARATOR + "\\s*", -1);
		int offset = 0;
		for (int i = 0; i < simpleRuleStrings.length; ++i) {
			if (0 == simpleRuleStrings[i].length()) {
				throw new ParseException(RULE_EMPTY_ERROR, offset);
			}
			SimpleSelectionRule simpleRule = parseSimpleRule(targetProductClass, simpleRuleStrings[i], offset);
			if (selectionRule.simpleRules.containsKey(simpleRule.getFilteredSourceProductType())) {
				simpleRule = simpleRule.merge(selectionRule.simpleRules.get(simpleRule.getFilteredSourceProductType()));
			}
			selectionRule.simpleRules.put(simpleRule.getFilteredSourceProductType(), simpleRule);
			offset += simpleRuleStrings[i].length() + RULE_SEPARATOR.length();
		}
		return selectionRule;
	}
	
	/**
	 * Tests whether this rule has a selection policy for the given item type (aux product type)
	 * 
	 * @param itemType the item type to test for
	 * @return true, if a policy for the item type exists, false otherwise
	 */
	public Boolean hasPolicyFor(String itemType) {
		return simpleRules.containsKey(itemType);
	}
	
	/**
	 * Merge this selection rule with another selection rule, thereby extending the validity periods per product type for each policy
	 * in such a way that the validity periods of this rule and of the other rule are covered. This method returns a new
	 * SelectionRule object, the current object remains unchanged.
	 * <p>
	 * For selection rules containing the policy 'LatestValidityClosest' a merge is possible, if and only if all occurrences
	 * of this policy for one product type in both rules have the same delta times (this policy actually refers to a point
	 * in time and not to a time interval, therefore a merge can only be done between policies referring to the same point in time).
	 * 
	 * @param anotherRule the selection rule to merge this rule with
	 * @return a new SelectionRule object reflecting the merged validity periods for each policy of both rules
	 * @throws IllegalArgumentException if a merge between rules with 'LatestValidityClosest' policies with differing
	 *   delta times for the same product type is attempted
	 */
	public SelectionRule merge(SelectionRule anotherRule) throws IllegalArgumentException {
		SelectionRule newSelectionRule = new SelectionRule();
		
		// Merge all simple rules of this rule with the corresponding simple rules of the other rule
		for (String productType: simpleRules.keySet()) {
			if (anotherRule.simpleRules.containsKey(productType)) {
				newSelectionRule.simpleRules.put(productType,
						simpleRules.get(productType).merge(anotherRule.simpleRules.get(productType)));
			} else {
				newSelectionRule.simpleRules.put(productType, simpleRules.get(productType));
			}
		}
		// Add the unmatched simple rules of the other rule
		for (String productType: anotherRule.simpleRules.keySet()) {
			if (!newSelectionRule.simpleRules.containsKey(productType)) {
				newSelectionRule.simpleRules.put(productType, anotherRule.simpleRules.get(productType));
			}
		}
		return newSelectionRule;
	}
	
	/**
	 * Select the one item in the given collection of items that fulfils the selection rule with respect to a given time interval.
	 * All items in the collection must be of the same item product type. If the rule does not contain policies
	 * for the item type given in the collection, the result is the same as if all items had qualified (i. e. if the collection
	 * contains only one item, its object is returned, if it contains more than one item, NoSuchElementException is thrown).
	 * 
	 * @param productType the product type of the items
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * 
	 * @return the single item object fulfilling the selection rule, if exactly one such item exists, or null, if no qualifying
	 * 		   item exists and the selection rule is marked as 'OPTIONAL'
	 * @throws NoSuchElementException if there is more than one item fulfilling the selection rule, or if there is no such
	 * 		   item and the selection rule is marked as 'MANDATORY'
	 * @throws IllegalArgumentException if any of the parameters is null or if not all of the items
	 * 		   are of the given type
	 */
	public Object selectUniqueItem(String productType, final Collection<SelectionItem> items, final Instant startTime, final Instant stopTime)
			throws NoSuchElementException, IllegalArgumentException {
		if (null == items || null == startTime || null == stopTime) {
			throw new IllegalArgumentException(MSG_MISSING_ARGUMENTS);
		}
		
		List<Object> itemList = selectItems(productType, items, startTime, stopTime);
		
		if (null == itemList) {
			// No qualifying items and rule is optional (mandatory case has been handled by "selectItems()"
			return null;
		}
		if (1 == itemList.size()) {
			// Unique qualifying item
			return itemList.get(0);
		}
		// Multiple qualifying items
		throw new NoSuchElementException(MSG_NO_UNIQUE_ITEM_FOUND + simpleRules.get(productType));
	}
	
	/**
	 * Select the one product in the given collection of products that fulfils the selection rule with respect to a given time interval.
	 * All products in the collection must be of the same product type. If the rule does not contain policies
	 * for the product type given in the collection, the result is the same as if all items had qualified (i. e. if the collection
	 * contains only one item, its object is returned, if it contains more than one item, NoSuchElementException is thrown).
	 * 
	 * @param productType the product type of the items
	 * @param products the collection of products to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return the single item object fulfilling the selection rule, if exactly one such item exists, or null, if no qualifying
	 * 		   item exists and the selection rule is marked as 'OPTIONAL'
	 * @throws NoSuchElementException if there is more than one item fulfilling the selection rule, or if there is no such
	 * 		   item and the selection rule is marked as 'MANDATORY'
	 * @throws IllegalArgumentException if any of the parameters is null or if not all of the items
	 * 		   are of the given type
	 */
	public Product selectUniqueProduct(String productType, final Collection<Product> products, final Instant startTime, final Instant stopTime)
			throws NoSuchElementException, IllegalArgumentException {
		if (null == products) {
			throw new IllegalArgumentException(MSG_MISSING_ARGUMENTS);
		}
		
		List<SelectionItem> items = new ArrayList<>();
		
		for (Product product: products) {
			items.add(new SelectionItem(product.getProductClass().getProductType(), product.getSensingStartTime(),
					product.getSensingStopTime(), product.getGenerationTime(), product));
		}
		
		return (Product) selectUniqueItem(productType, items, startTime, stopTime);
	}
	
	/**
	 * Select all items in the given collection of items that fulfil the selection rule with respect to a given time interval.
	 * All items in the collection must be of the same item product type. If the rule does not contain policies
	 * for the item type given in the collection, an empty list returned.
	 * 
	 * @param productType the type of the items to be searched
	 * @param items the collection of items to be searched (may be empty, but not null)
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * 
	 * @return a (possibly empty) list of all item objects fulfilling the selection rule, or null, if no such qualifying item
	 * 		   exists and the selection rule is marked as 'OPTIONAL'
	 * @throws NoSuchElementException if no item fulfils the selection rule, and the selection rule is marked as 'MANDATORY'
	 * @throws IllegalArgumentException if any of the parameters is null or if not all of the items
	 * 		   are of the given item type
	 */
	public List<Object> selectItems(final String productType, final Collection<SelectionItem> items, final Instant startTime, final Instant stopTime)
			throws NoSuchElementException, IllegalArgumentException {
		if (null == items || null == startTime || null == stopTime) {
			throw new IllegalArgumentException(MSG_MISSING_ARGUMENTS);
		}
		SimpleSelectionRule applicableRule = simpleRules.get(productType);
		
		// If no applicable rule is found, return an empty list
		if (null == applicableRule) {
			return new ArrayList<Object>();
		}
		
		return applicableRule.selectItems(items, startTime, stopTime);
	}
	
	/**
	 * Select all products in the given collection of products that fulfil the selection rule with respect to a given time interval.
	 * All products in the collection must be of the same product type. If the rule does not contain policies
	 * for the product type given in the collection, an empty list returned.
	 * 
	 * @param productType the type of the items to be searched
	 * @param products the collection of products to be searched (may be empty, but not null)
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * 
	 * @return a (possibly empty) list of all item objects fulfilling the selection rule, or null, if no such qualifying item
	 * 		   exists and the selection rule is marked as 'OPTIONAL'
	 * @throws NoSuchElementException if no item fulfils the selection rule, and the selection rule is marked as 'MANDATORY'
	 * @throws IllegalArgumentException if any of the parameters is null or if not all of the items
	 * 		   are of the given item type
	 */
	public List<Product> selectProducts(final String productType, final Collection<Product> products, final Instant startTime, final Instant stopTime)
			throws NoSuchElementException, IllegalArgumentException {
		if (null == products) {
			throw new IllegalArgumentException(MSG_MISSING_ARGUMENTS);
		}
		
		// Convert products to selection items
		List<SelectionItem> items = new ArrayList<>();
		
		for (Product product: products) {
			items.add(new SelectionItem(product.getProductClass().getProductType(), product.getSensingStartTime(),
					product.getSensingStopTime(), product.getGenerationTime(), product));
		}
		
		// Perform the selection
		List<Object> selectedItems = selectItems(productType, items, startTime, stopTime);
		
		// Convert result list to list of products
		List<Product> selectedProducts = new ArrayList<>();
		for (Object item: selectedItems) {
			if (item instanceof Product) {
				selectedProducts.add((Product) item);
			} else {
				throw new ClassCastException(MSG_WRONG_ITEM_CLASS + item.getClass().toString());
			}
		}
		
		return selectedProducts;
	}
	
	/**
	 * Converts the selection rule to a textual condition with respect to a given time interval that can be used in a PL query.
	 * <p>
	 * Limitation: For LatestValidityClosest the query may return two products, one to each side of the centre of the
	 * given time interval. It is up to the calling program to select the applicable product.
	 * 
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return a mapping of aux product types to query strings describing the selection rule
	 * @throws IllegalArgumentException if startTime or stopTime is null
	 */
	public Map<String, String> asPlQueryCondition(final Instant startTime, final Instant stopTime) throws IllegalArgumentException {
		if (null == startTime || null == stopTime) {
			throw new IllegalArgumentException(MSG_START_OR_STOP_TIME_NULL);
		}
		Map<String, String> plQueries = new HashMap<>();
		
		for (String productType: simpleRules.keySet()) {
			plQueries.put(productType, simpleRules.get(productType).asPlQueryCondition(startTime, stopTime));
		}
		
		return plQueries;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder ruleString = new StringBuilder();
		boolean first = true;
		for (SimpleSelectionRule simpleRule: simpleRules.values()) {
			if (first)
				first = false;
			else
				ruleString.append("; ");
			ruleString.append(simpleRule.toString());
		}
		return ruleString.toString();
	}
}
