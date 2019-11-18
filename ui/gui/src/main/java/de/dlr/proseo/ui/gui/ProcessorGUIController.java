package de.dlr.proseo.ui.gui;

import java.util.Map;

import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.rest.model.RestProcessorClass;

@Controller
public class ProcessorGUIController {
	
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorGUIController.class);
	
	@RequestMapping(value = "/processor-class-show/get")
	public String getProcessorClassName(@QueryParam("mission") String mission, @QueryParam("processorclassname") String processorclassname, Model model) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassName({}, {}, {})", mission, processorclassname, model.toString());

		RestTemplate restTemplate = new RestTemplate();
		String uri = config.getProcessorManager() + "/processorclasses?mission=" + mission + "&processorname=" + processorclassname;
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
		
		// Aus response.getBody() Daten rausholen und in model schreiben
		ObjectMapper mapper = new ObjectMapper();
		RestProcessorClass restProcessorClass = mapper.convertValue(response.getBody(), RestProcessorClass.class);
		
		if (logger.isDebugEnabled()) logger.debug("Received response: {}", response.getBody());
		
		model.addAttribute("id", restProcessorClass.getId());
		model.addAttribute("mission", restProcessorClass.getMissionCode());
		model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
		//....
		
		return "processor-class-show";
	}

	@RequestMapping(value = "/processor-class-create/post")
	public String postProcessor() {
		RestTemplate restTemplate = new RestTemplate();
		String uri = "processorclasses/post?";
		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		return response.toString();
	}

	@RequestMapping(value = "/processor-class-show/{id}?get")
	public String getProcessorClassById(@PathVariable long id) {
		RestTemplate restTemplate = new RestTemplate();
		String uri = "processorclasses/" + id + "?get";
		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		return response.toString();
	}
	@RequestMapping(value = "/processor-class-update/{id}?patch")
	public String patchProcessorClassById(@PathVariable long id) {
		RestTemplate restTemplate = new RestTemplate();
		String uri = "processorclasses/" + id + "?patch";
		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		return response.toString();
}
	@RequestMapping(value = "/processor-class-delete/{id}?delete")
	public String deleteProcessorClassById(@PathVariable long id) {
		RestTemplate restTemplate = new RestTemplate();
		String uri = "processorclasses/" + id + "?delete";
		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		return response.toString();
}
	
}
