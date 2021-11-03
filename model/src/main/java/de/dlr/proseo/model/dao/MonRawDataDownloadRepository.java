package de.dlr.proseo.model.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.MonRawDataDownload;

/**
 * Data Access Object for the MonRawDataDownload class
 * 
 * @author Ernst Melchinger
 *
 */
@Repository
public interface MonRawDataDownloadRepository extends JpaRepository<MonRawDataDownload, Long> {

	/**
	 * Get a list of products
	 * 
	 * @return a list of products satisfying the search criteria
	 */
	@Query("select p from MonRawDataDownload p where p.missionId = ?1 and p.spacecraftCode = ?2 and p.downloadTime >= ?3 and p.downloadTime < ?4")
	public List<MonRawDataDownload> findByMissionCodeAndSpacecraftCodeAndDownloadTimeBetween(long code, String spacecraft, Instant timeFrom,
			Instant timeTo);
}
