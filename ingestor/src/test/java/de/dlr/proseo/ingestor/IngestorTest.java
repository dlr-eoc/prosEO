/*
 * IngestorTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ingestor;

import java.util.Arrays;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import de.dlr.proseo.ingestor.rest.model.IngestorProduct;

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
@SpringApplicationConfiguration(classes = Ingestor.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class IngestorTest {

	@Autowired
	private SecurityProperties security;

	@Value("${local.server.port}")
	private int port;

	@Test
	public void testHomeIsSecure() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port, Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("Wrong body: " + body, "Unauthorized", body.get("error"));
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
	public void testMetrics() throws Exception {
		// Makes sure a request has been made
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> dummyEntity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/products", List.class);
		assertEquals(HttpStatus.OK, dummyEntity.getStatusCode());

		// Now check request metrics
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/metrics", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertTrue("Wrong body: " + body, body.containsKey("counter.status.200.products"));
	}

	@Test
	public void testEnv() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/env", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertTrue("Wrong body: " + body, body.containsKey("systemProperties"));
	}

	@Test
	public void testHealth() throws Exception {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/health", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body: " + entity.getBody(),
				entity.getBody().contains("\"status\":\"UP\""));
	}

	@Test
	public void testErrorPage() throws Exception {
		ResponseEntity<String> entity = new TestRestTemplate("user", getPassword())
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
		ResponseEntity<String> entity = new TestRestTemplate("user", getPassword())
				.exchange("http://localhost:" + this.port + "/foo", HttpMethod.GET,
						request, String.class);
		assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
		String body = entity.getBody();
		assertNotNull("Body was null", body);
		assertTrue("Wrong body: " + body,
				body.contains("This application has no explicit mapping for /error"));
	}

	@Test
	public void testTrace() throws Exception {
		new TestRestTemplate().getForEntity("http://localhost:" + this.port + "/health",
				String.class);
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/trace", List.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = entity.getBody();
		Map<String, Object> trace = list.get(list.size() - 1);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) trace
				.get("info")).get("headers")).get("response");
		assertEquals("200", map.get("status"));
	}

	@Test
	public void testErrorPageDirectAccess() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/error", Map.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("None", body.get("error"));
		assertEquals(999, body.get("status"));
	}

	@Test
	public void testBeans() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/beans", List.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(1, entity.getBody().size());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) entity.getBody().get(0);
		assertTrue("Wrong body: " + body,
				((String) body.get("context")).startsWith("application"));
	}

	private String getPassword() {
		return this.security.getUser().getPassword();
	}

}
