/**
 * SampleWrapper.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 * (C) 2019 Hubert Asamer, DLR
 */
package de.dlr.proseo.samplewrap;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.joborder.InputOutput;
import de.dlr.proseo.model.joborder.IpfFileName;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.joborder.Proc;
import de.dlr.proseo.samplewrap.s3.AmazonS3URI;
import de.dlr.proseo.samplewrap.s3.S3Ops;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * prosEO Sample Processor Wrapper - an example of a wrapper for processors conforming to ESA's
 * "Generic IPF Interface Specification" (MMFI-GSEG-EOPG-TN-07-0003, V.1.8)
 * 
 * @author Dr. Thomas Bassler
 * @author Hubert Asamer
 *
 */
public class SampleWrapper {
	
	/** Exit code for successful completion */
	private static final int EXIT_CODE_OK = 0;
	/** Exit code for failure */
	private static final int EXIT_CODE_FAILURE = 255;
	/** Exit code explanation for successful completion */
	private static final String EXIT_TEXT_OK = "OK";
	/** Exit code explanation for failure */
	private static final String EXIT_TEXT_FAILURE = "FAILURE";
	
	/** Error messages */
	private static final String MSG_LEAVING_SAMPLE_WRAPPER = "Leaving sample-wrapper with exit code {} ({})";
	private static final String MSG_STARTING_SAMPLE_WRAPPER = "Starting sample-wrapper V00.00.01 with JobOrder file {}";
	private static final String MSG_INVALID_VALUE_OF_ENVVAR = "Invalid value of EnvVar: {}";
	private static final String MSG_FILE_NOT_READABLE = "File {} is not readable";
	
	/** Nice colors*/
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(SampleWrapper.class);
	
	/** IPF_Proc valid Tags Enums */
    enum Ipf_Proc {Task_Name,Task_Version,List_of_Inputs,List_of_Outputs}
		
    /** File_Name valid Attributes Enums */
	enum File_Name {FS_TYPE}
		
	/** File_Name Attribute FS_TYPE valid entries */
	enum FS_TYPE {S3,POSIX}
	
	/** ENV-VAR valid entries */
	enum ENV_VARS {FS_TYPE,JOBORDER_FILE,S3_ENDPOINT,S3_ACCESS_KEY,S3_SECRET_ACCESS_KEY,LOGFILE_TARGET,STATE_CALLBACK_ENDPOINT,SUCCESS_STATE}
	
	/** Environment Variables from Container (set via run-invocation or directly from docker-image)*/
	private String ENV_FS_TYPE = System.getenv(ENV_VARS.FS_TYPE.toString());
	private String ENV_JOBORDER_FILE = System.getenv(ENV_VARS.JOBORDER_FILE.toString());
	private String ENV_S3_ENDPOINT = System.getenv(ENV_VARS.S3_ENDPOINT.toString());
	private String ENV_S3_ACCESS_KEY = System.getenv(ENV_VARS.S3_ACCESS_KEY.toString());
	private String ENV_S3_SECRET_ACCESS_KEY = System.getenv(ENV_VARS.S3_SECRET_ACCESS_KEY.toString());
	private String ENV_LOGFILE_TARGET = System.getenv(ENV_VARS.LOGFILE_TARGET.toString());
	private String ENV_STATE_CALLBACK_ENDPOINT = System.getenv(ENV_VARS.STATE_CALLBACK_ENDPOINT.toString());
	private String ENV_SUCCESS_STATE = System.getenv(ENV_VARS.SUCCESS_STATE.toString());
	
	
	/** Base S3-Client */
    private S3Client s3Client() {
    	try {
		AwsBasicCredentials creds = AwsBasicCredentials.create( ENV_S3_ACCESS_KEY,ENV_S3_SECRET_ACCESS_KEY);
		Region region = Region.EU_CENTRAL_1;
		S3Client s3 = S3Client.builder()
				.region(region)
				.endpointOverride(URI.create(ENV_S3_ENDPOINT))
				.credentialsProvider(StaticCredentialsProvider.create(creds))
				.build();
		return s3;
    	} catch(software.amazon.awssdk.core.exception.SdkClientException e) {
    		logger.error(e.getMessage());
    		return null;
    	} catch (java.lang.NullPointerException e1) {
    		logger.error(e1.getMessage());
    		return null;
    	}
	};
	
	
	/** Check if FS_TYPE value is valid */
	private Boolean checkFS_TYPE(String fs_type) {
		for (FS_TYPE c : FS_TYPE.values()) {
	        if (c.name().equals(fs_type)) {
	            return true;
	        } 
	    }
		return false;
	}
	
	private Boolean checkEnv() {
		//check all required env-vars
		if(!checkFS_TYPE(ENV_FS_TYPE)) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.FS_TYPE);return false;}
		if(ENV_FS_TYPE == null || ENV_FS_TYPE.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.FS_TYPE);return false;}
		if(ENV_JOBORDER_FILE == null || ENV_JOBORDER_FILE.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.JOBORDER_FILE);return false;}
		if(ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && (ENV_S3_ENDPOINT == null || ENV_S3_ENDPOINT.isEmpty())) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_ENDPOINT);return false;}
		if(ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && (ENV_S3_ACCESS_KEY == null || ENV_S3_ACCESS_KEY.isEmpty())) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_ACCESS_KEY);return false;}
		if(ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && (ENV_S3_SECRET_ACCESS_KEY == null || ENV_S3_SECRET_ACCESS_KEY.isEmpty())) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_SECRET_ACCESS_KEY);return false;}
		if(ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && !ENV_JOBORDER_FILE.startsWith("s3://")) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.FS_TYPE+": "+ENV_FS_TYPE+" does not allow "+ENV_JOBORDER_FILE);return false;};
		if(ENV_FS_TYPE.equals(FS_TYPE.POSIX.toString()) && ENV_JOBORDER_FILE.startsWith("s3://")) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.FS_TYPE+": "+ENV_FS_TYPE+" does not allow "+ENV_JOBORDER_FILE);return false;};
		if(ENV_LOGFILE_TARGET == null || ENV_LOGFILE_TARGET.isEmpty()) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.LOGFILE_TARGET);return false;}
		if(ENV_STATE_CALLBACK_ENDPOINT == null || ENV_STATE_CALLBACK_ENDPOINT.isEmpty()) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.STATE_CALLBACK_ENDPOINT);return false;}
		if(ENV_SUCCESS_STATE == null || ENV_SUCCESS_STATE.isEmpty()) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.SUCCESS_STATE);return false;}
		return true;
	}
	/**
	 * provide valid container-context JobOrderFile
	 * 
	 * @param JobOrder remapped JobOrder object
	 * @param path file path of newly created JOF
	 * @return the JobOrder file valid in container context
	 */
	private Boolean provideContainerJOF(JobOrder jo, String path) {	
		return jo.writeXML(path, false);
	}
	
	/**
	 * provide initial JobOrderFile
	 * 
	 * @return the JobOrder file
	 */
	private File provideInitialJOF() {	
		String JOFContainerPath = null;
		// set JOF-path based on ENV
		if (ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && ENV_JOBORDER_FILE.startsWith("s3://")) {
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
		if (ENV_FS_TYPE.equals(FS_TYPE.POSIX.toString()) && !ENV_JOBORDER_FILE.startsWith("s3://")) {
			JOFContainerPath = ENV_JOBORDER_FILE;
		}
		logger.info(ANSI_YELLOW+MSG_STARTING_SAMPLE_WRAPPER, JOFContainerPath+ANSI_RESET);

		/** Fetch JOF if container env-var `FS_TYPE` has value `S3` */
		if (ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && ENV_JOBORDER_FILE.startsWith("s3://")) {
				S3Client s3 = s3Client();
				if (null == s3) return null;
				Boolean transaction = S3Ops.fetch(s3, ENV_JOBORDER_FILE, JOFContainerPath);
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
	 * @return
	 */
	private JobOrder parseJobOrderFile(File jobOrderFile) {

		JobOrder jobOrderDoc = null;
		jobOrderDoc = new JobOrder();
		jobOrderDoc.read(jobOrderFile.getAbsolutePath());
		
		//jobOrderDoc = docBuilder.parse(jobOrderFile);
		return jobOrderDoc;
	}
	/**
	 * Fetch remote input-data to container-workdir(based on FS_TYPE) and return valid JobOrder object for container-runtime-context.
	 * 
	 * @param jo the JobOrder file to parse
	 * @return JobOrder object valid for container-context
	 */
	private JobOrder fetchInputData(JobOrder jo)
	{
		S3Client s3 = s3Client();
		if (null == s3) return null;
		// loop all procs -> mainly only one is present
		for(Proc item : jo.getListOfProcs()) {
			// loop all Input
			for (InputOutput io: item.getListOfInputs()) {
				// loop List_of_File_Names
				for (IpfFileName fn: io.getFileNames()) {
					//fill original filename with current val of `File_Name` --> for later use...
					fn.setOriginalFileName(fn.getFileName());
					// for all S3-data we try to fetch to workdir...
					if(fn.getFSType().equals(FS_TYPE.S3.toString()) && fn.getOriginalFileName().startsWith("s3://")) {
						// first set file_name to local work-dir path
						fn.setFileName(fn.getOriginalFileName().replace("s3://", ""));
						// now fetch from S3
						Boolean transaction = S3Ops.fetch(s3, fn.getOriginalFileName(), fn.getFileName());
						logger.info("fetched "+fn.getOriginalFileName());
						if (!transaction) return null;
					}
				}
			}
			// loop all Output
			for (InputOutput io: item.getListOfOutputs()) {
				// loop List_of_File_Names
				for (IpfFileName fn: io.getFileNames()) {
					//fill original filename with current val of `File_Name` --> for later use --> push results step
					fn.setOriginalFileName(fn.getFileName());
					if(fn.getFSType().equals(FS_TYPE.S3.toString()) && fn.getOriginalFileName().startsWith("s3://")) {
						// first set output file_name to local work-dir path
						fn.setFileName(fn.getOriginalFileName().replace("s3://", ""));
						try {
						  File f = new File(fn.getFileName());
						  if (Files.exists(Paths.get(fn.getFileName()), LinkOption.NOFOLLOW_LINKS)) f.delete();
						  else f.mkdirs();
						} catch (SecurityException e) {
							logger.error(e.getMessage());
							return null;
						}
					}
				}
			}
		}
		s3.close();
		return jo;
	}

	/**
	 * Perform the dummy processing: check env, parse JobOrder file, read one input file, create one output file
	 * 
	 * @param args (not used due env-var based invocation)
	 * @return the program exit code (OK or FAILURE)
	 */
	public int run() {

		/** ProcessorWrapperFlow */
		/** ==================== */

		/** STEP [4][5] Provide the JobOrder file from the invocation arguments */
		
		Boolean check = checkEnv();
		if (!check) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		File jobOrderFile = provideInitialJOF();
		if (null == jobOrderFile) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		JobOrder jobOrderDoc = parseJobOrderFile(jobOrderFile);
		if (null == jobOrderDoc) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		/** STEP [6][7][8] request Inputs & remap JOF*/
		JobOrder joWork = null;
		joWork = fetchInputData(jobOrderDoc);
		if (null == joWork) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		Boolean containerJOF = provideContainerJOF(joWork, "Container-JOF.xml");
		if (!containerJOF) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		
		// DEMO:
//		logger.info(ANSI_YELLOW+Ipf_Proc.List_of_Inputs.toString()+ANSI_RESET);
//		for (String[] in : listOfInputs) {
//			System.out.println(in[0]+":"+in[1]);
//		}
//		logger.info(ANSI_YELLOW+Ipf_Proc.List_of_Outputs.toString()+ANSI_RESET);
//		for (String[] out : listOfOutputs) {
//			System.out.println(out[0]+":"+out[1]);
//		}
		
		logger.info(ANSI_GREEN+MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_OK, EXIT_TEXT_OK+ANSI_RESET);
		return EXIT_CODE_OK;
	}

	/**
	 * Main routine
	 * 
	 * @param args not used due env-var based invocation
	 */
	public static void main(String[] args) {
		System.exit((new SampleWrapper()).run());
	}

}
