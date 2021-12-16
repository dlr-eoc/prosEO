package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonKpi02CompletenessMonth;

/**
 * Data Access Object for the MonProductProductionDay class
 * 
 * @author Ernst Melchinger
 *
 */
@Repository
public interface MonKpi02CompletenessMonthRepository extends JpaRepository<MonKpi02CompletenessMonth, Long> {

	/**
	 * Get a latest datetime
	 * 
	 * @return the latest datetime
	 */
	@Query("select max(d.datetime) from MonKpi02CompletenessMonth d")
	public Instant findLastDatetime();

	/**
	 * Get a list of entries
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from MonKpi02CompletenessMonth p where p.datetime >= ?1 and p.datetime < ?2")
	public List<MonKpi02CompletenessMonth> findByDateTimeBetween(Instant timeFrom, Instant timeTo);
}
