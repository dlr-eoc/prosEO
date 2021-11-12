package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonRawDataDownlinkDay;

/**
 * Data Access Object for the MonRawDataDownlinkDay class
 * 
 * @author Ernst Melchinger
 *
 */
@Repository
public interface MonRawDataDownlinkDayRepository extends JpaRepository<MonRawDataDownlinkDay, Long> {

	/**
	 * Get a list of products
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select max(d.datetime) from MonRawDataDownlinkDay d")
	public Instant findLastDatetime();

	/**
	 * Get a list of products
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from MonRawDataDownlinkDay p where p.mission.id = ?1 and p.spacecraftCode = ?2 and p.datetime = ?3")
	public List<MonRawDataDownlinkDay> findByMissionIdAndSpacecraftCodeAndDatetime(long missionId, String spacecraft, Instant datetime);

	/**
	 * Get a list of products
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from MonRawDataDownlinkDay p where p.mission.id = ?1 and p.spacecraftCode = ?2 and p.datetime >= ?3 and p.datetime < ?4")
	public List<MonRawDataDownlinkDay> findByMissionIdAndSpacecraftCodeAndDateTimeBetween(long missionId, String spacecraft, 
			Instant timeFrom, Instant timeTo);

}
