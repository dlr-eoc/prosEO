/**
 * ConfiguredProcessor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A specific version of a Processor combined with a specific Configuration object (i. e. a specific set of configuration data for
 * the given processor version). A ConfiguredProcessor is what must be specified for the generation of the products of a prosEO order.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = @Index(unique = true, columnList = "identifier"))
public class ConfiguredProcessor extends PersistentObject {

	/** 
	 * User-defined identifier for this processor configuration (recommended to be derived from ProcessorClass::processorName and
	 * the version information of the associated Processor and Configuration objects)
	 */
	private String identifier;
	
	/** The processor version */
	@ManyToOne
	private Processor processor;
	
	/** The configuration file version */
	@ManyToOne
	private Configuration configuration;

	/**
	 * Gets the identifier of the configured processor
	 * 
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the identifier of the configured processor
	 * 
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
