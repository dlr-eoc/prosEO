/**
 * XbipMonitorTest.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.xbipmon;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.api.xbipmon.rest.model.RestInterfaceStatus;

/**
 * @author thomas
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = XbipMonitorApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class XbipMonitorTest {
	
	private static final String TEST_XBIP_MONITOR_ID = "S1B-SGS-01";
	private static final String XBIP_MONITOR_BASE_URI = "/proseo/xbip-monitor/v0.1/" + TEST_XBIP_MONITOR_ID;

	@LocalServerPort
	private int port;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(XbipMonitorTest.class);
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void test() {
		String testUrl = "http://localhost:" + this.port + XBIP_MONITOR_BASE_URI + "/status";
		
		ResponseEntity<RestInterfaceStatus> restInterfaceStatus = new TestRestTemplate().getForEntity(testUrl, RestInterfaceStatus.class);
		
		assertEquals("Unexpected XBIP Monitor identifier: ", TEST_XBIP_MONITOR_ID, restInterfaceStatus.getBody().getId());
		assertEquals("Unexpected availability: ", true, restInterfaceStatus.getBody().getAvailable());
		assertEquals("Unexpected performance: ", Double.valueOf(0.0), restInterfaceStatus.getBody().getPerformance());
	}

}
