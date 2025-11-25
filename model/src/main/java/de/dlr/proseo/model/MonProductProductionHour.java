/**
 * MonProductProductionHour.java
 *
 * © 2021 Prophos Informatik GmbH
 */
package de.dlr.proseo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Product generation performance per hour
 * per production type (systematic/on-demand/reprocessing)
 */
@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "datetime"),
		@Index(unique = false, columnList = "mission_id, productionType")
	})
public class MonProductProductionHour extends MonProductProduction {

}
