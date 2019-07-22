/**
 * ConfiguredProcessor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * An Earth Observation mission.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class ConfiguredProcessor extends PersistentObject {

	/** Human-readable identifier for this processor configuration */
	private String identifier;
	
	/** The processor version */
	// private Processor processor;
	
	/** The configuration file version */
	// private Configuration configuration;

	/**
	 * Gets the identifier of the configured processor
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the identifier of the configured processor
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(identifier);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ConfiguredProcessor))
			return false;
		ConfiguredProcessor other = (ConfiguredProcessor) obj;
		return Objects.equals(identifier, other.identifier);
	}
	
}
