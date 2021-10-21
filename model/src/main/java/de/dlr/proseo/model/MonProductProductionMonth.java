package de.dlr.proseo.model;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "datetime"),
		@Index(unique = false, columnList = "mission_id, mon_production_type_id")
	})
public class MonProductProductionMonth extends MonProductProduction {

}
