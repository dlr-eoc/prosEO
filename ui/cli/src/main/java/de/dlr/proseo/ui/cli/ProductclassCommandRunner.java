/**
 * ProductclassCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.interfaces.rest.model.SelectionRuleString;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.ProductVisibility;
import de.dlr.proseo.model.rest.model.RestProductClass;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO product classes and selection rules (create, read, update, delete etc.). 
 * All methods assume that before invocation a syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class ProductclassCommandRunner {

	private static final String OPTION_DELETE_ATTRIBUTES = "delete-attributes";
	private static final String OPTION_VERBOSE = "verbose";
	private static final String OPTION_FORMAT = "format";
	private static final String OPTION_FILE = "file";
	/* General string constants */
	public static final String CMD_PRODUCTCLASS = "productclass";
	private static final String CMD_RULE = "rule";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";

	private static final String FORMAT_PLAIN = "PLAIN";
	
	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_PRODUCT_TYPE = "Product class name (empty field cancels): ";
	private static final String PROMPT_VISIBILITY = "Product visibility (I)nternal, R)estricted, P)ublic; empty field cancels): ";
	private static final String PROMPT_SELECTION_RULE = "Selection rule in Rule Language (empty field cancels, ^D (Linux) / ^Z (Windows) terminates): ";

	private static final String URI_PATH_PRODUCTCLASSES = "/productclasses";
	private static final String URI_PATH_SELECTIONRULES = "/selectionrules";
	
	private static final String PRODUCTCLASSES = "product classes";

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
	private static ProseoLogger logger = new ProseoLogger(ProcessorCommandRunner.class);

	/**
	 * Read several lines of text (until EOF/^D) from the console
	 * 
	 * @return a string containing the user input or an empty string, if an error occurred or the user cancelled the input
	 */
	private String readTextFromConsole() {
		Reader consoleReader = System.console().reader();
		StringBuilder response = new StringBuilder();
		char[] inputBuffer = new char[200];
		int numChars = 0;
		try {
			while (-1 != (numChars = consoleReader.read(inputBuffer))) {
				response.append(inputBuffer, 0, numChars);
			}
		} catch (IOException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			// return string so far
		}
		return response.toString();
	}
	
	/**
	 * Retrieves a product class for the current mission by product type
	 * 
	 * @param productType the product type
	 * @return a product class object or null, if no product class of the given type exists for the mission
	 */
	private RestProductClass retrieveProductClassByType(String productType) {
		if (logger.isTraceEnabled()) logger.trace(">>> retrieveProductClassByType({})", productType);
		
		/* Retrieve the product class using Product Class Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(), 
					URI_PATH_PRODUCTCLASSES + "?mission=" + loginManager.getMission() 
						+ "&productType=" + URLEncoder.encode(productType, Charset.defaultCharset()), 
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.PRODUCTCLASS_NOT_FOUND, productType);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return null;
		}
		if (resultList.isEmpty()) {
			String message = logger.log(UIMessage.PRODUCTCLASS_NOT_FOUND, productType);
			System.err.println(message);
			return null;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestProductClass restProductClass = mapper.convertValue(resultList.get(0), RestProductClass.class);
		return restProductClass;
	}
	
	/**
	 * Create a selection rule string from a plain text file with selection rules in Rule Language
	 * 
	 * @param selectionRuleFile the file name of the selection rule file
	 * @return a SelectionRuleString object with the content of the file and default mode null
	 * @throws FileNotFoundException if the given file does not exist or is not a readable file
	 * @throws IOException if a low-level I/O error occurs
	 */
	private SelectionRuleString readPlainSelectionRule(File selectionRuleFile) throws FileNotFoundException, IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> readPlainSelectionRule({})", selectionRuleFile);
		
		/* Read the file content into a rule string */
		BufferedReader in = new BufferedReader(new FileReader(selectionRuleFile));
		String ruleString = in.lines().collect(Collectors.joining());
		in.close();
		
		/* Create and return a selection rule string object */
		SelectionRuleString selectionRuleString = new SelectionRuleString();
		selectionRuleString.setSelectionRule(ruleString);
		selectionRuleString.setMode(null);
		
		return selectionRuleString;
	}

	/**
	 * Create a new product class; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "productclass create" command
	 */
	private void createProductClass(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProductClass({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File productClassFile = null;
		String productClassFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				productClassFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				productClassFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read product class file, if any */
		RestProductClass restProductClass = null;
		
		if (null == productClassFile) {
			restProductClass = new RestProductClass();
		} else {
			try {
				restProductClass = CLIUtil.parseObjectFile(productClassFile, productClassFileFormat, RestProductClass.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from product class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is product class name
				restProductClass.setProductType(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restProductClass, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restProductClass.getMissionCode() || 0 == restProductClass.getMissionCode().length()) {
			restProductClass.setMissionCode(loginManager.getMission());
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restProductClass.getProductType() || 0 == restProductClass.getProductType().length()) {
			System.out.print(PROMPT_PRODUCT_TYPE);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restProductClass.setProductType(response);
		}
		// Get product class visibility
		while (!Arrays.asList(ProductVisibility.INTERNAL.toString(), ProductVisibility.RESTRICTED.toString(), 
				ProductVisibility.PUBLIC.toString()).contains(restProductClass.getVisibility())) {
			System.out.print(PROMPT_VISIBILITY);
			String response = System.console().readLine().toUpperCase();
			switch (response) {
			case "I":	restProductClass.setVisibility(ProductVisibility.INTERNAL.toString()); break;
			case "R":	restProductClass.setVisibility(ProductVisibility.RESTRICTED.toString()); break;
			case "P":	restProductClass.setVisibility(ProductVisibility.PUBLIC.toString()); break;
			case "":
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			default:
				System.err.println(ProseoLogger.format(UIMessage.INVALID_VISIBILITY, response));
			}
		}
		
		/* Create product class */
		try {
			restProductClass = serviceConnection.postToService(serviceConfig.getProductClassManagerUrl(), URI_PATH_PRODUCTCLASSES, 
					restProductClass, RestProductClass.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.PRODUCTCLASS_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
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

		/* Report success, giving newly assigned product class ID */
		String message = logger.log(UIMessage.PRODUCTCLASS_CREATED,
				restProductClass.getProductType(), restProductClass.getId());
		System.out.println(message);
	}
	
	/**
	 * Show the product class specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "productclass show" command
	 */
	private void showProductClass(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showProductClass({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String productClassOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				productClassOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_PRODUCTCLASSES + "?mission=" + loginManager.getMission();
		
		if (!showCommand.getParameters().isEmpty()) {
			// Only product class name allowed as parameter
			requestURI += "&productType=" + URLEncoder.encode(showCommand.getParameters().get(0).getValue(), Charset.defaultCharset());
		}
		
		/* Get the product class information from the Product Class Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.NO_PRODUCTCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
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
			/* Display the product class(es) found */
			try {
				CLIUtil.printObject(System.out, resultList, productClassOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Must be a list of product classes
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(resultMap.get("productType"));
				}
			}
		}
	}
	
	/**
	 * Update a product class from a product class file or from "attribute=value" pairs (overriding any product class file entries)
	 * 
	 * @param updateCommand the parsed "productclass update" command
	 */
	private void updateProductClass(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateProductClass({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File productClassFile = null;
		String productClassFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				productClassFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				productClassFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read product class file, if any */
		RestProductClass updatedProductClass = null;
		if (null == productClassFile) {
			updatedProductClass = new RestProductClass();
		} else {
			try {
				updatedProductClass = CLIUtil.parseObjectFile(productClassFile, productClassFileFormat, RestProductClass.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from product class file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is product class name
				updatedProductClass.setProductType(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedProductClass, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Read original product class from Product Manager service */
		if (null == updatedProductClass.getProductType() || 0 == updatedProductClass.getProductType().length()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_PRODCLASS_NAME_GIVEN));
			return;
		}
		RestProductClass restProductClass = retrieveProductClassByType(updatedProductClass.getProductType());
		if (null == restProductClass) {
			// Already handled
			return;
		}

		/* Compare attributes of database product class with updated product class */
		// No modification of ID, version, mission code or product class name allowed
		if (isDeleteAttributes || (null != updatedProductClass.getTypeDescription() && 0 != updatedProductClass.getTypeDescription().length())) {
			restProductClass.setTypeDescription(updatedProductClass.getTypeDescription());
		}
		if (null != updatedProductClass.getProcessingLevel() && !updatedProductClass.getProcessingLevel().isBlank()) {
			restProductClass.setProcessingLevel(updatedProductClass.getProcessingLevel());
		}
		if (null != updatedProductClass.getVisibility() && !updatedProductClass.getVisibility().isBlank()) {
			restProductClass.setVisibility(updatedProductClass.getVisibility());
		}
		if (isDeleteAttributes || (null != updatedProductClass.getDefaultSlicingType() && !updatedProductClass.getDefaultSlicingType().isBlank())) {
			restProductClass.setDefaultSlicingType(updatedProductClass.getDefaultSlicingType());
		}
		if (OrderSlicingType.TIME_SLICE.toString().equals(restProductClass.getDefaultSlicingType())) {
			if (null != updatedProductClass.getDefaultSliceDuration()) {
				restProductClass.setDefaultSliceDuration(updatedProductClass.getDefaultSliceDuration());
			}
		} else {
			restProductClass.setDefaultSliceDuration(null);
		}
		if (isDeleteAttributes || (null != updatedProductClass.getComponentClasses() && !updatedProductClass.getComponentClasses().isEmpty())) {
			restProductClass.getComponentClasses().clear();
			restProductClass.getComponentClasses().addAll(updatedProductClass.getComponentClasses());
		}
		if (isDeleteAttributes || (null != updatedProductClass.getEnclosingClass() && !updatedProductClass.getEnclosingClass().isBlank())) {
			restProductClass.setEnclosingClass(updatedProductClass.getEnclosingClass());
		}
		if (isDeleteAttributes || (null != updatedProductClass.getProcessorClass() && !updatedProductClass.getProcessorClass().isBlank())) {
			restProductClass.setProcessorClass(updatedProductClass.getProcessorClass());
		}
		if (isDeleteAttributes || (null != updatedProductClass.getProductFileTemplate() && !updatedProductClass.getProductFileTemplate().isBlank())) {
			restProductClass.setProductFileTemplate(updatedProductClass.getProductFileTemplate());
		}
		
		/* Update product class using Product Class Manager service */
		try {
			restProductClass = serviceConnection.patchToService(serviceConfig.getProductClassManagerUrl(),
					URI_PATH_PRODUCTCLASSES + "/" + restProductClass.getId(),
					restProductClass, RestProductClass.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.PRODUCTCLASS_NOT_FOUND_BY_ID, restProductClass.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.PROCESSORCLASS_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
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
		
		/* Report success, giving new product class version */
		String message = logger.log(UIMessage.PRODUCTCLASS_UPDATED, restProductClass.getId(), restProductClass.getVersion());
		System.out.println(message);
	}
	
	/**
	 * Delete the given product class
	 * 
	 * @param deleteCommand the parsed "productclass delete" command
	 */
	private void deleteProductClass(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductClass({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get product class name from command parameters */
		if (deleteCommand.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_PRODCLASS_NAME_GIVEN));
			return;
		}
		String productType = deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the product class using Product Class Manager service */
		RestProductClass restProductClass = retrieveProductClassByType(productType);
		if (null == restProductClass) {
			// Already handled
			return;
		}
		
		/* Delete processor class using Product Class Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getProductClassManagerUrl(),
					URI_PATH_PRODUCTCLASSES + "/" + restProductClass.getId(), 
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.PRODUCTCLASS_NOT_FOUND_BY_ID, restProductClass.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = ProseoLogger.format(UIMessage.PRODUCTCLASS_DELETE_FAILED, productType, e.getMessage());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success */
		String message = logger.log(UIMessage.PRODUCTCLASS_DELETED, restProductClass.getId());
		System.out.println(message);
	}

	/**
	 * Create a new set of selection rules for the given target product class; if the input is not from a file, 
	 * the user will be prompted for mandatory attributes not given on the command line (including the rule itself, 
	 * if PLAIN format is specified)
	 * 
	 * @param createCommand the parsed "productclass rule create" command
	 */
	@SuppressWarnings("unchecked")
	private void createSelectionRule(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createSelectionRule({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File selectionRuleFile = null;
		String selectionRuleFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				selectionRuleFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				selectionRuleFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read selection rule file, if any */
		List<SelectionRuleString> selectionRuleList = new ArrayList<>();
		if (null == selectionRuleFile) {
			selectionRuleFileFormat = FORMAT_PLAIN; // No file given, so we assume interactive rule language input
			SelectionRuleString restSelectionRule = new SelectionRuleString();
			selectionRuleList.add(restSelectionRule);
		} else if (FORMAT_PLAIN.equals(selectionRuleFileFormat)) {
			try {
				SelectionRuleString restSelectionRule = readPlainSelectionRule(selectionRuleFile);
				selectionRuleList.add(restSelectionRule);
			} catch (FileNotFoundException e) {
				String message = logger.log(UIMessage.FILE_NOT_FOUND, selectionRuleFile);
					System.err.println(message);
				return;
			} catch (IOException e) {
				String message = logger.log(UIMessage.EXCEPTION, e.getMessage());
					System.err.println(message);
				return;
			}
		} else {
			try {
				selectionRuleList.addAll(CLIUtil.parseObjectFile(selectionRuleFile, selectionRuleFileFormat, List.class));
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from selection rule class file) */
		String targetClass = null;
		List<String> attributeList = new ArrayList<>();
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is target product class
				targetClass = param.getValue();
			} else {
				// Remaining parameters are "attribute=value" parameters
				attributeList.add(param.getValue());
			}
		}
		
		/* Read original product class from Product Manager service */
		if (null == targetClass || 0 == targetClass.length()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_PRODCLASS_NAME_GIVEN));
			return;
		}
		RestProductClass restProductClass = retrieveProductClassByType(targetClass);
		if (null == restProductClass) {
			// Already handled
			return;
		}
		
		/* Check input data for completeness */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		ObjectMapper mapper = new ObjectMapper();
		for (SelectionRuleString restSelectionRule: selectionRuleList) {
			
			/* Set values from attribute parameters */
			for (String attributeParam: attributeList) {
				try {
					CLIUtil.setAttribute(restSelectionRule, attributeParam);
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
			/* Set missing attributes to default values where possible */
			if (null != restSelectionRule.getMode() && restSelectionRule.getMode().isBlank()) {
				restSelectionRule.setMode(null);
			}
			
			/* Prompt user for missing mandatory attributes */
			if (null == restSelectionRule.getSelectionRule() || restSelectionRule.getSelectionRule().isBlank()) {
				System.out.println(PROMPT_SELECTION_RULE);
				String response = readTextFromConsole();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				restSelectionRule.setSelectionRule(response);
			} 
		}
		
		/* Create selection rule */
		try {
			restProductClass = serviceConnection.postToService(serviceConfig.getProductClassManagerUrl(),
					URI_PATH_PRODUCTCLASSES + "/" + restProductClass.getId() + URI_PATH_SELECTIONRULES, 
					selectionRuleList, RestProductClass.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.SELECTION_RULE_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
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
		String message = logger.log(UIMessage.SELECTION_RULES_CREATED, selectionRuleList.size(), targetClass);
		System.out.println(message);
	}

	/**
	 * Show the selection rules for the given product class
	 * 
	 * @param showCommand the parsed "productclass rule show" command
	 */
	private void showSelectionRule(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showSelectionRule({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String selectionRuleOutputFormat = FORMAT_PLAIN;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				selectionRuleOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
			}
		}
		
		/* Get product class name from command parameters */
		if (showCommand.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_PRODCLASS_NAME_GIVEN));
			return;
		}
		String targetClass = showCommand.getParameters().get(0).getValue();
		String sourceClass = (1 < showCommand.getParameters().size() ? showCommand.getParameters().get(1).getValue() : null);
		
		/* Retrieve the product class using Product Class Manager service */
		RestProductClass restProductClass = retrieveProductClassByType(targetClass);
		if (null == restProductClass) {
			// Already handled
			return;
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_PRODUCTCLASSES + "/" + restProductClass.getId() + URI_PATH_SELECTIONRULES;
		if (null != sourceClass) {
			requestURI += "?sourceClass=" + sourceClass;
		}
		
		/* Get the selection rule information from the Product Class Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = null == sourceClass ?
						ProseoLogger.format(UIMessage.NO_SELECTION_RULES_FOUND, targetClass) :
						ProseoLogger.format(UIMessage.NO_SELECTION_RULES_FOUND_FOR_SOURCE, targetClass, sourceClass);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
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
		
		/* Display the processor class(es) found */
		if (FORMAT_PLAIN.equals(selectionRuleOutputFormat) || !isVerbose) {
			ObjectMapper mapper = new ObjectMapper();
			for (Object resultObject: resultList) {
				SelectionRuleString selectionRule = mapper.convertValue(resultObject, SelectionRuleString.class);
				System.out.println("ID: " + selectionRule.getId());
				System.out.println(selectionRule.getSelectionRule());
				System.out.println(String.format("(Mode: %s, configuredProcessors: %s)\n", selectionRule.getMode(), selectionRule.getConfiguredProcessors().toString()));
			}
		} else {
			try {
				CLIUtil.printObject(System.out, resultList, selectionRuleOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
	}
	
	/**
	 * Update a selection rule from a selection rule file or from "attribute=value" pairs (overriding any selection rule file entries)
	 * 
	 * @param updateCommand the parsed "productclass rule update" command
	 */
	private void updateSelectionRule(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateSelectionRule({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File selectionRuleFile = null;
		String selectionRuleFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				selectionRuleFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				selectionRuleFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read selection rule file, if any */
		SelectionRuleString updatedSelectionRule = null;
		if (null == selectionRuleFile) {
			selectionRuleFileFormat = FORMAT_PLAIN; // No file given, so we assume interactive rule language input
			updatedSelectionRule = new SelectionRuleString();
		} else if (FORMAT_PLAIN.equals(selectionRuleFileFormat)) {
			try {
				updatedSelectionRule = readPlainSelectionRule(selectionRuleFile);
			} catch (FileNotFoundException e) {
				String message = logger.log(UIMessage.FILE_NOT_FOUND, selectionRuleFile);
					System.err.println(message);
				return;
			} catch (IOException e) {
				String message = logger.log(UIMessage.EXCEPTION, e.getMessage());
					System.err.println(message);
				return;
			}
		} else {
			try {
				updatedSelectionRule = CLIUtil.parseObjectFile(selectionRuleFile, selectionRuleFileFormat, SelectionRuleString.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from processor class file) */
		String targetClass = null;
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is target product class
				targetClass = param.getValue();
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedSelectionRule, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Retrieve the product class using Product Class Manager service */
		if (null == targetClass || 0 == targetClass.length()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_PRODCLASS_NAME_GIVEN));
			return;
		}
		RestProductClass restProductClass = retrieveProductClassByType(targetClass);
		if (null == restProductClass) {
			// Already handled
			return;
		}
		
		/* Read original selection rule from Product Class Manager service */
		String requestURI = URI_PATH_PRODUCTCLASSES + "/" + restProductClass.getId() + URI_PATH_SELECTIONRULES;
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.NO_SELECTION_RULES_FOUND, targetClass);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
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
		if (resultList.isEmpty()) {
			String message = logger.log(UIMessage.NO_SELECTION_RULES_FOUND, targetClass);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		SelectionRuleString restSelectionRule = null;
		
		/* If there is more than one selection rule, prompt the user for a choice */
		if (1 == resultList.size()) {
			restSelectionRule = mapper.convertValue(resultList.get(0), SelectionRuleString.class);
		} else {
			/* Print a short form of all selection rules */
			for (int i = 0; i < resultList.size(); ++i) {
				SelectionRuleString selectionRule = mapper.convertValue(resultList.get(i), SelectionRuleString.class);
				System.out.println(String.format("[%2d] %s", i+1, selectionRule.getSelectionRule()));
				System.out.println(String.format("     (Mode: %s, configured processors: %s)\n", selectionRule.getMode(), selectionRule.getConfiguredProcessors().toString()));
			}
			while (null == restSelectionRule) {
				System.out.print("Select rule (empty field cancels): ");
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				try {
					Integer selectedIndex = Integer.parseInt(response);
					if (1 > selectedIndex || resultList.size() < selectedIndex) {
						System.err.println(ProseoLogger.format(UIMessage.INPUT_OUT_OF_BOUNDS, selectedIndex, 1, resultList.size()));
					} else {
						restSelectionRule = mapper.convertValue(resultList.get(selectedIndex - 1), SelectionRuleString.class);
					}
				} catch (NumberFormatException e) {
					System.err.println(ProseoLogger.format(UIMessage.INPUT_NOT_NUMERIC, response));
					continue;
				}
			}
		}

		/* Compare attributes of database selection rule with updated selection rule */
		// No modification of ID, version and target product class allowed
		if (null != updatedSelectionRule.getSelectionRule()) { // not null
			restSelectionRule.setSelectionRule(updatedSelectionRule.getSelectionRule());
		}
		if (null != updatedSelectionRule.getMode()) { // not null
			restSelectionRule.setMode(updatedSelectionRule.getMode());
		}
		if (null != updatedSelectionRule.getConfiguredProcessors() && (isDeleteAttributes || !updatedSelectionRule.getConfiguredProcessors().isEmpty())) {
			restSelectionRule.getConfiguredProcessors().clear();
			restSelectionRule.getConfiguredProcessors().addAll(updatedSelectionRule.getConfiguredProcessors());
		}
		if (FORMAT_PLAIN.equals(selectionRuleFileFormat) &&
				(null == updatedSelectionRule.getSelectionRule() || 0 == updatedSelectionRule.getSelectionRule().length())) {
			System.out.println(PROMPT_SELECTION_RULE);
			String response = readTextFromConsole();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restSelectionRule.setSelectionRule(response);
		} 
		
		/* Update selection rule using Product Class Manager service */
		try {
			restSelectionRule = serviceConnection.patchToService(serviceConfig.getProductClassManagerUrl(),
					URI_PATH_PRODUCTCLASSES + "/" + restProductClass.getId() 
					+ URI_PATH_SELECTIONRULES + "/" + restSelectionRule.getId(),
					restSelectionRule, SelectionRuleString.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.SELECTION_RULE_NOT_FOUND_BY_ID, restSelectionRule.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.SELECTION_RULE_DATA_INVALID,  e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
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
		
		/* Report success, giving new selection rule version */
		String message = logger.log(UIMessage.SELECTION_RULE_UPDATED, restSelectionRule.getId(), restSelectionRule.getVersion());
		System.out.println(message);
	}
	
	/**
	 * Delete the given selection rule
	 * 
	 * @param deleteCommand the parsed "productclass rule delete" command
	 */
	private void deleteSelectionRule(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteSelectionRule({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get processor name from command parameters */
		if (1 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_PRODCLASS_NAME_GIVEN));
			return;
		}
		if (2 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_RULEID_GIVEN));
			return;
		}
		String targetClass = deleteCommand.getParameters().get(0).getValue();
		long ruleId = 0;
		try {
			ruleId = Long.parseLong(deleteCommand.getParameters().get(1).getValue());
		} catch (NumberFormatException e1) {
			System.err.println(ProseoLogger.format(UIMessage.RULEID_NOT_NUMERIC, deleteCommand.getParameters().get(1).getValue()));
			return;
		}
		
		/* Retrieve the product class using Product Class Manager service */
		RestProductClass restProductClass = retrieveProductClassByType(targetClass);
		if (null == restProductClass) {
			// Already handled
			return;
		}
		
		/* Delete selection rule using ProductClass Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getProductClassManagerUrl(),
					URI_PATH_PRODUCTCLASSES + "/" + restProductClass.getId() 
					+ URI_PATH_SELECTIONRULES + "/" + ruleId, 
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.SELECTION_RULE_NOT_FOUND_BY_ID, ruleId);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = ProseoLogger.format(UIMessage.SELECTION_RULE_DELETE_FAILED, ruleId, targetClass, e.getMessage());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success */
		String message = logger.log(UIMessage.SELECTION_RULE_DELETED, ruleId);
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
		if (!CMD_PRODUCTCLASS.equals(command.getName())) {
			System.err.println(ProseoLogger.format(UIMessage.INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given */
		ParsedCommand subcommand = command.getSubcommand();

		if (null == subcommand) {
			System.err.println(ProseoLogger.format(UIMessage.SUBCOMMAND_MISSING, command.getName()));
			return;
		}

		/* Check for subcommand help request */
		if (subcommand.isHelpRequested()) {
			subcommand.getSyntaxCommand().printHelp(System.out);
			return;
		}
				
		/* Make sure a sub-subcommand is given for "rule" */
		ParsedCommand subsubcommand = subcommand.getSubcommand();
		if (CMD_RULE.equals(subcommand.getName()) && null == subcommand.getSubcommand()) {
			System.err.println(ProseoLogger.format(UIMessage.SUBCOMMAND_MISSING, subcommand.getName()));
			return;
		}

		/* Check for sub-subcommand help request */
		if (null != subsubcommand && subsubcommand.isHelpRequested()) {
			subsubcommand.getSyntaxCommand().printHelp(System.out);
			return;
		} 
		
		/* Execute the (sub-)sub-command */
		switch (subcommand.getName()) {
		// Handle commands for product classes
		case CMD_CREATE:	createProductClass(subcommand); break;
		case CMD_SHOW:		showProductClass(subcommand); break;
		case CMD_UPDATE:	updateProductClass(subcommand); break;
		case CMD_DELETE:	deleteProductClass(subcommand); break;
		case CMD_RULE:
			// Handle commands for selection rules
			switch (subsubcommand.getName()) {
			case CMD_CREATE:	createSelectionRule(subsubcommand); break;
			case CMD_SHOW:		showSelectionRule(subsubcommand); break;
			case CMD_UPDATE:	updateSelectionRule(subsubcommand); break;
			case CMD_DELETE:	deleteSelectionRule(subsubcommand); break;
			default:
				System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, 
						command.getName() + " " + subcommand.getName() + " " + subsubcommand.getName()));
				return;
			}
			break;
		default:
			System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
			return;
		}
	}
}
