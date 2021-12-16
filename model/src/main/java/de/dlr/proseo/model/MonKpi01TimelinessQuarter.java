package de.dlr.proseo.model;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "mon_kpi01_timeliness_quarter", indexes = {
		@Index(unique = false, columnList = "datetime")
	})
public class MonKpi01TimelinessQuarter extends MonKpi01Base {
}
