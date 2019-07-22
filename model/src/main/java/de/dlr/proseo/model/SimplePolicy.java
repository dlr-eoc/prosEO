/**
 * SimplePolicy.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.persistence.Embeddable;
import javax.persistence.Entity;

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

	/** The policy type to use */
	private PolicyType type;

	/** The delta time to apply to the start of the selection period. Note that delta times always enlarge the selection period,
	 * they cannot be negative (i. e. reduce the interval).
	 */
	private DeltaTime deltaTimeT0;
	
	/** The delta time to apply to the end of the selection period. Note that delta times always enlarge the selection period,
	 * they cannot be negative (i. e. reduce the interval).
	 */
	private DeltaTime deltaTimeT1;
	
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
	 * @return the type
	 */
	public PolicyType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(PolicyType type) {
		this.type = type;
	}

	/**
	 * @return the deltaTimeT0
	 */
	public DeltaTime getDeltaTimeT0() {
		return deltaTimeT0;
	}

	/**
	 * @param deltaTimeT0 the deltaTimeT0 to set
	 */
	public void setDeltaTimeT0(DeltaTime deltaTimeT0) {
		this.deltaTimeT0 = deltaTimeT0;
	}

	/**
	 * @return the deltaTimeT1
	 */
	public DeltaTime getDeltaTimeT1() {
		return deltaTimeT1;
	}

	/**
	 * @param deltaTimeT1 the deltaTimeT1 to set
	 */
	public void setDeltaTimeT1(DeltaTime deltaTimeT1) {
		this.deltaTimeT1 = deltaTimeT1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(deltaTimeT0, deltaTimeT1, type);
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
		return Objects.equals(deltaTimeT0, other.deltaTimeT0) && Objects.equals(deltaTimeT1, other.deltaTimeT1)
				&& type == other.type;
	}
	
	
}
