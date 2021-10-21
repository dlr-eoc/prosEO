package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonProductProductionHour;

/**
 * Data Access Object for the MonProductProductionHour class
 * 
 * @author Ernst Melchinger
 *
 */
@Repository
public interface MonProductProductionHourRepository extends JpaRepository<MonProductProductionHour, Long> {

	/**
	 * Get a list of products
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select max(d.datetime) from MonProductProductionHour d")
	public Instant findLastDatetime();

	/**
	 * Get a list of products
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from MonProductProductionHour p where p.mission.id = ?1 and p.monProductionType.id = ?2 and p.datetime = ?3")
	public List<MonProductProductionHour> findByProductionTypeAndDatetime(long missionId, long mpt, Instant datetime);
}
