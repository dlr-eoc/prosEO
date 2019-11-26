/**
 * OrderCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
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
	private static final String MSG_INVALID_ORBIT_NUMBER = "(E%d) Orbit number %s not numeric";

	private static final String MSG_OPERATION_CANCELLED = "(I%d) Operation cancelled";
	private static final String MSG_ORDER_CREATED = "(I%d) Order with identifier %s created (database ID %d)";
	
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
	
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("UTC"));


	/** The configuration object for the prosEO User Interface */
	@Autowired
	private BackendConfiguration backendConfig;
	
	/** The user manager used by all command runners */
	@Autowired
	private BackendUserManager backendUserMgr;
	
	/** The configuration object for the prosEO CLI */
//	@Autowired
//	private CLIConfiguration config;
	
	/** The connector service to the prosEO backend services prosEO */
	@Autowired
	private BackendConnectionService backendConnector;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderCommandRunner.class);
	
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
		while (OrderSlicingType.TIME_SLICE.toString().equals(restOrder.getSlicingType()) 
				&& (null == restOrder.getSliceDuration() || 0 == restOrder.getSliceDuration())) {
			System.out.println(PROMPT_SLICE_DURATION);
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
		if (OrderSlicingType.TIME_SLICE.toString().equals(restOrder.getSlicingType()) 
				|| OrderSlicingType.CALENDAR_DAY.toString().equals(restOrder.getSlicingType())) {
			while (null == restOrder.getStartTime()) {
				System.out.println(PROMPT_START_TIME);
				String response = System.console().readLine();
				if ("".equals(response)) {
					System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
					return;
				}
				try {
					restOrder.setStartTime(java.util.Date.from(Instant.parse(response + "Z"))); // no time zone in input expected
				} catch (DateTimeParseException e) {
					System.err.println(String.format(MSG_INVALID_SLICE_DURATION, MSG_ID_INVALID_SLICE_DURATION, response));
				}
			}
			while (null == restOrder.getStopTime()) {
				System.out.println(PROMPT_STOP_TIME);
				String response = System.console().readLine();
				if ("".equals(response)) {
					System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
					return;
				}
				try {
					restOrder.setStopTime(java.util.Date.from(Instant.parse(response + "Z"))); // no time zone in input expected
				} catch (DateTimeParseException e) {
					System.err.println(String.format(MSG_INVALID_SLICE_DURATION, MSG_ID_INVALID_SLICE_DURATION, response));
				}
			} 
		}
		if (OrderSlicingType.ORBIT.toString().equals(restOrder.getSlicingType())) {
			ORBITS:
			while (restOrder.getOrbits().isEmpty()) {
				System.out.println(PROMPT_SPACECRAFT);
				String spacecraft = System.console().readLine();
				if ("".equals(spacecraft)) {
					System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
					return;
				}
				System.out.println(PROMPT_ORBIT_LIST);
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
		if (null == restOrder.getProcessingMode() || 0 == restOrder.getProcessingMode().length()) {
			System.out.print(PROMPT_PROCESSING_MODE);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restOrder.setProcessingMode(response);
		}
		if (null == restOrder.getOutputFileClass() || 0 == restOrder.getOutputFileClass().length()) {
			System.out.print(PROMPT_FILE_CLASS);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restOrder.setOutputFileClass(response);
		}
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
			restOrder = backendConnector.postToService(backendConfig.getOrderManagerUrl(), "/orders", 
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
		String requestURI = "/orders?mission=" + backendUserMgr.getMission();
		
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
		@SuppressWarnings("rawtypes")
		List resultList = null;
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
		for (Object result: resultList) {
			ObjectMapper mapper = new ObjectMapper();
			RestOrder restOrder = mapper.convertValue(result, RestOrder.class);
			
			// TODO Format output
			System.out.println(restOrder);
		}
	}
	
	/**
	 * Update a processing order from an order file or from "attribute=value" pairs (overriding any order file)
	 * 
	 * @param updateCommand the parsed "order update" command
	 */
	private void updateOrder(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateOrder({})", (null == updateCommand ? "null" : updateCommand.getName()));
		
		/* Check command options */
		
		/* Read original order from Order Manager service */
		
		/* Read order file, if any, and update order attributes */
		
		/* Check command parameters (overriding values from order file) */
		
		/* Update order using Order Manager service */
		
		/* Report success */

		// TODO
		
		System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, updateCommand.getName()));
	}
	
	/**
	 * Delete the named processing order
	 * 
	 * @param deleteCommand the parsed "order delete" command
	 */
	private void deleteOrder(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrder({})", (null == deleteCommand ? "null" : deleteCommand.getName()));
		
		/* Get order ID from command parameters */
		
		/* Delete order using Order Manager service */
		
		/* Report success */

		// TODO
		
		System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, deleteCommand.getName()));
	}
	
	/**
	 * Release the named processing order
	 * 
	 * @param approveCommand the parsed "order approve" command
	 */
	private void approveOrder(ParsedCommand approveCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> approveOrder({})", (null == approveCommand ? "null" : approveCommand.getName()));
		
		/* Get order ID from command parameters */
		
		/* Read order from Order Manager service and check order state (must be "INITIAL") */
		
		/* Update order state to "APPROVED" using Order Manager service */
		
		/* Report success */

		// TODO
		
		System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, approveCommand.getName()));
	}
	
	/**
	 * Plan the named processing order
	 * 
	 * @param planCommand the parsed "order plan" command
	 */
	private void planOrder(ParsedCommand planCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> planOrder({})", (null == planCommand ? "null" : planCommand.getName()));
		
		/* Get order ID from command parameters */
		
		/* Read order from Order Manager service and check order state (must be "APPROVED") */
		
		/* Plan jobs and job steps using Production Planner service */
		
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
		
		/* Get order ID from command parameters */
		
		/* Read order from Order Manager service and check order state (must be "PLANNED") */
		
		/* Update order state to "RELEASED" using Order Manager service */
		
		/* Report success */

		// TODO
		
		System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, releaseCommand.getName()));
	}
	
	/**
	 * Suspend the named processing order
	 * 
	 * @param suspendCommand the parsed "order suspend" command
	 */
	private void suspendOrder(ParsedCommand suspendCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspendOrder({})", (null == suspendCommand ? "null" : suspendCommand.getName()));
		
		/* Get order ID from command parameters */
		
		/* Read order from Order Manager service and check order state (must be "RUNNING") */
		
		/* Tell Production Planner service to suspend order processing, changing order state to "PLANNED" */
		
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
		
		/* Get order ID from command parameters */
		
		/* Read order from Order Manager service and check order state (must be "PLANNED") */
		
		/* Update order state to "FAILED" using Order Manager service */
		
		/* Report success */

		// TODO
		
		System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, cancelCommand.getName()));
	}
	
	/**
	 * Close the named processing order
	 * 
	 * @param closeCommand the parsed "order close" command
	 */
	private void closeOrder(ParsedCommand closeCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> closeOrder({})", (null == closeCommand ? "null" : closeCommand.getName()));
		
		/* Get order ID from command parameters */
		
		/* Read order from Order Manager service and check order state */
		
		/* Update order state to "CLOSED" using Order Manager service */
		
		/* Report success */

		// TODO
		
		System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, closeCommand.getName()));
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
		}
	}
}
