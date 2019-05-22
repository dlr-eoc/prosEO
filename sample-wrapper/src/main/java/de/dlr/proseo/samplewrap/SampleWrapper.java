/**
 * SampleWrapper.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 * (C) 2019 Hubert Asamer
 */
package de.dlr.proseo.samplewrap;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;

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
	private static final String MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER = "Error instantiating DocumentBuilder: {}";
	private static final String MSG_JOF_NOT_PARSEABLE = "JobOrder file {} not parseable ({})";
	private static final String MSG_JOF_XPATH_NOT_FOUND = "JobOrder file Xpath {} not parseable ({})";
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(SampleWrapper.class);
	
	/** IPF XPATH-consts */
	private static final String XPATH_LIST_OF_INPUTS = "/Ipf_Job_Order/List_of_Ipf_Procs/Ipf_Proc/List_of_Inputs/*/List_of_File_Names/File_Name";
	//private static final String XPATH_LIST_OF_INPUTS = "/Ipf_Job_Order/List_of_Ipf_Procs/Ipf_Proc/List_of_Inputs/*/List_of_File_Names/File_Name[@FS_TYPE='S3']";
	private static final String XPATH_LIST_OF_OUTPUTS = "/Ipf_Job_Order/List_of_Ipf_Procs/Ipf_Proc/List_of_Outputs/*/File_Name";
	
	/** IPF_Proc valid Tags Enums */
    enum Ipf_Proc {Task_Name,Task_Version,List_of_Inputs,List_of_Outputs}
		
    /** File_Name valid Attributes Enums */
	enum File_Name {FS_TYPE}
		
	/** File_Name Attribute FS_TYPE valid entries */
	enum FS_TYPE {S3,POSIX}
	
	/** Environment Variables from Container (set via run-invocation or directly from docker-image)*/
	private String ENV_FS_TYPE = System.getenv("FS_TYPE");
	private String ENV_JOBORDER_FILE = System.getenv("JOBORDER_FILE");
	private String ENV_S3_ENDPOINT = System.getenv("S3_ENDPOINT");
	private String ENV_S3_ACCESS_KEY = System.getenv("S3_ACCESS_KEY");
	private String ENV_S3_SECRET_ACCESS_KEY = System.getenv("S3_SECRET_ACCESS_KEY");
	private String ENV_LOGFILE_TARGET = System.getenv("LOGFILE_TARGET");
	private String ENV_STATE_CALLBACK_ENDPOINT = System.getenv("STATE_CALLBACK_ENDPOINT");
	private String ENV_SUCCESS_STATE = System.getenv("SUCCESS_STATE");
	private String ENV_CONTAINER_WORKDIR = System.getenv("CONTAINER_WORKDIR");
	
	/** Base S3-Client */
	private static S3Client s3;
	
	private Boolean checkEnvFS_TYPE() {
		for (FS_TYPE c : FS_TYPE.values()) {
	        if (c.name().equals(ENV_FS_TYPE)) {
	            return true;
	        } 
	    }
		return false;
	}
	
	private Boolean checkEnv() {
		//check all required env-vars
		
		if(!checkEnvFS_TYPE()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "FS_TYPE");return false;}
		if(ENV_FS_TYPE == null || ENV_FS_TYPE.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "FS_TYPE");return false;}
		if(ENV_JOBORDER_FILE == null || ENV_JOBORDER_FILE.isEmpty()) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "JOBORDER_FILE");return false;}
		if(ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && (ENV_S3_ENDPOINT == null || ENV_S3_ENDPOINT.isEmpty())) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "S3_ENDPOINT");return false;}
		if(ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && (ENV_S3_ACCESS_KEY == null || ENV_S3_ACCESS_KEY.isEmpty())) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "S3_ACCESS_KEY");return false;}
		if(ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && (ENV_S3_SECRET_ACCESS_KEY == null || ENV_S3_SECRET_ACCESS_KEY.isEmpty())) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "S3_SECRET_ACCESS_KEY");return false;}
		if(ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && !ENV_JOBORDER_FILE.startsWith("s3://")) {logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "FS_TYPE does not allow ENV_JOBORDER_FILE");return false;};
		if(ENV_LOGFILE_TARGET == null || ENV_LOGFILE_TARGET.isEmpty()) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "LOGFILE_TARGET");return false;}
		if(ENV_STATE_CALLBACK_ENDPOINT == null || ENV_STATE_CALLBACK_ENDPOINT.isEmpty()) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "STATE_CALLBACK_ENDPOINT");return false;}
		if(ENV_SUCCESS_STATE == null || ENV_SUCCESS_STATE.isEmpty()) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "SUCCESS_STATE");return false;}
		if(ENV_CONTAINER_WORKDIR == null || ENV_CONTAINER_WORKDIR.isEmpty()) { logger.error(MSG_INVALID_VALUE_OF_ENVVAR, "CONTAINER_WORKDIR");return false;}
		return true;
	}
	/**
	 * check environment and provide JobOrderFile
	 * 
	 * @return the JobOrder file
	 */
	private File provideJOF() {
		
		String JOFContainerPath = null;
		
		
		
		// set JOF-path based on ENV
		if (ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && ENV_JOBORDER_FILE.startsWith("s3://")) {
			AmazonS3URI s3uri = new AmazonS3URI(ENV_JOBORDER_FILE);
			JOFContainerPath = ENV_CONTAINER_WORKDIR+File.separator+s3uri.getKey();
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
		
		logger.info(MSG_STARTING_SAMPLE_WRAPPER, JOFContainerPath);
		

		/** Fetch JOF if container env-var `FS_TYPE` has value `S3` */
		if (ENV_FS_TYPE.equals(FS_TYPE.S3.toString()) && ENV_JOBORDER_FILE.startsWith("s3://")) {
			try {
				AmazonS3URI s3uri = new AmazonS3URI(ENV_JOBORDER_FILE);
				AwsBasicCredentials creds = AwsBasicCredentials.create( ENV_S3_ACCESS_KEY,ENV_S3_SECRET_ACCESS_KEY);
				Region region = Region.EU_CENTRAL_1;
				s3 = S3Client.builder()
						.region(region)
						.endpointOverride(URI.create(ENV_S3_ENDPOINT))
						.credentialsProvider(StaticCredentialsProvider.create(creds))
						.build();
				s3.getObject(GetObjectRequest.builder()
						.bucket(s3uri.getBucket())
						.key(s3uri.getKey())
						.build(),
						ResponseTransformer.toFile(Paths.get(JOFContainerPath)));
			} catch (software.amazon.awssdk.core.exception.SdkClientException e) {
				logger.error(e.getMessage());
				return null;
			} catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e1) {
				logger.error(e1.getMessage());
				return null;
			} catch (software.amazon.awssdk.services.s3.model.NoSuchBucketException e2) {
				logger.error(e2.getMessage());
				return null;
			} catch (software.amazon.awssdk.services.s3.model.S3Exception e3) {
				logger.error(e3.getMessage());
				return null;
			}
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
	private Document parseJobOrderFile(File jobOrderFile) {
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error(MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER, e.getMessage());
			return null;
		}
		Document jobOrderDoc = null;
		try {
			jobOrderDoc = docBuilder.parse(jobOrderFile);
		} catch (SAXException | IOException e) {
			logger.error(MSG_JOF_NOT_PARSEABLE, jobOrderFile.getPath(), e.getMessage());
			return null;
		}
		return jobOrderDoc;
	}
	/**
	 * Get List of Input/Output Products from JOF Doc
	 * 
	 * @param jobOrder the Documet file to parse
	 * @param ipfProc enum weather to list Inputs or Outputs
	 * @return
	 */
	private ArrayList<String> getFileListFromJobOrderDoc(Document jobOrder, Ipf_Proc ipfProc)
	{
		ArrayList<String> listOfInputs=new ArrayList<String>();
		String expression = null;;
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			if (ipfProc == Ipf_Proc.List_of_Inputs) expression = XPATH_LIST_OF_INPUTS;
			if (ipfProc == Ipf_Proc.List_of_Outputs) expression = XPATH_LIST_OF_OUTPUTS;
			XPathExpression expr = xpath.compile(expression);
			NodeList inputFiles = (NodeList) expr.evaluate(jobOrder, XPathConstants.NODESET);
			for (int i=0; i<inputFiles.getLength();i++) {
				listOfInputs.add(inputFiles.item(i).getTextContent()+"\n");
			}
		} catch (XPathExpressionException e) {
			logger.error(MSG_JOF_XPATH_NOT_FOUND, expression, e.getMessage());
			return null;
		}
		return listOfInputs;
	}
	
	/*
	 * Perform the dummy processing: check arguments, parse JobOrder file, read one input file, create one output file
	 * 
	 * @param args the invocation arguments (only one argument allowed, namely the path to a JobOrder XML file)
	 * @return the program exit code (OK or FAILURE)
	 */
	public int run() {

		// ProcessorWrapperFlow
		// ====================

		// STEP [4][5] Get the JobOrder file from the invocation arguments
		Boolean check = checkEnv();
		if (!check) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		File jobOrderFile = provideJOF();

		if (null == jobOrderFile) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		// Parse the JobOrder file
		Document jobOrderDoc = parseJobOrderFile(jobOrderFile);
		if (null == jobOrderDoc) {
			logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		
		// STEP [6][7] request Inputs
		System.out.println(getFileListFromJobOrderDoc(jobOrderDoc, Ipf_Proc.List_of_Outputs));
		
		
		
		logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_OK, EXIT_TEXT_OK);
		return EXIT_CODE_OK;
	}

	/**
	 * Main routine, to be called with a path to a JobOrder XML file as single argument
	 * 
	 * @param args the invocation arguments (only one argument allowed, namely the path to a JobOrder XML file)
	 */
	public static void main(String[] args) {
		System.exit((new SampleWrapper()).run());
	}

}
