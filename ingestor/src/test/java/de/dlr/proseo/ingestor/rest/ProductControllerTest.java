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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import de.dlr.proseo.ingestor.Ingestor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test cases for prosEO Ingestor REST services
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Ingestor.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class ProductControllerTest {

	private static final String HTTP_HEADER_WARNING = "Warning";

	@Autowired
	private SecurityProperties security;

	@Value("${local.server.port}")
	private int port;

	@Test
	public void testProducts() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/products", List.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> body = entity.getBody();
		assertEquals("123", body.get(0).get("id"));
		assertEquals("ABC", body.get(1).get("descriptor"));
	}

	@Test
	public void testProductsError() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/products/?id=789", List.class);
		assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
		HttpHeaders headers = entity.getHeaders();
		assertEquals("199", headers.get(HTTP_HEADER_WARNING).get(0).substring(0, 3)); // non-specific warning
		assertTrue("Wrong header: " + headers, headers.get(HTTP_HEADER_WARNING).get(0).endsWith("not found (1001)"));
	}

	private String getPassword() {
		return this.security.getUser().getPassword();
	}

}
