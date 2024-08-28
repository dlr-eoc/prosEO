/**
 * ProcessingOrderHistory.java
 * 
 * (C) 2024 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.Table;

import de.dlr.proseo.model.enums.OrderState;

/**
 * A history entry for logging the processing times of a production order
 * 
 * @author Ernst Melchinger
 *
 */
@Entity
@Table(indexes = {
	@Index(unique = true, columnList = "mission_code, identifier")
})
public class ProcessingOrderHistory extends PersistentObject {

	/** Mission, to which this order belongs */
	@Column(name = "mission_code")
	private String missionCode;
	
	/** User-defined order identifier (unique within the mission) */
	@Column(nullable = false)
	private String identifier;
	
	/**
	 * Date and time at which the ProcessingOrder was created
	 */
	@Column(name = "creation_time", columnDefinition = "TIMESTAMP")
	private Instant creationTime;

	/**
	 * Date and time at which the ProcessingOrder was released
	 */
	@Column(name = "release_time", columnDefinition = "TIMESTAMP")
	private Instant releaseTime;

	/**
	 * Date and time at which the ProcessingOrder was completed
	 */
	@Column(name = "completion_time", columnDefinition = "TIMESTAMP")
	private Instant completionTime;

	/**
	 * Date and time at which the ProcessingOrder was deleted
	 */
	@Column(name = "deletion_time", columnDefinition = "TIMESTAMP")
	private Instant deletionTime;

	/** State of the processing order */
	@Enumerated(EnumType.STRING)
	private OrderState orderState;

	/** The product types produced */
	@Column(name = "product_types")
	@ElementCollection
	private Set<String> productTypes  = new HashSet<>();

	/**
	 * @return the missionCode
	 */
	public String getMissionCode() {
		return missionCode;
	}

	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return the creationTime
	 */
	public Instant getCreationTime() {
		return creationTime;
	}

	/**
	 * @return the releaseTime
	 */
	public Instant getReleaseTime() {
		return releaseTime;
	}

	/**
	 * @return the completionTime
	 */
	public Instant getCompletionTime() {
		return completionTime;
	}

	/**
	 * @return the deletionTime
	 */
	public Instant getDeletionTime() {
		return deletionTime;
	}

	/**
	 * @return the orderState
	 */
	public OrderState getOrderState() {
		return orderState;
	}

	/**
	 * @return the productTypes
	 */
	public Set<String> getProductTypes() {
		return productTypes;
	}

	/**
	 * @param missionCode the missionCode to set
	 */
	public void setMissionCode(String missionCode) {
		this.missionCode = missionCode;
	}

	/**
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * @param creationTime the creationTime to set
	 */
	public void setCreationTime(Instant creationTime) {
		this.creationTime = creationTime;
	}

	/**
	 * @param releaseTime the releaseTime to set
	 */
	public void setReleaseTime(Instant releaseTime) {
		this.releaseTime = releaseTime;
	}

	/**
	 * @param completionTime the completionTime to set
	 */
	public void setCompletionTime(Instant completionTime) {
		this.completionTime = completionTime;
	}

	/**
	 * @param deletionTime the deletionTime to set
	 */
	public void setDeletionTime(Instant deletionTime) {
		this.deletionTime = deletionTime;
	}

	/**
	 * @param orderState the orderState to set
	 */
	public void setOrderState(OrderState orderState) {
		this.orderState = orderState;
	}

	/**
	 * @param productType the productType to set
	 */
	public void setProductTypes(Set<String> productTypes) {
		this.productTypes = productTypes;
	}

}
