/**
 * BaseController.java
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

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.ClientResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
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
import de.dlr.proseo.model.rest.model.RestProductArchive;
import de.dlr.proseo.model.rest.model.RestSpacecraft;
import de.dlr.proseo.model.rest.model.RestWorkflow;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;

/** A base controller for the prosEO GUI, to be extended for specific entities */
public class GUIBaseController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIBaseController.class);

	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;

	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;

	/** List with cached production types */
	private List<String> productiontypes = null;

	/** List with cached product qualities */
	private List<String> productqualities = null;

	/** List with cached slicing types */
	private List<String> slicingtypes = null;

	/** List with cached parameter types */
	private List<String> parametertypes = null;

	/** List with cached processing levels */
	private List<String> processinglevels = null;

	/** List with cached visibility levels */
	private List<String> visibilities = null;

	/** The prosEO version */
	private String version = null;

	/** A no-argument constructor */
	public GUIBaseController() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Gets the current version of prosEO
	 *
	 * @return the prosEO version
	 */
	@ModelAttribute("proseoversion")
	public String proseoversion() {
		if (version == null) {
			String res = "META-INF/maven/de.dlr.proseo/proseo-ui-gui/pom.properties";
			Properties props = new Properties();

			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			InputStream stream = loader.getResourceAsStream(res);
			try {
				props.load(stream);
				version = props.getProperty("version");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return version;
	}

	/**
	 * Retrieve the processing facilities
	 *
	 * @return a list with the names of available processing facilities
	 */
	@ModelAttribute("facilitynames")
	public List<String> facilities() {
		if (!hasrolefacilityreader()) {
			return new ArrayList<>();
		}
		checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		if (auth.getDataCache().getFacilities() != null && !auth.getDataCache().getFacilities().isEmpty())
			return auth.getDataCache().getFacilities();

		logger.trace("Get facilities");
		auth.getDataCache().setFacilities(new ArrayList<String>());
		List<?> resultList = null;

		try {
			resultList = serviceConnection.getFromService(serviceConfig.getFacilityManagerUrl(), "/facilities", List.class,
					auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return auth.getDataCache().getFacilities();
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getFacilities();
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object : resultList) {
				RestProcessingFacility restFacility = mapper.convertValue(object, RestProcessingFacility.class);
				auth.getDataCache().getFacilities().add(restFacility.getName());
			}
		}

		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getFacilities().sort(c);
		return auth.getDataCache().getFacilities();
	}

	/**
	 * Retrieve the enabled processing facilities 
	 *
	 * @return a list with the names of available processing facilities
	 */
	@ModelAttribute("enabledfacilitynames")
	public List<String> enabledfacilities() {
		if (!hasrolefacilityreader()) {
			return new ArrayList<>();
		}
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

		logger.trace("Get facilities");
		List<String> enabledFacilities = new ArrayList<String>();
		List<?> resultList = null;

		try {
			resultList = serviceConnection.getFromService(serviceConfig.getFacilityManagerUrl(), "/facilities", List.class,
					auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return enabledFacilities;
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getFacilities();
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object : resultList) {
				RestProcessingFacility restFacility = mapper.convertValue(object, RestProcessingFacility.class);
				if (!restFacility.getFacilityState().equals("DISABLED")) {
					enabledFacilities.add(restFacility.getName());
				}
			}
		}

		Comparator<String> c = Comparator.comparing((String x) -> x);
		enabledFacilities.sort(c);
		return enabledFacilities;
	}
	
	/**
	 * Retrieve the product archives
	 *
	 * @return a list with the names of available product archives
	 */
	@ModelAttribute("productarchivenames")
	public List<String> productarchives() {
		if (!hasroleproductarchivereader()) {
			return new ArrayList<>();
		}
		checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		if (auth.getDataCache().getProductArchives() != null && !auth.getDataCache().getProductArchives().isEmpty())
			return auth.getDataCache().getProductArchives();

		logger.trace("Get product archives");
		auth.getDataCache().setProductarchives(new ArrayList<String>());
		List<?> resultList = null;

		try {     
			resultList = serviceConnection.getFromService(serviceConfig.getArchiveManagerUrl(), "/archives", List.class,
					auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_ARCHIVES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return auth.getDataCache().getProductArchives();
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getProductArchives();
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object : resultList) {
				RestProductArchive restArchive = mapper.convertValue(object, RestProductArchive.class);
				auth.getDataCache().getProductArchives().add(restArchive.getName());
			}
		}

		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getProductArchives().sort(c);
		return auth.getDataCache().getProductArchives();
	}


	/**
	 * Retrieve the product classes of mission
	 *
	 * @return A list of product class names
	 */
	@ModelAttribute("productclassnames")
	public List<String> productclasses() {
		if (!hasroleproductclassreader()) {
			return new ArrayList<>();
		}
		checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		if (auth.getDataCache().getProductclasses() != null && !auth.getDataCache().getProductclasses().isEmpty())
			return auth.getDataCache().getProductclasses();

		logger.trace("Get productclasses");
		auth.getDataCache().setProductclasses(new ArrayList<String>());
		List<?> resultList = null;

		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
					"/productclasses/names?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_PRODUCTCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return auth.getDataCache().getProductclasses();
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getProductclasses();
		}

		if (resultList != null) {
			for (Object object : resultList) {
				auth.getDataCache().getProductclasses().add((String) object);
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getProductclasses().sort(c);
		return auth.getDataCache().getProductclasses();
	}

	/**
	 * Retrieve the product classes of mission
	 *
	 * @return A list of parameter types in String format
	 */
	@ModelAttribute("processorclassnames")
	public List<String> processorclassnames() {
		if (!hasroleprocessorreader()) {
			return new ArrayList<>();
		}
		checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		if (auth.getDataCache().getProcessorclasses() != null && !auth.getDataCache().getProcessorclasses().isEmpty())
			return auth.getDataCache().getProcessorclasses();

		logger.trace("Get processorclasses");
		auth.getDataCache().setProcessorclasses(new ArrayList<String>());
		List<?> resultList = null;

		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					"/processorclasses?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_PROCESSORCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return auth.getDataCache().getProcessorclasses();
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getProcessorclasses();
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object : resultList) {
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
	 * @return A list of parameter types in String format
	 */
	@ModelAttribute("configuredprocessornames")
	public List<String> configuredProcessors() {
		if (!hasroleprocessorreader()) {
			return new ArrayList<>();
		}
		checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		if (auth.getDataCache().getConfiguredProcessors() != null && !auth.getDataCache().getConfiguredProcessors().isEmpty())
			return auth.getDataCache().getConfiguredProcessors();

		logger.trace("Get configuredprocessors");
		auth.getDataCache().setConfiguredProcessors(new ArrayList<String>());
		List<?> resultList = null;

		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					"/configuredprocessors?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_CONFIGUREDPROCESSORS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return auth.getDataCache().getConfiguredProcessors();
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getConfiguredProcessors();
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object : resultList) {
				RestConfiguredProcessor restConfiguredProcessorClass = mapper.convertValue(object, RestConfiguredProcessor.class);
				auth.getDataCache().getConfiguredProcessors().add(restConfiguredProcessorClass.getIdentifier());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getConfiguredProcessors().sort(c);
		return auth.getDataCache().getConfiguredProcessors();
	}

	/**
	 * Retrieve the workflows of mission
	 *
	 * @return A list of parameter types in String format
	 */
	@ModelAttribute("workflownames")
	public List<String> workflows() {
		if (!hasroleprocessorreader()) {
			return new ArrayList<>();
		}
		checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		if (auth.getDataCache().getWorkflows() != null && !auth.getDataCache().getWorkflows().isEmpty())
			return auth.getDataCache().getWorkflows();

		logger.trace("Get workflows");
		auth.getDataCache().setWorkflows(new ArrayList<String>());
		List<?> resultList = null;

		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					"/workflows?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_WORKFLOWS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return auth.getDataCache().getWorkflows();
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getWorkflows();
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object object : resultList) {
				RestWorkflow restWorkflowClass = mapper.convertValue(object, RestWorkflow.class);
				auth.getDataCache().getWorkflows().add(restWorkflowClass.getName());
			}
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		auth.getDataCache().getWorkflows().sort(c);
		return auth.getDataCache().getWorkflows();
	}

	/**
	 * Retrieve the file classes of mission
	 *
	 * @return A list of parameter types in String format
	 */
	@ModelAttribute("fileclasses")
	public List<String> fileClasses() {
		checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		if (auth.getDataCache().getFileClasses() != null && !auth.getDataCache().getFileClasses().isEmpty())
			return auth.getDataCache().getFileClasses();

		logger.trace("Get fileclasses");
		auth.getDataCache().setFileClasses(new ArrayList<String>());
		List<?> resultList = null;

		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_FILECLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return auth.getDataCache().getFileClasses();
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getFileClasses();
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission = mapper.convertValue(resultList.get(0), RestMission.class);
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
	 * @return A list of parameter types in String format
	 */
	@ModelAttribute("processingmodes")
	public List<String> processingModes() {
		if (!hasroleorderreader()) {
			return new ArrayList<>();
		}
		checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		if (auth.getDataCache().getProcessingModes() != null && !auth.getDataCache().getProcessingModes().isEmpty())
			return auth.getDataCache().getProcessingModes();

		logger.trace("Get processingmodes");
		auth.getDataCache().setProcessingModes(new ArrayList<String>());
		List<?> resultList = null;

		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_PROCESSINGMODES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return auth.getDataCache().getProcessingModes();
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getProcessingModes();
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission = mapper.convertValue(resultList.get(0), RestMission.class);
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
	 * @return A list of parameter types in String format
	 */
	@ModelAttribute("spacecrafts")
	public List<String> spaceCrafts() {
		if (!hasrolemissionreader()) {
			return new ArrayList<>();
		}
		checkClearCache();
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		if (auth.getDataCache().getSpaceCrafts() != null && !auth.getDataCache().getSpaceCrafts().isEmpty())
			return auth.getDataCache().getSpaceCrafts();

		logger.trace("Get spacecrafts");
		auth.getDataCache().setSpaceCrafts(new ArrayList<String>());
		List<?> resultList = null;

		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					"/missions?mission=" + auth.getMission(), List.class, auth.getProseoName(), auth.getPassword());
		} catch (RestClientResponseException e) {

			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				logger.log(UIMessage.NO_SPACECRAFTS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				logger.log(UIMessage.NOT_AUTHORIZED, "null", "null", "null");
				break;
			default:
				logger.log(UIMessage.EXCEPTION, e.getMessage());
			}

			return auth.getDataCache().getSpaceCrafts();
		} catch (RuntimeException e) {
			logger.log(UIMessage.EXCEPTION, e.getMessage());
			return auth.getDataCache().getSpaceCrafts();
		}

		if (resultList != null) {
			ObjectMapper mapper = new ObjectMapper();
			if (resultList.size() == 1) {
				RestMission restMission = mapper.convertValue(resultList.get(0), RestMission.class);
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
	 * @return A list of parameter types in String format
	 */
	@ModelAttribute("productiontypes")
	public List<String> productiontypes() {
		if (productiontypes != null && !productiontypes.isEmpty())
			return productiontypes;

		productiontypes = new ArrayList<>();
		for (ProductionType value : ProductionType.values()) {
			productiontypes.add(value.toString());
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		productiontypes.sort(c);
		return productiontypes;
	}

	/**
	 * Retrieve the processing level enum
	 *
	 * @return A list of processing levels in String format
	 */
	@ModelAttribute("processinglevels")
	public List<String> processinglevels() {
		if (processinglevels != null && !processinglevels.isEmpty())
			return processinglevels;

		processinglevels = new ArrayList<>();
		for (ProcessingLevel value : ProcessingLevel.values()) {
			processinglevels.add(value.toString());
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		processinglevels.sort(c);
		return processinglevels;
	}

	/**
	 * Retrieve the product visibility enum
	 *
	 * @return A list of visibility levels in String format
	 */
	@ModelAttribute("visibilities")
	public List<String> visibilities() {
		if (visibilities != null && !visibilities.isEmpty())
			return visibilities;

		visibilities = new ArrayList<>();
		for (ProductVisibility value : ProductVisibility.values()) {
			visibilities.add(value.toString());
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		visibilities.sort(c);
		return visibilities;
	}

	/**
	 * Retrieve the product quality levels
	 *
	 * @return A list of product quality types in String format
	 */
	@ModelAttribute("productqualities")
	public List<String> productqualities() {
		if (productqualities != null && !productqualities.isEmpty())
			return productqualities;

		productqualities = new ArrayList<>();
		for (ProductQuality value : ProductQuality.values()) {
			productqualities.add(value.toString());
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		productqualities.sort(c);
		return productqualities;
	}

	/**
	 * Retrieve the slicing type enum
	 *
	 * @return A list of slicing types in String format
	 */
	@ModelAttribute("slicingtypes")
	public List<String> slicingtypes() {
		if (slicingtypes != null && !slicingtypes.isEmpty())
			return slicingtypes;

		slicingtypes = new ArrayList<>();
		for (OrderSlicingType value : OrderSlicingType.values()) {
			slicingtypes.add(value.toString());
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		slicingtypes.sort(c);
		return slicingtypes;
	}

	/**
	 * Retrieves the list of parameter types and returns it as a list of Strings.
	 *
	 * @return A list of parameter types in String format
	 */
	@ModelAttribute("parametertypes")
	public List<String> parametertypes() {
		if (parametertypes != null && !parametertypes.isEmpty())
			return parametertypes;

		parametertypes = new ArrayList<>();
		for (ParameterType value : ParameterType.values()) {
			parametertypes.add(value.toString());
		}
		Comparator<String> c = Comparator.comparing((String x) -> x);
		parametertypes.sort(c);
		return parametertypes;
	}

	/**
	 * Checks if the cache needs to be cleared based on the user's login status.
	 */
	public void checkClearCache() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		// TODO Handling of threads is not correct! Use ThreadLocal<T> or no caching at all (current workaround)
		// if (auth.isNewLogin()) {
		if (auth.isNewLogin()) {
			if (logger.isTraceEnabled())
				logger.trace("Cache in GUIBaseController cleared");
			clearCache();
			auth.setNewLogin(false);
		}
	}

	/**
	 * Clears the data cache stored in the authentication token.
	 */
	private void clearCache() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		auth.getDataCache().clear();
	}

	/**
	 * Returns the mission code of the authenticated user
	 *
	 * @return The mission code of the authenticated user
	 */
	@ModelAttribute("missioncode")
	public String missioncode() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getMission();
	}

	/**
	 * Returns the authenticated user
	 *
	 * @return The authenticated user
	 */
	@ModelAttribute("user")
	public String user() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getName();
	}

	/**
	 * Returns the roles of the authenticated user
	 *
	 * @return The roles of authenticated user
	 */
	@ModelAttribute("userroles")
	public List<String> userroles() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles();
	}

	/**
	 * Read access to missions, spacecrafts and orbits
	 *
	 * @return true, if the user has the mission reader role
	 */
	@ModelAttribute("hasrolemissionreader")
	public Boolean hasrolemissionreader() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.MISSION_READER.toString());
	}

	/**
	 * Read and update access to missions, spacecrafts and orbits
	 *
	 * @return true, if the user has the mission manager role
	 */
	@ModelAttribute("hasrolemissionmgr")
	public Boolean hasrolemissionmgr() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.MISSION_MGR.toString());
	}

	/**
	 * Read access to product classes and selection rules
	 *
	 * @return true, if the user has the product class reader role
	 */
	@ModelAttribute("hasroleproductclassreader")
	public Boolean hasroleproductclassreader() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.PRODUCTCLASS_READER.toString());
	}

	/**
	 * Create, update and delete access to product classes and selection rules
	 *
	 * @return true, if the user has the product class manager role
	 */
	@ModelAttribute("hasroleproductclassmgr")
	public Boolean hasroleproductclassmgr() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.PRODUCTCLASS_MGR.toString());
	}

	/**
	 * Query and download public products
	 *
	 * @return true, if the user has the product reader role
	 */
	@ModelAttribute("hasroleproductreader")
	public Boolean hasroleproductreader() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.PRODUCT_READER.toString())
				| auth.getUserRoles().contains(UserRole.PRODUCT_READER_ALL.toString())
				| auth.getUserRoles().contains(UserRole.PRODUCT_READER_RESTRICTED.toString());
	}

	/**
	 * Query and download public and restricted products
	 *
	 * @return true, if the user has the restricted product reader role
	 */
	@ModelAttribute("hasroleproductrestrictedreader")
	public Boolean hasroleproductrestrictedreader() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.PRODUCT_READER_RESTRICTED.toString());
	}

	/**
	 * Query and download all products
	 *
	 * @return true, if the user has the product reader (all) role
	 */
	@ModelAttribute("hasroleproductreaderall")
	public Boolean hasroleproductreaderall() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.PRODUCT_READER_ALL.toString());
	}

	/** Upload products from external source */
	// TODO PRODUCT_INGESTOR,

	/** Upload products from internal source */
	// TODO PRODUCT_GENERATOR,

	/**
	 * Update and delete products and product files
	 *
	 * @return true, if the user has the product manager role
	 */
	@ModelAttribute("hasroleprodutcmgr")
	public Boolean hasroleproductmgr() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.PRODUCT_MGR.toString());
	}

	// Processor management roles

	/**
	 * Read access to processor classes, processors, configurations, configured processors and any sub-objects of them
	 *
	 * @return true, if the user has the processor reader role
	 */
	@ModelAttribute("hasroleprocessorreader")
	public Boolean hasroleprocessorreader() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.PROCESSOR_READER.toString());
	}

	/**
	 * Create, update and delete access to processor classes, processors and tasks
	 *
	 * @return true, if the user has the processor class manager role
	 */
	@ModelAttribute("hasroleprocessorclassmgr")
	public Boolean hasroleprocessorclassmgr() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.PROCESSORCLASS_MGR.toString());
	}

	/**
	 * Read access to configurations and configured processors
	 *
	 * @return true, if the user has the configuration reader role
	 */
	@ModelAttribute("hasroleconfigurationreader")
	public Boolean hasroleconfigurationreader() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.PROCESSOR_READER.toString());
	}

	/**
	 * Create, update and delete access to configurations and configured processors
	 *
	 * @return true, if the user has the configuration manager role
	 */
	@ModelAttribute("hasroleconfigurationmgr")
	public Boolean hasroleconfigurationmgr() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.CONFIGURATION_MGR.toString());
	}

	// Processing facility management roles

	/**
	 * Read access to processing facilities
	 *
	 * @return true, if the user has the facility reader role
	 */
	@ModelAttribute("hasrolefacilityreader")
	public Boolean hasrolefacilityreader() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.FACILITY_READER.toString());
	}

	/**
	 * Create, update and delete access to processing facilities
	 *
	 * @return true, if the user has the facility manager role
	 */
	@ModelAttribute("hasrolefacilitymgr")
	public Boolean hasrolefacilitymgr() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.FACILITY_MGR.toString());
	}

	/**
	 * Read access to facility monitoring data
	 *
	 * @return true, if the user has the facility monitoring role
	 */
	@ModelAttribute("hasrolefacilitymonitor")
	public Boolean hasrolefacilitymonitor() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.FACILITY_MONITOR.toString());
	}
	
	// Product Archive management roles

	/**
	 * Read access to product archives
	 *
	 * @return true, if the user has the product archive reader role
	 */
	@ModelAttribute("hasroleproductarchivereader")
	public Boolean hasroleproductarchivereader() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.ARCHIVE_READER.toString());
	}

	/**
	 * Create, update and delete access to product archives
	 *
	 * @return true, if the user has the product archive manager role
	 */
	@ModelAttribute("hasroleproductarchivemgr")
	public Boolean hasroleproductarchivemgr() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.ARCHIVE_MGR.toString());
	}

	/**
	 * Read access to processing order, jobs and job steps
	 *
	 * @return true, if the user has the order reader role
	 */
	@ModelAttribute("hasroleorderreader")
	public Boolean hasroleorderreader() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.ORDER_READER.toString());
	}

	/**
	 * Create, update, close and delete orders
	 *
	 * @return true, if the user has the order manager role
	 */
	@ModelAttribute("hasroleordermgr")
	public Boolean hasroleordermgr() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.ORDER_MGR.toString());
	}

	/**
	 * Approve orders
	 *
	 * @return true, if the user has the order approver role
	 */
	@ModelAttribute("hasroleorderapprover")
	public Boolean hasroleorderapprover() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.ORDER_APPROVER.toString());
	}

	/**
	 * Plan, release, suspend, cancel and retry orders, jobs and job steps
	 *
	 * @return true, if the user has the order planner role
	 */
	@ModelAttribute("hasroleorderplanner")
	public Boolean hasroleorderplanner() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.ORDER_PLANNER.toString());
	}

	/**
	 * Read access to order monitoring data
	 *
	 * @return true, if the user has the order monitoring role
	 */
	@ModelAttribute("hasroleordermonitor")
	public Boolean hasroleordermonitor() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.ORDER_MONITOR.toString());
	}

	/**
	 * Notify of job step completion
	 *
	 * @return true, if the user has the job step processor role
	 */
	@ModelAttribute("hasrolejobstepprocessor")
	public Boolean hasrolejobstepprocessor() {
		GUIAuthenticationToken auth = (GUIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		return auth.getUserRoles().contains(UserRole.JOBSTEP_PROCESSOR.toString());
	}

	/**
	 * Handles HTTP errors and sets the appropriate error message in the model.
	 *
	 * @param clientResponse The HTTP response from the client
	 * @param model          The model to hold the error message
	 */
	protected void handleHTTPError(ClientResponse clientResponse, Model model) {
		if (clientResponse.statusCode().compareTo(HttpStatus.NOT_FOUND) == 0) {
			// Not Found error
			logger.trace(">>>Client error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("errormsg", "No elements found");
		} else if (clientResponse.statusCode().is5xxServerError()) {
			// Server error
			logger.trace(">>>Server error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("errormsg", "Server error " + clientResponse.statusCode().toString() + ": "
					+ clientResponse.statusCode().getReasonPhrase());
		} else if (clientResponse.statusCode().is4xxClientError()) {
			// Client error
			logger.trace(">>>Client error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("errormsg", "Client error " + clientResponse.statusCode().toString() + ": "
					+ clientResponse.statusCode().getReasonPhrase());
		} else {
			// Other status codes, do nothing
		}
	}

	/**
	 * Handles HTTP warnings and sets the appropriate warning message in the model and HTTP response headers.
	 *
	 * @param clientResponse The HTTP response from the client
	 * @param model          The model to hold the warning message
	 * @param httpResponse   The HTTP response to set the warning headers
	 */
	protected void handleHTTPWarning(ClientResponse clientResponse, Model model, HttpServletResponse httpResponse) {
		if (clientResponse.statusCode().is5xxServerError()) {
			// Server error warning
			logger.trace(">>>Server error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("warnmsg", "Server error " + clientResponse.statusCode().toString() + ": "
					+ clientResponse.statusCode().getReasonPhrase());
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
			// Client error warning
			logger.trace(">>>Client error ({}, {})", clientResponse.statusCode(), clientResponse.statusCode().getReasonPhrase());
			model.addAttribute("warnmsg", "Client error " + clientResponse.statusCode().toString() + ": "
					+ clientResponse.statusCode().getReasonPhrase());
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
		} else {
			// Other status codes, do nothing
		}
	}

	protected List<Long> calcShowPages(Long page, Long pages) {
		List<Long> showPages = new ArrayList<>();
		Long start = Math.max(page - 4, 1);
		Long end = Math.min(page + 4, pages);
		if (page < 5) {
			end = Math.min(end + (5 - page), pages);
		}
		if (pages - page < 5) {
			start = Math.max(start - (4 - (pages - page)), 1);
		}
		for (Long i = start; i <= end; i++) {
			showPages.add(i);
		}
		return showPages;
	}
	
	protected void modelAddAttributes(Model model, Long count, Long pageSize, Long pages, Long page) {
		model.addAttribute("count", count);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("pageCount", pages);
		model.addAttribute("page", page);
		model.addAttribute("numberOfPages", pages);
		model.addAttribute("currentPage", page);
		
		List<Long> showPages = calcShowPages(page, pages);
		model.addAttribute("showPages", showPages);
	}
}