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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OrderColumn;

import de.dlr.proseo.model.util.SelectionRule;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.util.SelectionItem;

/**
 * A product retrieval policy consisting of a retrieval mode and a time interval with delta times as defined in Annex B of 
 * ESA's Generic IPF Interface Specifications (MMFI-GSEG-EOPG-TN-07-0003, issue 1.8).
 * 
 * From Sentinel-1/Sentinel-3 additional policies have been derived according to the Sentinel-3 Core PDGS IPF ICD
 * (S3IPF.ICD.001, issue 1.4), sec. 2.3.2.
 * 
 * Note: As a future extension policy types based on geographical areas are envisioned.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class SimplePolicy extends PersistentObject {

	private static final String MSG_POLICY_TYPE_NOT_IMPLEMENTED = "Policy type %s not implemented";

	/**
	 * Available policy types as defined in ESA's Generic IPF Interface Specifications and other sources (e. g. Sentinel-1/3 IPF ICDs).
	 */
	public enum PolicyType {
		// Standard policies according to the Generic IPF Interface Specifications
		ValCover, LatestValCover, ValIntersect, LatestValIntersect, LatestValidityClosest,
		BestCenteredCover, LatestValCoverClosest, LargestOverlap, LargestOverlap85,
		LatestValidity, LatestValCoverNewestValidity,
		// Additional policies for Sentinel-1 and Sentinel-3
		ClosestStartValidity, ClosestStopValidity, LatestStartValidity, LatestStopValidity, ValIntersectWithoutDuplicates, LastCreated
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
	private static final String MSG_CANNOT_CREATE_QUERY = "Cannot create query (cause: %s)";

	/** The date format for SQL queries */
    private static final DateTimeFormatter DATEFORMAT_SQL = DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd' 'HH:mm:ss.SSSSSS").withZone(ZoneId.of("UTC"));
	
	/** The static class logger */
	private static final ProseoLogger logger = new ProseoLogger(SelectionRule.class);
	
	/**
	 * Class representing a single overlapping time period
	 */
	@Embeddable
	public static class DeltaTime implements Comparable<DeltaTime> {
		
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
			newDeltaTime.duration = newDeltaTime.unit.convert(
				compareTo(anotherDeltaTime) < 0 ? anotherDeltaTime.toMilliseconds() : toMilliseconds(), TimeUnit.MILLISECONDS);
			newDeltaTime.normalize();
			return newDeltaTime;
		}
		
		/**
		 * Convert the delta time to seconds (rounded to the nearest second)
		 * 
		 * @return the duration of the delta time in seconds
		 */
		public long toSeconds() {
			if (TimeUnit.MILLISECONDS.equals(unit)) {
				return (duration + 500) / 1000;
			}
			return unit.toSeconds(duration);
		}
		
		/**
		 * Convert the delta time to milliseconds
		 * 
		 * @return the duration of the delta time in milliseconds
		 */
		public long toMilliseconds() {
			return unit.toMillis(duration);
		}
		
		/**
		 * Normalize delta time to biggest unit, which can be represented with an integer duration
		 * 
		 * @return the delta time itself for method chaining
		 */
		public DeltaTime normalize() {
			switch (unit) {
			case MILLISECONDS:
				if (0 != duration % 1000) break;
				duration = duration / 1000;
				unit = TimeUnit.SECONDS;
			case SECONDS:
				if (0 != duration % 60) break;
				duration = duration / 60;
				unit = TimeUnit.MINUTES;
			case MINUTES:
				if (0 != duration % 60) break;
				duration = duration / 60;
				unit = TimeUnit.HOURS;
			case HOURS:
				if (0 != duration % 24) break;
				duration = duration / 24;
				unit = TimeUnit.DAYS;
			case DAYS:
			default:
				// No further normalization possible	
			}
			return this;
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
			case MILLISECONDS:	unitString = SelectionRule.RULE_DELTA_MILLIS; break;
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
			return (toMilliseconds() == other.toMilliseconds());
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(de.dlr.proseo.model.SimplePolicy.DeltaTime)
		 */
		@Override
		public int compareTo(DeltaTime o) {
			if (null == o) {
				throw new NullPointerException("Cannot compare DeltaTime to null object");
			}
			Long myMillis = unit.toMillis(duration);
			Long anotherMillis = o.unit.toMillis(o.duration);
			return myMillis.compareTo(anotherMillis);
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
		Instant selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
		Instant selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
		Set<SelectionItem> selectedItems = new HashSet<>();
		
		for (SelectionItem item: items) {
			if (item.startTime.isBefore(selectionStopTime) && item.stopTime.isAfter(selectionStartTime)) {
				selectedItems.add(item);
			} else if (startTime.equals(stopTime) &&
					(item.startTime.equals(startTime) || item.stopTime.equals(stopTime))) {
				// Special case of "point-in-time" products
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
		Instant selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
		Instant selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
		SelectionItem latestItem = null;
		
		// Test each of the items against the time interval and select the one with the latest generation time
		for (SelectionItem item: items) {
			if (item.startTime.isBefore(selectionStopTime) && item.stopTime.isAfter(selectionStartTime)) {
				if (null == latestItem || item.generationTime.isAfter(latestItem.generationTime)) {
					latestItem = item;
				}
			} else if (startTime.equals(stopTime) &&
					(item.startTime.equals(startTime) || item.stopTime.equals(stopTime))) {
				// Special case of "point-in-time" products
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
	 * If multiple items share the same latest validity start time, the item with the latest generation time will be selected.
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectLatestValidity(Collection<SelectionItem> items) {
		SelectionItem latestItem = null;
		
		// Test each of the items against the time interval and select the one with the latest start time
		for (SelectionItem item: items) {
			if (null == latestItem || item.startTime.isAfter(latestItem.startTime)
					|| (item.startTime.equals(latestItem.startTime) && item.generationTime.isAfter(latestItem.generationTime))) {
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
	 * Select the latest item (by validity stop time) from the given collection that covers partly the given time interval.
	 * If multiple items share the same latest validity stop time, the item with the latest generation time will be selected.
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectLatestStopValidity(Collection<SelectionItem> items) {
		SelectionItem latestItem = null;
		
		// Test each of the items against the time interval and select the one with the latest start time
		for (SelectionItem item: items) {
			if (null == latestItem || item.stopTime.isAfter(latestItem.stopTime)
					|| (item.stopTime.equals(latestItem.stopTime) && item.generationTime.isAfter(latestItem.generationTime))) {
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
	 * Select the latest item (by generation time) from the given collection, whose start time is "nearest"
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
		Instant selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
		Instant selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
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
			|| distanceToItem == distanceToLastItem && item.generationTime.isAfter(latestItem.generationTime)) {
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
	 * Select the latest item (by generation time) from the given collection, whose start time is "nearest"
	 * to the start of the given time interval (startTime - deltaTime0).
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectClosestStartValidity(Collection<SelectionItem> items, Instant startTime) {
		Instant selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
		SelectionItem latestItem = null;
		long distanceToLastItem = Long.MAX_VALUE;
		
		// Test each of the items against the time interval and select the one with the latest validity start time
		for (SelectionItem item: items) {
			long distanceToItem = Math.abs(Duration.between(item.startTime, selectionStartTime).getSeconds());
			if (logger.isDebugEnabled())
				logger.debug(String.format("Comparing item %s with distance %d to latest distance %d", item.itemObject, distanceToItem, distanceToLastItem));
			if (distanceToItem < distanceToLastItem
			|| distanceToItem == distanceToLastItem && item.generationTime.isAfter(latestItem.generationTime)) {
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
	 * Select the latest item (by generation time) from the given collection, whose stop time is "nearest"
	 * to the end of the given time interval (stopTime + deltaTime1).
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @param stopTime the end time of the time interval to check against
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectClosestStopValidity(Collection<SelectionItem> items, Instant stopTime) {
		Instant selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
		SelectionItem latestItem = null;
		long distanceToLastItem = Long.MAX_VALUE;
		
		// Test each of the items against the time interval and select the one with the latest validity start time
		for (SelectionItem item: items) {
			long distanceToItem = Math.abs(Duration.between(item.stopTime, selectionStopTime).getSeconds());
			if (logger.isDebugEnabled())
				logger.debug(String.format("Comparing item %s with distance %d to latest distance %d", item.itemObject, distanceToItem, distanceToLastItem));
			if (distanceToItem < distanceToLastItem
			|| distanceToItem == distanceToLastItem && item.generationTime.isAfter(latestItem.generationTime)) {
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
		Instant selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
		Instant selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
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
	 * Select the item from the given collection that has the largest overlap with the given time interval.
	 * If multiple items have the same overlap, the product with the start time that is closest to t0-dt0 is chosen.
	 * If multiple items with the same overlap and equally close start times exist, the one with the most recent
	 * generation time is selected.
	 * 
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectLargestOverlap(Collection<SelectionItem> items, Instant startTime, Instant stopTime) {
		
		// First select the items that actually intersect the given interval
		Set<SelectionItem> intersectingItems = selectValIntersect(items, startTime, stopTime);
		
		// Test each of the items against the time interval and select the one with the latest generation time
		Instant selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
		Instant selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
		SelectionItem largestOverlapItem = null;
		Duration maxOverlap = Duration.ZERO;
		
		for (SelectionItem item: intersectingItems) {
			Instant overlapStart = item.startTime.isAfter(selectionStartTime) ? item.startTime : selectionStartTime;
			Instant overlapEnd = item.stopTime.isBefore(selectionStopTime) ? item.stopTime : selectionStopTime;

			Duration overlap = Duration.between(overlapStart, overlapEnd);
			
			if (1 == overlap.compareTo(maxOverlap)) {
				maxOverlap = overlap;
				largestOverlapItem = item;
			} else if (0 == overlap.compareTo(maxOverlap)) {
				long itemDistance = Math.abs(item.startTime.toEpochMilli() - selectionStartTime.toEpochMilli());
				long largestOverlapItemDistance =
						Math.abs(largestOverlapItem.startTime.toEpochMilli() - selectionStartTime.toEpochMilli());
				if (itemDistance < largestOverlapItemDistance  || 
						itemDistance == largestOverlapItemDistance
						&& item.generationTime.isAfter(largestOverlapItem.generationTime)) {
					largestOverlapItem = item;
				}
			}
		}
		
		// Prepare the zero-to-one-element result list
		Set<SelectionItem> selectedItems = new HashSet<>();
		if (null != largestOverlapItem) {
			selectedItems.add(largestOverlapItem);
		}
		return selectedItems;
	}

	/**
	 * Select the item from the given collection that has the largest overlap with the given time interval,
	 * where the item covers at least 85 % of the time interval.
	 * If multiple items have the same overlap, the product with the start time that is closest to t0-dt0 is chosen.
	 * If multiple items with the same overlap and equally close start times exist, the one with the most recent
	 * generation time is selected.
	 * 
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectLargestOverlap85(Collection<SelectionItem> items, Instant startTime, Instant stopTime) {

		// Get the largest overlapping item
		Set<SelectionItem> overlappingItems = selectLargestOverlap(items, startTime, stopTime);
		
		if (overlappingItems.isEmpty()) {
			return overlappingItems;
		}
		
		// Check whether the resulting item covers at least 85 % of the given time interval
		SelectionItem largestOverlappingItem = overlappingItems.iterator().next();
		
		Instant selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
		Instant selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
		
		Instant overlapStart = largestOverlappingItem.startTime.isAfter(selectionStartTime) ?
				largestOverlappingItem.startTime : selectionStartTime;
		Instant overlapEnd = largestOverlappingItem.stopTime.isBefore(selectionStopTime) ?
				largestOverlappingItem.stopTime : selectionStopTime;

		Duration overlap = Duration.between(overlapStart, overlapEnd);
		
		if (logger.isTraceEnabled()) {
			logger.trace("... checking whether duration of {} ms is at least 85 % of duration of {} ms ...",
					Long.valueOf(overlap.toMillis()).doubleValue(),
					Long.valueOf(Duration.between(selectionStartTime, selectionStopTime).toMillis()).doubleValue());
		}
		
		if (0.85 > (Long.valueOf(overlap.toMillis()).doubleValue() / 
				Long.valueOf(Duration.between(selectionStartTime, selectionStopTime).toMillis()).doubleValue())) {
			if (logger.isTraceEnabled()) logger.trace("... false!");
			return new HashSet<>();
		}
		if (logger.isTraceEnabled()) logger.trace("... true!");
		
		return overlappingItems;
	}

	/**
	 * Select the latest item (by generation time) from the given collection.
	 * For all items the item type must match the given productType.
	 * 
	 * @param items the collection of items to be searched
	 * @return a list containing the selected item, or an empty list, if no qualifying item exists in the collection
	 */
	public Set<SelectionItem> selectLastCreated(Collection<SelectionItem> items) {
		SelectionItem latestItem = null;
		
		// Test each of the items against the time interval and select the one with the latest generation time
		for (SelectionItem item: items) {
			if (null == latestItem || item.generationTime.isAfter(latestItem.generationTime)) {
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
		case ValIntersectWithoutDuplicates:
		case ValIntersect:			return selectValIntersect(items, startTime, stopTime);
		case LatestValIntersect:	return selectLatestValIntersect(items, startTime, stopTime);
		case LatestStartValidity:
		case LatestValidity:		return selectLatestValidity(items);
		case LatestStopValidity:	return selectLatestStopValidity(items);
		case LatestValCover:		return selectLatestValCover(items, startTime, stopTime);
		case LatestValidityClosest:	return selectLatestValidityClosest(items, startTime, stopTime);
		case ClosestStartValidity:	return selectClosestStartValidity(items, startTime);
		case ClosestStopValidity:	return selectClosestStopValidity(items, stopTime);
		case LargestOverlap:		return selectLargestOverlap(items, startTime, stopTime);
		case LargestOverlap85:		return selectLargestOverlap85(items, startTime, stopTime);
		case LastCreated:			return selectLastCreated(items);
		default:
			throw new UnsupportedOperationException(String.format(MSG_POLICY_TYPE_NOT_IMPLEMENTED, policyType.toString()));
		}
	}
	
	/**
	 * Format this policy as a query condition in JPQL (Java Persistence Query Language). It is assumed that the Product
	 * class is denoted as "select ... from Product p ..." in the JPQL query, to which the resulting condition is to be
	 * appended.
	 * <p>
	 * Limitations:
	 * <ul>
	 * <li>
	 * For LatestValidityClosest the query may return two products, one to each side of the centre of the
	 * given time interval. It is up to the calling program to select the applicable product.
	 * </li>
	 * <li>
	 * For LargestOverlap and LargestOverlap85 the query returns all items intersecting the interal (like ValIntersect),
	 * further selection by the calling program using either selectLargestOverlap(...) or selectLargestOverlap85(...)
	 * is required.
	 * </li>
	 * </ul>
	 * 
	 * @param sourceProductClass the source product class to use for the query (only required for LatestValidity and LatestValidityClosest)
	 * @param startTime the start time to use in the condition
	 * @param stopTime the stop time to use in the condition
	 * @param filterConditions filter conditions to apply
	 * @return a ProductQuery object representing this policy
	 */
	public String asJpqlQueryCondition(ProductClass sourceProductClass, final Instant startTime, final Instant stopTime, Map<String, Parameter> filterConditions) {
		StringBuilder simplePolicyQuery = new StringBuilder();
		
		/* Build JOIN and WHERE clauses for sub-SELECT clauses */
		
		// Join with as many instances of the product_parameters table as there are filter conditions
		int i = 0;
		StringBuilder subSelectQuery = new StringBuilder();
		for (String filterKey: filterConditions.keySet()) {
			// Restrict to actual parameters
			try {
				Product.class.getDeclaredField(filterKey);
				// Nothing to do – not a parameter, but a Product attribute
			} catch (NoSuchFieldException e) {
				subSelectQuery.append(String.format("join p2.parameters pp2%d ", i));
				++i;
			} catch (SecurityException e) {
				throw new RuntimeException(String.format(MSG_CANNOT_CREATE_QUERY, e.getMessage()), e);
			}
		}
		
		// Format filter conditions
		i = 0;
		StringBuilder filterQuery = new StringBuilder();
		for (String filterKey: filterConditions.keySet()) {
			// If the key points to a class attribute, query the attribute value, otherwise query a parameter with this key
			try {
				Product.class.getDeclaredField(filterKey);
				filterQuery.append(
						String.format(" and p2.%s = '%s'", filterKey, filterConditions.get(filterKey).getStringValue()));
			} catch (NoSuchFieldException e) {
				filterQuery.append(String.format(" and key(pp2%d) = '%s' and pp2%d.parameterValue = '%s'", 
						i, filterKey, i, filterConditions.get(filterKey).getStringValue()));
				++i;
			} catch (SecurityException e) {
				throw new RuntimeException(String.format(MSG_CANNOT_CREATE_QUERY, e.getMessage()), e);
			}
		}

		/* Create query condition for policy */
		
		switch (policyType) {
		case LatestValidity:
		case LatestStartValidity:
			simplePolicyQuery.append("p.sensingStartTime >= ")
					.append("(select max(p2.sensingStartTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(filterQuery)
					.append(")");
			break;
		case LatestStopValidity:
			simplePolicyQuery.append("p.sensingStopTime >= ")
					.append("(select max(p2.sensingStopTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(filterQuery)
					.append(")");
			break;
		case LatestValidityClosest:
			// This will result in two products, one on either side of the interval centre
			Instant selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
			Instant selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
			Duration selectionDuration = Duration.between(selectionStartTime, selectionStopTime);
			Instant selectionCentre = selectionStartTime.plusSeconds(selectionDuration.getSeconds() / 2);
			String selectionCentreString = DATEFORMAT_SQL.format(selectionCentre);
			simplePolicyQuery.append("(p.sensingStartTime <= '").append(selectionCentreString)
				.append("' and p.sensingStartTime >= ")
					.append("(select max(p2.sensingStartTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(" and p2.sensingStartTime <= '").append(selectionCentreString).append("'")
					.append(filterQuery)
					.append(") ")
				.append("or p.sensingStartTime > '").append(selectionCentreString)
				.append("' and p.sensingStartTime <= ")
					.append("(select min(p2.sensingStartTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(" and p2.sensingStartTime > '").append(selectionCentreString).append("'")
					.append(filterQuery)
					.append("))");
			break;
		case ClosestStartValidity:
			// This will result in two products, one on either side of the interval start
			selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
			String selectionStartString = DATEFORMAT_SQL.format(selectionStartTime);
			simplePolicyQuery.append("(p.sensingStartTime <= '").append(selectionStartString)
				.append("' and p.sensingStartTime >= ")
					.append("(select max(p2.sensingStartTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(" and p2.sensingStartTime <= '").append(selectionStartString).append("'")
					.append(filterQuery)
					.append(") ")
				.append("or p.sensingStartTime > '").append(selectionStartString)
				.append("' and p.sensingStartTime <= ")
					.append("(select min(p2.sensingStartTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(" and p2.sensingStartTime > '").append(selectionStartString).append("'")
					.append(filterQuery)
					.append("))");
			break;
		case ClosestStopValidity:
			// This will result in two products, one on either side of the interval end
			selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
			String selectionStopString = DATEFORMAT_SQL.format(selectionStopTime);
			simplePolicyQuery.append("(p.sensingStopTime <= '").append(selectionStopString)
				.append("' and p.sensingStopTime >= ")
					.append("(select max(p2.sensingStopTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(" and p2.sensingStopTime <= '").append(selectionStopString).append("'")
					.append(filterQuery)
					.append(") ")
				.append("or p.sensingStopTime > '").append(selectionStopString)
				.append("' and p.sensingStopTime <= ")
					.append("(select min(p2.sensingStopTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(" and p2.sensingStopTime > '").append(selectionStopString).append("'")
					.append(filterQuery)
					.append("))");
			break;
		case LatestValCover:
			simplePolicyQuery.append("p.sensingStartTime <= '")
				.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds())))
				.append("' and p.sensingStopTime >= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds())))
				.append("' and p.generationTime >= ")
					.append("(select max(p2.generationTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(" and p2.sensingStartTime <= '")
					.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds())))
					.append("' and p2.sensingStopTime >= '")
					.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds()))).append("'")
					.append(filterQuery)
					.append(")");
			break;
		case ValIntersect:
		case ValIntersectWithoutDuplicates:
		// With limitations, see method comment:
		case LargestOverlap:
		case LargestOverlap85:
			simplePolicyQuery.append("p.sensingStartTime < '")
				.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds())))
				.append("' and p.sensingStopTime > '")
				.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds())))
				.append("'");
			break;
		case LatestValIntersect:
			simplePolicyQuery.append("p.sensingStartTime < '")
				.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds())))
				.append("' and p.sensingStopTime > '")
				.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds())))
				.append("' and p.generationTime >= ")
					.append("(select max(p2.generationTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(" and p2.sensingStartTime < '")
					.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds())))
					.append("' and p2.sensingStopTime > '")
					.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds()))).append("'")
					.append(filterQuery)
					.append(")");
			break;
		case LastCreated:
			simplePolicyQuery.append("p.generationTime >= ")
					.append("(select max(p2.generationTime) from Product p2 ")
					.append(subSelectQuery)
					.append("where p2.productClass.id = ").append(sourceProductClass.getId())
					.append(filterQuery)
					.append(")");
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
	 * Limitations:
	 * <ul>
	 * <li>
	 * For LatestValidityClosest the query may return two products, one to each side of the centre of the
	 * given time interval. It is up to the calling program to select the applicable product.
	 * </li>
	 * <li>
	 * For LargestOverlap and LargestOverlap85 the query returns all items intersecting the interal (like ValIntersect),
	 * further selection by the calling program using either selectLargestOverlap(...) or selectLargestOverlap85(...)
	 * is required.
	 * </li>
	 * </ul>
	 * 
	 * @param sourceProductClass the source product class to use for the query (only required for LatestValidity and LatestValidityClosest)
	 * @param startTime the start time to use in the condition
	 * @param stopTime the stop time to use in the condition
	 * @param filterConditions filter conditions to apply
	 * @param productColumnMapping a mapping from attribute names of the Product class to the corresponding SQL column names
	 * @param facilityQuerySqlSubselect an SQL selection string to add to sub-SELECTs in selection policy SQL query conditions
	 * @return a ProductQuery object representing this policy
	 */
	public String asSqlQueryCondition(ProductClass sourceProductClass, final Instant startTime, final Instant stopTime, 
			Map<String, Parameter> filterConditions, Map<String, String> productColumnMapping, String facilityQuerySqlSubselect) {
		StringBuilder simplePolicyQuery = new StringBuilder();
		
		if (null == facilityQuerySqlSubselect) {
			facilityQuerySqlSubselect = "";
		}

		/* Build JOIN and WHERE clauses for sub-SELECT clauses */
		
		// Join with as many instances of the product_parameters table as there are filter conditions
		int i = 0;
		StringBuilder subSelectQuery = new StringBuilder();
		for (String filterKey: filterConditions.keySet()) {
			// Restrict to actual parameters
			try {
				Product.class.getDeclaredField(filterKey);
				// Nothing to do – not a parameter, but a Product attribute
			} catch (NoSuchFieldException e) {
				subSelectQuery.append(String.format("JOIN product_parameters pp2%d ON p2.id = pp2%d.product_id ", i, i));
				++i;
			} catch (SecurityException e) {
				throw new RuntimeException(String.format(MSG_CANNOT_CREATE_QUERY, e.getMessage()), e);
			}
		}
		
		// Format filter conditions
		i = 0;
		StringBuilder filterQuery = new StringBuilder();
		for (String filterKey: filterConditions.keySet()) {
			// If the key points to a class attribute, query the attribute value, otherwise query a parameter with this key
			String columnName = productColumnMapping.get(filterKey);
			if (null == columnName) {
				filterQuery.append(
						String.format(" AND pp2%d.parameters_key = '%s' AND pp2%d.parameter_value = '%s'", 
								i, filterKey, i, filterConditions.get(filterKey).getStringValue()));
				++i;
			} else {
				filterQuery.append(
						String.format(" AND p2.%s = '%s'", columnName, filterConditions.get(filterKey).getStringValue()));
			}
		}
		
		/* Create query condition for policy */
		
		switch (policyType) {
		case LatestValidity:
		case LatestStartValidity:
			simplePolicyQuery.append("p.sensing_start_time >= ")
				.append("(SELECT MAX(p2.sensing_start_time) FROM product p2 ")
				.append(subSelectQuery)
				.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
				.append(filterQuery)
				.append(facilityQuerySqlSubselect)
				.append(")");
			break;
		case LatestStopValidity:
			simplePolicyQuery.append("p.sensing_stop_time >= ")
				.append("(SELECT MAX(p2.sensing_stop_time) FROM product p2 ")
				.append(subSelectQuery)
				.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
				.append(filterQuery)
				.append(facilityQuerySqlSubselect)
				.append(")");
			break;
		case LatestValidityClosest:
			// This will result in two products, one on either side of the interval centre
			Instant selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
			Instant selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
			Duration selectionDuration = Duration.between(selectionStartTime, selectionStopTime);
			Instant selectionCentre = selectionStartTime.plusSeconds(selectionDuration.getSeconds() / 2);
			String selectionCentreString = DATEFORMAT_SQL.format(selectionCentre);
			simplePolicyQuery.append("(p.sensing_start_time <= '").append(selectionCentreString)
				.append("' AND p.sensing_start_time >= ")
				    .append("(SELECT MAX(p2.sensing_start_time) FROM product p2 ")
					.append(subSelectQuery)
				    .append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
				    .append(" AND p2.sensing_start_time <= '").append(selectionCentreString).append("'")
					.append(filterQuery)
					.append(facilityQuerySqlSubselect)
				    .append(") ")
				.append("OR p.sensing_start_time > '").append(selectionCentreString)
				.append("' AND p.sensing_start_time <= ")
					.append("(SELECT MIN(p2.sensing_start_time) FROM product p2 ")
					.append(subSelectQuery)
					.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
					.append(" AND p2.sensing_start_time > '").append(selectionCentreString).append("'")
					.append(filterQuery)
					.append(facilityQuerySqlSubselect)
					.append("))");
			break;
		case ClosestStartValidity:
			// This will result in two products, one on either side of the interval start
			selectionStartTime = startTime.minusMillis(getDeltaTimeT0().toMilliseconds());
			String selectionStartString = DATEFORMAT_SQL.format(selectionStartTime);
			simplePolicyQuery.append("(p.sensing_start_time <= '").append(selectionStartString)
				.append("' AND p.sensing_start_time >= ")
					.append("(SELECT MAX(p2.sensing_start_time) FROM product p2 ")
					.append(subSelectQuery)
					.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
					.append(" AND p2.sensing_start_time <= '").append(selectionStartString).append("'")
					.append(filterQuery)
					.append(facilityQuerySqlSubselect)
					.append(") ")
				.append("OR p.sensing_start_time > '").append(selectionStartString)
				.append("' AND p.sensing_start_time <= ")
					.append("(SELECT MIN(p2.sensing_start_time) FROM product p2 ")
					.append(subSelectQuery)
					.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
					.append(" AND p2.sensing_start_time > '").append(selectionStartString).append("'")
					.append(filterQuery)
					.append(facilityQuerySqlSubselect)
					.append("))");
			break;
		case ClosestStopValidity:
			// This will result in two products, one on either side of the interval end
			selectionStopTime = stopTime.plusMillis(getDeltaTimeT1().toMilliseconds());
			String selectionStopString = DATEFORMAT_SQL.format(selectionStopTime);
			simplePolicyQuery.append("(p.sensing_stop_time <= '").append(selectionStopString)
				.append("' AND p.sensing_stop_time >= ")
					.append("(SELECT MAX(p2.sensing_stop_time) FROM product p2 ")
					.append(subSelectQuery)
					.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
					.append(" AND p2.sensing_stop_time <= '").append(selectionStopString).append("'")
					.append(filterQuery)
					.append(facilityQuerySqlSubselect)
					.append(") ")
				.append("OR p.sensing_stop_time > '").append(selectionStopString)
				.append("' AND p.sensing_stop_time <= ")
					.append("(SELECT MIN(p2.sensing_stop_time) FROM product p2 ")
					.append(subSelectQuery)
					.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
					.append(" AND p2.sensing_stop_time > '").append(selectionStopString).append("'")
					.append(filterQuery)
					.append(facilityQuerySqlSubselect)
					.append("))");
			break;
		case LatestValCover:
			simplePolicyQuery.append("p.sensing_start_time <= '")
				.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds())))
				.append("' AND p.sensing_stop_time >= '")
				.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds())))
				.append("' AND p.generation_time >= ")
					.append("(SELECT MAX(p2.generation_time) FROM product p2 ")
					.append(subSelectQuery)
					.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
					.append(" AND p2.sensing_start_time <= '")
					.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds())))
					.append("' AND p2.sensing_stop_time >= '")
					.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds()))).append("'")
					.append(filterQuery)
					.append(facilityQuerySqlSubselect)
					.append(")");
			break;
		case ValIntersect:
		case ValIntersectWithoutDuplicates:
		// With limitations, see method comment:
		case LargestOverlap:
		case LargestOverlap85:
			simplePolicyQuery.append("p.sensing_start_time < '")
				.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds())))
				.append("' AND p.sensing_stop_time > '")
				.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds())))
				.append("'");
			break;
		case LatestValIntersect:
			simplePolicyQuery.append("p.sensing_start_time < '")
				.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds())))
				.append("' AND p.sensing_stop_time > '")
				.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds())))
				.append("' AND p.generation_time >= ")
					.append("(SELECT MAX(p2.generation_time) FROM product p2 ")
					.append(subSelectQuery)
					.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
					.append(" AND p2.sensing_start_time < '")
					.append(DATEFORMAT_SQL.format(stopTime.plusMillis(getDeltaTimeT1().toMilliseconds())))
					.append("' AND p2.sensing_stop_time > '")
					.append(DATEFORMAT_SQL.format(startTime.minusMillis(getDeltaTimeT0().toMilliseconds()))).append("'")
					.append(filterQuery)
					.append(facilityQuerySqlSubselect)
					.append(")");
			break;
		case LastCreated:
			simplePolicyQuery.append("p.generation_time >= ")
				.append("(SELECT MAX(p2.generation_time) FROM product p2 ")
				.append(subSelectQuery)
				.append("WHERE p2.product_class_id = ").append(sourceProductClass.getId())
				.append(filterQuery)
				.append(facilityQuerySqlSubselect)
				.append(")");
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
		String deltaTimes = "";
		switch (policyType) {
		case LatestValidity:
		case LatestStartValidity:
		case LatestStopValidity:
		case LastCreated:
			break;
		default:
			deltaTimes = "(" + getDeltaTimeT0() + ", " + getDeltaTimeT1() + ")";
		}
		return policyType.toString() + deltaTimes;
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
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof SimplePolicy))
			return false;
		SimplePolicy other = (SimplePolicy) obj;
		return Objects.equals(deltaTimes, other.getDeltaTimes()) && policyType == other.getPolicyType();
	}

}
