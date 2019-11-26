package de.dlr.proseo.ui.gui;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.QueryParam;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.rest.model.RestProcessorClass;


@Controller
public class ProcessorGUIController {
	
	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;

	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorGUIController.class);
	
	@RequestMapping(value = "/processor-class-show/get")
	public String getProcessorClassName(@RequestParam("missioncode") String mission, @RequestParam("processorclassnamecode") String processorclassname, Model model) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassName({}, {}, {})", mission, processorclassname, model.toString());

		RestTemplate restTemplate = rtb.basicAuthentication("s5p-proseo", "sieb37.Schlaefer").build();
		//application.yml wurde nicht übernommen, base uri manuell übernommen config.getProcessorManager()
//		String uri = "http://localhost:8090/castlemock/mock/rest/project/eW2ujU/application/79M2j9" + "/processorclasses?mission=" + mission + "&processorname=" + processorclassname;
		String uri = "http://proseo-registry.eoc.dlr.de/proseo/processor-mgr/v0.1/processorclasses?mission=" + mission + "&processorname=" + processorclassname;
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> response = restTemplate.getForEntity(uri, List.class);
		
		// HTTP Status 404 abfangen
		
		// Rest muss einen Loop in eine Tabelle ergeben
		
		// Aus response.getBody() Daten rausholen und in model schreiben
		ObjectMapper mapper = new ObjectMapper();
		RestProcessorClass restProcessorClass = mapper.convertValue(response.getBody().iterator().next(), RestProcessorClass.class);
		
		if (logger.isDebugEnabled()) logger.debug("Received response: {}", response.getBody());
		
		model.addAttribute("id", restProcessorClass.getId());
		model.addAttribute("mission", restProcessorClass.getMissionCode());
		model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
		model.addAttribute("version", restProcessorClass.getVersion());
		model.addAttribute("productclasses", restProcessorClass.getProductClasses());
		model.addAttribute("all", restProcessorClass);
		//....
		
		return "processor-class-show";
	}

	@RequestMapping(value = "/processor-class-create/post")
	public String postProcessorClass(@QueryParam("processorClassId") String processorClassId,
									 @QueryParam("processorClassVersion") String processorClassVersion, 
									 @QueryParam("processorClassmissioncode") String processorClassmission, 
									 @QueryParam("processorClassname") String processorClassName,
									 @QueryParam("processorClassProductClass") String processorClassProductClass,
									 Model model) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassName({}, {}, {})", processorClassmission, processorClassName, processorClassId, processorClassVersion, processorClassmission, model.toString());

		RestTemplate restTemplate = new RestTemplate();
		//application.yml wurde nicht übernommen, base uri manuell übernommen config.getProcessorManager()
		String uri = "http://localhost:8090/castlemock/mock/rest/project/eW2ujU/application/79M2j9" 
		+ "/processorclasses?mission=" 
		+ processorClassmission 
		+ "&processorname=" 
		+ processorClassName
		+ "&id="
		+ processorClassId
		+ "&version="
		+ processorClassVersion
		+ "&productClass"
		+ processorClassProductClass;
		
		
		;
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = restTemplate.postForEntity(uri, null, Map.class);
		// Aus response.getBody() Daten rausholen und in model schreiben
		ObjectMapper mapper = new ObjectMapper();
		RestProcessorClass restProcessorClass = mapper.convertValue(response.getBody(), RestProcessorClass.class);
		
		if (logger.isDebugEnabled()) logger.debug("Received response: {}", response.getBody());
		
		model.addAttribute("id", restProcessorClass.getId());
		model.addAttribute("mission", restProcessorClass.getMissionCode());
		model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
		model.addAttribute("version", restProcessorClass.getVersion());
		model.addAttribute("productclasses", restProcessorClass.getProductClasses());
		model.addAttribute("all", restProcessorClass);
		//....
		
		return "processor-class-create";
	}
	@RequestMapping(value = "/processor-class-show-id/get")
	public String getProcessorClassName(@QueryParam("processorClassId") String processorClassId, Model model) {
		if (logger.isTraceEnabled()) logger.trace(">>> postProcessorClassName({}, {}, {})", processorClassId, model.toString());

		RestTemplate restTemplate = new RestTemplate();
		//application.yml wurde nicht übernommen, base uri manuell übernommen config.getProcessorManager()
		String uri = "http://localhost:8090/castlemock/mock/rest/project/eW2ujU/application/79M2j9" + "/processorclasses?id=" + processorClassId;
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
		
		// Aus response.getBody() Daten rausholen und in model schreiben
		ObjectMapper mapper = new ObjectMapper();
		RestProcessorClass restProcessorClass = mapper.convertValue(response.getBody(), RestProcessorClass.class);
		
		if (logger.isDebugEnabled()) logger.debug("Received response: {}", response.getBody());
		
		model.addAttribute("id", restProcessorClass.getId());
		model.addAttribute("mission", restProcessorClass.getMissionCode());
		model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
		model.addAttribute("version", restProcessorClass.getVersion());
		model.addAttribute("productclasses", restProcessorClass.getProductClasses());
		model.addAttribute("all", restProcessorClass);
		//....
		
		return "processor-class-show-id";
	}
	@RequestMapping(value = "/processor-class-update/patch")
	public String patchProcessorClass(@QueryParam("processorClassId") String processorClassId,
									 @QueryParam("processorClassVersion") String processorClassVersion, 
									 @QueryParam("processorClassmissioncode") String processorClassmission, 
									 @QueryParam("processorClassname") String processorClassName,
									 @QueryParam("processorClassProductClass") String processorClassProductClass,
									 Model model) {
		if (logger.isTraceEnabled()) logger.trace(">>> patchProcessorClassName({}, {}, {})", processorClassmission, processorClassName, processorClassId, processorClassVersion, processorClassmission, model.toString());

		RestTemplate restTemplate = new RestTemplate();
		//application.yml wurde nicht übernommen, base uri manuell übernommen config.getProcessorManager()
		String uri = "http://localhost:8090/castlemock/mock/rest/project/eW2ujU/application/79M2j9" 
		+ "/processorclasses?id="+ processorClassId + "&mission=" 
		+ processorClassmission 
		+ "&processorname=" 
		+ processorClassName
		+ "&version="
		+ processorClassVersion
		+ "&productClass="
		+ processorClassProductClass;
		
		
		
		HttpClient httpclient = HttpClientBuilder.create().build();
		HttpUriRequest req = new HttpPatch(uri);
		try {
			ObjectMapper mapper = new ObjectMapper();
			//mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
			HttpResponse response =  httpclient.execute(req);
			RestProcessorClass restProcessorClass = mapper.readValue(response.getEntity().getContent(), RestProcessorClass.class);
			
			model.addAttribute("id", restProcessorClass.getId());
			model.addAttribute("mission", restProcessorClass.getMissionCode());
			model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
			model.addAttribute("version", restProcessorClass.getVersion());
			model.addAttribute("productclasses", restProcessorClass.getProductClasses());
			model.addAttribute("all", restProcessorClass);
			
		} catch (ClientProtocolException e) {
			model.addAttribute("client", e.getMessage());
		} catch (IOException e) {
			model.addAttribute("IO", e.getMessage());
		}
		// Aus response.getBody() Daten rausholen und in model schreiben
		
		
		//if (logger.isDebugEnabled()) logger.debug("Received response: {}", response.getBody());
		
		
		//....
		
		return "processor-class-update";
	}
	@RequestMapping(value = "/processor-class-delete/id")
	public String deleteProcessorClassById(@QueryParam("processorClassId") String processorClassId, Model model) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProcessorClassName({}, {}, {})", processorClassId, model.toString());

		RestTemplate restTemplate = new RestTemplate();
		//application.yml wurde nicht übernommen, base uri manuell übernommen config.getProcessorManager()
		String uri = "http://localhost:8090/castlemock/mock/rest/project/eW2ujU/application/79M2j9" + "/processorclasses?id=" + processorClassId;
		
			
		
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, String.class);
		model.addAttribute("status", response.getStatusCodeValue());
		// Aus response.getBody() Daten rausholen und in model schreiben
		ObjectMapper mapper = new ObjectMapper();
	//	RestProcessorClass restProcessorClass = mapper.convertValue(response.getBody(), RestProcessorClass.class);
		
	//	if (logger.isDebugEnabled()) logger.debug("Received response: {}", response.getBody());
		
//		model.addAttribute("id", restProcessorClass.getId());
//		model.addAttribute("mission", restProcessorClass.getMissionCode());
//		model.addAttribute("processorclassname", restProcessorClass.getProcessorName());
//		model.addAttribute("version", restProcessorClass.getVersion());
//		model.addAttribute("productclasses", restProcessorClass.getProductClasses());
//		model.addAttribute("all", restProcessorClass);
//		model.addAttribute("message", "ERFOLGREICH");
//		//....
		
		return "processor-class-delete";
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
