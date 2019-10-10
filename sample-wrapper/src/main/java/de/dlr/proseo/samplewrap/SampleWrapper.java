/**
 * SampleWrapper.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 * (C) 2019 Hubert Asamer, DLR
 */
package de.dlr.proseo.samplewrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import alluxio.AlluxioURI;
import alluxio.exception.AlluxioException;
import alluxio.grpc.ReadPType;
import alluxio.grpc.WritePType;
import de.dlr.proseo.model.joborder.InputOutput;
import de.dlr.proseo.model.joborder.IpfFileName;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.joborder.Proc;
import de.dlr.proseo.samplewrap.alluxio.AlluxioOps;
import de.dlr.proseo.samplewrap.s3.AmazonS3URI;
import de.dlr.proseo.samplewrap.s3.S3Ops;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

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

	/** Set path finals for container-context & Alluxio FS-Client*/
	private static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));
	private static final long WRAPPER_TIMESTAMP = System.currentTimeMillis()/1000;
	private static final String CONTAINER_JOF_PATH = WORKING_DIR.toString()+File.separator+String.valueOf(WRAPPER_TIMESTAMP)+".xml";
	private static final String CONTAINER_INPUTS_PATH_PREFIX = "inputs";
	private static final String CONTAINER_OUTPUTS_PATH_PREFIX = String.valueOf(WRAPPER_TIMESTAMP);
	private static final ReadPType ALLUXIO_READ_TYPE = ReadPType.CACHE;
	private static final WritePType ALLUXIO_WRITE_TYPE = WritePType.CACHE_THROUGH;

	/** Error messages */
	private static final String MSG_LEAVING_SAMPLE_WRAPPER = "Leaving sample-wrapper with exit code {} ({})";
	private static final String MSG_STARTING_SAMPLE_WRAPPER = "Starting sample-wrapper V00.00.01 with JobOrder file {}";
	private static final String MSG_INVALID_VALUE_OF_ENVVAR = "Invalid value of EnvVar: {}";
	private static final String MSG_FILE_NOT_READABLE = "File {} is not readable";


	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(SampleWrapper.class);

	/** IPF_Proc valid Tags Enums */
	enum Ipf_Proc {Task_Name,Task_Version,List_of_Inputs,List_of_Outputs}

	/** File_Name valid Attributes Enums */
	enum File_Name {FS_TYPE}

	/** File_Name Attribute FS_TYPE valid entries */
	enum FS_TYPE {S3,POSIX, ALLUXIO}

	/** ENV-VAR valid entries */
	enum ENV_VARS {
		JOBORDER_FS_TYPE
		, JOBORDER_FILE
		, S3_ENDPOINT
		, S3_ACCESS_KEY
		, S3_SECRET_ACCESS_KEY
		, S3_BUCKET_OUTPUTS
		, LOGFILE_TARGET
		, INGESTOR_ENDPOINT
		, STATE_CALLBACK_ENDPOINT
		, SUCCESS_STATE
		, PROCESSOR_SHELL_COMMAND
		, PROCESSING_FACILITY_NAME
	}

	/** Environment Variables from Container (set via run-invocation or directly from docker-image)*/
	private String ENV_JOBORDER_FS_TYPE = System.getenv(ENV_VARS.JOBORDER_FS_TYPE.toString());
	private String ENV_JOBORDER_FILE = System.getenv(ENV_VARS.JOBORDER_FILE.toString());
	private String ENV_S3_ENDPOINT = System.getenv(ENV_VARS.S3_ENDPOINT.toString());
	private String ENV_S3_ACCESS_KEY = System.getenv(ENV_VARS.S3_ACCESS_KEY.toString());
	private String ENV_S3_SECRET_ACCESS_KEY = System.getenv(ENV_VARS.S3_SECRET_ACCESS_KEY.toString());
	private String ENV_S3_BUCKET_OUTPUTS = System.getenv(ENV_VARS.S3_BUCKET_OUTPUTS.toString());
	private String ENV_LOGFILE_TARGET = System.getenv(ENV_VARS.LOGFILE_TARGET.toString());
	private String ENV_STATE_CALLBACK_ENDPOINT = System.getenv(ENV_VARS.STATE_CALLBACK_ENDPOINT.toString());
	private String ENV_SUCCESS_STATE = System.getenv(ENV_VARS.SUCCESS_STATE.toString());
	private String ENV_PROCESSOR_SHELL_COMMAND = System.getenv(ENV_VARS.PROCESSOR_SHELL_COMMAND.toString());
	private String ENV_PROCESSING_FACILITY_NAME = System.getenv(ENV_VARS.PROCESSING_FACILITY_NAME.toString());
	private String ENV_INGESTOR_ENDPOINT = System.getenv(ENV_VARS.INGESTOR_ENDPOINT.toString());

	/** Base V2 S3-Client */
	private S3Client v2S3Client() {
		try {
			//check if S3 env vars are set
			if(ENV_S3_ACCESS_KEY == null || ENV_S3_ACCESS_KEY.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_ACCESS_KEY); return null;}
			if(ENV_S3_SECRET_ACCESS_KEY == null || ENV_S3_SECRET_ACCESS_KEY.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_SECRET_ACCESS_KEY); return null;}
			if(ENV_S3_ENDPOINT==null||ENV_S3_ENDPOINT.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_ENDPOINT); return null;}

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
	}

	/** Base V1 S3-Client */
	private AmazonS3 v1S3Client() {
		try {
			//check if S3 env vars are set
			if(ENV_S3_ACCESS_KEY == null || ENV_S3_ACCESS_KEY.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_ACCESS_KEY); return null;}
			if(ENV_S3_SECRET_ACCESS_KEY == null || ENV_S3_SECRET_ACCESS_KEY.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_SECRET_ACCESS_KEY); return null;}
			if(ENV_S3_ENDPOINT==null||ENV_S3_ENDPOINT.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_ENDPOINT); return null;}

			BasicAWSCredentials awsCreds = new BasicAWSCredentials(ENV_S3_ACCESS_KEY,ENV_S3_SECRET_ACCESS_KEY);
			ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setSignerOverride("AWSS3V4SignerType");
			AmazonS3 amazonS3 = AmazonS3ClientBuilder
					.standard()
					.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(ENV_S3_ENDPOINT, Regions.EU_CENTRAL_1.name()))
					.withPathStyleAccessEnabled(true)
					.withClientConfiguration(clientConfiguration)
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
					.build();
			return amazonS3;
		}	catch (AmazonServiceException e) {
			logger.error(e.getMessage());
			return null;
		}  catch(AmazonClientException e) {
			logger.error(e.getMessage());
			return null;
		} catch (java.lang.NullPointerException e) {
			logger.error(e.getMessage());
			return null;
		}

	}

	/** Check if FS_TYPE value is valid */
	private Boolean checkFS_TYPE(String fs_type) {
		for (FS_TYPE c : FS_TYPE.values()) {
			if (c.name().equals(fs_type)) {
				return true;
			} 
		}
		return false;
	}

	/** Check if Output-File/Dir - name has trailing slash */
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

	private ArrayList<String> envList(ENV_VARS[] env){
		ArrayList<String> list = new ArrayList<String>();
		for (int i=0;i<ENV_VARS.values().length;i++) {
			list.add(ENV_VARS.values()[i].toString()+"="+System.getenv(ENV_VARS.values()[i].toString().toString()));
		}
		return list;
	}

	private String splash() {	
		return "\n" +"\n" + 
				"                                                  _|_|_|_|      _|_|\n" + 
				" _|_|_|    _|  _|_|    _|_|      _|_|_|  _|            _|      _|\n" + 
				" _|    _|  _|_|      _|      _|  _|_|      _|_|_|       _|      _|\n" + 
				" _|    _|  _|        _|       _|      _|_|  _|            _|      _|\n" + 
				" _|_|_|   _|           _|_|    _|_|_|      _|_|_|_|      _|_|\n" + 
				" _|\n" + 
				" _|\n";
	}
	
	private Boolean checkEnv() {
		logger.info("Checking {} ENV_VARS...", ENV_VARS.values().length);
		for (String e : envList(ENV_VARS.values())) {
			logger.info("... {}", e);
		}
		// check all required env-vars
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
		if (ENV_LOGFILE_TARGET == null || ENV_LOGFILE_TARGET.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.LOGFILE_TARGET);
			return false;
		}
		if (ENV_STATE_CALLBACK_ENDPOINT == null || ENV_STATE_CALLBACK_ENDPOINT.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.STATE_CALLBACK_ENDPOINT);
			return false;
		}
		if (ENV_SUCCESS_STATE == null || ENV_SUCCESS_STATE.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.SUCCESS_STATE);
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
		if(ENV_S3_BUCKET_OUTPUTS==null || ENV_S3_BUCKET_OUTPUTS.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_BUCKET_OUTPUTS);
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
	 * provide initial JobOrderFile
	 * 
	 * @return the JobOrder file
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
			S3Client s3 = v2S3Client();
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
		logger.info("Fetch Inputs & provide a valid JobOrderFile for the container-context...");
		int numberOfInputs = 0;
		int numberOfFetchedInputs = 0;
		int numberOfOutputs = 0;
		S3Client s3 = v2S3Client();
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
			s3.close();
			return null;
		}
	}

	/**
	 * creates valid container-context JobOrderFile under given path
	 * 
	 * @param JobOrder remapped JobOrder object
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
	 * Pushes processing results to prosEO storage
	 * 
	 * @param jo jobOrder  JobOrder-Object (valid in container context)
	 * @return ArrayList<PushedProcessingOutput> all infos of pushed products
	 */
	private ArrayList<PushedProcessingOutput> pushResults(JobOrder jo) {
		logger.info("Uploading results to prosEO storage...");
		logger.info("Upload File-Pattern based on timestamp-prefix is: FS_TYPE://<product_id>/{}/<filename>", WRAPPER_TIMESTAMP);
		AmazonS3 s3 = v1S3Client();
		int numberOfOutputs = 0;
		int numberOfPushedOutputs = 0;
		ArrayList<PushedProcessingOutput> pushedOutputs = new ArrayList<PushedProcessingOutput>();
		for(Proc item : jo.getListOfProcs()) {
			// loop all Outputs
			for (InputOutput io: item.getListOfOutputs()) {
				for (IpfFileName fn: io.getFileNames()) {
					numberOfOutputs++;
					// Push files to ALLUXIO
					if(fn.getFSType().equals(FS_TYPE.ALLUXIO.toString())) {
						try {
							AlluxioURI srcPath = new AlluxioURI(fn.getFileName());
							AlluxioURI dstPath = new AlluxioURI(File.separator+io.getProductID()+File.separator+fn.getFileName());
							Boolean transaction = AlluxioOps.copyFromLocal(srcPath, dstPath, ALLUXIO_WRITE_TYPE);
							if(transaction) {
								numberOfPushedOutputs++;
								PushedProcessingOutput p = new PushedProcessingOutput();
								p.setFsType(FS_TYPE.ALLUXIO.toString());
								p.setId(io.getProductID());
								p.setPath(dstPath.toString());
								p.setRevision(WRAPPER_TIMESTAMP);
								pushedOutputs.add(p);
							}
						} catch(AlluxioException | IOException e) {
							logger.error(e.getMessage());
						}
					}
					// Push files to S3 using multipart upload
					if(fn.getFSType().equals(FS_TYPE.S3.toString())) {
						// check if we have a valid output bucket defined...
						if(ENV_S3_BUCKET_OUTPUTS==null||ENV_S3_BUCKET_OUTPUTS.isEmpty()) {
							logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.S3_BUCKET_OUTPUTS);
							return null;
						}
						try {
							Boolean transaction = S3Ops.v1Upload(s3, fn.getFileName(), ENV_S3_BUCKET_OUTPUTS, io.getProductID(), false);
							if(transaction) {
								numberOfPushedOutputs++;
								PushedProcessingOutput p = new PushedProcessingOutput();
								p.setFsType(FS_TYPE.S3.toString());
								p.setId(io.getProductID());
								p.setPath(ENV_S3_BUCKET_OUTPUTS+File.separator+io.getProductID()+File.separator+fn.getFileName());
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
	 * @param pushedProducts ArrayList<PushedProcessingOutput>
	 * @return HTTP response code of Ingestor-API
	 */
	private int ingestPushedOutputs(ArrayList<PushedProcessingOutput> pushedProducts) {

       //POST http://localhost:8080/ingest/proseo-otc01/928928398
		Client client = null;
		WebTarget webTarget = null;
		Invocation.Builder invocationBuilder = null;
		Response response = null;
		client = ClientBuilder.newClient( new ClientConfig());
		
		
		try {
		webTarget = client.target(ENV_INGESTOR_ENDPOINT).path("/ingest/"+ENV_PROCESSING_FACILITY_NAME+"/"+"XYZ");

		invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON);
		
		String request="{\n" + 
				"    \"id\": 1833725,\n" + 
				"    \"version\": 0,\n" + 
				"    \"productId\": 689327,\n" + 
				"    \"processingFacilityName\" : \"DLR_S5P_VAL\",\n" + 
				"    \"productFileName\": \"S5P_OPER_L0__ENG_A__20190509T212509_20190509T214507_08138_01.RAW\",\n" + 
				"    \"auxFileNames\": [\n" + 
				"        \"S5P_OPER_L0__ODB_1__20190509T212508_20190509T214507_08138_01.RAW\",\n" + 
				"        \"S5P_OPER_L0__ODB_2__20190509T212508_20190509T214507_08138_01.RAW\",\n" + 
				"        \"S5P_OPER_L0__ODB_3__20190509T212508_20190509T214507_08138_01.RAW\",\n" + 
				"        \"S5P_OPER_L0__ODB_4__20190509T212508_20190509T214507_08138_01.RAW\",\n" + 
				"        \"S5P_OPER_L0__ODB_5__20190509T212508_20190509T214507_08138_01.RAW\",\n" + 
				"        \"S5P_OPER_L0__ODB_6__20190509T212508_20190509T214507_08138_01.RAW\",\n" + 
				"        \"S5P_OPER_L0__ODB_7__20190509T212508_20190509T214507_08138_01.RAW\",\n" + 
				"        \"S5P_OPER_L0__ODB_8__20190509T212508_20190509T214507_08138_01.RAW\",\n" + 
				"        \"S5P_OPER_L0__SAT_A__20190509T212509_20190509T214507_08138_01.RAW\"\n" + 
				"    ],\n" + 
				"    \"filePath\": \"/data/proseo/storage_val/S5P_OPER_L0__08138_01\",\n" + 
				"    \"storageType\": \"POSIX\"\n" + 
				"}";
		response = invocationBuilder.post(Entity.entity(request, MediaType.APPLICATION_JSON ));
		

		logger.info(String.valueOf(response.getStatus()));
		return response.getStatus();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return 500;
		}
		
	}

	/**
	 * Perform processing: check env, parse JobOrder file, fetch input files, push output files
	 * 
	 * @param args (not used due env-var based invocation)
	 * @return the program exit code (OK or FAILURE)
	 */
	public int run() {
		
		logger.info(splash());

		/** ProcessorWrapperFlow */
		/** ==================== */

		/** STEP [4][5] Provide the JobOrder file from the invocation arguments */

		logger.info(MSG_STARTING_SAMPLE_WRAPPER, ENV_JOBORDER_FILE);
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

		/** STEP [6][7][8] fetch Inputs & create re-mapped JOF for container context*/
		JobOrder joWork = null;
		joWork = fetchInputData(jobOrderDoc);
		if (null == joWork) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}

		Boolean containerJOF = provideContainerJOF(joWork, CONTAINER_JOF_PATH);
		if (!containerJOF) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}

		/** STEP [CALC] Execute Processor */
		Boolean procRun = runProcessor(
				ENV_PROCESSOR_SHELL_COMMAND,
				CONTAINER_JOF_PATH
				);
		if (!procRun) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}

		/** STEP [9] Push Processing Results to prosEO Storage, if any */
		ArrayList<PushedProcessingOutput> pushedProducts = null;
		pushedProducts = pushResults(joWork);
		if (null == pushedProducts) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		logger.info("Upload summary: listing {} Outputs of type `PushedProcessingOutput`", pushedProducts.size());
		for (PushedProcessingOutput p : pushedProducts) {
			logger.info("PRODUCT_ID={}, FS_TYPE={}, PATH={}, REVISION={}",p.getId(), p.getFsType(),p.getPath(),p.getRevision());
		}

		/** STEP [11] Register pushed products using prosEO-Ingestor REST API */
		
		int httpReturnCode = ingestPushedOutputs(pushedProducts);
		if (httpReturnCode !=201) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_OK, EXIT_TEXT_OK);
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
