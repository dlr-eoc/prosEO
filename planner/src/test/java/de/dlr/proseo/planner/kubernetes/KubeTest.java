package de.dlr.proseo.planner.kubernetes;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
		KubeConfig.connect();
		if (KubeConfig.isConnected()) {
	    	KubeJob aJob = KubeConfig.createJob("hugo");
	    	if (aJob != null) {
	    		KubeConfig.deleteJob(aJob);
	    	}
		}
	}

}
