/**
 * MonServiceState.java
 *
 * © 2021 Prophos Informatik GmbH
 */
package de.dlr.proseo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A service state identified by its name
 * (used for database-based configuration of service monitoring)
 */
@Entity
@Table(indexes = {
	@Index(unique = true, columnList = "name")
})
public class MonServiceState extends PersistentObject {
	
	/**
	 * The service state name
	 */
	@Column(nullable = false)
	private String name;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
}
