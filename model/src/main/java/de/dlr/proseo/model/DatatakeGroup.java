/**
 * DatatakeGroup.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * A group of datatakes taken synchronously by a formation of satellites
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = {
		@Index(unique = true, columnList = "dt_group_identifier") 
})
public class DatatakeGroup extends PersistentObject {

	/** (External) identifier for the datatake group (typically from Mission Planning) */
	@Column(nullable = false)
	private String dtGroupIdentifier;
	
	/** The mission this datatake group belongs to */
	@ManyToOne(optional = false)
	private Mission mission;
	
	/** The datatakes belonging to this group */
	@OneToMany(mappedBy = "datatakeGroup")
	private Set<Datatake> datatakes = new HashSet<>();

	/**
	 * Gets the identifier for the datatake group
	 * 
	 * @return the datatake group identifier
	 */
	public String getDtGroupIdentifier() {
		return dtGroupIdentifier;
	}

	/**
	 * Sets the identifier for the datatake group
	 * 
	 * @param dtGroupIdentifier the datatake group identifier to set
	 */
	public void setDtGroupIdentifier(String dtGroupIdentifier) {
		this.dtGroupIdentifier = dtGroupIdentifier;
	}

	/**
	 * Gets the mission of the datatake group
	 * 
	 * @return the associated mission
	 */
	public Mission getMission() {
		return mission;
	}

	/**
	 * Sets the mission of the datatake group
	 * 
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}

	/**
	 * Gets the datatakes belonging to this group
	 * 
	 * @return a set of associated datatakes
	 */
	public Set<Datatake> getDatatakes() {
		return datatakes;
	}

	/**
	 * Sets the datatakes belonging to this group
	 * 
	 * @param datatakes the set of datatakes to set
	 */
	public void setDatatakes(Set<Datatake> datatakes) {
		this.datatakes = datatakes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(mission, dtGroupIdentifier);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof DatatakeGroup))
			return false;
		DatatakeGroup other = (DatatakeGroup) obj;
		return Objects.equals(mission, other.mission) && Objects.equals(dtGroupIdentifier, other.dtGroupIdentifier);
	}

	@Override
	public String toString() {
		return "DatatakeGroup [id=" + this.getId() + ", mission=" + mission.getCode() + ", dtGroupIdentifier=" + dtGroupIdentifier + ", datatakes=" + datatakes + "]";
	}
	
}
