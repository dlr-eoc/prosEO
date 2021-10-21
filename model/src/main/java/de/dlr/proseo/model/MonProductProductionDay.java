package de.dlr.proseo.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import org.springframework.data.jpa.repository.Query;

@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "datetime"),
		@Index(unique = false, columnList = "mission_id, mon_production_type_id")
	})
public class MonProductProductionDay extends MonProductProduction {

}
