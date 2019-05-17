/**
 * SampleWrapper.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.samplewrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;

/**
 * prosEO Sample Processor Wrapper - an example of a wrapper for processors conforming to ESA's
 * "Generic IPF Interface Specification" (MMFI-GSEG-EOPG-TN-07-0003, V.1.8)
 * 
 * @author Dr. Thomas Bassler
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
	
	// Error messages
	private static final String MSG_LEAVING_SAMPLE_WRAPPER = "Leaving sample-wrapper with exit code {} ({})";
	private static final String MSG_STARTING_SAMPLE_WRAPPER = "Starting sample-wrapper V00.00.01 with JobOrder file {}";
	private static final String MSG_INVALID_NUMBER_OF_ARGUMENTS = "Invalid number of invocation arguments: {} (only 1 allowed)";
	private static final String MSG_FILE_NOT_READABLE = "File {} is not readable";
	private static final String MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER = "Error instantiating DocumentBuilder: {}";
	private static final String MSG_JOF_NOT_PARSEABLE = "JobOrder file {} not parseable ({})";
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(SampleWrapper.class);
	
	/*
	 * Check the invocation arguments: only one argument allowed, which must be a path to a JobOrder XML file
	 * 
	 * @param args the invocation arguments
	 * @return the JobOrder file
	 */
	private File checkArguments(String[] args) {
		if (1 != args.length) {
			logger.error(MSG_INVALID_NUMBER_OF_ARGUMENTS, args.length);
			return null;
		}
		String jobOrderFileName = args[0];
		if (!Files.isReadable(FileSystems.getDefault().getPath(jobOrderFileName))) {
			logger.error(MSG_FILE_NOT_READABLE, jobOrderFileName);
			return null;
		}
		
		return new File(jobOrderFileName);
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
	
	/*
	 * Perform the dummy processing: check arguments, parse JobOrder file, read one input file, create one output file
	 * 
	 * @param args the invocation arguments (only one argument allowed, namely the path to a JobOrder XML file)
	 * @return the program exit code (OK or FAILURE)
	 */
	public int run(String[] args) {
		logger.info(MSG_STARTING_SAMPLE_WRAPPER, (0 < args.length ? args[0] : "null"));
		
		// Get the JobOrder file from the invocation arguments
		File jobOrderFile = checkArguments(args);
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
		
		// TODO Do something here
		
		// Everything went well
		logger.info(MSG_LEAVING_SAMPLE_WRAPPER, EXIT_CODE_OK, EXIT_TEXT_OK);
		return EXIT_CODE_OK;
	}

	/**
	 * Main routine, to be called with a path to a JobOrder XML file as single argument
	 * 
	 * @param args the invocation arguments (only one argument allowed, namely the path to a JobOrder XML file)
	 */
	public static void main(String[] args) {
		System.exit((new SampleWrapper()).run(args));
	}

}
