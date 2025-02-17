package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.dlr.proseo.model.MonServiceStateOperationMonth;

public interface MonServiceStateOperationMonthRepository extends JpaRepository<MonServiceStateOperationMonth, Long> {

	/**
	 * Get a latest datetime
	 * 
	 * @return the latest datetime
	 */
	@Query("select max(d.datetime) from MonServiceStateOperationMonth d")
	public Instant findLastDatetime();

	/**
	 * Get a list of entries
	 * 
	 * @param timeFrom     the earliest datetime
	 * @param timeTo       the latest datetime
	 * @param monServiceId the service id
	 * @return a list of services satisfying the search criteria
	 */
	@Query("select p from MonServiceStateOperationMonth p where p.datetime >= ?1 and p.datetime < ?2 and p.monServiceId = ?3")
	public List<MonServiceStateOperationMonth> findByDateTimeBetween(Instant timeFrom, Instant timeTo, long monServiceId);

}
