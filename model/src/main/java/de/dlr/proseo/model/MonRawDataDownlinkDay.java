package de.dlr.proseo.model;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "datetime")
	})
public class MonRawDataDownlinkDay extends MonRawDataDownlinkBase {

}
