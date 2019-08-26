package de.dlr.proseo.planner.kubernetes;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.dlr.proseo.planner.ProductionPlanner;

public class KubeTest {

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
		if (ProductionPlanner.addKubeConfig("Lerchenhof", "Testumgebung auf dem Lerchenhof", "http://192.168.20.159:8080") != null) {
			if (ProductionPlanner.getKubeConfig("Lerchenhof").isConnected()) {
				KubeJob aJob = ProductionPlanner.getKubeConfig("Lerchenhof").createJob("test");
				if (aJob != null) {
					ProductionPlanner.getKubeConfig("Lerchenhof").deleteJob(aJob);
				}
			}
		}
	}

}
