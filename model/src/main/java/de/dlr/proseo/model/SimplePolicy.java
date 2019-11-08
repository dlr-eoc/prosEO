/**
 * SimplePolicy.java
 * 
 * (C) 2016 - 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OrderColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.util.SelectionRule;
import de.dlr.proseo.model.util.SelectionItem;

/**
 * A product retrieval policy consisting of a retrieval mode and a time interval with delta times as defined in Annex B of ESA's Generic IPF Interface Specifications  { REF _Ref11952109 \r \h }. 
 * 
 * Note: As a future extension SelectionPolicys based on geographical areas are envisioned.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class SimplePolicy extends PersistentObject {

	private static final String MSG_POLICY_TYPE_NOT_IMPLEMENTED = "Policy type %s not implemented";

	/**
	 * Available policy types as defined in ESA's Generic IPF Interface Specifications.
	 * 
	 * Note: For Sentinel-5P only the policies LatestValCover, LatestValIntersect, ValIntersect, LatestValidityClosest, LatestValidity are used.
	 */
	public enum PolicyType { ValCover, LatestValCover, ValIntersect, LatestValIntersect, LatestValidityClosest,
		BestCenteredCover, LatestValCoverClosest, LargestOverlap, LargestOverlap85, LatestValidity, LatestValCoverNewestValidity 
	}
	
	/** The policy type to use */
	@Enumerated(EnumType.STRING)
	@Basic(optional = false)
	private PolicyType policyType;

	/** 
	 * The delta time to apply to the start (index 0) and end (index 1) of the selection period. Note that delta times always
	 * enlarge the selection period, they cannot be negative (i. e. reduce the interval).
	 */
	@ElementCollection
	@OrderColumn(name = "list_index")
	private List<DeltaTime> deltaTimes = new ArrayList<>();
	
	/* Message strings */
	private static final String MSG_DELTA_TIMES_NEGATIVE = "Delta times must not be negative.";
	private static final String MSG_ILLEGAL_LIST_OF_DELTA_TIMES = "List of delta times must contain exactly two entries for interval start and end.";

	/** The date format for OQL queries [OBSOLETE IN prosEO CONTEXT] */
    private static final DateTimeFormatter DATEFORMAT_PL = DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd'T'HH:mm:ss").withZone(ZoneId.of("UTC"));
    private static final DateTimeFormatter DATEFORMAT_SQL = DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd' 'HH:mm:ss").withZone(ZoneId.of("UTC"));
	
	/** The static class logger */
	private static final Logger logger = LoggerFactory.getLogger(SelectionRule.class);
	
	/**
	 * Class representing a single overlapping time period
	 */
	@Embeddable
	public static class DeltaTime {
		
		/** The duration of the time period in time units (non-negative) */
		public long duration;
		
		/** The time unit applicable for this time period */
		public TimeUnit unit;
		
		/**
		 * No-argument constructor sets delta time to zero days
		 */
		public DeltaTime() {
			this.duration = 0L;
			this.unit = TimeUnit.DAYS;
		}
		
		/**
		 * Create a delta time with duration and unit
		 * 
		 * @param duration the delta time duration
		 * @param unit the delta time unit
		 */
		public DeltaTime(long duration, TimeUnit unit) {
			this.duration = duration;
			this.unit = unit;
		}

		/**
		 * Merge two delta times by creating a new delta time with the smaller unit and the larger (converted) duration
		 * 
		 * @param anotherDeltaTime the delta time to merge this one with
		 * @return a new DeltaTime object with the larger duration
		 */
		public DeltaTime merge(DeltaTime anotherDeltaTime) {
			DeltaTime newDeltaTime = new DeltaTime();
			// Select the smaller time unit for the merged delta time
			newDeltaTime.unit = (unit.compareTo(anotherDeltaTime.unit) < 0 ? unit : anotherDeltaTime.unit);
			// Select the larger duration for the merged delta time
			long mySeconds = unit.toSeconds(duration);
			long anotherSeconds = anotherDeltaTime.unit.toSeconds(anotherDeltaTime.duration);
			long newSeconds = (mySeconds > anotherSeconds ? mySeconds : anotherSeconds);
			newDeltaTime.duration = newDeltaTime.unit.convert(newSeconds, TimeUnit.SECONDS);
			return newDeltaTime;
		}
		
		/**
		 * Convert the delta time to seconds
		 * 
		 * @return the duration of the delta time in seconds
		 */
		public long toSeconds() {
			return unit.toSeconds(duration);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			String unitString = null;
			switch (unit) {
			case HOURS:		unitString = SelectionRule.RULE_DELTA_HOURS; break;
			case MINUTES:	unitString = SelectionRule.RULE_DELTA_MINS; break;
			case SECONDS:	unitString = SelectionRule.RULE_DELTA_SECS; break;
			case DAYS:
			default:		unitString = SelectionRule.RULE_DELTA_DAYS; break;
			}
			return String.valueOf(duration) + " " + unitString; }

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return Objects.hash(duration, unit);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof DeltaTime))
				return false;
			DeltaTime other = (DeltaTime) obj;
			return (toSeconds() == other.toSeconds());
		}
	}
	
	/**
	 * No-argument sets default values for type (ValIntersect) and delta times (0/0)
	 */
	public SimplePolicy() {
		this.policyType = PolicyType.ValIntersect;
		this.deltaTimes.add(new DeltaTime());
		this.deltaTimes.add(new DeltaTime());
	}

	/**
	 * Gets the policy type
	 * @return the type
	 */
	public PolicyType getPolicyType() {
		return policyType;
	}

	/**
	 * Sets the policy type
	 * @param policyType the type to set
	 */
	public void setPolicyType(PolicyType policyType) {
		this.policyType = policyType;
	}

	/**
	 * Gets the delta times
	 * @return the deltaTimes
	 */
	public List<DeltaTime> getDeltaTimes() {
		return deltaTimes;
	}

	/**
	 * Sets the delta times
	 * @param deltaTimes the deltaTimes to set (a list of exactly two entries with non-negative duration values)
	 * @throws IllegalArgumentException if the list does not contain exactly two entries, 
	 * 		   or if the duration of any of the delta times is negative
	 */
	public void setDeltaTimes(List<DeltaTime> deltaTimes) throws IllegalArgumentException {
		if (2 != deltaTimes.size()) {
			throw new IllegalArgumentException(MSG_ILLEGAL_LIST_OF_DELTA_TIMES);
		}
		if (0 > deltaTimes.get(0).duration || 0 > deltaTimes.get(1).duration) {
			throw new IllegalArgumentException(MSG_DELTA_TIMES_NEGATIVE);
		}
		this.deltaTimes = deltaTimes;
	}

	/**
	 * Gets the delta time to apply to the beginning of the interval (T0)
	 * @return the delta time T0
	 */
	public DeltaTime getDeltaTimeT0() {
		return deltaTimes.get(0);
	}

	/**
	 * Sets the delta time to apply to the beginning of the interval (T0)
	 * 
	 * @param deltaTimeT0 the T0 delta time
	 * @throws IllegalArgumentException if the duration of the delta time is negative
	 */
	public void setDeltaTimeT0(DeltaTime deltaTimeT0) throws IllegalArgumentException {
		if (0 > deltaTimeT0.duration) {
			throw new IllegalArgumentException(MSG_DELTA_TIMES_NEGATIVE);
		}
		deltaTimes.set(0, deltaTimeT0);
	}

	/**
	 * Gets the delta time to apply to the end of the interval (T1)
	 * @return the deltaTimes
	 */
	public DeltaTime getDeltaTimeT1() {
		return deltaTimes.get(1);
	}

	/**
	 * Sets the delta time to apply to the beginning of the interval (T1)
	 * 
	 * @param deltaTimeT1 the T1 delta time
	 * @throws IllegalArgumentException if the duration of the delta time is negative
	 */
	public void setDeltaTimeT1(DeltaTime deltaTimeT1) throws IllegalArgumentException {
		if (0 > deltaTimeT1.duration) {
			throw new IllegalArgumentException(MSG_DELTA_TIMES_NEGATIVE);
		}
		deltaTimes.set(1, deltaTimeT1);
	}

	/**
	 * Merge two simple policies by creating a new simple policy with merged delta times; for the
	 * policy 'LatestValidityClosest' a merge is possible, if and only if both policies have the same
	 * delta times (this policy actually refers to a point in time and not to a time interval, therefore
	 * a merge can only be done between policies referring to the same point in time).
	 * 
	 * @param anotherSimplePolicy the simple policy to merge this one with
	 * @return a new SimplePolicy object covering the united validity periods of the two policies
	 * @throws IllegalArgumentException if a merge of simple policies of different types,
	 *   or of 'LatestValidityClosest' policies with different delta times was attempted
	 */
	public SimplePolicy merge(SimplePolicy anotherSimplePolicy) throws IllegalArgumentException {
		if (!policyType.equals(anotherSimplePolicy.policyType)) {
			throw new IllegalArgumentException("Cannot merge simple policies of different types!");
		}
		if (PolicyType.LatestValidityClosest.equals(policyType) &&
			(!getDeltaTimeT0().equals(anotherSimplePolicy.getDeltaTimeT0()) || !getDeltaTimeT1().equals(anotherSimplePolicy.getDeltaTimeT1()))) {
			throw new IllegalArgumentException("Cannot merge " + PolicyType.LatestValidityClosest + " policies with different delta times!");
		}
		SimplePolicy newSimplePolicy = new SimplePolicy();
		newSimplePolicy.policyType = policyType;
		if (!PolicyType.LatestValidity.equals(policyType)) {
			newSimplePolicy.setDeltaTimes(Arrays.asList(
					getDeltaTimeT0().merge(anotherSimplePolicy.getDeltaTimeT0()),
					getDeltaTimeT1().merge(anotherSimplePolicy.getDeltaTimeT1())));
		}
		return newSimplePolicy;
	}
	
	/**
	 * Select all items from the given collection that cover partly the given time interval.
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return a (possibly empty) list of all item objects fulfilling the policy
	 */
	public Set<SelectionItem> selectValIntersect(Collection<SelectionItem> items, Instant startTime, Instant stopTime) {
		Instant selectionStartTime = startTime.minusSeconds(getDeltaTimeT0().toSeconds());
		Instant selectionStopTime = stopTime.plusSeconds(getDeltaTimeT1().toSeconds());
		Set<SelectionItem> selectedItems = new HashSet<>();
		
		for (SelectionItem item: items) {
			if (item.startTime.isBefore(selectionStopTime) && item.stopTime.isAfter(selectionStartTime)) {
				selectedItems.add(item);
			}
		}
		return selectedItems;
	}
	
	/**
	 * Select the latest item (by generation time) from the given collection that covers partly the given time interval.
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectLatestValIntersect(Collection<SelectionItem> items, Instant startTime, Instant stopTime) {
		Instant selectionStartTime = startTime.minusSeconds(getDeltaTimeT0().toSeconds());
		Instant selectionStopTime = stopTime.plusSeconds(getDeltaTimeT1().toSeconds());
		SelectionItem latestItem = null;
		
		// Test each of the items against the time interval and select the one with the latest generation time
		for (SelectionItem item: items) {
			if (item.startTime.isBefore(selectionStopTime) && item.stopTime.isAfter(selectionStartTime)) {
				if (null == latestItem || item.generationTime.isAfter(latestItem.generationTime)) {
					latestItem = item;
				}
			}
		}
		
		// Prepare the zero-to-one-element result list
		Set<SelectionItem> selectedItems = new HashSet<>();
		if (null != latestItem) {
			selectedItems.add(latestItem);
		}
		return selectedItems;
	}
	
	/**
	 * Select the latest item (by validity start time) from the given collection that covers partly the given time interval.
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectLatestValidity(Collection<SelectionItem> items) {
		SelectionItem latestItem = null;
		
		// Test each of the items against the time interval and select the one with the latest start time
		for (SelectionItem item: items) {
			if (null == latestItem || item.startTime.isAfter(latestItem.startTime)) {
				latestItem = item;
			}
		}
		
		// Prepare the zero-to-one-element result list
		Set<SelectionItem> selectedItems = new HashSet<>();
		if (null != latestItem) {
			selectedItems.add(latestItem);
		}
		return selectedItems;
	}
	
	/**
	 * Select the latest item (by validity start time) from the given collection, whose start time is "nearest"
	 * to the given time interval. "Nearest" is defined as
	 * <span style="font-family:monospace">min(| ValidityStart - ((startTime - deltaTime0) + (stopTime + deltaTime1))/2 |).</span><p>
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectLatestValidityClosest(Collection<SelectionItem> items, Instant startTime, Instant stopTime) {
		Instant selectionStartTime = startTime.minusSeconds(getDeltaTimeT0().toSeconds());
		Instant selectionStopTime = stopTime.plusSeconds(getDeltaTimeT1().toSeconds());
		Duration selectionDuration = Duration.between(selectionStartTime, selectionStopTime);
		Instant selectionCentre = selectionStartTime.plusSeconds(selectionDuration.getSeconds() / 2);
		SelectionItem latestItem = null;
		long distanceToLastItem = Long.MAX_VALUE;
		
		// Test each of the items against the time interval and select the one with the latest validity start time
		for (SelectionItem item: items) {
			long distanceToItem = Math.abs(Duration.between(item.startTime, selectionCentre).getSeconds());
			if (logger.isDebugEnabled())
				logger.debug(String.format("Comparing item %s with distance %d to latest distance %d", item.itemObject, distanceToItem, distanceToLastItem));
			if (distanceToItem < distanceToLastItem
			|| distanceToItem == distanceToLastItem && item.startTime.isAfter(latestItem.startTime)) {
				latestItem = item;
				distanceToLastItem = distanceToItem;
			}
		}
		
		// Prepare the zero-to-one-element result list
		Set<SelectionItem> selectedItems = new HashSet<>();
		if (null != latestItem) {
			selectedItems.add(latestItem);
		}
		return selectedItems;
	}
	
	/**
	 * Select the latest item (by generation time) from the given collection that fully covers the given time interval.
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectLatestValCover(Collection<SelectionItem> items, Instant startTime, Instant stopTime) {
		Instant selectionStartTime = startTime.minusSeconds(getDeltaTimeT0().toSeconds());
		Instant selectionStopTime = stopTime.plusSeconds(getDeltaTimeT1().toSeconds());
		SelectionItem latestItem = null;
		
		// Test each of the items against the time interval and select the one with the latest generation time
		for (SelectionItem item: items) {
			if (!item.startTime.isAfter(selectionStartTime) && !item.stopTime.isBefore(selectionStopTime)) {
				if (null == latestItem || item.generationTime.isAfter(latestItem.generationTime)) {
					latestItem = item;
				}
			}
		}
		
		// Prepare the zero-to-one-element result list
		Set<SelectionItem> selectedItems = new HashSet<>();
		if (null != latestItem) {
			selectedItems.add(latestItem);
		}
		return selectedItems;
	}
	
	/**
	 * Select all items from the given collection that fulfil this policy for the given time interval.
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return a (possibly empty) list of all items fulfilling the policy
	 * @throws IllegalArgumentException if any of the items is not of the given type
	 */
	public Set<SelectionItem> selectItems(Collection<SelectionItem> items, Instant startTime, Instant stopTime) {
		switch(policyType) {
		case ValIntersect:			return selectValIntersect(items, startTime, stopTime);
		case LatestValIntersect:	return selectLatestValIntersect(items, startTime, stopTime);
		case LatestValidity:		return selectLatestValidity(items);
		case LatestValCover:		return selectLatestValCover(items, startTime, stopTime);
		case LatestValidityClosest:	return selectLatestValidityClosest(items, startTime, stopTime);
		default:
			throw new UnsupportedOperationException(String.format(MSG_POLICY_TYPE_NOT_IMPLEMENTED, policyType.toString()));
		}
	}
	
	/**
	 * Format this policy as an OQL query condition [OBSOLETE IN THE prosEO CONTEXT]
	 * <p>
	 * Limitation: For LatestValidityClosest the query may return two products, one to each side of the centre of the
	 * given time interval. It is up to the calling program to select the applicable product.
	 * 
	 * @param sourceProductClassName the source product class to use for the query (only required for LatestValidity and LatestValidityClosest)
	 * @param startTime the start time to use in the condition
	 * @param stopTime the stop time to use in the condition
	 * 
	 * @return an OQL string representing this policy
	 */
	public String asPlQueryCondition(String sourceProductClassName, final Instant startTime, final Instant stopTime) {
		StringBuilder simplePolicyQuery = new StringBuilder();
		
		switch (policyType) {
		case LatestValidity:
			simplePolicyQuery.append("there exists no corresponding ").append(sourceProductClassName).append(" with greater startTime");
			break;
		case LatestValidityClosest:
			// This will result in two products, one on either side of the interval centre
			Instant selectionStartTime = startTime.minusSeconds(getDeltaTimeT0().toSeconds());
			Instant selectionStopTime = stopTime.plusSeconds(getDeltaTimeT1().toSeconds());
			Duration selectionDuration = Duration.between(selectionStartTime, selectionStopTime);
			Instant selectionCentre = selectionStartTime.plusSeconds(selectionDuration.getSeconds() / 2);
			String selectionCentreString = DATEFORMAT_PL.format(selectionCentre);
			simplePolicyQuery.append("((startTime <= '").append(selectionCentreString)
				.append("' and there exists no corresponding ").append(sourceProductClassName)
				.append(" with greater startTime where startTime <= '").append(selectionCentreString)
				.append("') or (startTime > '").append(selectionCentreString)
				.append("' and there exists no corresponding ").append(sourceProductClassName)
				.append(" with less startTime where startTime > '").append(selectionCentreString)
				.append("'))");
			break;
		case LatestValCover:
			simplePolicyQuery.append("(startTime <= '")
				.append(DATEFORMAT_PL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("' and stopTime >= '")
				.append(DATEFORMAT_PL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("')");
			break;
		case ValIntersect:
		case LatestValIntersect:
			simplePolicyQuery.append("(startTime <= '")
				.append(DATEFORMAT_PL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("' and stopTime >= '")
				.append(DATEFORMAT_PL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("')");
			break;
		default:
			throw new UnsupportedOperationException(String.format(MSG_POLICY_TYPE_NOT_IMPLEMENTED, policyType.toString()));
		}
		
		return simplePolicyQuery.toString();
	}
	
	/**
	 * Format this policy as a query condition in JPQL (Java Persistence Query Language). It is assumed that the Product
	 * class is denoted as "select ... from Product p ..." in the JPQL query, to which the resulting condition is to be
	 * appended.
	 * <p>
	 * Limitation [TBC]: For LatestValidityClosest the query may return two products, one to each side of the centre of the
	 * given time interval. It is up to the calling program to select the applicable product.
	 * 
	 * @param sourceProductClass the source product class to use for the query (only required for LatestValidity and LatestValidityClosest)
	 * @param startTime the start time to use in the condition
	 * @param stopTime the stop time to use in the condition
	 * @return a ProductQuery object representing this policy
	 */
	public String asJpqlQueryCondition(ProductClass sourceProductClass, final Instant startTime, final Instant stopTime) {
		StringBuilder simplePolicyQuery = new StringBuilder();
		
		switch (policyType) {
		case LatestValidity:
			simplePolicyQuery.append("p.sensingStartTime >= all ")
					.append("(select p2.sensingStartTime from Product p2 where p2.productClass.id = ")
					.append(sourceProductClass.getId()).append(")");
			break;
		case LatestValidityClosest:
			// This will result in two products, one on either side of the interval centre
			Instant selectionStartTime = startTime.minusSeconds(getDeltaTimeT0().toSeconds());
			Instant selectionStopTime = stopTime.plusSeconds(getDeltaTimeT1().toSeconds());
			Duration selectionDuration = Duration.between(selectionStartTime, selectionStopTime);
			Instant selectionCentre = selectionStartTime.plusSeconds(selectionDuration.getSeconds() / 2);
			String selectionCentreString = DATEFORMAT_SQL.format(selectionCentre);
			simplePolicyQuery.append("(p.sensingStartTime <= '").append(selectionCentreString)
				.append("' and p.sensingStartTime >= all ")
				.append("(select p2.sensingStartTime from Product p2 where p2.productClass.id = ").append(sourceProductClass.getId())
				.append(" and p2.sensingStartTime <= '").append(selectionCentreString).append("') ")
				.append("or p.sensingStartTime > '").append(selectionCentreString)
				.append("' and p.sensingStartTime < all ")
				.append("(select p2.sensingStartTime from Product p2 where p2.productClass.id = ").append(sourceProductClass.getId())
				.append(" and p2.sensingStartTime > '").append(selectionCentreString).append("'))");
			break;
		case LatestValCover:
			simplePolicyQuery.append("p.sensingStartTime <= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("' and p.sensingStopTime >= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("' and p.generationTime >= all ")
				.append("(select p2.generationTime from Product p2 where p2.productClass.id = ").append(sourceProductClass.getId())
				.append(" and p2.sensingStartTime <= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("' and p2.sensingStopTime >= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("')");
			break;
		case ValIntersect:
			simplePolicyQuery.append("p.sensingStartTime <= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("' and p.sensingStopTime >= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("'");
			break;
		case LatestValIntersect:
			simplePolicyQuery.append("p.sensingStartTime <= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("' and p.sensingStopTime >= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("' and p.generationTime >= all ")
				.append("(select p2.generationTime from Product p2 where p2.productClass.id = ").append(sourceProductClass.getId())
				.append(" and p2.sensingStartTime <= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("' and p2.sensingStopTime >= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("')");
			break;
		default:
			throw new UnsupportedOperationException(String.format(MSG_POLICY_TYPE_NOT_IMPLEMENTED, policyType.toString()));
		}
		
		return simplePolicyQuery.toString();
	}
	
	/**
	 * Format this policy as a query condition in native SQL. It is assumed that the Product and ProductClass
	 * classes are denoted as "SELECT ... FROM product p JOIN product_class pc ON p.product_class_id = pc.id" in the SQL query,
	 * to which the resulting condition is to be appended.
	 * <p>
	 * Limitation [TBC]: For LatestValidityClosest the query may return two products, one to each side of the centre of the
	 * given time interval. It is up to the calling program to select the applicable product.
	 * 
	 * @param sourceProductClass the source product class to use for the query (only required for LatestValidity and LatestValidityClosest)
	 * @param startTime the start time to use in the condition
	 * @param stopTime the stop time to use in the condition
	 * @return a ProductQuery object representing this policy
	 */
	public String asSqlQueryCondition(ProductClass sourceProductClass, final Instant startTime, final Instant stopTime) {
		StringBuilder simplePolicyQuery = new StringBuilder();
		
		switch (policyType) {
		case LatestValidity:
			simplePolicyQuery.append("p.sensing_start_time >= ")
				.append("(SELECT MAX(p2.sensing_start_time) FROM product p2 WHERE p2.product_class_id = ")
				.append(sourceProductClass.getId()).append(")");
			break;
		case LatestValidityClosest:
			// This will result in two products, one on either side of the interval centre
			Instant selectionStartTime = startTime.minusSeconds(getDeltaTimeT0().toSeconds());
			Instant selectionStopTime = stopTime.plusSeconds(getDeltaTimeT1().toSeconds());
			Duration selectionDuration = Duration.between(selectionStartTime, selectionStopTime);
			Instant selectionCentre = selectionStartTime.plusSeconds(selectionDuration.getSeconds() / 2);
			String selectionCentreString = DATEFORMAT_SQL.format(selectionCentre);
			simplePolicyQuery.append("(p.sensing_start_time <= '").append(selectionCentreString)
				.append("' AND p.sensing_start_time >= ")
				.append("(SELECT MAX(p2.sensing_start_time) FROM product p2 WHERE p2.product_class_id = ").append(sourceProductClass.getId())
				.append(" AND p2.sensing_start_time <= '").append(selectionCentreString).append("') ")
				.append("OR p.sensing_start_time > '").append(selectionCentreString)
				.append("' AND p.sensing_start_time < ")
				.append("(SELECT MIN(p2.sensing_start_time) FROM product p2 WHERE p2.product_class_id = ").append(sourceProductClass.getId())
				.append(" AND p2.sensing_start_time > '").append(selectionCentreString).append("'))");
			break;
		case LatestValCover:
			simplePolicyQuery.append("p.sensing_start_time <= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("' AND p.sensing_stop_time >= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("' AND p.generation_time >= ")
				.append("(SELECT MAX(p2.generation_time) FROM product p2 WHERE p2.product_class_id = ").append(sourceProductClass.getId())
				.append(" AND p2.sensing_start_time <= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("' AND p2.sensing_stop_time >= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("')");
			break;
		case ValIntersect:
			simplePolicyQuery.append("p.sensing_start_time <= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("' AND p.sensing_stop_time >= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("'");
			break;
		case LatestValIntersect:
			simplePolicyQuery.append("p.sensing_start_time <= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("' AND p.sensing_stop_time >= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("' AND p.generation_time >= ")
				.append("(SELECT MAX(p2.generation_time) FROM product p2 WHERE p2.product_class_id = ").append(sourceProductClass.getId())
				.append(" AND p2.sensing_start_time <= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusSeconds(getDeltaTimeT1().toSeconds())))
				.append("' AND p2.sensing_stop_time >= '")
				.append(DATEFORMAT_SQL.format(startTime.minusSeconds(getDeltaTimeT0().toSeconds())))
				.append("')");
			break;
		default:
			throw new UnsupportedOperationException(String.format(MSG_POLICY_TYPE_NOT_IMPLEMENTED, policyType.toString()));
		}
		
		return simplePolicyQuery.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return policyType.toString() + ( PolicyType.LatestValidity == policyType ? "" : "(" + getDeltaTimeT0() + ", " + getDeltaTimeT1() + ")" );
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(deltaTimes, policyType);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof SimplePolicy))
			return false;
		SimplePolicy other = (SimplePolicy) obj;
		return Objects.equals(deltaTimes, other.deltaTimes) && policyType == other.policyType;
	}

}
