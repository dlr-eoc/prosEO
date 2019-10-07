package de.dlr.proseo.planner.kubernetes;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.dao.JobStepRepository;
import de.dlr.proseo.planner.ProductionPlanner;
import dispatcher.JobDispatcher;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProductionPlanner.class, webEnvironment = WebEnvironment.RANDOM_PORT)
//@DirtiesContext
//@Transactional
@AutoConfigureTestEntityManager
public class KubeTest {

    @Autowired
    private JobStepRepository jobSteps;
    
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
		JobStep js = new JobStep();
		js.setProcessingMode("nix"); 
		jobSteps.save(js);
		JobDispatcher jd = new JobDispatcher();
		jd.createJobOrder(js);
		ProductionPlanner.updateKubeConfigs();
		if (ProductionPlanner.getKubeConfig(null).isConnected()) {
			KubeJob aJob = ProductionPlanner.getKubeConfig(null).createJob("test");
			if (aJob != null) {
				ProductionPlanner.getKubeConfig(null).deleteJob(aJob);
			}
		}
	}

}
