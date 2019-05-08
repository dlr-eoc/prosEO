/**
 * SampleProcessor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.sampleproc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * prosEO Sample Processor - a minimal dummy processor conforming to ESA's
 * "Generic IPF Interface Specification" (MMFI-GSEG-EOPG-TN-07-0003, V.1.8)
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class SampleProcessor {
	
	/** Exit code for successful completion */
	private static final int EXIT_CODE_OK = 0;
	/** Exit code for failure */
	private static final int EXIT_CODE_FAILURE = 255;
	/** Exit code explanation for successful completion */
	private static final String EXIT_TEXT_OK = "OK";
	/** Exit code explanation for failure */
	private static final String EXIT_TEXT_FAILURE = "FAILURE";
	
	// Error messages
	private static final String MSG_LEAVING_SAMPLE_PROCESSOR = "Leaving sample-processor with exit code {} ({})";
	private static final String MSG_STARTING_SAMPLE_PROCESSOR = "Starting sample-processor V00.00.01 with JobOrder file {}";
	private static final String MSG_INVALID_NUMBER_OF_ARGUMENTS = "Invalid number of invocation arguments: {} (only 1 allowed)";
	private static final String MSG_INVALID_FILE_CONTENT = "Invalid content in file {} (expecting 'id|type|startTime|stopTime|revision')";
	private static final String MSG_FILE_NOT_READABLE = "File {} is not readable";
	private static final String MSG_FILE_NOT_CLOSABLE = "Cannot close file {}";
	private static final String MSG_FILE_NOT_WRITABLE = "File {} is not writable";
	private static final String MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER = "Error instantiating DocumentBuilder: {}";
	private static final String MSG_JOF_NOT_PARSEABLE = "JobOrder file {} not parseable ({})";
	private static final String MSG_JOF_TAG_MISSING = "JobOrder file does not contain element with tag {}";

	// Tags for JobOrder XML file
	private static final String JOF_TAG_INPUT = "Input";
	private static final String JOF_TAG_OUTPUT = "Output";
	private static final String JOF_TAG_FILE_NAME = "File_Name";
	private static final String JOF_TAG_FILE_TYPE = "File_Type";
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(SampleProcessor.class);
	
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
	
	/**
	 * Read the input product file named in the given JobOrder document
	 * 
	 * @param jobOrderDoc the JobOrder document containing the input product file path
	 * @return
	 */
	private SampleProduct readInputProduct(Document jobOrderDoc) {
		// Get the first input file name from the JobOrder document (ignore everything else)
		NodeList inputFiles = jobOrderDoc.getElementsByTagName(JOF_TAG_INPUT);
		if (0 == inputFiles.getLength()) {
			logger.error(MSG_JOF_TAG_MISSING, JOF_TAG_INPUT);
			return null;
		}
		NodeList inputFileNames = ((Element) inputFiles.item(0)).getElementsByTagName(JOF_TAG_FILE_NAME);
		if (0 == inputFileNames.getLength()) {
			logger.error(MSG_JOF_TAG_MISSING, JOF_TAG_FILE_NAME);
			return null;
		}
		
		// Create an input product from the file content (expecting 'id|type|startTime|stopTime|revision')
		String inputFileName = inputFileNames.item(0).getTextContent();
		BufferedReader input = null;
		try {
			input = new BufferedReader(new FileReader(new File(inputFileName)));
		} catch (FileNotFoundException e) {
			logger.error(MSG_FILE_NOT_READABLE, inputFileName);
			return null;
		}
		
		SampleProduct inputProduct = new SampleProduct();
		try {
			String[] fields = input.readLine().split("\\|");
			logger.debug("... input product read with {} fields", fields.length);
			if (5 != fields.length) {
				logger.error(MSG_INVALID_FILE_CONTENT, inputFileName);
				return null;
			}
			inputProduct.setId(fields[0]);
			inputProduct.setType(fields[1]);
			inputProduct.setStartTime(Instant.parse(fields[2]));
			inputProduct.setStopTime(Instant.parse(fields[3]));
			inputProduct.setRevision(Integer.parseInt(fields[4]));
		} catch (IOException e1) {
			logger.error(MSG_FILE_NOT_READABLE, inputFileName);
			return null;
		} catch (DateTimeParseException | NumberFormatException e) {
			logger.error(MSG_INVALID_FILE_CONTENT, inputFileName);
			return null;
		} finally {
			// Close input file
			try {
				input.close();
			} catch (IOException e) {
				logger.warn(MSG_FILE_NOT_CLOSABLE, inputFileName);
			}
		}
				
		// Return newly created product
		return inputProduct;
	}
	
	/**
	 * Create output products from the input product (more or less ;-) ), using the
	 * output product file names given in the JobOrder document
	 * 
	 * @param inputProduct the input product to process
	 * @param jobOrderDoc the JobOrder document containing the output product file path
	 * @return
	 */
	private boolean processOutputProduct(SampleProduct inputProduct, Document jobOrderDoc) {
		// Get all output products from the JobOrder document (ignore everything else)
		NodeList outputFiles = jobOrderDoc.getElementsByTagName(JOF_TAG_OUTPUT);
		if (0 == outputFiles.getLength()) {
			logger.error(MSG_JOF_TAG_MISSING, JOF_TAG_OUTPUT);
			return false;
		}
		
		// For each output product, get the requested file name and type and generate the product
		for (int i = 0; i < outputFiles.getLength(); ++i) {
			NodeList outputFileNames = ((Element) outputFiles.item(i)).getElementsByTagName(JOF_TAG_FILE_NAME);
			if (0 == outputFileNames.getLength()) {
				logger.error(MSG_JOF_TAG_MISSING, JOF_TAG_FILE_NAME);
				return false;
			}
			NodeList outputFileTypes = ((Element) outputFiles.item(i)).getElementsByTagName(JOF_TAG_FILE_TYPE);
			if (0 == outputFileTypes.getLength()) {
				logger.error(MSG_JOF_TAG_MISSING, JOF_TAG_FILE_TYPE);
				return false;
			}
			
			// Generate a product with random identifier and the file type specified in the JobOrder document
			SampleProduct outputProduct = new SampleProduct();
			outputProduct.setId(String.valueOf((long) Math.random()*(2^24 - 1)));
			outputProduct.setType(outputFileTypes.item(0).getTextContent());
			outputProduct.setStartTime(inputProduct.getStartTime());
			outputProduct.setStopTime(inputProduct.getStopTime());
			outputProduct.setRevision(inputProduct.getRevision());
			
			// Write the output product to a file
			String outputFileName = outputFileNames.item(0).getTextContent();
			logger.debug("... creating output product file {}", outputFileName);
			BufferedWriter output = null;
			try {
				output = new BufferedWriter(new FileWriter(new File(outputFileName)));
				output.write(
					String.format("%s|%s|%s|%s|%d", outputProduct.getId(), outputProduct.getType(),
						outputProduct.getStartTime().toString(), outputProduct.getStopTime().toString(),
						outputProduct.getRevision()
					)
				);
				output.newLine();
			} catch (IOException e) {
				logger.error(MSG_FILE_NOT_WRITABLE, outputFileName);
				return false;
			} finally {
				if (null != output) {
					try {
						output.close();
					} catch (IOException e) {
						logger.warn(MSG_FILE_NOT_CLOSABLE, outputFileName);
					}
				}
			}
		}
		
		// Everything OK
		return true;
	}
	
	/*
	 * Perform the dummy processing: check arguments, parse JobOrder file, read one input file, create one output file
	 * 
	 * @param args the invocation arguments (only one argument allowed, namely the path to a JobOrder XML file)
	 * @return the program exit code (OK or FAILURE)
	 */
	public int run(String[] args) {
		logger.info(MSG_STARTING_SAMPLE_PROCESSOR, (0 < args.length ? args[0] : "null"));
		
		// Get the JobOrder file from the invocation arguments
		File jobOrderFile = checkArguments(args);
		if (null == jobOrderFile) {
			logger.info(MSG_LEAVING_SAMPLE_PROCESSOR, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		// Parse the JobOrder file
		Document jobOrderDoc = parseJobOrderFile(jobOrderFile);
		if (null == jobOrderDoc) {
			logger.info(MSG_LEAVING_SAMPLE_PROCESSOR, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		// Read some data from the input file
		SampleProduct inputProduct = readInputProduct(jobOrderDoc);
		if (null == inputProduct) {
			logger.info(MSG_LEAVING_SAMPLE_PROCESSOR, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		// Process input product to output product
		boolean ok = processOutputProduct(inputProduct, jobOrderDoc);
		if (!ok) {
			logger.info(MSG_LEAVING_SAMPLE_PROCESSOR, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		}
		
		// Everything went well
		logger.info(MSG_LEAVING_SAMPLE_PROCESSOR, EXIT_CODE_OK, EXIT_TEXT_OK);
		return EXIT_CODE_OK;
	}

	/**
	 * Main routine, to be called with a path to a JobOrder XML file as single argument
	 * 
	 * @param args the invocation arguments (only one argument allowed, namely the path to a JobOrder XML file)
	 */
	public static void main(String[] args) {
		System.exit((new SampleProcessor()).run(args));
	}

}
