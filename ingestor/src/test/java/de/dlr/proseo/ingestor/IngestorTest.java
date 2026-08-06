/*
 * IngestorTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ingestor;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import de.dlr.proseo.logging.logger.ProseoLogger;


/**
 * Unit test cases for prosEO Ingestor
 *
 * @author Dr. Thomas Bassler
 */

@SpringBootTest(classes = IngestorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext
public class IngestorTest {

	/** Test configuration */
	@Autowired
	IngestorTestConfiguration config;
	
	/** The security environment for this test */
	@Autowired
	IngestorSecurityConfig ingestorSecurityConfig;
	
	@LocalServerPort
	private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;
    
    /** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(IngestorTest.class);
	
	@Test
	public void testHomeIsSecure() throws Exception {
		logger.trace(">>> testHomeIsSecure()");

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = testRestTemplate.getForEntity(
				"http://localhost:" + this.port, Map.class);
		assertEquals(entity.getStatusCode(), HttpStatus.UNAUTHORIZED);
		assertFalse(entity.getHeaders()
				.containsHeader("Set-Cookie"), "Wrong headers: " + entity.getHeaders());

		logger.trace("<<< testHomeIsSecure()");
	}

	@Test
	public void testMetricsIsSecure() throws Exception {
		logger.trace(">>> testMetricsIsSecure()");

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = testRestTemplate.getForEntity(
				"http://localhost:" + this.port + "/metrics", Map.class);
		assertEquals(entity.getStatusCode(), HttpStatus.UNAUTHORIZED);
		entity = testRestTemplate.getForEntity("http://localhost:" + this.port
				+ "/metrics/", Map.class);
		assertEquals(entity.getStatusCode(), HttpStatus.UNAUTHORIZED);
		entity = testRestTemplate.getForEntity("http://localhost:" + this.port
				+ "/metrics/foo", Map.class);
		assertEquals(entity.getStatusCode(), HttpStatus.UNAUTHORIZED);
		entity = testRestTemplate.getForEntity("http://localhost:" + this.port
				+ "/metrics.json", Map.class);
		assertEquals(entity.getStatusCode(), HttpStatus.UNAUTHORIZED);

		logger.trace("<<< testMetricsIsSecure()");
	}

	@Test
	public void testInfo() throws Exception {
		logger.trace(">>> testInfo()");

		ResponseEntity<String> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity("http://localhost:" + this.port + "/actuator/info", String.class);
		assertEquals(entity.getStatusCode(), HttpStatus.OK);
		assertTrue(	entity.getBody().equals("{}"), "Wrong body: " + entity.getBody());

		logger.trace("<<< testInfo()");
	}

	@Test
	public void testHealth() throws Exception {
		logger.trace(">>> testHealth()");

		ResponseEntity<String> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity("http://localhost:" + this.port + "/actuator/health", String.class);
		assertEquals(entity.getStatusCode(), HttpStatus.OK);
		assertTrue(	entity.getBody().contains("\"status\":\"UP\""), "Wrong body: " + entity.getBody());

		logger.trace("<<< testHealth()");
	}

	@Test
	public void testErrorPage() throws Exception {
		logger.trace(">>> testErrorPage()");

		ResponseEntity<String> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity("http://localhost:" + this.port + "/foo", String.class);
		assertEquals(entity.getStatusCode(), HttpStatus.NOT_FOUND);
		String body = entity.getBody();
		assertNotNull(body);
		assertTrue(body.contains("Not Found"), "Wrong body: " + body);

		logger.trace("<<< testErrorPage()");
	}

	@Test
	public void testHtmlErrorPage() throws Exception {
		logger.trace(">>> testHtmlErrorPage()");

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		HttpEntity<?> request = new HttpEntity<Void>(headers);
		ResponseEntity<String> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.exchange("http://localhost:" + this.port + "/foo", HttpMethod.GET,
						request, String.class);
		assertEquals(entity.getStatusCode(), HttpStatus.NOT_FOUND);
		String body = entity.getBody();
		assertNotNull("Body was null", body);
		assertTrue(body.contains("This application has no explicit mapping for /error"), "Wrong body: " + body);

		logger.trace("<<< testHtmlErrorPage()");
	}

}
