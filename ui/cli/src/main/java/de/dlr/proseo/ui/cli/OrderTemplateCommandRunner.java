/**
 * OrderTemplateTemplateCommandRunner.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.rest.model.RestOrderTemplate;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO order templates (create, read, update, delete etc.). All methods assume that before invocation a
 * syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class OrderTemplateCommandRunner {

	/* General string constants */
	public static final String CMD_ORDERTEMPLATE = "ordertemplate";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";
	
	private static final String OPTION_DELETE_ATTRIBUTES = "delete-attributes";
	private static final String OPTION_VERBOSE = "verbose";
	private static final String OPTION_FORMAT = "format";
	private static final String OPTION_FILE = "file";
	
	private static final String ORDERTEMPLATES = "order templates";

	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_NAME = "Order template name (empty field cancels): ";
	private static final String PROMPT_SLICING_TYPE = "Slicing type (O)rbit, C)alendar day, calendar M)onth, calendar Y)ear, T)ime slice, N)one; empty field cancels): ";
	private static final String PROMPT_SLICE_DURATION = "Time slice duration (empty field cancels): ";
	private static final String PROMPT_PROCESSING_MODE = "Processing mode (empty field cancels): ";
	private static final String PROMPT_FILE_CLASS = "Output file class (empty field cancels): ";
	private static final String PROMPT_PRODUCT_CLASSES = "Product classes to deliver (comma-separated list; empty field cancels): ";

	private static final String URI_PATH_ORDERTEMPLATES = "/ordertemplates";
	
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS").withZone(ZoneId.of("UTC"));

	/** The user manager used by all command runners */
	@Autowired
	private LoginManager loginManager;
	
	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;
	
	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderTemplateCommandRunner.class);

	/**
	 * Retrieve an order template by name, which is the first parameter of the given command.
	 * Outputs all necessary messages to the log and the user.
	 * 
	 * @param command the command containing the name parameter
	 * @return a processing order or null, if none was found or an error occurred
	 */
	private RestOrderTemplate retrieveOrderTemplateByIdentifierParameter(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> retrieveOrderTemplateByIdentifierParameter({})", (null == command ? "null" : command.getName()));

		/* Get order ID from command parameters */
		if (command.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_IDENTIFIER_GIVEN));
			return null;
		}
		String orderIdentifier = command.getParameters().get(0).getValue();
		
		/* Retrieve the order using OrderTemplate Manager service */
		try {
			List<?> resultList = null;
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					URI_PATH_ORDERTEMPLATES + "?mission=" + loginManager.getMission() + "&name=" + URLEncoder.encode(orderIdentifier, Charset.defaultCharset()),
					List.class, loginManager.getUser(), loginManager.getPassword());
			if (resultList.isEmpty()) {
				throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
			} else {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.convertValue(resultList.get(0), RestOrderTemplate.class);
			}
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.ORDERTEMPLATE_NOT_FOUND, orderIdentifier);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORDERTEMPLATES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return null;
		}
	}
	
	/**
	 * Create a new order template; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "ordertemplate create" command
	 */
	private void createOrderTemplate(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createOrderTemplate({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File orderFile = null;
		String orderFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				orderFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				orderFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read order file, if any */
		RestOrderTemplate restOrderTemplate = null;
		if (null == orderFile) {
			restOrderTemplate = new RestOrderTemplate();
		} else {
			try {
				restOrderTemplate = CLIUtil.parseObjectFile(orderFile, orderFileFormat, RestOrderTemplate.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from order file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is order name
				restOrderTemplate.setName(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restOrderTemplate, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restOrderTemplate.getMissionCode() || 0 == restOrderTemplate.getMissionCode().length()) {
			restOrderTemplate.setMissionCode(loginManager.getMission());
		}
		if (null == restOrderTemplate.getSliceOverlap()) {
			restOrderTemplate.setSliceOverlap(0L);
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restOrderTemplate.getName() || 0 == restOrderTemplate.getName().length()) {
			System.out.print(PROMPT_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restOrderTemplate.setName(response);
		}
		// Get order slicing type
		while (!Arrays.asList(OrderSlicingType.ORBIT.toString(), OrderSlicingType.CALENDAR_DAY.toString(), 
				OrderSlicingType.CALENDAR_MONTH.toString(), OrderSlicingType.CALENDAR_YEAR.toString(),
				OrderSlicingType.TIME_SLICE.toString(), OrderSlicingType.NONE.toString()).contains(restOrderTemplate.getSlicingType())) {
			System.out.print(PROMPT_SLICING_TYPE);
			String response = System.console().readLine().toUpperCase();
			switch (response) {
			case "O":	restOrderTemplate.setSlicingType(OrderSlicingType.ORBIT.toString()); break;
			case "C":	restOrderTemplate.setSlicingType(OrderSlicingType.CALENDAR_DAY.toString()); break;
			case "M":	restOrderTemplate.setSlicingType(OrderSlicingType.CALENDAR_MONTH.toString()); break;
			case "Y":	restOrderTemplate.setSlicingType(OrderSlicingType.CALENDAR_YEAR.toString()); break;
			case "T":	restOrderTemplate.setSlicingType(OrderSlicingType.TIME_SLICE.toString()); break;
			case "N":	restOrderTemplate.setSlicingType(OrderSlicingType.NONE.toString()); break;
			case "":
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			default:
				System.err.println(ProseoLogger.format(UIMessage.INVALID_SLICING_TYPE, response));
			}
		}
		// For TIME_SLICE orders get slice duration
		while (OrderSlicingType.TIME_SLICE.toString().equals(restOrderTemplate.getSlicingType()) 
				&& (null == restOrderTemplate.getSliceDuration() || 0 == restOrderTemplate.getSliceDuration())) {
			System.out.print(PROMPT_SLICE_DURATION);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			try {
				restOrderTemplate.setSliceDuration(Long.parseLong(response));
			} catch (NumberFormatException e) {
				System.err.println(ProseoLogger.format(UIMessage.INVALID_SLICE_DURATION, response));
			}
		}
		// For TIME_SLICE and CALENDAR_DAY/MONTH/YEAR orders, get start and end time
		if (OrderSlicingType.TIME_SLICE.toString().equals(restOrderTemplate.getSlicingType()) 
				|| OrderSlicingType.CALENDAR_DAY.toString().equals(restOrderTemplate.getSlicingType())
				|| OrderSlicingType.CALENDAR_MONTH.toString().equals(restOrderTemplate.getSlicingType())
				|| OrderSlicingType.CALENDAR_YEAR.toString().equals(restOrderTemplate.getSlicingType())
				|| OrderSlicingType.NONE.toString().equals(restOrderTemplate.getSlicingType())) {
		}
		// For ORBIT orders get list of orbits (comma-separated with ranges, e. g. "3000-3003,3005,3009-3011")
		if (OrderSlicingType.ORBIT.toString().equals(restOrderTemplate.getSlicingType())) {
		}
		// Get processing mode
		if (null == restOrderTemplate.getProcessingMode() || 0 == restOrderTemplate.getProcessingMode().length()) {
			System.out.print(PROMPT_PROCESSING_MODE);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restOrderTemplate.setProcessingMode(response);
		}
		// Get file class
		if (null == restOrderTemplate.getOutputFileClass() || 0 == restOrderTemplate.getOutputFileClass().length()) {
			System.out.print(PROMPT_FILE_CLASS);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restOrderTemplate.setOutputFileClass(response);
		}
		// Get requested product classes
		if (restOrderTemplate.getRequestedProductClasses().isEmpty()) {
			System.out.print(PROMPT_PRODUCT_CLASSES);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restOrderTemplate.setRequestedProductClasses(Arrays.asList(response.split(",")));
		}
		
		/* Create order */
		try {
			restOrderTemplate = serviceConnection.postToService(serviceConfig.getOrderManagerUrl(), URI_PATH_ORDERTEMPLATES, 
					restOrderTemplate, RestOrderTemplate.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.ORDERTEMPLATE_DATA_INVALID,  e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORDERTEMPLATES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving newly assigned order ID */
		String message = logger.log(UIMessage.ORDERTEMPLATE_CREATED, restOrderTemplate.getName(), restOrderTemplate.getId());
		System.out.println(message);
	}
	
	/**
	 * Show the order template specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "ordertemplate show" command
	 */
	private void showOrderTemplate(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showOrderTemplate({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String orderOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				orderOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_ORDERTEMPLATES + "?mission=" + loginManager.getMission();
		
		/* Check whether order template name is given */
		if (!showCommand.getParameters().isEmpty()) {
			requestURI += "&name=" + URLEncoder.encode(showCommand.getParameters().get(0).getValue(), Charset.defaultCharset());  // only one parameter expected
		}
		
		
		/* Get the order information from the OrderTemplate Manager */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.NO_ORDERTEMPLATES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORDERTEMPLATES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		if (isVerbose) {
			/* Display the order template(s) found */
			try {
				CLIUtil.printObject(System.out, resultList, orderOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Must be a list of order templates
			String listFormat = "%-30s %-38s";
			System.out.println(String.format(listFormat, "Name", "Mode"));
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(String.format(listFormat,
							resultMap.get("name"),
							resultMap.get("processingMode")));
				}
			}
		}
	}
	
	/**
	 * Update a order template from an order file or from "attribute=value" pairs (overriding any order file entries)
	 * 
	 * @param updateCommand the parsed "ordertemplate update" command
	 */
	private void updateOrderTemplate(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateOrderTemplate({})", (null == updateCommand ? "null" : updateCommand.getName()));
		
		/* Check command options */
		File orderFile = null;
		String orderFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				orderFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				orderFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read order template file, if any, and update order attributes */
		RestOrderTemplate updatedOrderTemplate = null;
		if (null == orderFile) {
			updatedOrderTemplate = new RestOrderTemplate();
		} else {
			try {
				updatedOrderTemplate = CLIUtil.parseObjectFile(orderFile, orderFileFormat, RestOrderTemplate.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from order file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is order name
				updatedOrderTemplate.setName(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedOrderTemplate, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Read original order from OrderTemplate Manager service */
		RestOrderTemplate restOrderTemplate = null;
		try {
			if (null != updatedOrderTemplate.getId() && 0 != updatedOrderTemplate.getId().longValue()) {
				// Read order by database ID
				restOrderTemplate = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(), URI_PATH_ORDERTEMPLATES + "/" + updatedOrderTemplate.getId(),
						RestOrderTemplate.class, loginManager.getUser(), loginManager.getPassword());
			} else if (null != updatedOrderTemplate.getName() && 0 != updatedOrderTemplate.getName().length()) {
				// Read order by user-defined name
				List<?> resultList = null;
				resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
						URI_PATH_ORDERTEMPLATES + "?mission=" + loginManager.getMission() + "&name=" + URLEncoder.encode(updatedOrderTemplate.getName(), Charset.defaultCharset()),
						List.class, loginManager.getUser(), loginManager.getPassword());
				if (resultList.isEmpty()) {
					throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
				} else {
					ObjectMapper mapper = new ObjectMapper();
					restOrderTemplate =  mapper.convertValue(resultList.get(0), RestOrderTemplate.class);
				}
			} else {
				// No identifying value given
				System.err.println(ProseoLogger.format(UIMessage.NO_IDENTIFIER_GIVEN));
				return;
			}
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.ORDERTEMPLATE_NOT_FOUND, updatedOrderTemplate.getName());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORDERTEMPLATES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		if (null != updatedOrderTemplate.getSlicingType()) { // mandatory
			restOrderTemplate.setSlicingType(updatedOrderTemplate.getSlicingType());
		}
		if ((isDeleteAttributes && !OrderSlicingType.TIME_SLICE.toString().equals(restOrderTemplate.getSlicingType()))
				|| null != updatedOrderTemplate.getSliceDuration()) {
			restOrderTemplate.setSliceDuration(updatedOrderTemplate.getSliceDuration());
		}
		if (null != updatedOrderTemplate.getSliceOverlap()) { // mandatory
			restOrderTemplate.setSliceOverlap(updatedOrderTemplate.getSliceOverlap());
		}
		if (isDeleteAttributes || !updatedOrderTemplate.getInputFilters().isEmpty()) {
			restOrderTemplate.setInputFilters(updatedOrderTemplate.getInputFilters());
		}
		if (isDeleteAttributes || !updatedOrderTemplate.getClassOutputParameters().isEmpty()) {
			restOrderTemplate.setClassOutputParameters(updatedOrderTemplate.getClassOutputParameters());
		}
		if (isDeleteAttributes || !updatedOrderTemplate.getOutputParameters().isEmpty()) {
			restOrderTemplate.setOutputParameters(updatedOrderTemplate.getOutputParameters());
		}
		if (isDeleteAttributes || !updatedOrderTemplate.getConfiguredProcessors().isEmpty()) {
			restOrderTemplate.setConfiguredProcessors(updatedOrderTemplate.getConfiguredProcessors());
		}
		if (!updatedOrderTemplate.getRequestedProductClasses().isEmpty()) { // mandatory
			restOrderTemplate.setRequestedProductClasses(updatedOrderTemplate.getRequestedProductClasses());
		}
		if (isDeleteAttributes || !updatedOrderTemplate.getInputProductClasses().isEmpty()) {
			restOrderTemplate.setInputProductClasses(updatedOrderTemplate.getInputProductClasses());
		}
		if (null != updatedOrderTemplate.getOutputFileClass()) { // mandatory
			restOrderTemplate.setOutputFileClass(updatedOrderTemplate.getOutputFileClass());
		}
		if (null != updatedOrderTemplate.getProcessingMode()) { // mandatory
			restOrderTemplate.setProcessingMode(updatedOrderTemplate.getProcessingMode());
		}
		if (null != updatedOrderTemplate.getProductionType()) { // mandatory
			restOrderTemplate.setProductionType(updatedOrderTemplate.getProductionType());
		}
		if (isDeleteAttributes || null != updatedOrderTemplate.getProductRetentionPeriod()) {
			restOrderTemplate.setProductRetentionPeriod(updatedOrderTemplate.getProductRetentionPeriod());
		}
		if (isDeleteAttributes || null != updatedOrderTemplate.getDynamicProcessingParameters()) {
			restOrderTemplate.setDynamicProcessingParameters(updatedOrderTemplate.getDynamicProcessingParameters());
		}
		if (isDeleteAttributes || null != updatedOrderTemplate.getPriority()) {
			restOrderTemplate.setPriority(updatedOrderTemplate.getPriority());
		}
		if (isDeleteAttributes || null != updatedOrderTemplate.getNotificationEndpoint()) {
			restOrderTemplate.setNotificationEndpoint(updatedOrderTemplate.getNotificationEndpoint());
		}
		if(isDeleteAttributes || null != updatedOrderTemplate.getInputDataTimeoutPeriod()) {
			restOrderTemplate.setInputDataTimeoutPeriod(updatedOrderTemplate.getInputDataTimeoutPeriod());
		}
		if (isDeleteAttributes || null != updatedOrderTemplate.getOnInputDataTimeoutFail()) {
			restOrderTemplate.setOnInputDataTimeoutFail(updatedOrderTemplate.getOnInputDataTimeoutFail());
		}
		if (isDeleteAttributes || null != updatedOrderTemplate.getAutoRelease()) {
			restOrderTemplate.setAutoRelease(updatedOrderTemplate.getAutoRelease());
		}
		if (isDeleteAttributes || null != updatedOrderTemplate.getAutoClose()) {
			restOrderTemplate.setAutoClose(updatedOrderTemplate.getAutoClose());
		}
		if (isDeleteAttributes || null != updatedOrderTemplate.getEnabled()) {
			restOrderTemplate.setEnabled(updatedOrderTemplate.getEnabled());
		}
		
		/* Update order using OrderTemplate Manager service */
		try {
			restOrderTemplate = serviceConnection.patchToService(serviceConfig.getOrderManagerUrl(), URI_PATH_ORDERTEMPLATES + "/" + restOrderTemplate.getId(),
					restOrderTemplate, RestOrderTemplate.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.ORDERTEMPLATE_NOT_FOUND, restOrderTemplate.getName());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.ORDERTEMPLATE_DATA_INVALID,  e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORDERTEMPLATES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new order template version */
		String message = logger.log(UIMessage.ORDERTEMPLATE_UPDATED, restOrderTemplate.getName(), restOrderTemplate.getVersion());
		System.out.println(message);
	}
	
	/**
	 * Delete the named order template
	 * 
	 * @param deleteCommand the parsed "ordertemplate delete" command
	 */
	private void deleteOrderTemplate(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrderTemplate({})", (null == deleteCommand ? "null" : deleteCommand.getName()));
		
		/* Get order template ID from command parameters and retrieve the order using OrderTemplate Manager service */
		RestOrderTemplate restOrderTemplate = retrieveOrderTemplateByIdentifierParameter(deleteCommand);
		if (null == restOrderTemplate)
			return;
				
		/* Delete order using OrderTemplate Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getOrderManagerUrl(), URI_PATH_ORDERTEMPLATES + "/" + restOrderTemplate.getId(),
						loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.ORDERTEMPLATE_NOT_FOUND, restOrderTemplate.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORDERTEMPLATES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success */
		String message = logger.log(UIMessage.ORDERTEMPLATE_DELETED, restOrderTemplate.getName());
		System.out.println(message);
	}


	/**
	 * Run the given command
	 * 
	 * @param command the command to execute
	 */
	void executeCommand(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> executeCommand({})", (null == command ? "null" : command.getName()));
		
		/* Check that user is logged in */
		if (null == loginManager.getUser()) {
			System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN, command.getName()));
			return;
		}
		if (null == loginManager.getMission()) {
			System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN_TO_MISSION, command.getName()));
			return;
		}
		
		/* Check argument */
		if (!CMD_ORDERTEMPLATE.equals(command.getName())) {
			System.err.println(ProseoLogger.format(UIMessage.INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given */
		if (null == command.getSubcommand() || null == command.getSubcommand().getName()) {
			System.err.println(ProseoLogger.format(UIMessage.SUBCOMMAND_MISSING, command.getName()));
			return;
		}
		
		/* Check for subcommand help request */
		ParsedCommand subcommand = command.getSubcommand();
		if (subcommand.isHelpRequested()) {
			subcommand.getSyntaxCommand().printHelp(System.out);
			return;
		}
		
		/* Execute the subcommand */
		switch(subcommand.getName()) {
		case CMD_CREATE:	createOrderTemplate(subcommand); break;
		case CMD_SHOW:		showOrderTemplate(subcommand); break;
		case CMD_UPDATE:	updateOrderTemplate(subcommand); break;
		case CMD_DELETE:	deleteOrderTemplate(subcommand); break;
		default:
			System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
			return;
		}
	}
}
