/*
 * IngestorTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ingestor;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test cases for prosEO Ingestor
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = IngestorApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
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

	@Test
	public void testHomeIsSecure() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port, Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		assertFalse("Wrong headers: " + entity.getHeaders(), entity.getHeaders()
				.containsKey("Set-Cookie"));
	}

	@Test
	public void testMetricsIsSecure() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/metrics", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		entity = new TestRestTemplate().getForEntity("http://localhost:" + this.port
				+ "/metrics/", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		entity = new TestRestTemplate().getForEntity("http://localhost:" + this.port
				+ "/metrics/foo", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		entity = new TestRestTemplate().getForEntity("http://localhost:" + this.port
				+ "/metrics.json", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
	}

	@Test
	public void testInfo() throws Exception {
		ResponseEntity<String> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity("http://localhost:" + this.port + "/actuator/info", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body: " + entity.getBody(),
				entity.getBody().equals("{}"));
	}

	@Test
	public void testHealth() throws Exception {
		ResponseEntity<String> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity("http://localhost:" + this.port + "/actuator/health", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body: " + entity.getBody(),
				entity.getBody().contains("\"status\":\"UP\""));
	}

	@Test
	public void testErrorPage() throws Exception {
		ResponseEntity<String> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity("http://localhost:" + this.port + "/foo", String.class);
		assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
		String body = entity.getBody();
		assertNotNull(body);
		assertTrue("Wrong body: " + body, body.contains("Not Found"));
	}

	@Test
	public void testHtmlErrorPage() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		HttpEntity<?> request = new HttpEntity<Void>(headers);
		ResponseEntity<String> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.exchange("http://localhost:" + this.port + "/foo", HttpMethod.GET,
						request, String.class);
		assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
		String body = entity.getBody();
		assertNotNull("Body was null", body);
		assertTrue("Wrong body: " + body,
				body.contains("This application has no explicit mapping for /error"));
	}

}
