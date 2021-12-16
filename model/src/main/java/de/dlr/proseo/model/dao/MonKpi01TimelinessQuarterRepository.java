package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonKpi01TimelinessQuarter;


@Repository
public interface MonKpi01TimelinessQuarterRepository extends JpaRepository<MonKpi01TimelinessQuarter, Long> {

	/**
	 * Get a latest datetime
	 * 
	 * @return the latest datetime
	 */
	@Query("select max(d.datetime) from MonKpi01TimelinessQuarter d")
	public Instant findLastDatetime();

	/**
	 * Get a list of entries
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from MonKpi01TimelinessQuarter p where p.datetime >= ?1 and p.datetime < ?2")
	public List<MonKpi01TimelinessQuarter> findByDateTimeBetween(Instant timeFrom, Instant timeTo);

}
