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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * prosEO Sample Processor - a minimal dummy processor conforming to ESA's
 * "Generic IPF Interface Specification" (MMFI-GSEG-EOPG-TN-07-0003, V.1.8)
 * 
 * @author Dr. Thomas Bassler
 * @author Hubert Asamer
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
	private static final String MSG_INVALID_NUMBER_OF_OUTPUT_FILES = "Invalid number of output files: {} (exactly 1 expected)";
	private static final String MSG_INVALID_NUMBER_OF_OUTPUT_FILE_TYPES = "Invalid number of output file types: {} (exactly 1 expected)";
	private static final String MSG_INVALID_OUTPUT_FILE_TYPE = "Invalid output file type: {}";
	private static final String MSG_INVALID_NUMBER_OF_INPUT_FILES = "Invalid number of input files for output file type {}: {} (exactly 3 expected)";
	private static final String MSG_INVALID_NUMBER_OF_INPUT_FILE_TYPES = "Invalid number of input file types: {} (exactly 1 expected)";
	private static final String MSG_INVALID_NUMBER_OF_INPUT_FILE_NAMES = "Invalid number of input file names: {} (exactly 1 expected)";
	private static final String MSG_INVALID_NUMBER_OF_TASK_NAMES = "Invalid number of task names: {} (exactly 1 expected)";
	private static final String MSG_INVALID_TASK_NAME_IN_JOF = "Invalid task name {} in JobOrder file (expected {})";
	private static final String MSG_INVALID_NUMBER_OF_CONFIGURATION_FILES = "Invalid number of configuration files for output file type {}: {} (exactly 1 expected)";
	private static final String MSG_INVALID_NUMBER_OF_CONFIGURATION_FILE_NAMES = "Invalid number of configuration file names: {} (exactly 1 expected)";
	private static final String MSG_NO_VALID_INPUT_FILE_IN_JOF = "No valid input file in Job Order file for output file type {}";
	private static final String MSG_INVALID_FILE_CONTENT = "Invalid content in file {} (expecting 'id|type|startTime|stopTime|revision')";
	private static final String MSG_FILE_NOT_READABLE = "File {} is not readable";
	private static final String MSG_FILE_NOT_CLOSABLE = "Cannot close file {}";
	private static final String MSG_FILE_NOT_WRITABLE = "File {} is not writable";
	private static final String MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER = "Error instantiating DocumentBuilder: {}";
	private static final String MSG_JOF_NOT_PARSEABLE = "JobOrder file {} not parseable ({})";
	private static final String MSG_JOF_TAG_MISSING = "JobOrder file does not contain element with tag {}";
	private static final String MSG_CONFIGURATION_DOC_NOT_PARSEABLE = "Configuration file {} not parseable ({})";
	private static final String MSG_PARAMETER_NAME_MISSING = "Configuration file contains PARAM element without name attribute";
	private static final String MSG_TASK_NAME_MISSING_IN_CONF_FILE = "PARAM element with name 'task_name' missing in configuration file {}";

	// Tags for JobOrder XML file
	private static final String JOF_TAG_INPUT = "Input";
	private static final String JOF_TAG_OUTPUT = "Output";
	private static final String JOF_TAG_FILE_NAME = "File_Name";
	private static final String JOF_TAG_FILE_TYPE = "File_Type";
	private static final String JOF_TAG_FILE_VERSION = "File_Type";
	private static final String JOF_TAG_TASK_NAME = "Task_Name";
	private static final String JOF_TAG_CONFIG_FILES = "Config_Files";
	private static final String JOF_TAG_CONFIG_FILE_NAME = "Conf_File_Name";
	
	// Tags for the configuration file
	private static final String CONF_TAG_PARAM = "PARAM";
	private static final String CONF_ATTR_NAME = "name";
	private static final String CONF_ATTR_VALUE_TASK_NAME = "task_name";
	private static final String CONF_ATTR_VALUE_VERSION = "version";
	
	// Allowed product types and requested input
	private static final String STATIC_INPUT_CONFIG = "processing_configuration";
	private static final String DYNAMIC_INPUT_AUX = "AUX_IERS_B";
	private static final String PRODUCT_TYPE_L0 = "L0";
	private static final String PRODUCT_TYPE_L1B = "L1B";
	private static final String PRODUCT_TYPE_L1B_1 = "L1B_PART1";
	private static final String PRODUCT_TYPE_L1B_2 = "L1B_PART2";
	private static final String PRODUCT_TYPE_L2A = "PTM_L2A";
	private static final String PRODUCT_TYPE_L2B = "PTM_L2B";
	private static final String PRODUCT_TYPE_L3 = "PTM_L3";
	
	private static Map<String, List<String>> PRODUCT_TYPES = new HashMap<>();
	private static final String[][] PRODUCT_TYPE_DEPENDENCIES = {
			// type, input type 1, input type 2, ...
			{ PRODUCT_TYPE_L0 }, // Not producible
			{ PRODUCT_TYPE_L1B, PRODUCT_TYPE_L0 },
			{ PRODUCT_TYPE_L2A, PRODUCT_TYPE_L1B },
			{ PRODUCT_TYPE_L2B, PRODUCT_TYPE_L1B_1 },
			{ PRODUCT_TYPE_L3, PRODUCT_TYPE_L2A, PRODUCT_TYPE_L2B }
	};
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(SampleProcessor.class);
	
	/**
	 * Static initializer: Initialize map of product types
	 */
	{
		for (int i = 0; i < PRODUCT_TYPE_DEPENDENCIES.length; ++i) {
			List<String> inputTypes = new ArrayList<>();
			for (int k = 1; k < PRODUCT_TYPE_DEPENDENCIES[i].length; ++k) {
				inputTypes.add(PRODUCT_TYPE_DEPENDENCIES[i][k]);
			}
			PRODUCT_TYPES.put(PRODUCT_TYPE_DEPENDENCIES[i][0], inputTypes);
		}
	}
	
	
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
	 * Check whether the parameters in the configuration file (expected parameters) match the parameters found in the Job Order Document
	 * 
	 * @param configurationFileName name of the XML configuration file to parse
	 * @param taskName the task name in the Job Order Document
	 * @return true, if the given parameters match, false otherwise
	 */
	private boolean checkConfigurationFile(String configurationFileName, String taskName) {
		// Read the configuration file
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error(MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER, e.getMessage());
			return false;
		}
		Document configurationDoc = null;
		try {
			configurationDoc = docBuilder.parse(configurationFileName);
		} catch (SAXException | IOException e) {
			logger.error(MSG_CONFIGURATION_DOC_NOT_PARSEABLE, configurationFileName, e.getMessage());
			return false;
		}
		
		// Find the task_name parameter
		NodeList params = configurationDoc.getElementsByTagName(CONF_TAG_PARAM);
		for (int i = 0; i < params.getLength(); ++i) {
			Node paramName = params.item(i).getAttributes().getNamedItem(CONF_ATTR_NAME);
			if (null == paramName) {
				logger.warn(MSG_PARAMETER_NAME_MISSING);
				continue;
			}
			if (CONF_ATTR_VALUE_TASK_NAME.equals(paramName.getTextContent())) {
				if (taskName.equals(params.item(i).getTextContent())) {
					return true;
				} else {
					logger.error(MSG_INVALID_TASK_NAME_IN_JOF, taskName, params.item(i).getTextContent());
					return false;
				}
			} 
		}
		
		logger.error(MSG_TASK_NAME_MISSING_IN_CONF_FILE, configurationFileName);
		return false;
	}

	/**
	 * Checks the content of the given static input file for output product type L1B: 
	 * static input file expected with parameter "task_name" = task name from Job Order document
	 * 
	 * @param jobOrderDoc the Job Order document
	 * @return true, if the static input file is OK, false otherwise
	 */
	private boolean checkL1bConfiguration(Document jobOrderDoc) {
		// Find the static input file for processor configuration
		NodeList inputFiles = jobOrderDoc.getElementsByTagName(JOF_TAG_INPUT);
		if (3 != inputFiles.getLength()) {
			logger.error(MSG_INVALID_NUMBER_OF_INPUT_FILES, PRODUCT_TYPE_L1B, inputFiles.getLength());
			return false;
		}
		String configurationFileName = null;
		for (int i = 0; i < inputFiles.getLength(); ++i) {
			NodeList inputFileTypes = ((Element) inputFiles.item(i)).getElementsByTagName(JOF_TAG_FILE_TYPE);
			if (1 != inputFileTypes.getLength()) {
				logger.error(MSG_INVALID_NUMBER_OF_INPUT_FILE_TYPES, inputFileTypes.getLength());
				return false;
			}
			NodeList inputFileNames = ((Element) inputFiles.item(i)).getElementsByTagName(JOF_TAG_FILE_NAME);
			if (1 != inputFileNames.getLength()) {
				logger.error(MSG_INVALID_NUMBER_OF_INPUT_FILE_NAMES, inputFileNames.getLength());
				return false;
			}
			
			if (STATIC_INPUT_CONFIG.equals(inputFileTypes.item(0).getTextContent())) {
				configurationFileName = inputFileNames.item(0).getTextContent();
				break;
			}
		}
		
		// Find the task name
		NodeList taskNames = jobOrderDoc.getElementsByTagName(JOF_TAG_TASK_NAME);
		if (1 != taskNames.getLength()) {
			logger.error(MSG_INVALID_NUMBER_OF_TASK_NAMES, taskNames.getLength());
			return false;
		}
		
		// Check the "task_name" parameter in the static config file
		return checkConfigurationFile(configurationFileName, taskNames.item(0).getTextContent());
	}

	/**
	 * Checks the content of the given configuration file for output product types PTM_L2A and PTM_L2B:
	 * configuration file expected with "task_name" = task name from Job Order document
	 * 
	 * @param jobOrderDoc the Job Order document
	 * @return true, if the configuration input file is OK, false otherwise
	 */
	private boolean checkL2Configuration(Document jobOrderDoc) {
		// Find the processor configuration file for processor configuration
		NodeList configurationFiles = jobOrderDoc.getElementsByTagName(JOF_TAG_CONFIG_FILES);
		if (1 != configurationFiles.getLength()) {
			logger.error(MSG_INVALID_NUMBER_OF_CONFIGURATION_FILES, PRODUCT_TYPE_L2A + " or " + PRODUCT_TYPE_L2B, configurationFiles.getLength());
			return false;
		}

		NodeList configFileNames = ((Element) configurationFiles.item(0)).getElementsByTagName(JOF_TAG_CONFIG_FILE_NAME);
		if (1 != configFileNames.getLength()) {
			logger.error(MSG_INVALID_NUMBER_OF_CONFIGURATION_FILE_NAMES, configFileNames.getLength());
			return false;
		}
			
		// Find the task name
		NodeList taskNames = jobOrderDoc.getElementsByTagName(JOF_TAG_TASK_NAME);
		if (1 != taskNames.getLength()) {
			logger.error(MSG_INVALID_NUMBER_OF_TASK_NAMES, taskNames.getLength());
			return false;
		}
		
		// Check the "task_name" parameter in the static config file
		return checkConfigurationFile(configFileNames.item(0).getTextContent(), taskNames.item(0).getTextContent());
	}

	/**
	 * Checks the content of the given configuration and/or static input files:
	 * <ul>
	 *   <li>For output product type L1B: static input file expected with parameter "task_name" = task name from Job Order document
	 *   <li>For output product types PTM_L2A and PTM_L2B: configuration file expected with "task_name" = task name from Job Order 
	 *       document
	 *   <li>For output product type PTM_L3: no configuration or static input file expected
	 * </ul>
	 * @param jobOrderDoc the Job Order document
	 * @return true, if the configuration/static input file is OK, false otherwise
	 */
	private boolean checkConfiguration(Document jobOrderDoc) {
		// Get the output product type
		NodeList outputFiles = jobOrderDoc.getElementsByTagName(JOF_TAG_OUTPUT);
		if (1 != outputFiles.getLength()) {
			logger.error(MSG_INVALID_NUMBER_OF_OUTPUT_FILES, outputFiles.getLength());
			return false;
		}
		NodeList outputFileTypes = ((Element) outputFiles.item(0)).getElementsByTagName(JOF_TAG_FILE_TYPE);
		if (1 != outputFileTypes.getLength()) {
			logger.error(MSG_INVALID_NUMBER_OF_OUTPUT_FILE_TYPES, outputFileTypes.getLength());
			return false;
		}
		String outputFileType = outputFileTypes.item(0).getTextContent();
		
		// Check the configuration/static input files according to output product type
		switch(outputFileType) {
		case PRODUCT_TYPE_L1B:
			return checkL1bConfiguration(jobOrderDoc);
		case PRODUCT_TYPE_L2A:
		case PRODUCT_TYPE_L2B:
			return checkL2Configuration(jobOrderDoc);
		case PRODUCT_TYPE_L3:
			return true; // Ignore superfluous entries
		default:
			// Product type cannot be handled by this processor
			logger.error(MSG_INVALID_OUTPUT_FILE_TYPE, outputFileType);
			return false;
		}
	}
	
	/**
	 * Read the input product file named in the given JobOrder document
	 * 
	 * @param jobOrderDoc the JobOrder document containing the input product file path
	 * @return
	 */
	private SampleProduct readInputProduct(Document jobOrderDoc) {
		// Determine the output file type from the Job Order Document
		NodeList outputFiles = jobOrderDoc.getElementsByTagName(JOF_TAG_OUTPUT);
		if (1 != outputFiles.getLength()) {
			logger.error(MSG_INVALID_NUMBER_OF_OUTPUT_FILES, outputFiles.getLength());
			return null;
		}
		NodeList outputFileTypes = ((Element) outputFiles.item(0)).getElementsByTagName(JOF_TAG_FILE_TYPE);
		if (1 != outputFileTypes.getLength()) {
			logger.error(MSG_INVALID_NUMBER_OF_OUTPUT_FILE_TYPES, outputFileTypes.getLength());
			return null;
		}
		String outputFileType = outputFileTypes.item(0).getTextContent();
		
		// Get the input files from the JobOrder document
		NodeList inputFiles = jobOrderDoc.getElementsByTagName(JOF_TAG_INPUT);
		if (0 == inputFiles.getLength()) {
			logger.error(MSG_JOF_TAG_MISSING, JOF_TAG_INPUT);
			return null;
		}
		
		// Find the first usable input file
		String inputFileName = null;
		for (int i = 0; i < inputFiles.getLength(); i++) {
			NodeList inputFileTypes = ((Element) inputFiles.item(i)).getElementsByTagName(JOF_TAG_FILE_TYPE);
			if (1 != inputFileTypes.getLength()) {
				logger.error(MSG_INVALID_NUMBER_OF_INPUT_FILE_TYPES, inputFileTypes.getLength());
				return null;
			}
			NodeList inputFileNames = ((Element) inputFiles.item(i)).getElementsByTagName(JOF_TAG_FILE_NAME);
			if (1 != inputFileNames.getLength()) {
				logger.error(MSG_INVALID_NUMBER_OF_INPUT_FILE_NAMES, inputFileNames.getLength());
				return null;
			}
			if (PRODUCT_TYPES.get(outputFileType).contains(inputFileTypes.item(0).getTextContent())) {
				inputFileName = inputFileNames.item(0).getTextContent();
				break;
			}
		}
		if (null == inputFileName) {
			logger.error(MSG_NO_VALID_INPUT_FILE_IN_JOF, outputFileType);
			return null;
		}
		
		// Create an input product from the file content (expecting 'id|type|startTime|stopTime|revision')
		BufferedReader input = null;
		try {
			input = new BufferedReader(new FileReader(new File(inputFileName)));
		} catch (FileNotFoundException e) {
			logger.error(MSG_FILE_NOT_READABLE, inputFileName);
			return null;
		}
		
		SampleProduct inputProduct = new SampleProduct();
		try {
			// test if readable...
			String[] fields = input.readLine().split("\\|");
			logger.debug("... input product test-read with file {} having {} fields: {}", inputFileName, fields.length, fields);
			
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
			outputProduct.setId("DERIVED_FROM_"+inputProduct.getId().replace("/", "_"));
			outputProduct.setType(outputFileTypes.item(0).getTextContent());
			outputProduct.setStartTime(inputProduct.getStartTime());
			outputProduct.setStopTime(inputProduct.getStopTime());
			outputProduct.setGenerationTime(Instant.now());
			outputProduct.setRevision(inputProduct.getRevision());
			
			// Write the output product to a file
			String outputFileName = outputFileNames.item(0).getTextContent();
			logger.debug("... creating output product file {}", outputFileName);
			BufferedWriter output = null;
			try {
				output = new BufferedWriter(new FileWriter(new File(outputFileName)));
				output.write(
					String.format("%s|%s|%s|%s|%s|%d", outputProduct.getId(), outputProduct.getType(),
						outputProduct.getStartTime().toString(), outputProduct.getStopTime().toString(),
						outputProduct.getGenerationTime().toString(), outputProduct.getRevision()
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
		
		// Check the configuration/static input files
		boolean ok = checkConfiguration(jobOrderDoc);
		if (!ok) {
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
		ok = processOutputProduct(inputProduct, jobOrderDoc);
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
