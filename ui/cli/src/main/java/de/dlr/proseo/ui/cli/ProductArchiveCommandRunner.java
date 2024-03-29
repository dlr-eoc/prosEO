/**
 * ProductArchiveCommandRunner.java
 * 
 * (C) 2023 DLR
 */
package de.dlr.proseo.ui.cli;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.enums.ArchiveType;
import de.dlr.proseo.model.rest.model.RestProductArchive;
import de.dlr.proseo.model.util.StringUtils;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO product archives (create, read, update, delete). 
 * All methods assume that before invocation a syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Denys Chaykovskiy
 */
@Component
public class ProductArchiveCommandRunner {

	private static final String OPTION_DELETE_ATTRIBUTES = "delete-attributes";
	private static final String OPTION_SHOW_PASSWORDS = "showPasswords";
	private static final String OPTION_VERBOSE = "verbose";
	private static final String OPTION_FORMAT = "format";
	private static final String OPTION_FILE = "file";
	
	/* General string constants */
	public static final String CMD_ARCHIVE = "archive";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";
	
	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_ARCHIVE_NAME = "Archive name (empty field cancels): ";
	private static final String PROMPT_ARCHIVE_CODE = "Archive code (empty field cancels): ";
	private static final String PROMPT_ARCHIVE_BASE_URI = "Base URI(empty field cancels): ";	
	private static final String PROMPT_ARCHIVE_CONTEXT = "Archive context (empty field means no context): ";
	private static final String PROMPT_ARCHIVE_TYPE = "Archive type (empty field cancels): ";
	private static final String PROMPT_ARCHIVE_TOKEN_REQUIRED = "Archive token required (empty field cancels): ";

	private static final String URI_PATH_ARCHIVES = "/archives";
	
	private static final String ARCHIVES = "archives";
	
	private static final String PWD_PLACEHOLDER = "********";

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
	private static ProseoLogger logger = new ProseoLogger(ProductArchiveCommandRunner.class);

	/**
	 * Retrieve the processing product archive with the given code, notifying the user of any errors occurring
	 * 
	 * @param archiveCode the archive code
	 * @return the requested archive or null, if the archive does not exist
	 */
	private RestProductArchive retrieveArchiveByCode(String archiveCode) {
		List<?> resultList = null;
		try {
			String encodedArchiveCode = "?code=" + URLEncoder.encode(archiveCode, Charset.defaultCharset());
			resultList = serviceConnection.getFromService(serviceConfig.getArchiveManagerUrl(),
					URI_PATH_ARCHIVES + encodedArchiveCode, List.class, loginManager.getUser(), loginManager.getPassword() );
			if (resultList.isEmpty()) {
				String message = logger.log(UIMessage.ARCHIVE_NOT_FOUND, archiveCode);
				System.err.println(message);
				return null;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				try {
					return mapper.convertValue(resultList.get(0), RestProductArchive.class);
				} catch (Exception e) {
					String message = logger.log(UIMessage.ARCHIVE_NOT_READABLE, archiveCode, e.getMessage());
					System.err.println(message);
					return null;
				}
			}
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = logger.log(UIMessage.ARCHIVE_NOT_FOUND, archiveCode);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ARCHIVES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, "(" + e.getRawStatusCode() + ") " + e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			String message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			System.err.println(message);
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	/**
	 * Sets rest archive attributes from changed archive
	 * 
	 * @param restArchive rest archive
	 * @param updatedArchive updated archive
	 */
	private void setRestArchiveAttributes(RestProductArchive restArchive, RestProductArchive updatedArchive, boolean isDeleteAttributes) {

		// mandatory fields	
		if (!StringUtils.isNullOrBlank(updatedArchive.getCode())) { 			
			restArchive.setCode(updatedArchive.getCode());
		}
		
		if (!StringUtils.isNullOrBlank(updatedArchive.getName())) { 			
			restArchive.setName(updatedArchive.getName());
		}
		
		if (null != updatedArchive.getArchiveType()) {			
			restArchive.setArchiveType(updatedArchive.getArchiveType());
		}
		
		if (!StringUtils.isNullOrBlank(updatedArchive.getBaseUri())) {
			restArchive.setBaseUri(updatedArchive.getBaseUri());
		}
		
		if (null != updatedArchive.getContext()) { 
			restArchive.setContext(updatedArchive.getContext());
		}
		if (null != updatedArchive.getTokenRequired()) {
			restArchive.setTokenRequired(updatedArchive.getTokenRequired());
		}
		
		if (null != updatedArchive.getAvailableProductClasses()) {
			restArchive.setAvailableProductClasses(updatedArchive.getAvailableProductClasses());			
		}
		
		// optional fields		
		if (isDeleteAttributes || !StringUtils.isNullOrBlank(updatedArchive.getTokenUri())) {
			restArchive.setTokenUri(updatedArchive.getTokenUri());
		}
		if (isDeleteAttributes || !StringUtils.isNullOrBlank(updatedArchive.getUsername())) {
			restArchive.setUsername(updatedArchive.getUsername());
		}
		if (isDeleteAttributes || !StringUtils.isNullOrBlank(updatedArchive.getPassword())) {
			restArchive.setPassword(updatedArchive.getPassword());
		}
		if (isDeleteAttributes || !StringUtils.isNullOrBlank(updatedArchive.getClientId())) {
			restArchive.setClientId(updatedArchive.getClientId());
		}
		if (isDeleteAttributes || !StringUtils.isNullOrBlank(updatedArchive.getClientSecret())) {
			restArchive.setClientSecret(updatedArchive.getClientSecret());
		}
		if (isDeleteAttributes || null != updatedArchive.getSendAuthInBody()) {
			restArchive.setSendAuthInBody(updatedArchive.getSendAuthInBody());
		}
	}

	/**
	 * Clones rest product archive 
	 * 
	 * @param source source rest product archive
	 * @return cloned rest product archive
	 */
	private RestProductArchive cloneRestProductArchive(RestProductArchive source) {
		
		if (source == null) return null;  
		
		RestProductArchive cloned = new RestProductArchive(); 
		
		cloned.setId(source.getId());
		
		cloned.setCode(source.getCode());
		cloned.setName(source.getName());
		cloned.setArchiveType(source.getArchiveType());
		
		cloned.setBaseUri(source.getBaseUri());
		cloned.setCode(source.getCode());
		cloned.setContext(source.getContext());
		
		cloned.setTokenRequired(source.getTokenRequired());
		cloned.setTokenUri(source.getTokenUri());
		cloned.setUsername(source.getUsername());
		
		cloned.setPassword(source.getPassword());
		cloned.setClientId(source.getClientId());
		cloned.setClientSecret(source.getClientSecret());
		
		cloned.setSendAuthInBody(source.getSendAuthInBody());
		
		List<String> clonedProductClasses = new ArrayList<String>();
		clonedProductClasses.addAll(source.getAvailableProductClasses());
		cloned.setAvailableProductClasses(clonedProductClasses);

		return cloned; 
	}
	
	/**
	 * Create a new product archive in prosEO; if the input is not from a file, the user will be prompted for mandatory 
	 * attributes not given on the command line
	 * 
	 * @param createCommand the parsed "archive create" command
	 */
	private void createArchive(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createArchive({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File archiveFile = null;
		String archiveFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				archiveFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				archiveFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read product archive file, if any */
		RestProductArchive restArchive = null;
		if (null == archiveFile) {
			restArchive = new RestProductArchive();
		} else {
			try {
				restArchive = CLIUtil.parseObjectFile(archiveFile, archiveFileFormat, RestProductArchive.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from product archive file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is archive name
				restArchive.setName(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restArchive, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Set default rest attribute values where appropriate */
		if (null == restArchive.getArchiveType()) {
			restArchive.setArchiveType(ArchiveType.AIP.toString());
		}
				
		if (null == restArchive.getTokenRequired()) {
			restArchive.setTokenRequired(false);			
		}
		
		if (null == restArchive.getSendAuthInBody()) {
			restArchive.setSendAuthInBody(false);			
		}
				
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		
		if (StringUtils.isNullOrBlank(restArchive.getName())) {		
			System.out.print(PROMPT_ARCHIVE_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restArchive.setName(response);
		}
		
		if (StringUtils.isNullOrBlank(restArchive.getCode())) {
			System.out.print(PROMPT_ARCHIVE_CODE);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restArchive.setCode(response);
		}
		
		if (StringUtils.isNullOrBlank(restArchive.getBaseUri())) {
			System.out.print(PROMPT_ARCHIVE_BASE_URI);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restArchive.setBaseUri(response);
		}
		
		if (null == restArchive.getContext()) {
			System.out.print(PROMPT_ARCHIVE_CONTEXT);
			String response = System.console().readLine();
			if (null == response) {
				response = "";
			}
			restArchive.setContext(response);
		}
							
		if (StringUtils.isNullOrBlank(restArchive.getArchiveType())) {
			System.out.print(PROMPT_ARCHIVE_TYPE);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restArchive.setArchiveType(response);
		}
		
		if (null == restArchive.getTokenRequired()) {
			System.out.print(PROMPT_ARCHIVE_TOKEN_REQUIRED);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restArchive.setTokenRequired(Boolean.getBoolean(response));
		}
		
		/* Create product archive */
		try {
			restArchive = serviceConnection.postToService(serviceConfig.getArchiveManagerUrl(), URI_PATH_ARCHIVES, 
					restArchive, RestProductArchive.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = logger.log(UIMessage.ARCHIVE_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ARCHIVES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving newly assigned product archive ID */
		String message = logger.log(UIMessage.ARCHIVE_CREATED,
				restArchive.getCode(), restArchive.getId());
		System.out.println(message);
	}
	
	/**
	 * Show the product archive specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "archive show" command
	 */
	@SuppressWarnings("unchecked")
	private void showArchive(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showArchive({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String archiveOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		Boolean isVerbose = false, showPasswords = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				archiveOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			case OPTION_SHOW_PASSWORDS:
				showPasswords = true;
				break;
			}
		}
		
		/* If archive name is set, show just the requested archive */
		if (!showCommand.getParameters().isEmpty()) {
			// Only archive name allowed as parameter
			RestProductArchive restArchive = retrieveArchiveByCode(showCommand.getParameters().get(0).getValue());
						
			if (null != restArchive) {
				
				// Hide passwords
				if (!showPasswords) {
					restArchive.setPassword(PWD_PLACEHOLDER);
					restArchive.setClientSecret(PWD_PLACEHOLDER);
				}
				
				// print the requested archive
				try {
					CLIUtil.printObject(System.out, restArchive, archiveOutputFormat);
				} catch (IllegalArgumentException e) {
					System.err.println(e.getMessage());
				} catch (IOException e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				}
			}
			return;
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_ARCHIVES;
		
		/* Get the archive information from the Archive Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getArchiveManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = logger.log(UIMessage.NO_ARCHIVES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ARCHIVES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		// Remove passwords unless explicitly requested
		if (!showPasswords) {
			// Must be a list of product archives
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					((Map<String, Object>) resultObject).put("password", PWD_PLACEHOLDER);
					((Map<String, Object>) resultObject).put("clientSecret", PWD_PLACEHOLDER);
				}
			}
		}
		
		/* Display the archive(s) found */
		if (isVerbose) {
			// Print archive details
			try {
				CLIUtil.printObject(System.out, resultList, archiveOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Print archive codes only; resultList must be a list of product archives
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					System.out.println(((Map<?, ?>) resultObject).get("code"));
				}
			}
		}
	}
	
	/**
	 * Update a product archive from a product archive file or from "attribute=value" pairs
	 * (overriding any product archive file entries)
	 * 
	 * @param updateCommand the parsed "archive update" command
	 */
	private void updateArchive(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateArchive({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File archiveFile = null;
		String archiveFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				archiveFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				archiveFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read product archive file, if any */
		RestProductArchive updatedArchive = null;
		if (null == archiveFile) {
			
			if (updateCommand.getParameters().size() == 0) {
				
				// TODO: Add processing if needed
				return; // no "code" parameter
			}
			
			String code = updateCommand.getParameters().get(0).getValue();
			updatedArchive = retrieveArchiveByCode(code);
			if (null == updatedArchive) {
				return; // no archive with "code" found
			}
			updatedArchive = this.cloneRestProductArchive(updatedArchive);
			
			// TODO: check if other parameters needed
			// updatedArchive = new RestProductArchive();
			// updatedArchive.setCode(code);
			// updatedArchive.setTokenRequired(false);
			// updatedArchive.setSendAuthInBody(false);
		} else {
			try {
				updatedArchive = CLIUtil.parseObjectFile(archiveFile, archiveFileFormat, RestProductArchive.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		RestProductArchive restArchive = retrieveArchiveByCode(updatedArchive.getCode());
		if (null == restArchive) {
			
			// TODO: Add processing if needed
			return;
		}
		
		/* Check command parameters (overriding values from product archive file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is archive code
				updatedArchive.setCode(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedArchive, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Read original archive from Archive Manager service */
		if (null == updatedArchive.getCode() || 0 == updatedArchive.getCode().length()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_ARCHIVE_CODE_GIVEN));
			return;
		}

		/* Compare attributes of database archive with updated archive */
		// No modification of ID, version and archive name allowed
		setRestArchiveAttributes(restArchive, updatedArchive, isDeleteAttributes);
		
		
		/* Update product archive using Archive Manager service */
		try {
			restArchive = serviceConnection.patchToService(serviceConfig.getArchiveManagerUrl(),
					URI_PATH_ARCHIVES + "/" + restArchive.getId(),
					restArchive, RestProductArchive.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = logger.log(UIMessage.ARCHIVE_NOT_FOUND_BY_ID, restArchive.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = logger.log(UIMessage.ARCHIVE_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ARCHIVES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new processor class version */
		String message = logger.log(UIMessage.ARCHIVE_UPDATED, restArchive.getId(), restArchive.getVersion());
		System.out.println(message);
	}

	/**
	 * Delete the given product archive
	 * 
	 * @param deleteCommand the parsed "archive delete" command
	 */
	private void deleteArchive(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteArchive({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get product archive name from command parameters */
		if (1 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_ARCHIVE_CODE_GIVEN));
			return;
		}
		String archiveCode = deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the product archive using Archive Manager service */
		RestProductArchive restArchive = retrieveArchiveByCode(archiveCode);
		if (null == restArchive) {
			return;
		}
		
		/* Delete archive using Archive Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getArchiveManagerUrl(),
					URI_PATH_ARCHIVES + "/" + restArchive.getId(), 
					loginManager.getUser(), loginManager.getPassword());
			
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = logger.log(UIMessage.ARCHIVE_NOT_FOUND_BY_ID, restArchive.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ARCHIVES, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = logger.log(UIMessage.ARCHIVE_DELETE_FAILED, archiveCode, e.getMessage());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success */
		String message = logger.log(UIMessage.ARCHIVE_DELETED, restArchive.getCode(), restArchive.getId());
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
		
		/* Check argument */
		if (!CMD_ARCHIVE.equals(command.getName())) {
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
				
		/* Execute the (sub-command */
		switch (subcommand.getName()) {
		case CMD_CREATE:	createArchive(subcommand); break;
		case CMD_SHOW:		showArchive(subcommand); break;
		case CMD_UPDATE:	updateArchive(subcommand); break;
		case CMD_DELETE:	deleteArchive(subcommand); break;
		default:
			System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
			return;
		}
	}
}
