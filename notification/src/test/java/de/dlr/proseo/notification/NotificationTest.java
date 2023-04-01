package de.dlr.proseo.notification;

import static org.junit.Assert.assertEquals;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.notification.rest.NotifyControllerImpl;
import de.dlr.proseo.interfaces.rest.model.RestMessage;


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
	private static WireMockServer wireMockServer;
	
	@Rule
	public WireMockRule wm = new WireMockRule(WireMockConfiguration.options()
	    .extensions(new ResponseTemplateTransformer(true))
	);

	/**
	 * Prepare the test environment
	 *
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		wireMockServer = new WireMockServer(
				WireMockConfiguration.options().extensions(new ResponseTemplateTransformer(true)).port(WIREMOCK_PORT));
		wireMockServer.start();

		wireMockServer
				.stubFor(WireMock.post(WireMock.urlEqualTo("/notify")).willReturn(WireMock.aResponse()
						.withStatus(HttpStatus.CREATED.value()).withHeader("Content-Type", "application/json").withBody("{{request.body}}")));
	}

	/**
	 * Clean up the test environment
	 *
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		wireMockServer.stop();
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
