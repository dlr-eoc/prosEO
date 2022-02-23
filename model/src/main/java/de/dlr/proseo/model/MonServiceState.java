package de.dlr.proseo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(indexes = {
	@Index(unique = true, columnList = "name")
})
public class MonServiceState extends PersistentObject {
	
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
