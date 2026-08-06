package de.dlr.proseo.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import com.github.tomakehurst.wiremock.WireMockServer;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.notification.rest.NotifyControllerImpl;
import de.dlr.proseo.notification.rest.model.RestMessage;


@SpringBootTest(classes = NotificationService.class)
@WithMockUser(username = "UTM-testuser", password = "password")
public class NotificationTest {

	private static ProseoLogger logger = new ProseoLogger(NotificationTest.class);

	@Autowired
	private NotifyControllerImpl notifyController;

	/** Mocking the storage manager and planner */
	private static int WIREMOCK_PORT = 4050;

    WireMockServer wireMockServer;

    @BeforeEach
    public void setup () {
        wireMockServer = new WireMockServer(wireMockConfig().port(WIREMOCK_PORT));
        wireMockServer.start();
        setupStub();

    }


    @AfterEach
    public void teardown () {
        wireMockServer.stop();
    }

	/**
	 * Prepare the test environment
	 *
	 * @throws java.lang.Exception
	 */

	public void setupStub() {

		wireMockServer
				.stubFor(post(urlEqualTo("/notify")).willReturn(aResponse()
						.withStatus(HttpStatus.CREATED.value()).withHeader("Content-Type", "application/json").withBody("{{request.body}}")));
		wireMockServer
				.stubFor(post(urlEqualTo("/notifynotknown")).willReturn(aResponse()
						.withStatus(HttpStatus.NOT_FOUND.value())));
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
		assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Unexpected HTTP status code: ");
		logger.trace(response.getBody().toString());

		restMessage.setEndpoint("http://localhost:" + WIREMOCK_PORT + "/notifynotknown");
		response = notifyController.notifyx(restMessage);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Unexpected HTTP status code: ");

	}
}
