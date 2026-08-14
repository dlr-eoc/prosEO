package de.dlr.proseo.planner.kubernetes;

import java.nio.file.FileSystems;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;


@SpringBootTest(classes = ProductionPlanner.class, webEnvironment = WebEnvironment.RANDOM_PORT)
//@DirtiesContext
@WithMockUser(username = "PTM-proseo", roles = { "ORDER_APPROVER", "ORDER_MGR" })
@Transactional
@Sql("/ptm.sql")
public class KubeTest {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(KubeTest.class);

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
//	@Sql({"/schema.sql", "/data.sql"})
	public void test() {
		logger.debug(">>> Starting test()");
		String userDirectory = FileSystems.getDefault()
		        .getPath("")
		        .toAbsolutePath()
		        .toString();
		logger.debug("cwd: {}", userDirectory);
		Mission m = RepositoryService.getMissionRepository().findByCode("PTM");
		logger.debug("Mission: {}", m);
		m.setProductFileTemplate("PTM_${fileClass}_${productClass.productType}_${T(java.time.format.DateTimeFormatter).ofPattern(\"uuuuMMdd'T'HHmmss\").withZone(T(java.time.ZoneId).of(\"UTC\")).format(sensingStartTime)}_${T(java.time.format.DateTimeFormatter).ofPattern(\"uuuuMMdd'T'HHmmss\").withZone(T(java.time.ZoneId).of(\"UTC\")).format(sensingStopTime)}_${(new java.text.DecimalFormat(\"00000\")).format(null == orbit ? 0 : orbit.orbitNumber)}_${parameters.get(\"revision\").getParameterValue()}_${configuredProcessor.processor.processorVersion.replaceAll(\"\\.\", \"\")}_${T(java.time.format.DateTimeFormatter).ofPattern(\"uuuuMMdd'T'HHmmss\").withZone(T(java.time.ZoneId).of(\"UTC\")).format(generationTime)}.nc");
		RepositoryService.getMissionRepository().save(m);
//		JobStep js = new JobStep();
//		js.setProcessingMode("nix");
//		RepositoryService.getJobStepRepository().save(js);
//		JobDispatcher jd = new JobDispatcher();
//		try {
//			jd.createJobOrder(js);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		productionPlanner.updateKubeConfigs();
//		if (productionPlanner.getKubeConfig(null).isConnected()) {
//			KubeJob aJob = productionPlanner.getKubeConfig(null).createJob("test", "INFO", "INFO");
//			if (aJob != null) {
//				productionPlanner.getKubeConfig(null).deleteJob(aJob);
//			}
//		}
//		productionPlanner.stopDispatcher();
	}

}
