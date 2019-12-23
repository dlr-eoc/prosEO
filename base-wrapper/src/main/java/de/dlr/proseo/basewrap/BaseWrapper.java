/**
 * BaseWrapper.java
 * 
 *  (C) 2019 Hubert Asamer, DLR
 *  (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.basewrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import alluxio.AlluxioURI;
import alluxio.exception.AlluxioException;
import alluxio.grpc.ReadPType;
import alluxio.grpc.WritePType;
import de.dlr.proseo.basewrap.rest.HttpResponseInfo;
import de.dlr.proseo.basewrap.rest.RestOps;
import de.dlr.proseo.model.fs.alluxio.AlluxioOps;
import de.dlr.proseo.model.fs.s3.AmazonS3URI;
import de.dlr.proseo.model.fs.s3.S3Ops;
import de.dlr.proseo.model.joborder.InputOutput;
import de.dlr.proseo.model.joborder.IpfFileName;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.joborder.Proc;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * prosEO Base Processor Wrapper - for processors conforming to ESA's
 * "Generic IPF Interface Specification" (MMFI-GSEG-EOPG-TN-07-0003, V.1.8)
 * 
 * @author Hubert Asamer
 * @author Dr. Thomas Bassler
 *
 */
public class BaseWrapper {

	/** Exit code for successful completion */
	private static final int EXIT_CODE_OK = 0;
	/** Exit code for failure */
	private static final int EXIT_CODE_FAILURE = 255;
	/** Exit code explanation for successful completion */
	private static final String EXIT_TEXT_OK = "OK";
	/** Exit code explanation for failure */
	private static final String EXIT_TEXT_FAILURE = "FAILURE";
	/** CallBack-Message for failure */
	private static final String CALLBACK_STATUS_FAILURE = "FAILURE";
	/** CallBack-Message for success */
	private static final String CALLBACK_STATUS_SUCCESS = "SUCCESS";

	/** current directory of this program is used as work-dir */
	private static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));
	/** current timestamp used for output-file prefixes*/
	private static final long WRAPPER_TIMESTAMP = System.currentTimeMillis()/1000;
	/**  auto-created path/filename of JobOrderFile within container */
	private static final String CONTAINER_JOF_PATH = WORKING_DIR.toString()+File.separator+String.valueOf(WRAPPER_TIMESTAMP)+".xml";
	/** directory-prefix of fetched input data */
	private static final String CONTAINER_INPUTS_PATH_PREFIX = "inputs";
	/** directory-prefix of produced output data */
	private static final String CONTAINER_OUTPUTS_PATH_PREFIX = String.valueOf(WRAPPER_TIMESTAMP);
	/**ALLUXIO-read type  */
	private static final ReadPType ALLUXIO_READ_TYPE = ReadPType.CACHE;
	/**ALLUXIO-write type  */
	private static final WritePType ALLUXIO_WRITE_TYPE = WritePType.CACHE_THROUGH;
	/**End-Message  of wrapper */
	private static final String MSG_LEAVING_BASE_WRAPPER = "Leaving base-wrapper with exit code {} ({})";
	/**Start-Message  of wrapper */
	private static final String MSG_STARTING_BASE_WRAPPER = "Starting base-wrapper V00.00.04 with JobOrder file {}";
	/**Invalid Environment Variable message */
	private static final String MSG_INVALID_VALUE_OF_ENVVAR = "Invalid value of EnvVar: {}";
	/**File not readable message */
	private static final String MSG_FILE_NOT_READABLE = "File {} is not readable";
	private static final String MSG_FILE_NOT_FOUND = "File {} does not exist";
	private static final String MSG_FILE_FOUND = "File {} exists";


	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(BaseWrapper.class);

	/**
	 *  Enumeration with possible FileSystem-Types.
	 *  FS_TYPE is set within a Joborder-File at Input/Output File level 
	 *  <ul>
	 *  <li>{@link #S3} API-based FileStorage - object storage aka SimpleStorageService - S3</li>
	 *  <li>{@link #POSIX} a POSIX FileSystem</li>
	 *  <li>{@link #ALLUXIO} an in-memory/SSD backed/tiered ditributed FileSystem (aka hadoop)</li>
	 *  </ul>
	 */
	enum FS_TYPE {
		S3
		, POSIX
		, ALLUXIO
	}

	/** Enumeration with valid environment variable names.
	 *  At runtime-start the BaseWrapper checks the presence and values of each variable.
	 *  <ul>
	 *  <li>{@link #JOBORDER_FS_TYPE} FileSystem-Type (one of FS_TYPE) of referenced JobOrder File ({@link #JOBORDER_FILE})</li>
	 *  <li>{@link #JOBORDER_FILE} URI of valid JobOrder-File</li>
	 *  <li>{@link #S3_ENDPOINT} S3-API Endpoint URL (<i>Set via k8s-configMap!</i>)</li>
	 *  <li>{@link #S3_ACCESS_KEY} S3-API access key (<i>Set via k8s-configMap!</i>)</li>
	 *  <li>{@link #S3_SECRET_ACCESS_KEY} S3-API secret access key (<i>Set via k8s-configMap!</i>)</li>
	 *  <li>{@link #S3_STORAGE_ID_OUTPUTS} S3-Storage-ID (reference to prosEO storage-manager storages) of processed Outputs. (<i>Set via k8s-configMap!</i>) </li>
	 *  <li>{@link #ALLUXIO_STORAGE_ID_OUTPUTS} Alluxio-Storage-ID (reference to prosEO storage-manager storages) of processed Outputs. (<i>Set via k8s-configMap!</i>)</li>
	 *  <li>{@link #INGESTOR_ENDPOINT} public API-Endpoint URL of prosEO-Ingestor</li>
	 *  <li>{@link #STATE_CALLBACK_ENDPOINT} public API-Endpoint URL of prosEO-Planner for submitting the final state of the wrapper-run.</li>
	 *  <li>{@link #PROCESSOR_SHELL_COMMAND} the processor shell command to be invoked by the wrapper</li>
	 *  <li>{@link #PROCESSING_FACILITY_NAME} name of the processing-facility this wrapper runs in.</li>
	 *  </ul>
	 *  
	 */
	enum ENV_VARS {
		JOBORDER_FS_TYPE
		, JOBORDER_FILE
		, S3_ENDPOINT
		, S3_ACCESS_KEY
		, S3_SECRET_ACCESS_KEY
		, S3_STORAGE_ID_OUTPUTS
		, ALLUXIO_STORAGE_ID_OUTPUTS
		, INGESTOR_ENDPOINT
		, STATE_CALLBACK_ENDPOINT
		, PROCESSOR_SHELL_COMMAND
		, PROCESSING_FACILITY_NAME
	}

	/** Environment Variables from Container (set via run-invocation or directly from docker-image)*/
	private String ENV_JOBORDER_FS_TYPE = System.getenv(ENV_VARS.JOBORDER_FS_TYPE.toString());
	private String ENV_JOBORDER_FILE = System.getenv(ENV_VARS.JOBORDER_FILE.toString());
	private String ENV_S3_ENDPOINT = System.getenv(ENV_VARS.S3_ENDPOINT.toString());
	private String ENV_S3_ACCESS_KEY = System.getenv(ENV_VARS.S3_ACCESS_KEY.toString());
	private String ENV_S3_SECRET_ACCESS_KEY = System.getenv(ENV_VARS.S3_SECRET_ACCESS_KEY.toString());
	private String ENV_S3_STORAGE_ID_OUTPUTS = System.getenv(ENV_VARS.S3_STORAGE_ID_OUTPUTS.toString());
	private String ENV_ALLUXIO_STORAGE_ID_OUTPUTS = System.getenv(ENV_VARS.ALLUXIO_STORAGE_ID_OUTPUTS.toString());
	private String ENV_STATE_CALLBACK_ENDPOINT = System.getenv(ENV_VARS.STATE_CALLBACK_ENDPOINT.toString());
	private String ENV_PROCESSOR_SHELL_COMMAND = System.getenv(ENV_VARS.PROCESSOR_SHELL_COMMAND.toString());
	private String ENV_PROCESSING_FACILITY_NAME = System.getenv(ENV_VARS.PROCESSING_FACILITY_NAME.toString());
	private String ENV_INGESTOR_ENDPOINT = System.getenv(ENV_VARS.INGESTOR_ENDPOINT.toString());


	/**
	 * Checks if given String is one of FS_TYPE enum
	 * 
	 * @param fs_type String
	 * @return true if one of FS_TYPE enum
	 */
	private Boolean checkFS_TYPE(String fs_type) {
		for (FS_TYPE c : FS_TYPE.values()) {
			if (c.name().equals(fs_type)) {
				return true;
			} 
		}
		return false;
	}

	/**
	 * Checks if given String for Output-File/Dir name has trailing slash
	 * 
	 * @param file_name String
	 * @return String of file_name with trailing slash
	 */
	private String fileNameHavingTrailingSlash(String file_name) {
		// TODO: better pattern detection
		file_name=file_name
				.replaceAll("alluxio://", "")
				.replaceAll("s3://", "")
				.replaceAll(":","")
				.replaceAll("//","");

		if (file_name.startsWith("/")) return file_name;
		else file_name = "/"+file_name;
		return file_name;
	}

	
	/**
	 * Returns values of BaseWrapper's ENV_VARS
	 * 
	 * @param env the ENV_VARS enum
	 * @return ArrayList with env_vars in form of "name=value"
	 */
	private ArrayList<String> envList(ENV_VARS[] env){
		ArrayList<String> list = new ArrayList<String>();
		for (int i=0;i<ENV_VARS.values().length;i++) {
			list.add(ENV_VARS.values()[i].toString()+"="+System.getenv(ENV_VARS.values()[i].toString().toString()));
		}
		return list;
	}

	/**
	 * A splash-message on program-startup...
	 * 
	 * @return String with splsh-message
	 */
	private String splash() {	
		return "\n\n{\"prosEO\" : \"A Processing System for Earth Observation Data\"}\n";
	}

	/**
	 * Check presence and values of all required Environment Variables
	 * @return true/false
	 */
	private Boolean checkEnv() {
		logger.info("Checking {} ENV_VARS...", ENV_VARS.values().length);
		for (String e : envList(ENV_VARS.values())) {
			logger.info("... {}", e);
		}

		if (!checkFS_TYPE(ENV_JOBORDER_FS_TYPE)) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.JOBORDER_FS_TYPE);
			return false;
		}
		if (ENV_JOBORDER_FS_TYPE == null || ENV_JOBORDER_FS_TYPE.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.JOBORDER_FS_TYPE);
			return false;
		}
		if (ENV_JOBORDER_FILE == null || ENV_JOBORDER_FILE.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.JOBORDER_FILE);
			return false;
		}
		if (ENV_JOBORDER_FS_TYPE.equals(FS_TYPE.S3.toString()) && (ENV_S3_ENDPOINT == null || ENV_S3_ENDPOINT.isEmpty())) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_ENDPOINT);
			return false;
		}
		if (ENV_JOBORDER_FS_TYPE.equals(FS_TYPE.S3.toString()) && (ENV_S3_ACCESS_KEY == null || ENV_S3_ACCESS_KEY.isEmpty())) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_ACCESS_KEY);
			return false;
		}
		if (ENV_JOBORDER_FS_TYPE.equals(FS_TYPE.S3.toString())
				&& (ENV_S3_SECRET_ACCESS_KEY == null || ENV_S3_SECRET_ACCESS_KEY.isEmpty())) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_SECRET_ACCESS_KEY);
			return false;
		}
		if (ENV_JOBORDER_FS_TYPE.equals(FS_TYPE.S3.toString()) && !ENV_JOBORDER_FILE.startsWith("s3://")) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR,
					ENV_VARS.JOBORDER_FS_TYPE + ": " + ENV_JOBORDER_FS_TYPE + " does not allow " + ENV_JOBORDER_FILE);
			return false;
		}
		;
		if (ENV_JOBORDER_FS_TYPE.equals(FS_TYPE.POSIX.toString()) && ENV_JOBORDER_FILE.startsWith("s3://")) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR,
					ENV_VARS.JOBORDER_FS_TYPE + ": " + ENV_JOBORDER_FS_TYPE + " does not allow " + ENV_JOBORDER_FILE);
			return false;
		}
		;
		if (ENV_STATE_CALLBACK_ENDPOINT == null || ENV_STATE_CALLBACK_ENDPOINT.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.STATE_CALLBACK_ENDPOINT);
			return false;
		}
		if (ENV_PROCESSOR_SHELL_COMMAND == null || ENV_PROCESSOR_SHELL_COMMAND.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.PROCESSOR_SHELL_COMMAND);
			return false;
		}
		if (ENV_PROCESSING_FACILITY_NAME == null || ENV_PROCESSING_FACILITY_NAME.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.PROCESSING_FACILITY_NAME);
			return false;
		}
		if(ENV_S3_STORAGE_ID_OUTPUTS==null || ENV_S3_STORAGE_ID_OUTPUTS.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_STORAGE_ID_OUTPUTS);
			return false;
		}
		if(ENV_ALLUXIO_STORAGE_ID_OUTPUTS==null || ENV_ALLUXIO_STORAGE_ID_OUTPUTS.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.ALLUXIO_STORAGE_ID_OUTPUTS);
			return false;
		}
		if(ENV_INGESTOR_ENDPOINT==null || ENV_INGESTOR_ENDPOINT.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.INGESTOR_ENDPOINT);
			return false;
		}
		logger.info("ENV_VARS looking good...");
		logger.info("PREFIX timestamp used for JobOrderFile-Naming & Results is {}", WRAPPER_TIMESTAMP);
		return true;
	}
	/**
	 * provide initial JobOrderFile as local File. Fetch JobOrderFile from Storage (according to FS_TYPE) and return as local file.
	 * 
	 * @return the JobOrder file as File
	 */
	private File provideInitialJOF() {
		String JOFContainerPath = null;
		// set JOF-path based on ENV
		if (ENV_JOBORDER_FS_TYPE.equals(FS_TYPE.S3.toString()) && ENV_JOBORDER_FILE.startsWith("s3://")) {
			AmazonS3URI s3uri = new AmazonS3URI(ENV_JOBORDER_FILE);
			JOFContainerPath = s3uri.getKey();
			try {
				Files.deleteIfExists(Paths.get(JOFContainerPath));
			} catch (IOException e) {
				logger.error(e.getMessage());
				return null;
			}
		}
		/** Set JOF-path if FS_TYPE env var (=container env var) has value `POSIX` */
		if (ENV_JOBORDER_FS_TYPE.equals(FS_TYPE.POSIX.toString()) && !ENV_JOBORDER_FILE.startsWith("s3://")) {
			JOFContainerPath = ENV_JOBORDER_FILE;
		}


		/** Fetch JOF if container env-var `FS_TYPE` has value `S3` */
		if (ENV_JOBORDER_FS_TYPE.equals(FS_TYPE.S3.toString()) && ENV_JOBORDER_FILE.startsWith("s3://")) {
			S3Client s3 = S3Ops.v2S3Client(ENV_S3_ACCESS_KEY, ENV_S3_SECRET_ACCESS_KEY, ENV_S3_ENDPOINT);
			if (null == s3) return null;
			Boolean transaction = S3Ops.v2FetchFile(s3, ENV_JOBORDER_FILE, JOFContainerPath);
			if (!transaction) return null;
		}

		/** Check if prepared JOF is readable */
		Boolean isReadable = true;
		try {
			isReadable = Files.isReadable(FileSystems.getDefault().getPath(JOFContainerPath));
		} catch (java.nio.file.InvalidPathException e) {
			logger.error(MSG_FILE_NOT_READABLE, JOFContainerPath);
			return null;
		}

		if (!isReadable) {
			logger.error(MSG_FILE_NOT_READABLE, JOFContainerPath);
			return null;
		}
		return new File(JOFContainerPath);
	}

	/**
	 * Parse the given JobOrder XML file
	 * 
	 * @param jobOrderFile the XML file to parse
	 * @return JobOrder Object
	 */
	private JobOrder parseJobOrderFile(File jobOrderFile) {

		JobOrder jobOrderDoc = null;
		jobOrderDoc = new JobOrder();
		jobOrderDoc.read(jobOrderFile.getAbsolutePath());

		//jobOrderDoc = docBuilder.parse(jobOrderFile);
		return jobOrderDoc;
	}
	/**
	 * Fetch remote input-data to container-workdir(based on FS_TYPE) and return valid JobOrder object for container-runtime-context. (=remapped file-pathes)
	 * 
	 * @param jo the JobOrder file to parse
	 * @return JobOrder object valid for container-context
	 */
	private JobOrder fetchInputData(JobOrder jo)
	{
		logger.info("Fetch Inputs & provide a valid JobOrderFile for the container-context...");
		int numberOfInputs = 0;
		int numberOfFetchedInputs = 0;
		int numberOfOutputs = 0;
		S3Client s3 = S3Ops.v2S3Client(ENV_S3_ACCESS_KEY, ENV_S3_SECRET_ACCESS_KEY, ENV_S3_ENDPOINT);
		if (null == s3) return null;
		// loop all procs -> mainly only one is present
		for(Proc item : jo.getListOfProcs()) {
			// loop all Input
			for (InputOutput io: item.getListOfInputs()) {
				// loop List_of_File_Names
				for (IpfFileName fn: io.getFileNames()) {
					numberOfInputs++;
					//fill original filename with current val of `File_Name` --> for later use...
					fn.setOriginalFileName(fn.getFileName());
					// for all S3-data we try to fetch to workdir...
					if(fn.getFSType().equals(FS_TYPE.S3.toString()) && fn.getOriginalFileName().startsWith("s3://")) {
						// first set file_name to local work-dir path
						fn.setFileName(fn.getOriginalFileName().replace("s3://", CONTAINER_INPUTS_PATH_PREFIX+File.separator));
						// now fetch from S3
						Boolean transaction = S3Ops.v2FetchFile(s3, fn.getOriginalFileName(), fn.getFileName());
						if (transaction)numberOfFetchedInputs++;
						if (!transaction) return null;
					}
					if(fn.getFSType().equals(FS_TYPE.ALLUXIO.toString())) {
						// now fetch from ALLUXIO
						fn.setFileName(CONTAINER_INPUTS_PATH_PREFIX+fn.getOriginalFileName());
						AlluxioURI srcPath = new AlluxioURI(fn.getOriginalFileName());
						AlluxioURI dstPath = new AlluxioURI(fn.getFileName());
						Boolean transaction = AlluxioOps.copyToLocal(srcPath, dstPath, ALLUXIO_READ_TYPE);
						if (transaction)numberOfFetchedInputs++;
						if (!transaction) return null;
					}
					if (fn.getFSType().equals(FS_TYPE.POSIX.toString())) {
						File f = new File(fn.getFileName());
						if (f.exists()) {
							logger.info(MSG_FILE_FOUND, fn.getFileName());
							numberOfFetchedInputs++;
						} else {
							logger.info(MSG_FILE_NOT_FOUND, fn.getFileName());
						}
					}
				}
			}
			// loop all Output & prepare dirs
			for (InputOutput io: item.getListOfOutputs()) {
				// loop List_of_File_Names
				for (IpfFileName fn: io.getFileNames()) {
					numberOfOutputs++;
					//fill original filename with current val of `File_Name` --> for later use --> push results step
					fn.setOriginalFileName(fn.getFileName());
					if(fn.getFSType().equals(FS_TYPE.S3.toString())) {
						// first set output file_name to local work-dir path
						fn.setFileName(CONTAINER_OUTPUTS_PATH_PREFIX+fileNameHavingTrailingSlash(fn.getOriginalFileName()));
						try {
							File f = new File(fn.getFileName());
							if (Files.exists(Paths.get(fn.getFileName()), LinkOption.NOFOLLOW_LINKS)) f.delete();
							File subdirs = new File(FilenameUtils.getPath(fn.getFileName()));
							subdirs.mkdirs();
						} catch (SecurityException e) {
							logger.error(e.getMessage());
							return null;
						}
					}
					if(fn.getFSType().equals(FS_TYPE.ALLUXIO.toString())) {
						fn.setFileName(CONTAINER_OUTPUTS_PATH_PREFIX+fileNameHavingTrailingSlash(fn.getOriginalFileName()));
						try {
							File f = new File(fn.getFileName());
							if (Files.exists(Paths.get(fn.getFileName()), LinkOption.NOFOLLOW_LINKS)) f.delete();
							File subdirs = new File(FilenameUtils.getPath(fn.getFileName()));
							subdirs.mkdirs();
						} catch (SecurityException e) {
							logger.error(e.getMessage());
							return null;
						}
					}
				}
			}
		}
		if (numberOfFetchedInputs==numberOfInputs) {
			logger.info("Fetched {} Input-Files and prepared dirs for {} Outputs -- Ready for processing using Container-JOF {}", numberOfFetchedInputs,numberOfOutputs,CONTAINER_JOF_PATH);
			s3.close();
			return jo;
		}
		else {
			logger.info("Number of fetched/found inputs {} differs from JOF-Definition: {}", numberOfFetchedInputs, numberOfInputs);
			s3.close();
			return null;
		}
	}

	/**
	 * creates valid container-context JobOrderFile under given path
	 * 
	 * @param jo JobOrder remapped JobOrder object
	 * @param path file path of newly created JOF
	 * @return True/False
	 */
	private Boolean provideContainerJOF(JobOrder jo, String path) {	
		return jo.writeXML(path, false);
	}

	/**
	 * Executes the processor
	 * 
	 * @param shellCommand command for executing the processor
	 * @param jofPath path of re-mapped JobOrder file valid in container context
	 * @return True/False
	 */
	private Boolean runProcessor(String shellCommand, String jofPath) {
		logger.info("Starting Processing using command {} and local JobOrderFile: {}",shellCommand, jofPath);
		Boolean check = false;

		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.redirectErrorStream(true); 

		String fullCommand = shellCommand+" "+jofPath;
		String[] execCommand = fullCommand.split(" ");

		processBuilder.command(execCommand);
		try {
			Process process = processBuilder.start();
			BufferedReader reader =
					new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				logger.info("... "+line);
			}
			int exitCode = process.waitFor();
			logger.info("Processing finished with return code {}", exitCode);
			if (exitCode == 0) check = true;

		} catch (IOException e) {
			logger.error(e.getMessage());
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		return check;
	}


	/**
	 * Check if given String can be parsed as Integer
	 * @param strLong Integer as String 
	 * @return true/false
	 */
	public static boolean isInteger(String strLong) {
		try {
			@SuppressWarnings("unused")
			long lng = Long.parseLong(strLong);
		} catch (NumberFormatException | NullPointerException nfe) {
			return false;
		}
		return true;
	}
	/**
	 * Pushes processing results to prosEO storage
	 * 
	 * @param jo jobOrder  JobOrder-Object (valid in container context)
	 * @return ArrayList all infos of pushed products
	 */
	private ArrayList<PushedProcessingOutput> pushResults(JobOrder jo) {
		logger.info("Uploading results to prosEO storage...");
		logger.info("Upload File-Pattern based on timestamp-prefix is: FS_TYPE://<storageID>/<product_id>/{}/<filename>", WRAPPER_TIMESTAMP);
		AmazonS3 s3 = S3Ops.v1S3Client(ENV_S3_ACCESS_KEY, ENV_S3_SECRET_ACCESS_KEY, ENV_S3_ENDPOINT);
		String separator = "/";
		int numberOfOutputs = 0;
		int numberOfPushedOutputs = 0;
		ArrayList<PushedProcessingOutput> pushedOutputs = new ArrayList<PushedProcessingOutput>();
		for(Proc item : jo.getListOfProcs()) {
			// loop all Outputs
			for (InputOutput io: item.getListOfOutputs()) {
				for (IpfFileName fn: io.getFileNames()) {
					numberOfOutputs++;
					if (!isInteger(io.getProductID())) {
						logger.error("Product_ID:{} is not parseable as Long-Integer...", io.getProductID());
						return null;
					}
					// Push files to ALLUXIO
					// Output-Top-prefix==ENV_ALLUXIO_STORAGE_ID_OUTPUTS
					if(fn.getFSType().equals(FS_TYPE.ALLUXIO.toString()) && isInteger(io.getProductID())) {
						try {
							AlluxioURI srcPath = new AlluxioURI(fn.getFileName());
							AlluxioURI dstPath = new AlluxioURI(separator+ENV_ALLUXIO_STORAGE_ID_OUTPUTS+separator+io.getProductID()+separator+fn.getFileName());
							Boolean transaction = AlluxioOps.copyFromLocal(srcPath, dstPath, ALLUXIO_WRITE_TYPE);
							if(transaction) {
								numberOfPushedOutputs++;
								PushedProcessingOutput p = new PushedProcessingOutput();
								p.setFsType(FS_TYPE.ALLUXIO.toString());
								p.setId(Long.parseLong((io.getProductID())));
								p.setPath(dstPath.toString());
								p.setRevision(WRAPPER_TIMESTAMP);
								pushedOutputs.add(p);
							}
						} catch(AlluxioException | IOException e) {
							logger.error(e.getMessage());
						}
					}
					// Push files to S3 using multipart upload
					// Output-Bucket=ENV_S3_STORAGE_ID_OUTPUTS
					if(fn.getFSType().equals(FS_TYPE.S3.toString()) && isInteger(io.getProductID())) {
						try {
							ArrayList<String> transaction = S3Ops.v1Upload(s3, fn.getFileName(), ENV_S3_STORAGE_ID_OUTPUTS, io.getProductID(), false);
							if(null != transaction) {
								for (String s : transaction) {
									logger.info("  ... " + s);
								}
								numberOfPushedOutputs++;
								PushedProcessingOutput p = new PushedProcessingOutput();
								p.setFsType(FS_TYPE.S3.toString());
								p.setId(Long.parseLong((io.getProductID())));
								p.setPath(ENV_S3_STORAGE_ID_OUTPUTS+separator+io.getProductID()+separator+fn.getFileName());
								p.setRevision(WRAPPER_TIMESTAMP);
								pushedOutputs.add(p);
							}
						} catch ( java.lang.IllegalArgumentException e) {
							logger.error(e.getMessage());
							return null;
						}
					} 
				}
			}
		}
		if (numberOfPushedOutputs==numberOfOutputs && pushedOutputs.size()==numberOfOutputs) {
			logger.info("Uploaded {} results to prosEO storage...",numberOfPushedOutputs);
		}
		else {
			logger.info("error in push, outputs {}, number of pushed outputs {}, pushed outputs size {}",
					numberOfOutputs,
					numberOfPushedOutputs,
					pushedOutputs.size());
			return null;
		}

		//TODO: add to Unit Tests
		//		// ALLUXIO-DIR-UPLOAD-TEST
		//		AlluxioURI srcPath = new AlluxioURI("inputs/");
		//		AlluxioURI dstPath = new AlluxioURI("/AlluxioDirUploadTest/");
		//		try {
		//			AlluxioOps.copyFromLocal(srcPath, dstPath, ALLUXIO_WRITE_TYPE);
		//		} catch (AlluxioException | IOException e) { 
		//			logger.error(e.getMessage());
		//			return null;
		//		}
		//
		//		// S3-DIR-UPLOAD-TEST
		//		try {
		//			S3Ops.v1Upload(
		//					s3
		//					, "inputs/"
		//					, ENV_S3_BUCKET_OUTPUTS
		//					, "S3DirUploadTest"
		//					, false
		//					);
		//		} catch (Exception e) { 
		//			logger.error(e.getMessage());
		//			return null;
		//		}
		return pushedOutputs;
	}

	/**
	 * Register pushed Products using prosEO-Ingestor REST API
	 * 
	 * @param pushedProducts ArrayList
	 * @return HTTP response code of Ingestor-API
	 */
	private ArrayList<IngestedProcessingOutput> ingestPushedOutputs(ArrayList<PushedProcessingOutput> pushedProducts) {
		logger.info("Trying to register {} products with prosEO-Ingestor@{}",pushedProducts.size(), ENV_INGESTOR_ENDPOINT);

		ArrayList<IngestedProcessingOutput> ingests = new ArrayList<IngestedProcessingOutput>();

		// loop pushed products and send HTTP-POST to prosEO-Ingestor in order to register the products
		//POST http://localhost:8080/ingest/proseo-otc01/928928398
		for (PushedProcessingOutput p : pushedProducts) {
			String ingestorRestUrl =   "/ingest/"+ENV_PROCESSING_FACILITY_NAME+"/"+p.getId();
			
			// Build request based on de.dlr.proseo.ingestor.rest.model.ProductFile
			IngestorProductFile request = new IngestorProductFile();
			request.setProcessingFacilityName(ENV_PROCESSING_FACILITY_NAME);
			//request.setAuxFileNames(null); --> must be empty list, not null!
			
			request.setFilePath(p.getNormedPath());
			request.setStorageType(p.getFsType());
			request.setProductFileName(p.getFileName());
			request.setProductId(p.getId());
			//request.setVersion(null); --> redundant
			
			ObjectMapper obj = new ObjectMapper(); 
			String jsonRequest="";
			try {
				jsonRequest = obj.writeValueAsString(request);
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage());
			} 
			HttpResponseInfo singleResponse = RestOps.restApiCall(ENV_INGESTOR_ENDPOINT, ingestorRestUrl, jsonRequest, null, RestOps.HttpMethod.POST);

			if (singleResponse != null && singleResponse.gethttpCode()==201) {
				logger.info("... ingestor response is  {}",singleResponse.gethttpResponse());
				IngestedProcessingOutput ingest = new IngestedProcessingOutput();
				ingest.setFsType(p.getFsType());
				ingest.setProduct_id(p.getId());
				ingest.setIngestorHttpResponse(singleResponse.gethttpResponse());
				ingest.setPath(p.getNormedPath() + "/" + p.getFileName());
				ingest.setRevision(p.getRevision());
				ingests.add(ingest);
			}
			if(singleResponse != null && singleResponse.gethttpCode()!=201) {
				logger.info("... ingestor response is {}",singleResponse.gethttpResponse());
			}
		}
		if (ingests.size() != pushedProducts.size()) {
			logger.error("{} out of {} products have been ingested...", ingests.size(), pushedProducts.size());
			return null;
		}

		logger.info("{} out of {} products have been ingested...Well done!", ingests.size(), pushedProducts.size());
		return ingests;
	}


	/**
	 * triggers a Callback to ENV_STATE_CALLBACK_ENDPOINT (prosEO-Planner)
	 * 
	 * @param msg the callback message
	 * @return HttpResponseInfo callback
	 */
	private HttpResponseInfo callBack(String msg) {
		// ENV_STATE_CALLBACK_ENDPOINT shall look like:
		// <planner-URL>/processingfacilities/<procFacilityName>/finish/<podName>
		// queryParam status is set by wrapper
		HttpResponseInfo callback = RestOps.restApiCall(ENV_STATE_CALLBACK_ENDPOINT, "", msg, "status", RestOps.HttpMethod.PATCH);
		if(callback != null) logger.info("... planner response is {} ({})", callback.gethttpResponse(), callback.gethttpCode());
		else return null;

		return callback;
	}

	/**
	 * Hook for mission-specific modifications to the job order document before fetching input data
	 * Intended for override by mission-specific job classes, NO-OP in BaseWrapper.
	 * 
	 * @param jobOrderDoc the job order document to modify
	 */
	protected void preFetchInputHook(JobOrder jobOrderDoc) {
		// No operation
	}

	/**
	 * Hook for mission-specific modifications to the final job order document after execution of the processor (before push of
	 * results).
	 * Intended for override by mission-specific job classes, NO-OP in BaseWrapper.
	 * 
	 * @param joWork the job order document to modify
	 */
	protected void postProcessingHook(JobOrder joWork) {
		// No operation
	}

	/**
	 * Perform processing: check env, parse JobOrder file, fetch input files, push output files
	 * 
	 * @return the program exit code (OK or FAILURE)
	 */
	final public int run() {

		logger.info(splash());

		/** ProcessorWrapperFlow */
		/** ==================== */

		/** STEP [4][5] Provide the JobOrder file from the invocation arguments */

		logger.info(MSG_STARTING_BASE_WRAPPER,  ENV_JOBORDER_FILE);
		Boolean check = checkEnv();
		if (!check) {
			logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}

		File jobOrderFile = provideInitialJOF();
		if (null == jobOrderFile) {
			callBack(CALLBACK_STATUS_FAILURE);
			logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}

		JobOrder jobOrderDoc = parseJobOrderFile(jobOrderFile);
		if (null == jobOrderDoc) {
			callBack(CALLBACK_STATUS_FAILURE);
			logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		/** Hook for additional mission-specific pre-fetch operations on the job order document */
		preFetchInputHook(jobOrderDoc);

		/** STEP [6][7][8] fetch Inputs & create re-mapped JOF for container context*/
		JobOrder joWork = null;
		joWork = fetchInputData(jobOrderDoc);
		if (null == joWork) {
			callBack(CALLBACK_STATUS_FAILURE);
			logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}

		Boolean containerJOF = provideContainerJOF(joWork, CONTAINER_JOF_PATH);
		if (!containerJOF) {
			callBack(CALLBACK_STATUS_FAILURE);
			logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}

		/** STEP [CALC] Execute Processor */
		Boolean procRun = runProcessor(
				ENV_PROCESSOR_SHELL_COMMAND,
				CONTAINER_JOF_PATH
				);
		if (!procRun) {
			callBack(CALLBACK_STATUS_FAILURE);
			logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		/** Hook for additional post-processing operations on the job order document */
		postProcessingHook(joWork);

		/** STEP [9] Push Processing Results to prosEO Storage, if any */
		ArrayList<PushedProcessingOutput> pushedProducts = null;
		pushedProducts = pushResults(joWork);
		if (null == pushedProducts) {
			callBack(CALLBACK_STATUS_FAILURE);
			logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		logger.info("Upload summary: listing {} Outputs of type `PushedProcessingOutput`", pushedProducts.size());
		for (PushedProcessingOutput p : pushedProducts) {
			logger.info("PRODUCT_ID={}, FS_TYPE={}, PATH={}, REVISION={}",p.getId(), p.getFsType(),p.getNormedPath()+p.getFileName(),p.getRevision());
		}

		/** STEP [11] Register pushed products using prosEO-Ingestor REST API */
		ArrayList<IngestedProcessingOutput> ingestedProducts = null;
		ingestedProducts = ingestPushedOutputs(pushedProducts);

		if (null == ingestedProducts) {
			callBack(CALLBACK_STATUS_FAILURE);
			logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}

		/** STEP [12] Callback */
		HttpResponseInfo callBackMsg = null;
		callBackMsg = callBack(CALLBACK_STATUS_SUCCESS);
		if (null == callBackMsg) {
			logger.warn("Callback for status {} failed, but wrapper finished with return code {}", CALLBACK_STATUS_FAILURE, EXIT_CODE_OK);	
		}
		logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_OK, EXIT_TEXT_OK);
		return EXIT_CODE_OK;
	}

	/**
	 * Main routine
	 * 
	 * @param args not used due env-var based invocation
	 */
	public static void main(String[] args) {
		System.exit((new BaseWrapper()).run());
	}

}
