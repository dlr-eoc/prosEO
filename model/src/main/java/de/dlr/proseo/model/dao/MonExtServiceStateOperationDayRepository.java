package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.MonExtServiceStateOperationDay;

public interface MonExtServiceStateOperationDayRepository extends JpaRepository<MonExtServiceStateOperationDay, Long> {

	/**
	 * Get a latest datetime
	 * 
	 * @return the latest datetime
	 */
	@Query("select max(d.datetime) from MonExtServiceStateOperationDay d")
	public Instant findLastDatetime();

	/**
	 * Get a list of entries
	 * 
	 * @param timeFrom        earliest time
	 * @param timeTo          latest time
	 * @param monExtServiceId service id
	 * @return a list of services satisfying the search criteria
	 */
	@Query("select p from MonExtServiceStateOperationDay p where p.datetime >= ?1 and p.datetime < ?2 and p.monExtServiceId = ?3")
	public List<MonExtServiceStateOperationDay> findByDateTimeBetween(Instant timeFrom, Instant timeTo, long monExtServiceId);

}
