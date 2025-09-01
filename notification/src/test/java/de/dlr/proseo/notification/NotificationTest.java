package de.dlr.proseo.notification;

import static org.junit.Assert.assertEquals;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.notification.rest.NotifyControllerImpl;
import de.dlr.proseo.notification.rest.model.RestMessage;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = NotificationService.class)
@WithMockUser(username = "UTM-testuser", password = "password")
@AutoConfigureTestEntityManager
public class NotificationTest {

	private static ProseoLogger logger = new ProseoLogger(NotificationTest.class);

	@Autowired
	private NotifyControllerImpl notifyController;

	/** Mocking the storage manager and planner */
	private static int WIREMOCK_PORT = 4050;
	@ClassRule
	public static WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

	/**
	 * Prepare the test environment
	 *
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		wireMockRule.start();

		wireMockRule
				.stubFor(WireMock.post(WireMock.urlEqualTo("/notify")).willReturn(WireMock.aResponse()
						.withStatus(HttpStatus.CREATED.value()).withHeader("Content-Type", "application/json").withBody("{{request.body}}")));
		wireMockRule
				.stubFor(WireMock.post(WireMock.urlEqualTo("/notifynotknown")).willReturn(WireMock.aResponse()
						.withStatus(HttpStatus.NOT_FOUND.value())));
	}

	/**
	 * Clean up the test environment
	 *
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		wireMockRule.stop();
	}


	@Test
	public final void testNotifiy() {
		logger.trace(">>> testNotifiy()");

		RestMessage restMessage = new RestMessage();
		restMessage.setEndpoint("http://localhost:" + WIREMOCK_PORT + "/notify");
		restMessage.setUser("UTM-testuser");
		restMessage.setPassword("password");
		restMessage.setSender("Test");
		restMessage.setContentType("application/json");
		restMessage.setRaw(true);
		restMessage.setMessage("{\n \"hallo\": \"welt\"\n }");
		ResponseEntity<?> response = notifyController.notifyx(restMessage);
		// Check that the deletion was successful
		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, response.getStatusCode());
		logger.trace(response.getBody().toString());

		restMessage.setEndpoint("http://localhost:" + WIREMOCK_PORT + "/notifynotknown");
		response = notifyController.notifyx(restMessage);
		assertEquals("Unexpected HTTP status code: ", HttpStatus.NOT_FOUND, response.getStatusCode());

	}
}
