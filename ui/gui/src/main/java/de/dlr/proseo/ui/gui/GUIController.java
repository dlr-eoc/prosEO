/**
 * GUIController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.ui.backend.ServiceConnection;

/**
 * A bridge between the GUI application and the prosEO backend services, used for testing purposes, displaying custom login pages,
 * and retrieving missions
 *
 * @author David Mazo
 */
@Controller
public class GUIController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIController.class);

	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/**
	 * Show the test view
	 *
	 * @return the name of the test view template
	 */
	@GetMapping("/test")
	public String index1() {
		return "Greetings from Spring Boot!";
	}

	/**
	 * Show the custom login view
	 *
	 * @param model the attributes to return
	 * @return the name of the custom login view template
	 */
	@GetMapping("/customlogin")
	public String index12(Model model) {
		String version = null;
		String resource = "META-INF/maven/de.dlr.proseo/proseo-ui-gui/pom.properties";

		try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
			if (stream != null) {
				Properties props = new Properties();
				props.load(stream);
				version = props.getProperty("version");
			} else {
				logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, "Failed to load properties file: " + resource);
			}
		} catch (IOException e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
		}

		model.addAttribute("proseoversion", version);

		return "customlogin.html";
	}

	/**
	 * Retrieve the defined missions.
	 *
	 * @return List of missions
	 */
	@ModelAttribute("missions")
	public List<String> missioncodes() {
		List<String> missions = new ArrayList<>();
		List<?> resultList = null;
		try {
			// Retrieve the list of missions from the service using the Order Manager URL
			resultList = serviceConnection.getFromService(config.getOrderManager(), "/missions", List.class, null, null);
		} catch (RestClientResponseException e) {

			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				// Handle the case when no missions are found
				logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				// Handle the case when the user is not authorized to access missions
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				// Handle other exceptions and log the error message
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			return missions;
		} catch (RuntimeException e) {
			// Handle runtime exceptions and log the error message
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return missions;
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			// Convert each object in the result list to a RestMission object
			for (Object object : resultList) {
				RestMission restMission = mapper.convertValue(object, RestMission.class);
				// Add the mission code to the missions list
				missions.add(restMission.getCode());
			}
		}

		Comparator<String> c = Comparator.comparing((String x) -> x);
		// Sort the missions list in ascending order
		missions.sort(c);
		return missions;
	}

}
