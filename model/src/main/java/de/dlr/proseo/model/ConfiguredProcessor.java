/**
 * ConfiguredProcessor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
	// TODO Re-test column definition after migration to Spring Boot 3 / Hibernate 6 and remove if possible
	@Column(nullable = false, columnDefinition = "uuid")
	private UUID uuid;
	
	/** The processor version */
	@ManyToOne(fetch = FetchType.LAZY)
	private Processor processor;
	
	/** The configuration file version */
	@ManyToOne(fetch = FetchType.LAZY)
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
		return Objects.hash(identifier);
	}

	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof ConfiguredProcessor))
			return false;
		ConfiguredProcessor other = (ConfiguredProcessor) obj;
		return Objects.equals(identifier, other.getIdentifier());
	}

	@Override
	public String toString() {
		return "ConfiguredProcessor [identifier=" + identifier + "]";
	}
	
}
