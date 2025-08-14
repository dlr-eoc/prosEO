package de.dlr.proseo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
	@Index(unique = true, columnList = "name")
})
public class MonExtService extends PersistentObject {

	/**
	 * The service caption
	 */
	@Column(nullable = false)
	private String name;

	/**
	 * The service name
	 */
	@Column(nullable = false)
	private String nameId;

	/**
	 * @return the nameId
	 */
	public String getNameId() {
		return nameId;
	}

	/**
	 * @param nameId the nameId to set
	 */
	public void setNameId(String nameId) {
		this.nameId = nameId;
	}
	
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
