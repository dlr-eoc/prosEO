package de.dlr.proseo.ui.gui;

import java.util.Map;

import javax.ws.rs.QueryParam;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.http.HttpMethodName;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.procmgr.rest.model.RestProcessorClass;

@Controller
public class ProcessorGUIController {

	@RequestMapping(value = "/proccesor-class-show/get")
	@ResponseBody
	public String getProcessorClassName(@QueryParam("mission") String mission, @QueryParam("processorclassname") String processorclassname, Model model) {
		RestTemplate restTemplate = new RestTemplate();
		String uri = "processorclasses?" + "mission=" + mission + "?" + "processorname=" + processorclassname;
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
		
		// Aus response.getBody() Daten rausholen und in model schreiben
		ObjectMapper mapper = new ObjectMapper();
		RestProcessorClass restProcessorClass = mapper.convertValue(response.getBody(), RestProcessorClass.class);
		
		model.addAttribute("id", restProcessorClass.getId());
		model.addAttribute("mission", restProcessorClass.getMissionCode());
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
