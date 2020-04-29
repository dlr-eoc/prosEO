/**
 * JobCommandRunner.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.rest.model.JobState;
import de.dlr.proseo.model.rest.model.JobStepState;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;

/**
 * Run commands for managing prosEO orders (create, read, update, delete etc.). All methods assume that before invocation a
 * syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class JobCommandRunner {

	/* General string constants */
	public static final String CMD_JOB = "job";
	private static final String CMD_STEP = "step";
	private static final String CMD_SHOW = "show";
	private static final String CMD_SUSPEND = "suspend";
	private static final String CMD_RESUME = "resume";
	private static final String CMD_CANCEL = "cancel";
	private static final String CMD_RETRY = "retry";

	private static final String URI_PATH_ORDERS = "/orders";
	private static final String URI_PATH_JOBS = "/jobs";
	private static final String URI_PATH_JOBSTEPS = "/jobsteps";
	private static final String URI_PATH_JOB_RESUME = URI_PATH_JOBS + "/resume";
	private static final String URI_PATH_JOB_SUSPEND = URI_PATH_JOBS + "/suspend";
	private static final String URI_PATH_JOB_CANCEL = URI_PATH_JOBS + "/cancel";
	private static final String URI_PATH_JOB_RETRY = URI_PATH_JOBS + "/retry";
	private static final String URI_PATH_JOBSTEP_RESUME = URI_PATH_JOBSTEPS + "/resume";
	private static final String URI_PATH_JOBSTEP_SUSPEND = URI_PATH_JOBSTEPS + "/suspend";
	private static final String URI_PATH_JOBSTEP_CANCEL = URI_PATH_JOBSTEPS + "/cancel";
	private static final String URI_PATH_JOBSTEP_RETRY = URI_PATH_JOBSTEPS + "/retry";
	
	private static final String JOBS = "jobs";
	private static final String JOBSTEPS = "job steps";
	
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
	private static Logger logger = LoggerFactory.getLogger(JobCommandRunner.class);
	
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
			System.err.println(uiMsg(MSG_ID_NO_IDENTIFIER_GIVEN));
			return null;
		}
		String orderIdentifier = command.getParameters().get(0).getValue();
		
		/* Retrieve the order using Order Manager service */
		try {
			List<?> resultList = null;
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					URI_PATH_ORDERS + "?identifier=" + orderIdentifier,
					List.class, loginManager.getUser(), loginManager.getPassword());
			if (resultList.isEmpty()) {
				throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
			} else {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.convertValue(resultList.get(0), RestOrder.class);
			}
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_ORDER_NOT_FOUND, orderIdentifier);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return null;
		}
	}
	
	/**
	 * Retrieve a job by database ID, which is the first parameter of the given command.
	 * Outputs all necessary messages to the log and the user.
	 * 
	 * @param command the command containing the identifier parameter
	 * @return a job object or null, if none was found or an error occurred
	 */
	private RestJob retrieveJobByIdParameter(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> retrieveJobByIdParameter({})", (null == command ? "null" : command.getName()));

		/* Get job ID from command parameters */
		if (command.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_JOB_DBID_GIVEN));
			return null;
		}
		String jobId = command.getParameters().get(0).getValue();
		
		/* Retrieve the job using Production Planner service */
		try {
			RestJob restJob = serviceConnection.getFromService(serviceConfig.getProductionPlannerUrl(),
					URI_PATH_JOBS + "/" + jobId,
					RestJob.class, loginManager.getUser(), loginManager.getPassword());
			if (null == restJob) {
				throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
			}
			return restJob;
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOB_NOT_FOUND, jobId);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return null;
		}
	}
	
	/**
	 * Retrieve a job step by database ID, which is the first parameter of the given command.
	 * Outputs all necessary messages to the log and the user.
	 * 
	 * @param command the command containing the identifier parameter
	 * @return a job step object or null, if none was found or an error occurred
	 */
	private RestJobStep retrieveJobStepByIdParameter(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> retrieveJobStepByIdParameter({})", (null == command ? "null" : command.getName()));

		/* Get job step ID from command parameters */
		if (command.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_JOBSTEP_DBID_GIVEN));
			return null;
		}
		String jobStepId = command.getParameters().get(0).getValue();
		
		/* Retrieve the job using Production Planner service */
		try {
			RestJobStep restJobStep = serviceConnection.getFromService(serviceConfig.getProductionPlannerUrl(),
					URI_PATH_JOBSTEPS + "/" + jobStepId,
					RestJobStep.class, loginManager.getUser(), loginManager.getPassword());
			if (null == restJobStep) {
				throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
			}
			return restJobStep;
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOBSTEP_NOT_FOUND, jobStepId);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBSTEPS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return null;
		}
	}
	
	/**
	 * Show the job specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "job show" command
	 */
	private void showJob(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showJob({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String jobOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case "format":
				jobOutputFormat = option.getValue().toUpperCase();
				break;
			case "verbose":
				isVerbose = true;
				break;
			}
		}
		
		/* Check command parameters */
		// Find the order using the Order Manager
		RestOrder restOrder = retrieveOrderByIdentifierParameter(showCommand);
		if (null == restOrder) {
			return;
		}

		JobState requestedJobState = null;
		if (1 < showCommand.getParameters().size()) {
			try {
				requestedJobState = JobState.valueOf(showCommand.getParameters().get(1).getValue().toUpperCase());
			} catch (Exception e) {
				System.err.println(uiMsg(MSG_ID_INVALID_JOB_STATE_VALUE, showCommand.getParameters().get(1).getValue()));
				return;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_JOBS + "?orderid=" + restOrder.getId()
			+ (null == requestedJobState ? "" : "&state=" + requestedJobState);
		
		/* Get the job information from the Production Planner */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductionPlannerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_JOBS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Display the job(s) found */
		// Return the job list
		if (isVerbose) {
			try {
				CLIUtil.printObject(System.out, resultList, jobOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		} else {
			for (Object listObject: resultList) {
				if (listObject instanceof Map) {
					Map<?, ?> jobMap = (Map<?, ?>) listObject;
					System.out.println(String.format("%16s %-25s %-25s %s",
						jobMap.get("id").toString(),
						jobMap.get("startTime"),
						jobMap.get("stopTime"),
						jobMap.get("jobState")));
				}
			}
		} 
	}
	
	/**
	 * Suspend the named job
	 * 
	 * @param suspendCommand the parsed "job suspend" command
	 */
	private void suspendJob(ParsedCommand suspendCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspendJob({})", (null == suspendCommand ? "null" : suspendCommand.getName()));
		
		/* Check command options */
		boolean isForcing = false;
		for (ParsedOption option: suspendCommand.getOptions()) {
			switch(option.getName()) {
			case "force":
				isForcing = true;
				break;
			}
		}
		
		/* Get job ID from command parameters and retrieve the job using Production Planner service */
		RestJob restJob = retrieveJobByIdParameter(suspendCommand);
		if (null == restJob) {
			return;
		}
		
		/* Check whether (database) job is in state "RELEASED" or "STARTED", otherwise suspending not allowed */
		if (!JobState.RELEASED.equals(restJob.getJobState())
				&& !JobState.STARTED.equals(restJob.getJobState())) {
			System.err.println(uiMsg(MSG_ID_INVALID_JOB_STATE,
					CMD_SUSPEND, restJob.getJobState(), JobState.RELEASED.toString() + " or " + JobState.STARTED.toString()));
			return;
		}
		
		
		/* Tell Production Planner service to suspend job, changing job to "INITIAL" */
		try {
			restJob = serviceConnection.patchToService(serviceConfig.getProductionPlannerUrl(),
					URI_PATH_JOB_SUSPEND + "/" + restJob.getId() + (isForcing ? "?force=true" : ""),
					restJob, RestJob.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOB_NOT_FOUND, restJob.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_JOB_DATA_INVALID,  e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new job version */
		String message = uiMsg(MSG_ID_JOB_SUSPENDED, restJob.getId(), restJob.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Resume the named job
	 * 
	 * @param resumeCommand the parsed "job resume" command
	 */
	private void resumeJob(ParsedCommand resumeCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeJob({})", (null == resumeCommand ? "null" : resumeCommand.getName()));
		
		/* Get job ID from command parameters and retrieve the job using Production Planner service */
		RestJob restJob = retrieveJobByIdParameter(resumeCommand);
		if (null == restJob) {
			return;
		}
		
		/* Check whether (database) job is in state "INITIAL", otherwise resuming not allowed */
		if (!JobState.INITIAL.equals(restJob.getJobState())) {
			System.err.println(uiMsg(MSG_ID_INVALID_JOB_STATE,
					CMD_RESUME, restJob.getJobState(), JobState.INITIAL.toString()));
			return;
		}
		
		/* Update job state to "RELEASED" using Production Planner service */
		try {
			restJob = serviceConnection.patchToService(serviceConfig.getProductionPlannerUrl(), URI_PATH_JOB_RESUME + "/" + restJob.getId(),
					restJob, RestJob.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOB_NOT_FOUND, restJob.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_JOB_DATA_INVALID,  e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new job version */
		String message = uiMsg(MSG_ID_JOB_RESUMED, restJob.getId(), restJob.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Cancel the named job
	 * 
	 * @param cancelCommand the parsed "job cancel" command
	 */
	private void cancelJob(ParsedCommand cancelCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancelJob({})", (null == cancelCommand ? "null" : cancelCommand.getName()));
		
		/* Get job ID from command parameters and retrieve the job using Production Planner service */
		RestJob restJob = retrieveJobByIdParameter(cancelCommand);
		if (null == restJob) {
			return;
		}
		
		/* Check whether (database) job is in state "INITIAL", otherwise cancelling not allowed */
		if (!JobState.INITIAL.equals(restJob.getJobState())) {
			System.err.println(uiMsg(MSG_ID_INVALID_JOB_STATE,
					CMD_CANCEL, restJob.getJobState(), JobState.INITIAL.toString()));
			return;
		}
		
		/* Update job state to "FAILED" using Production Planner service */
		try {
			restJob = serviceConnection.patchToService(serviceConfig.getProductionPlannerUrl(), URI_PATH_JOB_CANCEL + "/" + restJob.getId(),
					restJob, RestJob.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOB_NOT_FOUND, restJob.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_JOB_DATA_INVALID,  e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new job version */
		String message = uiMsg(MSG_ID_JOB_CANCELLED, restJob.getId(), restJob.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Retry the named job
	 * 
	 * @param retryCommand the parsed "job retry" command
	 */
	private void retryJob(ParsedCommand retryCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> retryJob({})", (null == retryCommand ? "null" : retryCommand.getName()));
		
		/* Get job ID from command parameters and retrieve the job using Production Planner service */
		RestJob restJob = retrieveJobByIdParameter(retryCommand);
		if (null == restJob) {
			return;
		}
		
		/* Check whether (database) job is in state "FAILED", otherwise retrying not allowed */
		if (!JobState.FAILED.equals(restJob.getJobState())) {
			System.err.println(uiMsg(MSG_ID_INVALID_JOB_STATE,
					CMD_RETRY, restJob.getJobState(), JobState.FAILED.toString()));
			return;
		}
		
		/* Update job state to "INITIAL" using Production Planner service */
		try {
			restJob = serviceConnection.patchToService(serviceConfig.getProductionPlannerUrl(), URI_PATH_JOB_RETRY + "/" + restJob.getId(),
					restJob, RestJob.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOB_NOT_FOUND, restJob.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_JOB_DATA_INVALID,  e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new job version */
		String message = uiMsg(MSG_ID_RETRYING_JOB, restJob.getId(), restJob.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Show the job step specified in the command parameters
	 * 
	 * @param showCommand the parsed "job step show" command
	 */
	private void showJobStep(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showJobStep({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String jobStepOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case "format":
				jobStepOutputFormat = option.getValue().toUpperCase();
				break;
			case "verbose":
				isVerbose = true;
				break;
			}
		}
		
		/* Check command parameters */
		// Find the job using the Production Planner
		RestJob restJob = retrieveJobByIdParameter(showCommand);
		if (null == restJob) {
			return;
		}

		JobStepState requestedJobStepState = null;
		if (1 < showCommand.getParameters().size()) {
			try {
				requestedJobStepState = JobStepState.valueOf(showCommand.getParameters().get(1).getValue().toUpperCase());
			} catch (Exception e) {
				System.err.println(uiMsg(MSG_ID_INVALID_JOBSTEP_STATE_VALUE, showCommand.getParameters().get(1).getValue()));
				return;
			}
		}
		
		/* Display the job step(s) found */
		for (RestJobStep restJobStep: restJob.getJobSteps()) {
			if (null == requestedJobStepState || requestedJobStepState.equals(restJobStep.getJobStepState())) {
				if (isVerbose) {
					try {
						CLIUtil.printObject(System.out, restJobStep, jobStepOutputFormat);
					} catch (IllegalArgumentException e) {
						System.err.println(e.getMessage());
						return;
					} catch (IOException e) {
						System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
						return;
					} 
				} else {
					System.out.println(String.format("%16s %-12s %s", restJobStep.getId(), restJobStep.getOutputProductClass(),
							restJobStep.getJobStepState().toString()));
				}
			}
		}
	}
	
	/**
	 * Suspend the named job
	 * 
	 * @param suspendCommand the parsed "job step suspend" command
	 */
	private void suspendJobStep(ParsedCommand suspendCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspendJob({})", (null == suspendCommand ? "null" : suspendCommand.getName()));
		
		/* Check command options */
		boolean isForcing = false;
		for (ParsedOption option: suspendCommand.getOptions()) {
			switch(option.getName()) {
			case "force":
				isForcing = true;
				break;
			}
		}

		/* Get job step ID from command parameters and retrieve the job using Production Planner service */
		RestJobStep restJobStep = retrieveJobStepByIdParameter(suspendCommand);
		if (null == restJobStep) {
			return;
		}
		
		/* Check whether (database) job step is in state "READY", otherwise suspending not allowed */
		if (!JobStepState.READY.equals(restJobStep.getJobStepState())
				&& !JobStepState.RUNNING.equals(restJobStep.getJobStepState())) {
			System.err.println(uiMsg(MSG_ID_INVALID_JOBSTEP_STATE,
					CMD_SUSPEND, restJobStep.getJobStepState(),
					JobStepState.READY.toString() + " or " + JobStepState.RUNNING.toString()));
			return;
		}
		
		/* Tell Production Planner service to suspend job step, changing job step state to "INITIAL" */
		try {
			restJobStep = serviceConnection.patchToService(serviceConfig.getProductionPlannerUrl(),
					URI_PATH_JOBSTEP_SUSPEND + "/" + restJobStep.getId() + (isForcing ? "?force=true" : ""),
					restJobStep, RestJobStep.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOBSTEP_NOT_FOUND, restJobStep.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_JOBSTEP_DATA_INVALID,  e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBSTEPS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new job step version */
		String message = uiMsg(MSG_ID_JOBSTEP_SUSPENDED, restJobStep.getId(), restJobStep.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Resume the named job step
	 * 
	 * @param resumeCommand the parsed "job step resume" command
	 */
	private void resumeJobStep(ParsedCommand resumeCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeJobStep({})", (null == resumeCommand ? "null" : resumeCommand.getName()));
		
		/* Get job step ID from command parameters and retrieve the job using Production Planner service */
		RestJobStep restJobStep = retrieveJobStepByIdParameter(resumeCommand);
		if (null == restJobStep) {
			return;
		}
		
		/* Check whether (database) job step is in state "READY", otherwise resuming not allowed */
		if (!JobStepState.INITIAL.equals(restJobStep.getJobStepState())) {
			System.err.println(uiMsg(MSG_ID_INVALID_JOBSTEP_STATE,
					CMD_RESUME, restJobStep.getJobStepState(), JobStepState.INITIAL.toString()));
			return;
		}
		
		/* Update job step state to "WAITING_INPUT" or "READY" using Production Planner service */
		try {
			restJobStep = serviceConnection.patchToService(serviceConfig.getProductionPlannerUrl(), URI_PATH_JOBSTEP_RESUME + "/" + restJobStep.getId(),
					restJobStep, RestJobStep.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOBSTEP_NOT_FOUND, restJobStep.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_JOBSTEP_DATA_INVALID,  e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBSTEPS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new job step version */
		String message = uiMsg(MSG_ID_JOBSTEP_RESUMED, restJobStep.getId(), restJobStep.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Cancel the named job step
	 * 
	 * @param cancelCommand the parsed "job step cancel" command
	 */
	private void cancelJobStep(ParsedCommand cancelCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancelJobStep({})", (null == cancelCommand ? "null" : cancelCommand.getName()));
		
		/* Get job step ID from command parameters and retrieve the job using Production Planner service */
		RestJobStep restJobStep = retrieveJobStepByIdParameter(cancelCommand);
		if (null == restJobStep) {
			return;
		}
		
		/* Check whether (database) job is in state "INITIAL" or "RUNNING", otherwise cancelling not allowed */
		if (!JobStepState.INITIAL.equals(restJobStep.getJobStepState())
				&&!JobStepState.RUNNING.equals(restJobStep.getJobStepState())) {
			System.err.println(uiMsg(MSG_ID_INVALID_JOBSTEP_STATE,
					CMD_CANCEL, restJobStep.getJobStepState(), JobStepState.INITIAL.toString()));
			return;
		}
		
		/* Update job step state to "FAILED" using Production Planner service */
		try {
			restJobStep = serviceConnection.patchToService(serviceConfig.getProductionPlannerUrl(), URI_PATH_JOBSTEP_CANCEL + "/" + restJobStep.getId(),
					restJobStep, RestJobStep.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOBSTEP_NOT_FOUND, restJobStep.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_JOBSTEP_DATA_INVALID,  e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBSTEPS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new job step version */
		String message = uiMsg(MSG_ID_JOBSTEP_CANCELLED, restJobStep.getId(), restJobStep.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Retry the named job step
	 * 
	 * @param retryCommand the parsed "job step retry" command
	 */
	private void retryJobStep(ParsedCommand retryCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> retryJobStep({})", (null == retryCommand ? "null" : retryCommand.getName()));
		
		/* Get job step ID from command parameters and retrieve the job step using Production Planner service */
		RestJobStep restJobStep = retrieveJobStepByIdParameter(retryCommand);
		if (null == restJobStep) {
			return;
		}
		
		/* Check whether (database) job step is in state "FAILED", otherwise retrying not allowed */
		if (!JobStepState.FAILED.equals(restJobStep.getJobStepState())) {
			System.err.println(uiMsg(MSG_ID_INVALID_JOBSTEP_STATE,
					CMD_RETRY, restJobStep.getJobStepState(), JobState.FAILED.toString()));
			return;
		}
		
		/* Update job step state to "INITIAL" using Production Planner service */
		try {
			restJobStep = serviceConnection.patchToService(serviceConfig.getProductionPlannerUrl(), URI_PATH_JOBSTEP_RETRY + "/" + restJobStep.getId(),
					restJobStep, RestJobStep.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_JOBSTEP_NOT_FOUND, restJobStep.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_JOBSTEP_DATA_INVALID,  e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), JOBSTEPS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new job step version */
		String message = uiMsg(MSG_ID_RETRYING_JOBSTEP, restJobStep.getId(), restJobStep.getVersion());
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
		if (null == loginManager.getUser()) {
			System.err.println(uiMsg(MSG_ID_USER_NOT_LOGGED_IN, command.getName()));
			return;
		}
		
		/* Check argument */
		if (!CMD_JOB.equals(command.getName())) {
			System.err.println(uiMsg(MSG_ID_INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given */
		if (null == command.getSubcommand() || null == command.getSubcommand().getName()) {
			System.err.println(uiMsg(MSG_ID_SUBCOMMAND_MISSING, command.getName()));
			return;
		}
		
		/* Check for subcommand help request */
		ParsedCommand subcommand = command.getSubcommand();
		if (subcommand.isHelpRequested()) {
			subcommand.getSyntaxCommand().printHelp(System.out);
			return;
		}
		
		/* Make sure a sub-subcommand is given for "step" */
		ParsedCommand subsubcommand = subcommand.getSubcommand();
		if (CMD_STEP.equals(subcommand.getName()) && null == subcommand.getSubcommand()) {
			System.err.println(uiMsg(MSG_ID_SUBCOMMAND_MISSING, subcommand.getName()));
			return;
		}

		/* Check for sub-subcommand help request */
		if (null != subsubcommand && subsubcommand.isHelpRequested()) {
			subsubcommand.getSyntaxCommand().printHelp(System.out);
			return;
		} 
		
		/* Execute the (sub-)subcommand */
		switch(subcommand.getName()) {
		case CMD_SHOW:		showJob(subcommand); break;
		case CMD_SUSPEND:	suspendJob(subcommand); break;
		case CMD_RESUME:	resumeJob(subcommand); break;
		case CMD_CANCEL:	cancelJob(subcommand); break;
		case CMD_RETRY:		retryJob(subcommand); break;
		case CMD_STEP:
			// Handle commands for job steps
			switch (subsubcommand.getName()) {
			case CMD_SHOW:		showJobStep(subsubcommand); break;
			case CMD_SUSPEND:	suspendJobStep(subsubcommand); break;
			case CMD_RESUME:	resumeJobStep(subsubcommand); break;
			case CMD_CANCEL:	cancelJobStep(subsubcommand); break;
			case CMD_RETRY:		retryJobStep(subsubcommand); break;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, 
						command.getName() + " " + subcommand.getName() + " " + subsubcommand.getName()));
				return;
			}
			break;
		default:
			System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
			return;
		}
	}
}
