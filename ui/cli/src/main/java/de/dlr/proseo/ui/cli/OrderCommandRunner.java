/**
 * OrderCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.ProcessingOrder.OrderSlicingType;
import de.dlr.proseo.model.ProcessingOrder.OrderState;
import de.dlr.proseo.model.rest.model.RestOrbitQuery;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.ui.backend.BackendConfiguration;
import de.dlr.proseo.ui.backend.BackendConnectionService;
import de.dlr.proseo.ui.backend.BackendUserManager;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO orders (create, read, update, delete etc.). All methods assume that before invocation a
 * syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class OrderCommandRunner {

	/* Message ID constants */
	private static final int MSG_ID_INVALID_COMMAND_NAME = 2930;
	private static final int MSG_ID_SUBCOMMAND_MISSING = 2931;
	private static final int MSG_ID_USER_NOT_LOGGED_IN = 2932;
	private static final int MSG_ID_NOT_AUTHORIZED = 2933;
	private static final int MSG_ID_NO_ORDERS_FOUND = 2934;
	private static final int MSG_ID_OPERATION_CANCELLED = 2935;
	private static final int MSG_ID_INVALID_SLICING_TYPE = 2936;
	private static final int MSG_ID_INVALID_SLICE_DURATION = 2937;
	private static final int MSG_ID_INVALID_ORBIT_NUMBER = 2938;
	private static final int MSG_ID_ORDER_CREATED = 2939;
	private static final int MSG_ID_ORDER_NOT_FOUND = 2940;
	private static final int MSG_ID_NO_IDENTIFIER_GIVEN = 2941;
	private static final int MSG_ID_INVALID_ORDER_STATE = 2942;
	private static final int MSG_ID_ORDER_UPDATED = 2943;
	private static final int MSG_ID_ORDER_DELETED = 2944;
	private static final int MSG_ID_INVALID_TIME = 2945;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_INVALID_COMMAND_NAME = "(E%d) Invalid command name %s";
	private static final String MSG_SUBCOMMAND_MISSING = "(E%d) Subcommand missing for command %s";
	private static final String MSG_USER_NOT_LOGGED_IN = "(E%d) User not logged in";
	private static final String MSG_NOT_AUTHORIZED = "(E%d) User %s not authorized to manage orders for mission %s";
	private static final String MSG_NOT_IMPLEMENTED = "(E%d) Command %s not implemented";
	private static final String MSG_NO_ORDERS_FOUND = "(E%d) No orders found for given search criteria";
	private static final String MSG_INVALID_SLICING_TYPE = "(E%d) Invalid order slicing type %s";
	private static final String MSG_INVALID_SLICE_DURATION = "(E%d) Slice duration %s not numeric";
	private static final String MSG_INVALID_TIME = "(E%d) Time format %s not parseable";
	private static final String MSG_INVALID_ORBIT_NUMBER = "(E%d) Orbit number %s not numeric";
	private static final String MSG_ORDER_NOT_FOUND = "(E%d) Order with identifier %s not found";
	private static final String MSG_INVALID_ORDER_STATE = "(E%d) Operation %s not allowed for order state %s (must be %s)";
	private static final String MSG_NO_IDENTIFIER_GIVEN = "(E%d) No order identifier or database ID given";

	private static final String MSG_OPERATION_CANCELLED = "(I%d) Operation cancelled";
	private static final String MSG_ORDER_CREATED = "(I%d) Order with identifier %s created (database ID %d)";
	private static final String MSG_ORDER_UPDATED = "(I%d) Order with identifier %s updated (new version %d)";
	private static final String MSG_ORDER_DELETED = "(I%d) Order with identifier %s deleted";
	
	/* Other string constants */
	public static final String CMD_ORDER = "order";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";
	private static final String CMD_APPROVE = "approve";
	private static final String CMD_PLAN = "plan";
	private static final String CMD_RELEASE = "release";
	private static final String CMD_SUSPEND = "suspend";
	private static final String CMD_RESUME = "resume";
	private static final String CMD_CANCEL = "cancel";
	private static final String CMD_CLOSE = "close";
	private static final String CMD_RESET = "reset";

	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_IDENTIFIER = "Order identifier (empty field cancels): ";
	private static final String PROMPT_SLICING_TYPE = "Slicing type (O)rbit, C)alendar day, T)ime slice; empty field cancels): ";
	private static final String PROMPT_SLICE_DURATION = "Time slice duration (empty field cancels): ";
	private static final String PROMPT_START_TIME = "Order time interval start (YYYY-MM-DDTHH:MM:SS; emtpy field cancels): ";
	private static final String PROMPT_STOP_TIME = "Order time interval end (YYYY-MM-DDTHH:MM:SS; emtpy field cancels): ";
	private static final String PROMPT_SPACECRAFT = "Spacecraft code (empty field cancels): ";
	private static final String PROMPT_ORBIT_LIST = "Orbits to process (comma-separated list; empty field cancels): ";
	private static final String PROMPT_PROCESSING_MODE = "Processing mode (empty field cancels): ";
	private static final String PROMPT_FILE_CLASS = "Output file class (empty field cancels): ";
	private static final String PROMPT_PRODUCT_CLASSES = "Product classes to deliver (comma-separated list; empty field cancels): ";

	private static final String URI_PATH_ORDERS = "/orders";
	
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("UTC"));

	/** The configuration object for the prosEO User Interface */
	@Autowired
	private BackendConfiguration backendConfig;
	
	/** The user manager used by all command runners */
	@Autowired
	private BackendUserManager backendUserMgr;
	
	/** The connector service to the prosEO backend services prosEO */
	@Autowired
	private BackendConnectionService backendConnector;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderCommandRunner.class);
	
	/**
	 * Retrieve a processing order by identifier, which is the first parameter of the given command.
	 * Outputs all necessary messages to the log and the user.
	 * 
	 * @param command the command containing the identifier parameter
	 * @return a processing order or null, if none was found or an error occurred
	 */
	private RestOrder retrieveOrderByIdentifierParameter(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> retrieveOrderByIdentifierParameter({})", (null == command ? "null" : command.getName()));

		/* Get order ID from command parameters */
		if (command.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(String.format(MSG_NO_IDENTIFIER_GIVEN, MSG_ID_NO_IDENTIFIER_GIVEN));
			return null;
		}
		String orderIdentifier = command.getParameters().get(0).getValue();
		
		/* Retrieve the order using Order Manager service */
		try {
			return backendConnector.getFromService(backendConfig.getOrderManagerUrl(),
					URI_PATH_ORDERS + "?identifier=" + orderIdentifier,
					RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, orderIdentifier);
			logger.error(message);
			System.err.println(message);
			return null;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return null;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return null;
		}
	}
	
	/**
	 * Create a new processing order; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "order create" command
	 */
	private void createOrder(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createOrder({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File orderFile = null;
		String orderFileFormat = null;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				orderFile = new File(option.getValue());
				break;
			case "format":
				orderFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read order file, if any */
		RestOrder restOrder = null;
		if (null == orderFile) {
			restOrder = new RestOrder();
		} else {
			try {
				restOrder = CLIUtil.parseObjectFile(orderFile, orderFileFormat, RestOrder.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(e.getMessage());
				return;
			}
		}
		
		/* Check command parameters (overriding values from order file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is order identifier
				restOrder.setIdentifier(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restOrder, param.getValue());
				} catch (Exception e) {
					System.err.println(e.getMessage());
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restOrder.getMissionCode() || 0 == restOrder.getMissionCode().length()) {
			restOrder.setMissionCode(backendUserMgr.getMission());
		}
		if (null == restOrder.getOrderState() || 0 == restOrder.getOrderState().length()) {
			restOrder.setOrderState(OrderState.INITIAL.toString());
		}
		if (null == restOrder.getSliceOverlap()) {
			restOrder.setSliceOverlap(0L);
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restOrder.getIdentifier() || 0 == restOrder.getIdentifier().length()) {
			System.out.print(PROMPT_IDENTIFIER);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restOrder.setIdentifier(response);
		}
		// Get order slicing type
		while (!Arrays.asList(OrderSlicingType.ORBIT.toString(), OrderSlicingType.CALENDAR_DAY.toString(), 
				OrderSlicingType.TIME_SLICE.toString()).contains(restOrder.getSlicingType())) {
			System.out.print(PROMPT_SLICING_TYPE);
			String response = System.console().readLine().toUpperCase();
			switch (response) {
			case "O":	restOrder.setSlicingType(OrderSlicingType.ORBIT.toString()); break;
			case "C":	restOrder.setSlicingType(OrderSlicingType.CALENDAR_DAY.toString()); break;
			case "T":	restOrder.setSlicingType(OrderSlicingType.TIME_SLICE.toString()); break;
			case "":
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			default:
				System.err.println(String.format(MSG_INVALID_SLICING_TYPE, MSG_ID_INVALID_SLICING_TYPE, response));
			}
		}
		// For TIME_SLICE orders get slice duration
		while (OrderSlicingType.TIME_SLICE.toString().equals(restOrder.getSlicingType()) 
				&& (null == restOrder.getSliceDuration() || 0 == restOrder.getSliceDuration())) {
			System.out.print(PROMPT_SLICE_DURATION);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			}
			try {
				restOrder.setSliceDuration(Long.parseLong(response));
			} catch (NumberFormatException e) {
				System.err.println(String.format(MSG_INVALID_SLICE_DURATION, MSG_ID_INVALID_SLICE_DURATION, response));
			}
		}
		// For TIME_SLICE and CALENDAR_DAY orders, get start and end time
		if (OrderSlicingType.TIME_SLICE.toString().equals(restOrder.getSlicingType()) 
				|| OrderSlicingType.CALENDAR_DAY.toString().equals(restOrder.getSlicingType())) {
			while (null == restOrder.getStartTime()) {
				System.out.print(PROMPT_START_TIME);
				String response = System.console().readLine();
				if ("".equals(response)) {
					System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
					return;
				}
				try {
					restOrder.setStartTime(java.util.Date.from(Instant.parse(response + "Z"))); // no time zone in input expected
				} catch (DateTimeParseException e) {
					System.err.println(String.format(MSG_INVALID_TIME, MSG_ID_INVALID_TIME, response));
				}
			}
			while (null == restOrder.getStopTime()) {
				System.out.print(PROMPT_STOP_TIME);
				String response = System.console().readLine();
				if ("".equals(response)) {
					System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
					return;
				}
				try {
					restOrder.setStopTime(java.util.Date.from(Instant.parse(response + "Z"))); // no time zone in input expected
				} catch (DateTimeParseException e) {
					System.err.println(String.format(MSG_INVALID_TIME, MSG_ID_INVALID_TIME, response));
				}
			} 
		}
		// For ORBIT orders get list of orbits (comma-separated, no ranges) TODO add ranges
		if (OrderSlicingType.ORBIT.toString().equals(restOrder.getSlicingType())) {
			ORBITS:
			while (restOrder.getOrbits().isEmpty()) {
				System.out.print(PROMPT_SPACECRAFT);
				String spacecraft = System.console().readLine();
				if ("".equals(spacecraft)) {
					System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
					return;
				}
				System.out.print(PROMPT_ORBIT_LIST);
				String orbitList = System.console().readLine();
				if ("".equals(orbitList)) {
					System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
					return;
				}
				String[] orbits = orbitList.split(",");
				Long[] orbitNumbers = new Long[orbits.length];
				for (int i = 0; i < orbits.length; ++i) {
					try {
						orbitNumbers[i] = Long.parseLong(orbits[i]);
					} catch (NumberFormatException e) {
						System.err.println(String.format(MSG_INVALID_ORBIT_NUMBER, MSG_ID_INVALID_ORBIT_NUMBER, orbits[i]));
						continue ORBITS;
					}
				}
				for (Long orbitNumber: orbitNumbers) {
					RestOrbitQuery query = new RestOrbitQuery();
					query.setSpacecraftCode(spacecraft);
					query.setOrbitNumberFrom(orbitNumber);
					query.setOrbitNumberTo(orbitNumber);
					restOrder.getOrbits().add(query);
				}
			}
		}
		// Get processing mode
		if (null == restOrder.getProcessingMode() || 0 == restOrder.getProcessingMode().length()) {
			System.out.print(PROMPT_PROCESSING_MODE);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restOrder.setProcessingMode(response);
		}
		// Get file class
		if (null == restOrder.getOutputFileClass() || 0 == restOrder.getOutputFileClass().length()) {
			System.out.print(PROMPT_FILE_CLASS);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restOrder.setOutputFileClass(response);
		}
		// Get requested product classes
		if (restOrder.getRequestedProductClasses().isEmpty()) {
			System.out.print(PROMPT_PRODUCT_CLASSES);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restOrder.setRequestedProductClasses(Arrays.asList(response.split(",")));
		}
		
		/* Create order */
		try {
			restOrder = backendConnector.postToService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS, 
					restOrder, RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving newly assigned order ID */
		String message = String.format(MSG_ORDER_CREATED, MSG_ID_ORDER_CREATED, restOrder.getIdentifier(), restOrder.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Show the order specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "order show" command
	 */
	private void showOrder(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showOrder({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Prepare request URI */
		String requestURI = URI_PATH_ORDERS + "?mission=" + backendUserMgr.getMission();
		
		/* Check whether order ID is given (overrides --from and --to options) or --from and/or --to parameters are set */
		if (showCommand.getParameters().isEmpty()) {
			for (ParsedOption option: showCommand.getOptions()) {
				if ("from".equals(option.getName())) {
					requestURI += "&executionTimeFrom=" + formatter.format(Instant.parse(option.getValue()));
				} else if ("to".equals(option.getName())) {
					requestURI += "&executionTimeTo=" + formatter.format(Instant.parse(option.getValue()));
				}
			}
		} else {
			requestURI += "&identifier=" + showCommand.getParameters().get(0).getValue();  // only one parameter expected
		}
		
		/* Get the order information from the Order Manager */
		List<?> resultList = null;
		try {
			resultList = backendConnector.getFromService(backendConfig.getOrderManagerUrl(),
					requestURI, List.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_NO_ORDERS_FOUND, MSG_ID_NO_ORDERS_FOUND);
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Display the order(s) found */
		ObjectMapper mapper = new ObjectMapper();
		for (Object result: resultList) {
			RestOrder restOrder = mapper.convertValue(result, RestOrder.class);
			
			// TODO Format output
			System.out.println(restOrder);
		}
	}
	
	/**
	 * Update a processing order from an order file or from "attribute=value" pairs (overriding any order file entries)
	 * 
	 * @param updateCommand the parsed "order update" command
	 */
	private void updateOrder(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateOrder({})", (null == updateCommand ? "null" : updateCommand.getName()));
		
		/* Check command options */
		File orderFile = null;
		String orderFileFormat = null;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				orderFile = new File(option.getValue());
				break;
			case "format":
				orderFileFormat = option.getValue().toUpperCase();
				break;
			case "delete-attributes":
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read order file, if any, and update order attributes */
		RestOrder updatedOrder = null;
		if (null == orderFile) {
			updatedOrder = new RestOrder();
		} else {
			try {
				updatedOrder = CLIUtil.parseObjectFile(orderFile, orderFileFormat, RestOrder.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(e.getMessage());
				return;
			}
		}
		
		/* Check command parameters (overriding values from order file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is order identifier
				updatedOrder.setIdentifier(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedOrder, param.getValue());
				} catch (Exception e) {
					System.err.println(e.getMessage());
					return;
				}
			}
		}
		
		/* Read original order from Order Manager service */
		RestOrder restOrder = null;
		try {
			if (null != updatedOrder.getId() && 0 != updatedOrder.getId().longValue()) {
				// Read order by database ID
				restOrder = backendConnector.getFromService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + updatedOrder.getId(),
						RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
			} else if (null != updatedOrder.getIdentifier() && 0 != updatedOrder.getIdentifier().length()) {
				// Read order by user-defined identifier
				restOrder = backendConnector.getFromService(backendConfig.getOrderManagerUrl(),
						URI_PATH_ORDERS + "?identifier=" + updatedOrder.getIdentifier(),
						RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
			} else {
				// No identifying value given
				System.err.println(String.format(MSG_NO_IDENTIFIER_GIVEN, MSG_ID_NO_IDENTIFIER_GIVEN));
				return;
			}
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getIdentifier());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Check whether (database) order is in state "INITIAL", otherwise no user updates allowed */
		if (!OrderState.INITIAL.toString().equals(restOrder.getOrderState())) {
			System.err.println(String.format(MSG_INVALID_ORDER_STATE, MSG_ID_INVALID_ORDER_STATE,
					CMD_UPDATE, restOrder.getOrderState(), OrderState.INITIAL.toString()));
			return;
		}
		
		/* Compare attributes of database order with updated order */
		// No modification of ID, version, mission code or identifier allowed
		if (isDeleteAttributes || null != updatedOrder.getExecutionTime()) {
			restOrder.setExecutionTime(updatedOrder.getExecutionTime());
		}
		if (null != updatedOrder.getSlicingType()) { // mandatory
			restOrder.setSlicingType(updatedOrder.getSlicingType());
		}
		if ((isDeleteAttributes && !OrderSlicingType.TIME_SLICE.toString().equals(restOrder.getSlicingType())
				&& !OrderSlicingType.CALENDAR_DAY.toString().equals(restOrder.getSlicingType())) 
				|| null != updatedOrder.getStartTime()) { // mandatory for TIME_SLICE and CALENDAR_DAY
			restOrder.setStartTime(updatedOrder.getStartTime());
		}
		if ((isDeleteAttributes && !OrderSlicingType.TIME_SLICE.toString().equals(restOrder.getSlicingType())
				&& !OrderSlicingType.CALENDAR_DAY.toString().equals(restOrder.getSlicingType())) 
				|| null != updatedOrder.getStopTime()) { // mandatory for TIME_SLICE and CALENDAR_DAY
			restOrder.setStopTime(updatedOrder.getStopTime());
		}
		if ((isDeleteAttributes && !OrderSlicingType.TIME_SLICE.toString().equals(restOrder.getSlicingType()))
				|| null != updatedOrder.getSliceDuration()) {
			restOrder.setSliceDuration(updatedOrder.getSliceDuration());
		}
		if (null != updatedOrder.getSliceOverlap()) { // mandatory
			restOrder.setSliceOverlap(updatedOrder.getSliceOverlap());
		}
		if (isDeleteAttributes || !updatedOrder.getFilterConditions().isEmpty()) {
			restOrder.setFilterConditions(updatedOrder.getFilterConditions());
		}
		if (isDeleteAttributes || !updatedOrder.getOutputParameters().isEmpty()) {
			restOrder.setOutputParameters(updatedOrder.getOutputParameters());
		}
		if (isDeleteAttributes || !updatedOrder.getConfiguredProcessors().isEmpty()) {
			restOrder.setConfiguredProcessors(updatedOrder.getConfiguredProcessors());
		}
		if ((isDeleteAttributes && !OrderSlicingType.ORBIT.toString().equals(restOrder.getSlicingType()))
				|| !updatedOrder.getOrbits().isEmpty()) {
			restOrder.setOrbits(updatedOrder.getOrbits());
		}
		if (!updatedOrder.getRequestedProductClasses().isEmpty()) { // mandatory
			restOrder.setRequestedProductClasses(updatedOrder.getRequestedProductClasses());
		}
		if (isDeleteAttributes || !updatedOrder.getInputProductClasses().isEmpty()) {
			restOrder.setInputProductClasses(updatedOrder.getInputProductClasses());
		}
		if (isDeleteAttributes || null != updatedOrder.getOutputFileClass()) { // mandatory? TODO
			restOrder.setOutputFileClass(updatedOrder.getOutputFileClass());
		}
		if (isDeleteAttributes || null != updatedOrder.getProcessingMode()) { // mandatory? TODO
			restOrder.setProcessingMode(updatedOrder.getProcessingMode());
		}
		
		/* Update order using Order Manager service */
		try {
			restOrder = backendConnector.patchToService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + restOrder.getId(),
					restOrder, RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getIdentifier());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new order version */
		String message = String.format(MSG_ORDER_UPDATED, MSG_ID_ORDER_UPDATED, restOrder.getIdentifier(), restOrder.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Delete the named processing order
	 * 
	 * @param deleteCommand the parsed "order delete" command
	 */
	private void deleteOrder(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrder({})", (null == deleteCommand ? "null" : deleteCommand.getName()));
		
		/* Get order ID from command parameters and retrieve the order using Order Manager service */
		RestOrder restOrder = retrieveOrderByIdentifierParameter(deleteCommand);
		if (null == restOrder)
			return;
		
		/* Check whether (database) order is in state "CLOSED", otherwise no user updates allowed */
		if (!OrderState.CLOSED.toString().equals(restOrder.getOrderState())) {
			System.err.println(String.format(MSG_INVALID_ORDER_STATE, MSG_ID_INVALID_ORDER_STATE,
					CMD_DELETE, restOrder.getOrderState(), OrderState.CLOSED.toString()));
			return;
		}
		
		/* Delete order using Order Manager service */
		try {
			backendConnector.deleteFromService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + restOrder.getId(),
						backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getId());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (Exception e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success */
		String message = String.format(MSG_ORDER_DELETED, MSG_ID_ORDER_DELETED, restOrder.getIdentifier());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Release the named processing order
	 * 
	 * @param approveCommand the parsed "order approve" command
	 */
	private void approveOrder(ParsedCommand approveCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> approveOrder({})", (null == approveCommand ? "null" : approveCommand.getName()));
		
		/* Get order ID from command parameters and retrieve the order using Order Manager service */
		RestOrder restOrder = retrieveOrderByIdentifierParameter(approveCommand);
		if (null == restOrder)
			return;
		
		/* Check whether (database) order is in state "INITIAL", otherwise approval not allowed */
		if (!OrderState.INITIAL.toString().equals(restOrder.getOrderState())) {
			System.err.println(String.format(MSG_INVALID_ORDER_STATE, MSG_ID_INVALID_ORDER_STATE,
					CMD_APPROVE, restOrder.getOrderState(), OrderState.INITIAL.toString()));
			return;
		}
		
		/* Update order state to "APPROVED" using Order Manager service */
		restOrder.setOrderState(OrderState.APPROVED.toString());
		try {
			restOrder = backendConnector.patchToService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + restOrder.getId(),
					restOrder, RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getIdentifier());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new order version */
		String message = String.format(MSG_ORDER_UPDATED, MSG_ID_ORDER_UPDATED, restOrder.getIdentifier(), restOrder.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Plan the named processing order
	 * 
	 * @param planCommand the parsed "order plan" command
	 */
	private void planOrder(ParsedCommand planCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> planOrder({})", (null == planCommand ? "null" : planCommand.getName()));
		
		/* Get order ID from command parameters and retrieve the order using Order Manager service */
		RestOrder restOrder = retrieveOrderByIdentifierParameter(planCommand);
		if (null == restOrder)
			return;
		
		/* Check whether (database) order is in state "APPROVED", otherwise planning not allowed */
		if (!OrderState.APPROVED.toString().equals(restOrder.getOrderState())) {
			System.err.println(String.format(MSG_INVALID_ORDER_STATE, MSG_ID_INVALID_ORDER_STATE,
					CMD_CANCEL, restOrder.getOrderState(), OrderState.APPROVED.toString()));
			return;
		}
		
		/* Plan jobs and job steps using Production Planner service */
		// TODO
		
		// --- Dummy: Just advance the state ---
		restOrder.setOrderState(OrderState.PLANNED.toString());
		try {
			restOrder = backendConnector.patchToService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + restOrder.getId(),
					restOrder, RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getIdentifier());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new order version */
		String message = String.format(MSG_ORDER_UPDATED, MSG_ID_ORDER_UPDATED, restOrder.getIdentifier(), restOrder.getVersion());
		logger.info(message);
		System.out.println(message);
		// --- END DUMMY ---
		
		/* Report success and list jobs and job steps (if user wants to see them) */

		// TODO
		
		System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, planCommand.getName()));
	}
	
	/**
	 * Release the named processing order
	 * 
	 * @param releaseCommand the parsed "order release" command
	 */
	private void releaseOrder(ParsedCommand releaseCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> releaseOrder({})", (null == releaseCommand ? "null" : releaseCommand.getName()));
		
		/* Get order ID from command parameters and retrieve the order using Order Manager service */
		RestOrder restOrder = retrieveOrderByIdentifierParameter(releaseCommand);
		if (null == restOrder)
			return;
		
		/* Check whether (database) order is in state "PLANNED", otherwise release not allowed */
		if (!OrderState.PLANNED.toString().equals(restOrder.getOrderState())) {
			System.err.println(String.format(MSG_INVALID_ORDER_STATE, MSG_ID_INVALID_ORDER_STATE,
					CMD_RELEASE, restOrder.getOrderState(), OrderState.PLANNED.toString()));
			return;
		}
		
		/* Update order state to "APPROVED" using Order Manager service */
		restOrder.setOrderState(OrderState.RELEASED.toString());
		try {
			restOrder = backendConnector.patchToService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + restOrder.getId(),
					restOrder, RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getIdentifier());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new order version */
		String message = String.format(MSG_ORDER_UPDATED, MSG_ID_ORDER_UPDATED, restOrder.getIdentifier(), restOrder.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Suspend the named processing order
	 * 
	 * @param suspendCommand the parsed "order suspend" command
	 */
	private void suspendOrder(ParsedCommand suspendCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspendOrder({})", (null == suspendCommand ? "null" : suspendCommand.getName()));
		
		/* Get order ID from command parameters and retrieve the order using Order Manager service */
		RestOrder restOrder = retrieveOrderByIdentifierParameter(suspendCommand);
		if (null == restOrder)
			return;
		
		/* Check whether (database) order is in state "RUNNING", otherwise suspending not allowed */
		if (!OrderState.RUNNING.toString().equals(restOrder.getOrderState())) {
			System.err.println(String.format(MSG_INVALID_ORDER_STATE, MSG_ID_INVALID_ORDER_STATE,
					CMD_CANCEL, restOrder.getOrderState(), OrderState.RUNNING.toString()));
			return;
		}
		
		
		/* Tell Production Planner service to suspend order processing, changing order state to "PLANNED" */
		// TODO

		// --- Dummy: Just advance the state ---
		restOrder.setOrderState(OrderState.PLANNED.toString());
		try {
			restOrder = backendConnector.patchToService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + restOrder.getId(),
					restOrder, RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getIdentifier());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new order version */
		String message = String.format(MSG_ORDER_UPDATED, MSG_ID_ORDER_UPDATED, restOrder.getIdentifier(), restOrder.getVersion());
		logger.info(message);
		System.out.println(message);
		// --- END DUMMY ---
		
		/* Report success */

		// TODO
		
		System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, suspendCommand.getName()));
	}
	
	/**
	 * Resume the named processing order (actually an alias for "release", but more intuitive after "suspend")
	 * 
	 * @param resumeCommand the parsed "order resume" command
	 */
	private void resumeOrder(ParsedCommand resumeCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeOrder({})", (null == resumeCommand ? "null" : resumeCommand.getName()));
		
		releaseOrder(resumeCommand);
	}
	
	/**
	 * Cancel the named processing order
	 * 
	 * @param cancelCommand the parsed "order cancel" command
	 */
	private void cancelOrder(ParsedCommand cancelCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancelOrder({})", (null == cancelCommand ? "null" : cancelCommand.getName()));
		
		/* Get order ID from command parameters and retrieve the order using Order Manager service */
		RestOrder restOrder = retrieveOrderByIdentifierParameter(cancelCommand);
		if (null == restOrder)
			return;
		
		/* Check whether (database) order is in state "PLANNED", otherwise cancel not allowed */
		if (!OrderState.PLANNED.toString().equals(restOrder.getOrderState())) {
			System.err.println(String.format(MSG_INVALID_ORDER_STATE, MSG_ID_INVALID_ORDER_STATE,
					CMD_CANCEL, restOrder.getOrderState(), OrderState.PLANNED.toString()));
			return;
		}
		
		/* Update order state to "FAILED" using Order Manager service */
		restOrder.setOrderState(OrderState.FAILED.toString());
		try {
			restOrder = backendConnector.patchToService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + restOrder.getId(),
					restOrder, RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getIdentifier());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new order version */
		String message = String.format(MSG_ORDER_UPDATED, MSG_ID_ORDER_UPDATED, restOrder.getIdentifier(), restOrder.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Close the named processing order
	 * 
	 * @param closeCommand the parsed "order close" command
	 */
	private void closeOrder(ParsedCommand closeCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> closeOrder({})", (null == closeCommand ? "null" : closeCommand.getName()));
		
		/* Get order ID from command parameters and retrieve the order using Order Manager service */
		RestOrder restOrder = retrieveOrderByIdentifierParameter(closeCommand);
		if (null == restOrder)
			return;
		
		/* Check whether (database) order is in state "COMPLETED" or "FAILED", otherwise close not allowed */
		if (!OrderState.FAILED.toString().equals(restOrder.getOrderState()) && !OrderState.COMPLETED.toString().equals(restOrder.getOrderState())) {
			System.err.println(String.format(MSG_INVALID_ORDER_STATE, MSG_ID_INVALID_ORDER_STATE,
					CMD_CLOSE, restOrder.getOrderState(), OrderState.COMPLETED.toString() + ", " + OrderState.FAILED.toString()));
			return;
		}
		
		/* Update order state to "APPROVED" using Order Manager service */
		restOrder.setOrderState(OrderState.CLOSED.toString());
		try {
			restOrder = backendConnector.patchToService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + restOrder.getId(),
					restOrder, RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getIdentifier());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new order version */
		String message = String.format(MSG_ORDER_UPDATED, MSG_ID_ORDER_UPDATED, restOrder.getIdentifier(), restOrder.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Reset the named processing order
	 * 
	 * @param resetCommand the parsed "order close" command
	 */
	private void resetOrder(ParsedCommand resetCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> resetOrder({})", (null == resetCommand ? "null" : resetCommand.getName()));
		
		/* Get order ID from command parameters and retrieve the order using Order Manager service */
		RestOrder restOrder = retrieveOrderByIdentifierParameter(resetCommand);
		if (null == restOrder)
			return;
		
		/* Check whether (database) order is in state "APPROVED" or "PLANNED", otherwise reset not allowed */
		if (!OrderState.APPROVED.toString().equals(restOrder.getOrderState()) && !OrderState.PLANNED.toString().equals(restOrder.getOrderState())) {
			System.err.println(String.format(MSG_INVALID_ORDER_STATE, MSG_ID_INVALID_ORDER_STATE,
					CMD_CLOSE, restOrder.getOrderState(), OrderState.APPROVED.toString() + ", " + OrderState.PLANNED.toString()));
			return;
		}
		
		/* Update order state to "INITIAL" using Order Manager service */
		restOrder.setOrderState(OrderState.INITIAL.toString());
		try {
			restOrder = backendConnector.patchToService(backendConfig.getOrderManagerUrl(), URI_PATH_ORDERS + "/" + restOrder.getId(),
					restOrder, RestOrder.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, restOrder.getIdentifier());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new order version */
		String message = String.format(MSG_ORDER_UPDATED, MSG_ID_ORDER_UPDATED, restOrder.getIdentifier(), restOrder.getVersion());
		logger.info(message);
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
		if (null == backendUserMgr.getUser()) {
			System.err.println(String.format(MSG_USER_NOT_LOGGED_IN, MSG_ID_USER_NOT_LOGGED_IN, command.getName()));
		}
		
		/* Check argument */
		if (!CMD_ORDER.equals(command.getName())) {
			System.err.println(String.format(MSG_INVALID_COMMAND_NAME, MSG_ID_INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given */
		if (null == command.getSubcommand() || null == command.getSubcommand().getName()) {
			System.err.println(String.format(MSG_SUBCOMMAND_MISSING, MSG_ID_SUBCOMMAND_MISSING, command.getName()));
			return;
		}
		
		/* Execute the subcommand */
		ParsedCommand subcommand = command.getSubcommand();
		switch(subcommand.getName()) {
		case CMD_CREATE:	createOrder(subcommand); break;
		case CMD_SHOW:		showOrder(subcommand); break;
		case CMD_UPDATE:	updateOrder(subcommand); break;
		case CMD_DELETE:	deleteOrder(subcommand); break;
		case CMD_APPROVE:	approveOrder(subcommand); break;
		case CMD_PLAN:		planOrder(subcommand); break;
		case CMD_RELEASE:	releaseOrder(subcommand); break;
		case CMD_SUSPEND:	suspendOrder(subcommand); break;
		case CMD_RESUME:	resumeOrder(subcommand); break;
		case CMD_CANCEL:	cancelOrder(subcommand); break;
		case CMD_CLOSE:		closeOrder(subcommand); break;
		case CMD_RESET:		resetOrder(subcommand); break;
		}
	}
}
