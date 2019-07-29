/**
 * SimplePolicy.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.OrderColumn;

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

	/* Message strings */
	private static final String MSG_DELTA_TIMES_NEGATIVE = "Delta times must not be negative.";
	private static final String MSG_ILLEGAL_LIST_OF_DELTA_TIMES = "List of delta times must contain exactly two entries for interval start and end.";

	/** The policy type to use */
	private PolicyType policyType;

	/** 
	 * The delta time to apply to the start (index 0) and end (index 1) of the selection period. Note that delta times always
	 * enlarge the selection period, they cannot be negative (i. e. reduce the interval).
	 */
	@ElementCollection
	@OrderColumn(name = "list_index")
	private List<DeltaTime> deltaTimes;
	
	/**
	 * Available policy types as defined in ESA's Generic IPF Interface Specifications.
	 * 
	 * Note: For Sentinel-5P only the policies LatestValCover, LatestValIntersect, ValIntersect, LatestValidityClosest, LatestValidity are used.
	 */
	public enum PolicyType { ValCover, LatestValCover, ValIntersect, LatestValIntersect, LatestValidityClosest,
		BestCenteredCover, LatestValCoverClosest, LargestOverlap, LargestOverlap85, LatestValidity, LatestValCoverNewestValidity 
	}
	
	/**
	 * Class representing a single overlapping time period
	 */
	@Embeddable
	public static class DeltaTime {
		
		/** The duration of the time period in time units */
		public Long duration;
		
		/** The time unit applicable for this time period */
		public TimeUnit unit;
		
		/**
		 * No-argument constructor sets delta time to zero days
		 */
		public DeltaTime() {
			this.duration = 0L;
			this.unit = TimeUnit.DAYS;
		}

		@Override
		public int hashCode() {
			return Objects.hash(duration, unit);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof DeltaTime))
				return false;
			DeltaTime other = (DeltaTime) obj;
			return Objects.equals(duration, other.duration) && unit == other.unit;
		}
	}
	
	/**
	 * No-argument sets default values for type (ValIntersect) and delta times (0/0)
	 */
	public SimplePolicy() {
		this.policyType = PolicyType.ValIntersect;
		this.deltaTimes = new ArrayList<>();
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
	 */
	public void setDeltaTimes(List<DeltaTime> deltaTimes) {
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
	 * @return the deltaTimes
	 */
	public DeltaTime getDeltaTimeT0() {
		return deltaTimes.get(0);
	}

	/**
	 * Gets the delta time to apply to the end of the interval (T1)
	 * @return the deltaTimes
	 */
	public DeltaTime getDeltaTimeT1() {
		return deltaTimes.get(1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(deltaTimes, policyType);
		return result;
	}

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
