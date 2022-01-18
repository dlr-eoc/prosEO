package de.dlr.proseo.model;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "mon_kpi02_completeness_quarter", indexes = {
		@Index(unique = false, columnList = "datetime")
	})
public class MonKpi02CompletenessQuarter extends MonKpi02Base {
}
