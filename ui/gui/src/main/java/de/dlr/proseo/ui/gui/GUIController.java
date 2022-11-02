package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.RestClientResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.ui.backend.ServiceConnection;

@Controller

public class GUIController {
	private static ProseoLogger logger = new ProseoLogger(GUIController.class);
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;
	
	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
		
	@GetMapping("/test")
    public String index1() {
        return "Greetings from Spring Boot!";
        
    }

    @GetMapping("/customlogin") 
    public String index12(Model model) {
    	return "customlogin.html";

    }
    /**
     * Retrieve the defined missions.
     * 
     * @return List of missions
     */
    @ModelAttribute("missions")
    public List<String> missioncodes() {
    	List<String> missions = new ArrayList<String>();   
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(config.getOrderManager(),
					"/missions", List.class, null, null);
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = ProseoLogger.format(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return missions;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return missions;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestMission restMission = mapper.convertValue(object, RestMission.class);
				missions.add(restMission.getCode());
			}
		}

		Comparator<String> c = Comparator.comparing((String x) -> x);
		missions.sort(c);
        return missions;
    }
    
}
