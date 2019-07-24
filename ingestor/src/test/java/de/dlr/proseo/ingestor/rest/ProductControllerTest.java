/*
 * ProductControllerTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ingestor.rest;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.ingestor.Ingestor;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.dao.ProductRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test cases for prosEO Ingestor REST services
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Ingestor.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class ProductControllerTest {

	private static final String HTTP_HEADER_WARNING = "Warning";

	@Autowired
	private SecurityProperties security;

	@LocalServerPort
	private int port;

    @Autowired
    private ProductRepository products;
	

	private static Logger logger = LoggerFactory.getLogger(ProductControllerTest.class);
	
	@Test
	public void testJpa() {
		logger.info("Preparing test products");
		Product product1 = new Product();
		products.save(product1);
		
		logger.info("Looking for all products");
		
		products.findAll().forEach(product -> { logger.info("Found product {}", product.getId()); });
		logger.info("JPA test complete");
	}

	@Test
	public void testProducts() throws Exception {
		String testUrl = "http://localhost:" + this.port + "/proseo/ingestor/v0.1/products";
		logger.info("Testing URL {}", testUrl);
		
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = new TestRestTemplate("user", getPassword())
				.getForEntity(testUrl, List.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> body = entity.getBody();
		assertEquals(123, body.get(0).get("id"));
		assertEquals("2019-07-22T13:57:38.654321", body.get(1).get("sensingStart"));
	}

	@Test
	public void testProductsError() throws Exception {
		String testUrl = "http://localhost:" + this.port + "/proseo/ingestor/v0.1/products/?id=789";
		logger.info("Testing URL {}", testUrl);
		
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate("user", getPassword())
				.getForEntity(testUrl, Map.class);
		assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
		HttpHeaders headers = entity.getHeaders();
		assertEquals("199", headers.get(HTTP_HEADER_WARNING).get(0).substring(0, 3)); // non-specific warning
		assertTrue("Wrong header: " + headers, headers.get(HTTP_HEADER_WARNING).get(0).endsWith("not found (1001)"));
	}

	private String getPassword() {
		return this.security.getUser().getPassword();
	}

}
