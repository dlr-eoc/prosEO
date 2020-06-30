/**
 * InputFilter.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;

/**
 * Filter conditions to apply to an order input product of a specific product class in addition to filter conditions contained in the
 * applicable selection rule
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class InputFilter extends PersistentObject {

	/**
	 * Input filter conditions consisting of a key (a product attribute or parameter name) and a value, which must be matched
	 */
	@ElementCollection
	private Map<String, Parameter> filterConditions = new HashMap<>();

	/**
	 * @return the outputParameters
	 */
	public Map<String, Parameter> getFilterConditions() {
		return filterConditions;
	}

	/**
	 * @param outputParameters the outputParameters to set
	 */
	public void setFilterConditions(Map<String, Parameter> filterConditions) {
		this.filterConditions = filterConditions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(filterConditions);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof InputFilter))
			return false;
		InputFilter other = (InputFilter) obj;
		return Objects.equals(filterConditions, other.filterConditions);
	}
}
