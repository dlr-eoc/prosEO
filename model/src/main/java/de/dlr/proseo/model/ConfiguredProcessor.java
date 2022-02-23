/**
 * ConfiguredProcessor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;
import java.util.UUID;

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
@Table(indexes = {
	@Index(unique = false, columnList = "identifier"),
	@Index(unique = true, columnList = "uuid")
})
public class ConfiguredProcessor extends PersistentObject {

	/** 
	 * User-defined identifier for this processor configuration (recommended to be derived from ProcessorClass::processorName and
	 * the version information of the associated Processor and Configuration objects), unique within the mission
	 */
	private String identifier;
	
	/**
	 * A universally unique identifier (UUID) for this configured processor to identify it as "workflow" on ESA's ODPRIP API.
	 */
	private UUID uuid;
	
	/** The processor version */
	@ManyToOne
	private Processor processor;
	
	/** The configuration file version */
	@ManyToOne
	private Configuration configuration;
	
	/**
	 * A job step using this configured processor for the generation of its output product can only be started,
	 * if the processor is enabled
	 */
	private Boolean enabled = true;

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

	/**
	 * Gets the universally unique identifier (UUID) of this processor configuration
	 * 
	 * @return the UUID
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Sets the universally unique identifier (UUID) of this processor configuration
	 * 
	 * @param uuid the UUID to set
	 */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * Gets the associated versioned processor
	 * 
	 * @return the processor
	 */
	public Processor getProcessor() {
		return processor;
	}

	/**
	 * Sets the associated versioned processor
	 * 
	 * @param processor the processor to set
	 */
	public void setProcessor(Processor processor) {
		this.processor = processor;
	}

	/**
	 * Gets the associated processor configuration
	 * 
	 * @return the configuration
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Sets the associated processor configuration
	 * 
	 * @param configuration the configuration to set
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Gets the enabled status
	 * 
	 * @return true, if the configured processor is enabled, false otherwise
	 */
	public Boolean getEnabled() {
		return enabled;
	}

	/**
	 * Sets the enabled status
	 * 
	 * @param enabled the enabled status to set
	 */
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
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

	@Override
	public String toString() {
		return "ConfiguredProcessor [identifier=" + identifier + "]";
	}
	
}
