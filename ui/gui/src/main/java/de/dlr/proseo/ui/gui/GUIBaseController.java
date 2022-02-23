package de.dlr.proseo.ui.gui;

import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_EXCEPTION;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NOT_AUTHORIZED;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_MISSIONS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_PRODUCTCLASSES_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_PROCESSORCLASSES_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_CONFIGUREDPROCESSORS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_FILECLASSES_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_PROCESSINGMODES_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.MSG_ID_NO_SPACECRAFTS_FOUND;
import static de.dlr.proseo.ui.backend.UIMessages.uiMsg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.ProcessingLevel;
import de.dlr.proseo.model.enums.ProductQuality;
import de.dlr.proseo.model.enums.ProductVisibility;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.model.rest.model.RestConfiguredProcessor;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.rest.model.RestProcessingFacility;
import de.dlr.proseo.model.rest.model.RestProcessorClass;
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
	private List<String> productiontypes = null;
	/**
	 * List with cached data
	 */
	private List<String> productqualities = null;
	/**
	 * List with cached data
	 */
	private List<String> slicingtypes = null;
	/**
	 * List with cached data
	 */
	private List<String> parametertypes = null;
	/**
	 * List with cached data
	 */
	private List<String> processinglevels = null;
	/**
	 * List with cached data
	 */
	private List<String> visibilities = null;
			
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
    	if (!hasrolefacilityreader()) {
    		return new ArrayList<String>();
    	}
    	checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
    	if (auth.getDataCache().getFacilities() != null && !auth.getDataCache().getFacilities().isEmpty()) return auth.getDataCache().getFacilities();
    	
    	logger.trace("Get facilities");
    	auth.getDataCache().setFacilities(new ArrayList<String>());   
		List<?> resultList = null;
		
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
			return auth.getDataCache().getFacilities();
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return auth.getDataCache().getFacilities();
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestProcessingFacility restFacility = mapper.convertValue(object, RestProcessingFacility.class);
				auth.getDataCache().getFacilities().add(restFacility.getName());
			}
		}

		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getFacilities().sort(c);
        return auth.getDataCache().getFacilities();
    }
    
    /**
     * Retrieve the product classes of mission
     * 
     * @return String list
     */
    @ModelAttribute("productclassnames")
    public List<String> productclasses() {
    	if (!hasroleproductclassreader()) {
    		return new ArrayList<String>();
    	}
    	checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
    	if (auth.getDataCache().getProductclasses() != null && !auth.getDataCache().getProductclasses().isEmpty()) return auth.getDataCache().getProductclasses();

    	logger.trace("Get productclasses");
    	auth.getDataCache().setProductclasses(new ArrayList<String>());   
		List<?> resultList = null;
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
					"/productclasses/names?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
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
			return auth.getDataCache().getProductclasses();
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return auth.getDataCache().getProductclasses();
		}
		
		if (resultList != null) {
			for (Object object: resultList) {
				auth.getDataCache().getProductclasses().add((String)object);
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getProductclasses().sort(c);
        return auth.getDataCache().getProductclasses();
    }

    /**
     * Retrieve the product classes of mission
     * 
     * @return String list
     */
    @ModelAttribute("processorclassnames")
    public List<String> processorclassnames() {
    	if (!hasroleprocessorreader()) {
    		return new ArrayList<String>();
    	}
    	checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
    	if (auth.getDataCache().getProcessorclasses() != null && !auth.getDataCache().getProcessorclasses().isEmpty()) return auth.getDataCache().getProcessorclasses();

    	logger.trace("Get processorclasses");
    	auth.getDataCache().setProcessorclasses(new ArrayList<String>());   
		List<?> resultList = null;
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					"/processorclasses?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PROCESSORCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return auth.getDataCache().getProcessorclasses();
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return auth.getDataCache().getProcessorclasses();
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestProcessorClass restProcessorClass = mapper.convertValue(object, RestProcessorClass.class);
				auth.getDataCache().getProcessorclasses().add(restProcessorClass.getProcessorName());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getProcessorclasses().sort(c);
        return auth.getDataCache().getProcessorclasses();
    }

    /**
     * Retrieve the configured processors of mission
     * 
     * @return String list
     */
    @ModelAttribute("configuredprocessornames")
    public List<String> configuredProcessors() {
    	if (!hasroleprocessorreader()) {
    		return new ArrayList<String>();
    	}
    	checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
    	if (auth.getDataCache().getConfiguredProcessors() != null && !auth.getDataCache().getConfiguredProcessors().isEmpty()) return auth.getDataCache().getConfiguredProcessors();

    	logger.trace("Get configuredprocessors");
    	auth.getDataCache().setConfiguredProcessors(new ArrayList<String>());   
		List<?> resultList = null;
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					"/configuredprocessors?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_CONFIGUREDPROCESSORS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return auth.getDataCache().getConfiguredProcessors();
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return auth.getDataCache().getConfiguredProcessors();
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object: resultList) {
				RestConfiguredProcessor restConfiguredProcessorClass = mapper.convertValue(object, RestConfiguredProcessor.class);
				auth.getDataCache().getConfiguredProcessors().add(restConfiguredProcessorClass.getIdentifier());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getConfiguredProcessors().sort(c);
        return auth.getDataCache().getConfiguredProcessors();
    }
    
    /**
     * Retrieve the file classes of mission
     * 
     * @return String list
     */
    @ModelAttribute("fileclasses")
    public List<String> fileClasses() {
    	checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
    	if (auth.getDataCache().getFileClasses() != null && !auth.getDataCache().getFileClasses().isEmpty()) return auth.getDataCache().getFileClasses();

    	logger.trace("Get fileclasses");
    	auth.getDataCache().setFileClasses(new ArrayList<String>());   
		List<?> resultList = null;
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_FILECLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return auth.getDataCache().getFileClasses();
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return auth.getDataCache().getFileClasses();
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission =  mapper.convertValue(resultList.get(0), RestMission.class);
				auth.getDataCache().getFileClasses().addAll(restMission.getFileClasses());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getFileClasses().sort(c);
        return auth.getDataCache().getFileClasses();
    }
    
    /**
     * Retrieve the processing modes of mission
     * 
     * @return String list
     */
    @ModelAttribute("processingmodes")
    public List<String> processingModes() {
    	if (!hasroleorderreader()) {
    		return new ArrayList<String>();
    	}
    	checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
    	if (auth.getDataCache().getProcessingModes() != null && !auth.getDataCache().getProcessingModes().isEmpty()) return auth.getDataCache().getProcessingModes();

    	logger.trace("Get processingmodes");
    	auth.getDataCache().setProcessingModes(new ArrayList<String>());   
		List<?> resultList = null;
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PROCESSINGMODES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return auth.getDataCache().getProcessingModes();
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return auth.getDataCache().getProcessingModes();
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission =  mapper.convertValue(resultList.get(0), RestMission.class);
				auth.getDataCache().getProcessingModes().addAll(restMission.getProcessingModes());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getProcessingModes().sort(c);
        return auth.getDataCache().getProcessingModes();
    }

    /**
     * Retrieve the space crafts of mission
     * 
     * @return String list
     */
    @ModelAttribute("spacecrafts")
    public List<String> spaceCrafts() {
    	if (!hasrolemissionreader()) {
    		return new ArrayList<String>();
    	}
    	checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
    	if (auth.getDataCache().getSpaceCrafts() != null && !auth.getDataCache().getSpaceCrafts().isEmpty()) return auth.getDataCache().getSpaceCrafts();

    	logger.trace("Get spacecrafts");
    	auth.getDataCache().setSpaceCrafts(new ArrayList<String>());   
		List<?> resultList = null;
		
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_SPACECRAFTS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return auth.getDataCache().getSpaceCrafts();
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return auth.getDataCache().getSpaceCrafts();
		}
		
		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission =  mapper.convertValue(resultList.get(0), RestMission.class);
				for (RestSpacecraft spaceCraft : restMission.getSpacecrafts()) {
					auth.getDataCache().getSpaceCrafts().add(spaceCraft.getCode());
				}
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getSpaceCrafts().sort(c);
        return auth.getDataCache().getSpaceCrafts();
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
     * Retrieve the processing level enum
     * 
     * @return String list
     */
    @ModelAttribute("processinglevels")
    public List<String> processinglevels() {
    	if (processinglevels != null && !processinglevels.isEmpty()) return processinglevels;
    	
    	processinglevels = new ArrayList<String>(); 
    	for (ProcessingLevel value: ProcessingLevel.values()) {
    		processinglevels.add(value.toString());
    	}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		processinglevels.sort(c);
        return processinglevels;
    }
    
    /**
     * Retrieve the product visibility enum
     * 
     * @return String list
     */
    @ModelAttribute("visibilities")
    public List<String> visibilities() {
    	if (visibilities != null && !visibilities.isEmpty()) return visibilities;
    	
    	visibilities = new ArrayList<String>(); 
    	for (ProductVisibility value: ProductVisibility.values()) {
    		visibilities.add(value.toString());
    	}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		visibilities.sort(c);
        return visibilities;
    }

    /**
     * Retrieve the production type enum
     * 
     * @return String list
     */
    @ModelAttribute("productqualities")
    public List<String> productqualities() {
    	if (productqualities != null && !productqualities.isEmpty()) return productqualities;
    	
    	productqualities = new ArrayList<String>(); 
    	for (ProductQuality value: ProductQuality.values()) {
    		productqualities.add(value.toString());
    	}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		productqualities.sort(c);
        return productqualities;
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
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
		auth.getDataCache().clear();
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
    
    /**
     * @return The roles of authenticated user
     */
    @ModelAttribute("userroles")
    public List<String> userroles() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles();
    }
	
	/** Read access to missions, spacecrafts and orbits */
    @ModelAttribute("hasrolemissionreader")
    public Boolean hasrolemissionreader() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.MISSION_READER.toString());
    }

	/** Read and update access to missions, spacecrafts and orbits */
    @ModelAttribute("hasrolemissionmgr")
    public Boolean hasrolemissionmgr() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.MISSION_MGR.toString());
    }
	
	/** Read access to product classes and selection rules */
    @ModelAttribute("hasroleproductclassreader")
    public Boolean hasroleproductclassreader() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.PRODUCTCLASS_READER.toString());
    }
    
	/** Create, update and delete access to product classes and selection rules */
    @ModelAttribute("hasroleproductclassmgr")
    public Boolean hasroleproductclassmgr() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.PRODUCTCLASS_MGR.toString());
    }
	
	/** Query and download public products */
    @ModelAttribute("hasroleproductreader")
    public Boolean hasroleproductreader() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.PRODUCT_READER.toString()) | auth.getUserRoles().contains(UserRole.PRODUCT_READER_ALL.toString())
        		 | auth.getUserRoles().contains(UserRole.PRODUCT_READER_RESTRICTED.toString());
    }
	
	/** Query and download public and restricted products */
    @ModelAttribute("hasroleproductrestrictedreader")
    public Boolean hasroleproductrestrictedreader() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.PRODUCT_READER_RESTRICTED.toString());
    }

	/** Query and download all products */
    @ModelAttribute("hasroleproductreaderall")
    public Boolean hasroleproductreaderall() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.PRODUCT_READER_ALL.toString());
    }
	
	/** Upload products from external source */
	// PRODUCT_INGESTOR,
	
	/** Upload products from internal source */
	// PRODUCT_GENERATOR,
	
	/** Update and delete products and product files */
    @ModelAttribute("hasroleprodutcmgr")
    public Boolean hasroleproductmgr() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.PRODUCT_MGR.toString());
    }	
	
	// Processor management roles
	
	/** Read access to processor classes, processors, configurations, configured processors and any sub-objects of them */
    @ModelAttribute("hasroleprocessorreader")
    public Boolean hasroleprocessorreader() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.PROCESSOR_READER.toString());
    }	
	
	/** Create, update and delete access to processor classes, processors and tasks */
    @ModelAttribute("hasroleprocessorclassmgr")
    public Boolean hasroleprocessorclassmgr() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.PROCESSORCLASS_MGR.toString());
    }	
	
	/** Read access to configurations and configured processors */
    @ModelAttribute("hasroleconfigurationreader")
    public Boolean hasroleconfigurationreader() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.PROCESSOR_READER.toString());
    }	
    
	/** Create, update and delete access to configurations and configured processors */
    @ModelAttribute("hasroleconfigurationmgr")
    public Boolean hasroleconfigurationmgr() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.CONFIGURATION_MGR.toString());
    }	
	
	// Processing facility management roles
	
	/** Read access to processing facilities */
    @ModelAttribute("hasrolefacilityreader")
    public Boolean hasrolefacilityreader() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.FACILITY_READER.toString());
    }	
	
	/** Create, update and delete access to processing facilities */
    @ModelAttribute("hasrolefacilitymgr")
    public Boolean hasrolefacilitymgr() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.FACILITY_MGR.toString());
    }	
    
	/** Read access to facility monitoring data */
    @ModelAttribute("hasrolefacilitymonitor")
    public Boolean hasrolefacilitymonitor() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.FACILITY_MONITOR.toString());
    }	
	
	/** Read access to processing order, jobs and job steps */
    @ModelAttribute("hasroleorderreader")
    public Boolean hasroleorderreader() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.ORDER_READER.toString());
    }	
	
	/** Create, update, close and delete orders */
    @ModelAttribute("hasroleordermgr")
    public Boolean hasroleordermgr() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.ORDER_MGR.toString()) ;
    }	
	
	/** Approve orders */
    @ModelAttribute("hasroleorderapprover")
    public Boolean hasroleorderapprover() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.ORDER_APPROVER.toString());
    }	
	
	/** Plan, release, suspend, cancel and retry orders, jobs and job steps */
    @ModelAttribute("hasroleorderplanner")
    public Boolean hasroleorderplanner() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.ORDER_PLANNER.toString());
    }	
	
	/** Read access to order monitoring data */
    @ModelAttribute("hasroleordermonitor")
    public Boolean hasroleordermonitor() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.ORDER_MONITOR.toString());
    }	
	
	/** Notify of job step completion */
    @ModelAttribute("hasrolejobstepprocessor")
    public Boolean hasrolejobstepprocessor() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getUserRoles().contains(UserRole.JOBSTEP_PROCESSOR.toString());
    }	
    
    protected void handleHTTPError(ClientResponse clientResponse, Model model) {
		if (clientResponse.statusCode().compareTo(HttpStatus.NOT_FOUND) == 0) {
			logger.trace(">>>Client error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("errormsg", "No elements found");
		} else if (clientResponse.statusCode().is5xxServerError()) {
			logger.trace(">>>Server error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("errormsg", "Server error " + clientResponse.statusCode().toString() + ": " + clientResponse.statusCode().getReasonPhrase());
		} else if (clientResponse.statusCode().is4xxClientError()) {
			logger.trace(">>>Client error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("errormsg", "Client error " + clientResponse.statusCode().toString() + ": " + clientResponse.statusCode().getReasonPhrase());
		} else {
			// do nothing
		}
    }
    
    protected void handleHTTPWarning(ClientResponse clientResponse, Model model, HttpServletResponse httpResponse) {
		if (clientResponse.statusCode().is5xxServerError()) {
			logger.trace(">>>Server error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("warnmsg", "Server error " + clientResponse.statusCode().toString() + ": " + clientResponse.statusCode().getReasonPhrase());
			List<String> descList = clientResponse.headers().header("Warning");
			String desc = "";
			for (String d : descList) {
				desc += d + " ";
			}
			model.addAttribute("warndesc", desc);
			model.addAttribute("warnstatus", clientResponse.statusCode().toString());
			httpResponse.setHeader("warnstatus", (String) model.asMap().get("warnstatus"));
			httpResponse.setHeader("warnmsg", (String) model.asMap().get("warnmsg"));
			httpResponse.setHeader("warndesc", (String) model.asMap().get("warndesc"));
		} else if (clientResponse.statusCode().is4xxClientError()) {
			logger.trace(">>>Client error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("warnmsg", "Client error " + clientResponse.statusCode().toString() + ": " + clientResponse.statusCode().getReasonPhrase());
			List<String> descList = clientResponse.headers().header("Warning");
			String desc = "";
			for (String d : descList) {
				desc += d + " ";
			}
			model.addAttribute("warndesc", desc);model.addAttribute("warnstatus", clientResponse.statusCode().toString());
			httpResponse.setHeader("warnstatus", (String) model.asMap().get("warnstatus"));
			httpResponse.setHeader("warnmsg", (String) model.asMap().get("warnmsg"));
			httpResponse.setHeader("warndesc", (String) model.asMap().get("warndesc"));
		} else {
			// do nothing
		}
    }
}
