package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_PRODUCTCLASSES_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.rest.model.RestConfiguredProcessor;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.rest.model.RestProcessingFacility;
import de.dlr.proseo.model.rest.model.RestProductClass;
import de.dlr.proseo.model.rest.model.RestSpacecraft;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;

public class GUIBaseController {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIBaseController.class);

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/** The GUI configuration */
	@Autowired
	private GUIConfiguration config;
	
	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;
		
	/**
	 * List with cached data
	 */
	private List<String> facilities = null;
	/**
	 * List with cached data
	 */
	private List<String> productclasses = null;
	/**
	 * List with cached data
	 */
	private List<String> configuredProcessors = null;
	/**
	 * List with cached data
	 */
	private List<String> fileClasses = null;
	/**
	 * List with cached data
	 */
	private List<String> processingModes = null;
	/**
	 * List with cached data
	 */
	private List<String> spaceCrafts = null;
	/**
	 * List with cached data
	 */
	private List<String> productiontypes = null;
	/**
	 * List with cached data
	 */
	private List<String> slicingtypes = null;
	/**
	 * List with cached data
	 */
	private List<String> parametertypes = null;
			
	public GUIBaseController() {
		// TODO Auto-generated constructor stub
	}

    /**
     * Retrieve the processing facilities
     * 
     * @return String list
     */
    @ModelAttribute("facilitynames")
    public List<String> facilities() {
    	checkClearCache();
    	if (facilities != null && !facilities.isEmpty()) return facilities;
    	
    	logger.trace("Get facilities");
    	facilities = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		
		try {
			resultList = serviceConnection.getFromService(config.getProductionPlanner(),
					"/processingfacilities", List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return facilities;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return facilities;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestProcessingFacility restFacility = mapper.convertValue(object, RestProcessingFacility.class);
				facilities.add(restFacility.getName());
			}
		}

		Comparator<String> c = Comparator.comparing((String x) -> x);
		facilities.sort(c);
        return facilities;
    }
    
    /**
     * Retrieve the product classes of mission
     * 
     * @return String list
     */
    @ModelAttribute("productclassnames")
    public List<String> productclasses() {
    	checkClearCache();
    	if (productclasses != null && !productclasses.isEmpty()) return productclasses;

    	logger.trace("Get productclasses");
    	productclasses = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
					"/productclasses?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PRODUCTCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return productclasses;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return productclasses;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestProductClass restProductClass = mapper.convertValue(object, RestProductClass.class);
				productclasses.add(restProductClass.getProductType());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		productclasses.sort(c);
        return productclasses;
    }

    /**
     * Retrieve the configured processors of mission
     * 
     * @return String list
     */
    @ModelAttribute("configuredprocessornames")
    public List<String> configuredProcessors() {
    	checkClearCache();
    	if (configuredProcessors != null && !configuredProcessors.isEmpty()) return configuredProcessors;

    	logger.trace("Get configuredprocessors");
    	configuredProcessors = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					"/configuredprocessors?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PRODUCTCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return configuredProcessors;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return configuredProcessors;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestConfiguredProcessor restConfiguredProcessorClass = mapper.convertValue(object, RestConfiguredProcessor.class);
				configuredProcessors.add(restConfiguredProcessorClass.getIdentifier());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		configuredProcessors.sort(c);
        return configuredProcessors;
    }
    
    /**
     * Retrieve the file classes of mission
     * 
     * @return String list
     */
    @ModelAttribute("fileclasses")
    public List<String> fileClasses() {
    	checkClearCache();
    	if (fileClasses != null && !fileClasses.isEmpty()) return fileClasses;

    	logger.trace("Get fileclasses");
    	fileClasses = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PRODUCTCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return fileClasses;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return fileClasses;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission =  mapper.convertValue(resultList.get(0), RestMission.class);
				fileClasses.addAll(restMission.getFileClasses());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		fileClasses.sort(c);
        return fileClasses;
    }
    
    /**
     * Retrieve the processing modes of mission
     * 
     * @return String list
     */
    @ModelAttribute("processingmodes")
    public List<String> processingModes() {
    	checkClearCache();
    	if (processingModes != null && !processingModes.isEmpty()) return processingModes;

    	logger.trace("Get processingmodes");
    	processingModes = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PRODUCTCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return processingModes;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return processingModes;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission =  mapper.convertValue(resultList.get(0), RestMission.class);
				processingModes.addAll(restMission.getProcessingModes());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		processingModes.sort(c);
        return processingModes;
    }

    /**
     * Retrieve the space crafts of mission
     * 
     * @return String list
     */
    @ModelAttribute("spacecrafts")
    public List<String> spaceCrafts() {
    	checkClearCache();
    	if (spaceCrafts != null && !spaceCrafts.isEmpty()) return spaceCrafts;

    	logger.trace("Get spacecrafts");
    	spaceCrafts = new ArrayList<String>();   
		List<?> resultList = null;
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PRODUCTCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return spaceCrafts;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return spaceCrafts;
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission =  mapper.convertValue(resultList.get(0), RestMission.class);
				for (RestSpacecraft spaceCraft : restMission.getSpacecrafts()) {
					spaceCrafts.add(spaceCraft.getCode());
				}
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		spaceCrafts.sort(c);
        return spaceCrafts;
    }

    /**
     * Retrieve the production type enum
     * 
     * @return String list
     */
    @ModelAttribute("productiontypes")
    public List<String> productiontypes() {
    	if (productiontypes != null && !productiontypes.isEmpty()) return productiontypes;
    	
    	productiontypes = new ArrayList<String>(); 
    	for (ProductionType value: ProductionType.values()) {
    		productiontypes.add(value.toString());
    	}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		productiontypes.sort(c);
        return productiontypes;
    }

    /**
     * Retrieve the slicing type enum
     * 
     * @return String list
     */
    @ModelAttribute("slicingtypes")
    public List<String> slicingtypes() {
    	if (slicingtypes != null && !slicingtypes.isEmpty()) return slicingtypes;
    	
    	slicingtypes = new ArrayList<String>(); 
    	for (OrderSlicingType value: OrderSlicingType.values()) {
    		slicingtypes.add(value.toString());
    	}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		slicingtypes.sort(c);
        return slicingtypes;
    }

    /**
     * Retrieve the parameter type enum
     * 
     * @return String list
     */
    @ModelAttribute("parametertypes")
    public List<String> parametertypes() {
    	if (parametertypes != null && !parametertypes.isEmpty()) return parametertypes;
    	
    	parametertypes = new ArrayList<String>(); 
    	for (ParameterType value: ParameterType.values()) {
    		parametertypes.add(value.toString());
    	}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		parametertypes.sort(c);
        return parametertypes;
    }
    

    public void checkClearCache() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
    	//TODO Handling of threads is not correct! Use ThreadLocal<T> or no caching at all (current workaround)
    	//if (auth.isNewLogin()) {
        if (auth.isNewLogin()) {
    		if (logger.isTraceEnabled())
    			logger.trace("Cache in GUIBaseController cleared");
    		clearCache();
    		auth.setNewLogin(false);
    	}
    }
    
	private void clearCache() {
		configuredProcessors = null;
		facilities = null;
		productclasses = null;
		spaceCrafts = null;
		fileClasses = null;
		processingModes = null;
	}
    /**
     * @return The mission code of the authenticated user
     */
    @ModelAttribute("missioncode")
    public String missioncode() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getMission();
    }
    
    /**
     * @return The authenticated user
     */
    @ModelAttribute("user")
    public String user() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
