package de.dlr.proseo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "datetime"),
		@Index(unique = false, columnList = "mission_id, productionType")
	})
public class MonProductProductionHour extends MonProductProduction {

}
