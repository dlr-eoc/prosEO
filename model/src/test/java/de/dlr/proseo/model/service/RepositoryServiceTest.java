/*
 * RepositoryServiceTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.model.service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit test cases for prosEO Repository Service
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class RepositoryServiceTest {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(RepositoryServiceTest.class);
	
	/**
	 * Tests calling the RepositoryService from a conventionally created plain Java object
	 */
	@Test
	public void testJpa() {
		logger.info("Starting JPA test");
		JustAPlainJavaClass japjc = new JustAPlainJavaClass();
		japjc.testJpa();
	}

}
