/*
 * RepositoryServiceTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.model.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit test cases for prosEO Repository Service
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class RepositoryServiceTest {

	private static Logger logger = LoggerFactory.getLogger(RepositoryServiceTest.class);
	
	@Test
	public void testJpa() {
		logger.info("Starting JPA test");
		JustAPlainJavaClass japjc = new JustAPlainJavaClass();
		japjc.testJpa();
	}

}
