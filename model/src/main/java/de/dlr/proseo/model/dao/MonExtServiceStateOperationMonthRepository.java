package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonExtServiceStateOperationMonth;

@Repository
public interface MonExtServiceStateOperationMonthRepository extends JpaRepository<MonExtServiceStateOperationMonth, Long> {

	/**
	 * Get a latest datetime
	 * 
	 * @return the latest datetime
	 */
	@Query("select max(d.datetime) from MonExtServiceStateOperationMonth d")
	public Instant findLastDatetime();

	/**
	 * Get a list of entries
	 * 
	 * @return a list of services satisfying the search criteria
	 */
	@Query("select p from MonExtServiceStateOperationMonth p where p.datetime >= ?1 and p.datetime < ?2 and p.monServiceId = ?3")
	public List<MonExtServiceStateOperationMonth> findByDateTimeBetween(Instant timeFrom, Instant timeTo, long monServiceId);

}
