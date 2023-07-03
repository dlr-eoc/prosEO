package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonProductProductionMonth;

/**
 * Data Access Object for the MonProductProductionMonth class
 * 
 * @author Ernst Melchinger
 *
 */
@Repository
public interface MonProductProductionMonthRepository extends JpaRepository<MonProductProductionMonth, Long> {

	/**
	 * Get a list of products
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select max(d.datetime) from MonProductProductionMonth d")
	public Instant findLastDatetime();
	
	/**
	 * Get a list of products
	 * 
	 * @param missionId the mission id
	 * @param mpt       the production type
	 * @param datetime  the datetime
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from MonProductProductionMonth p where p.mission.id = ?1 and p.productionType = ?2 and p.datetime = ?3")
	public List<MonProductProductionMonth> findByProductionTypeAndDatetime(long missionId, String mpt, Instant datetime);

	/**
	 * Get a list of products
	 * 
	 * @param missionId the mission id
	 * @param mpt       the production type
	 * @param timeFrom  the earliest datetime
	 * @param timeTo    the latest datetime
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from MonProductProductionMonth p where p.mission.id = ?1 and p.productionType = ?2 and p.datetime >= ?3 and p.datetime < ?4")
	public List<MonProductProductionMonth> findByMissionCodeAndProductionTypeAndDateTimeBetween(long missionId, String mpt, Instant timeFrom, Instant timeTo);

}
