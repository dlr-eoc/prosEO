/**
 * DatatakeGroup.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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
	
}
