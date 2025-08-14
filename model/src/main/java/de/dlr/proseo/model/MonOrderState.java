package de.dlr.proseo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
	@Index(unique = true, columnList = "name")
})
public class MonOrderState extends PersistentObject {
	
	/**
	 * The service name
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
