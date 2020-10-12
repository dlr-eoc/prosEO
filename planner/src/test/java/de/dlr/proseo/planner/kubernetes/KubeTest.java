package de.dlr.proseo.planner.kubernetes;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.planner.ProductionPlanner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProductionPlanner.class, webEnvironment = WebEnvironment.RANDOM_PORT)
//@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class KubeTest {

//    @Autowired
//    private JobStepRepository jobSteps;
//    
//    @Autowired
//    private ProductionPlanner productionPlanner;
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
//		Mission m = RepositoryService.getMissionRepository().findByCode("PTM");
//		String pft = m.getProductFileTemplate();
//		RepositoryService.getMissionRepository().save(m);
//		JobStep js = new JobStep();
//		js.setProcessingMode("nix"); 
//		jobSteps.save(js);
//		JobDispatcher jd = new JobDispatcher();
//		jd.createJobOrder(js);
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
