/**
 * JobDispatcher.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.interfaces.rest.model.RestJoborder;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.ConfigurationFile;
import de.dlr.proseo.model.ConfigurationInputFile;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.Task;
import de.dlr.proseo.model.enums.JobOrderVersion;
import de.dlr.proseo.model.enums.StorageType;
import de.dlr.proseo.model.joborder.Conf;
import de.dlr.proseo.model.joborder.InputOutput;
import de.dlr.proseo.model.joborder.IpfFileName;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.joborder.Proc;
import de.dlr.proseo.model.joborder.ProcessingParameter;
import de.dlr.proseo.model.joborder.SensingTime;
import de.dlr.proseo.model.joborder.TimeInterval;
import de.dlr.proseo.planner.kubernetes.KubeConfig;

/**
 * This class is responsible for creating and dispatching job orders for job steps, including information about the processor,
 * configuration files, input files, and output files. It also provides methods for sending the job order to the storage manager.
 *
 * @author Ernst Melchinger
 */
public class JobDispatcher {

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(JobDispatcher.class);

	/** Date time formatter */
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'_'HHmmssSSSSSS")
		.withZone(ZoneId.of("UTC"));

	/** The job order */
	private JobOrder jobOrder;

	/** Create a job dispatcher. */
	public JobDispatcher() {
	}

	/**
	 * Create the job order for a job step.
	 *
	 * @param jobStep The job step for which the job order is created.
	 * @return The new job order.
	 * @throws Exception if the job order creation was unsuccessful
	 */
	public JobOrder createJobOrder(JobStep jobStep) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> createJobOrder({})", jobStep.getId());
		}

		// Initialize the job order structure
		jobOrder = null;

		if (jobStep == null) {
			// TODO Maybe log?
			return jobOrder;
		}

		try {
			// Initialize a new job order
			jobOrder = new JobOrder();

			// Retrieve information from the provided job step
			String processorName = jobStep.getOutputProduct()
				.getConfiguredProcessor()
				.getProcessor()
				.getProcessorClass()
				.getProcessorName();
			String version = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getProcessorVersion();
			String stdoutLogLevel = jobStep.getStdoutLogLevel().name();
			String stderrLogLevel = jobStep.getStderrLogLevel().name();
			String isTest = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getIsTest() ? "true" : "false";
			String breakpointEnable = "false";
			String processingStation = jobStep.getJob().getProcessingOrder().getMission().getName() + " "
					+ jobStep.getJob().getProcessingFacility().getName();
			// String acquisitionStation = ""; // unknown, not to set
			
			// Create a new configuration based on the retrieved data
			Conf configuration = new Conf(processorName, version, stdoutLogLevel, stderrLogLevel, isTest, breakpointEnable,
					processingStation, null);
			String start = timeFormatter.format(jobStep.getJob().getStartTime());
			String stop = timeFormatter.format(jobStep.getJob().getStopTime());
			configuration.setSensingTime(new SensingTime(start, stop));

			// Add configuration files
			for (ConfigurationFile cf : jobStep.getOutputProduct()
				.getConfiguredProcessor()
				.getConfiguration()
				.getConfigurationFiles()) {
				configuration.getConfigFileNames().add(cf.getFileName());
			}

			// Add dynamic processing parameters
			Map<String, Parameter> dynamicProcessingParameters = jobStep.getOutputProduct()
				.getConfiguredProcessor()
				.getConfiguration()
				.getDynProcParameters();
			for (String processingParameter : jobStep.getOutputProduct()
				.getConfiguredProcessor()
				.getConfiguration()
				.getDynProcParameters()
				.keySet()) {
				configuration.getDynamicProcessingParameters()
					.add((new ProcessingParameter(processingParameter,
							dynamicProcessingParameters.get(processingParameter).getParameterValue())));
			}

			// Set the configuration of the new job order
			jobOrder.setConf(configuration);

			// For each of the processor's tasks, configure and add a new processing task to the job order to be created
			for (Task task : jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getTasks()) {

				// Initialize a new processing task with name and version
				Proc processingTask = new Proc(task.getTaskName(), task.getTaskVersion());

				// Add the input files
				for (ConfigurationInputFile confInputFile : jobStep.getOutputProduct()
					.getConfiguredProcessor()
					.getConfiguration()
					.getStaticInputFiles()) {

					InputOutput inputOutputInfo = new InputOutput(confInputFile.getFileType(), confInputFile.getFileNameType(),
							InputOutput.IO_TYPE_INPUT, null);

					for (String fileName : confInputFile.getFileNames()) {
						inputOutputInfo.getFileNames().add(new IpfFileName(fileName));
						processingTask.getListOfInputs().add(inputOutputInfo);
					}
				}

				// Add the dynamic input files (i.e., prior inputs needed to create the desired inputs)
				Map<String, List<Product>> productClasses = new HashMap<>();
				for (ProductQuery productQuery : jobStep.getInputProductQueries()) {
					for (Product product : productQuery.getSatisfyingProducts()) {
						addProductToMap(product, productClasses);
					}
				}
				addIpfIOInput(productClasses, processingTask, jobStep, task.getProcessor().getUseInputFileTimeIntervals());

				// Add the output product files (need to be calculated)
				Product product = jobStep.getOutputProduct();
				addIpfIOOutput(product, processingTask, jobStep, "");

				// Add the configured processing task to the job order
				jobOrder.getListOfProcs().add(processingTask);

			}
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());

			if (logger.isDebugEnabled()) {
				logger.debug("An exception occurred. Cause: ", e);
			}

			throw e;
		}

		/*
		 * FOR TESTING PURPOSES ONLY: Write a job order file as an XML document to a specific file path, but only if the code is
		 * running on a machine with the hostname "ME580".
		 *
		 * TODO Maybe find a more generic test?
		 */
		try {
			InetAddress ip = InetAddress.getLocalHost();
			String hostname = ip.getHostName();
			if (hostname.equalsIgnoreCase("ME580")) {
				jobOrder.writeXML("c:\\tmp\\jo" + jobStep.getId() + ".xml", JobOrderVersion.MMFI_1_8, true);
			}
		} catch (UnknownHostException e) {
			// Do nothing
		} // TODO Catch and log other exceptions for test failure analysis.

		// Return the new job order
		return jobOrder;
	}

	/**
	 * Add input file definition of the given product recursively.
	 *
	 * @param product          The product.
	 * @param processingTask   The processing task.
	 * @param jobStep          The job step.
	 * @param useTimeIntervals Determines whether to generate TimeInterval elements in the input definition.
	 */
	public void addIpfIOInput(Product product, Proc processingTask, JobStep jobStep, boolean useTimeIntervals) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> addIpfIOInput({}, {}, {}, {})", product.getId(), processingTask.getTaskName(), jobStep.getId(),
					useTimeIntervals);
		}

		if (product.getComponentProducts().isEmpty()) {
			// Add input files for the product
			for (ProductFile productFile : product.getProductFile()) {
				String filePath = productFile.getFilePath();
				String productFilePathAndName = (filePath != null && !filePath.isBlank()) ? filePath + "/" : "";
				productFilePathAndName += productFile.getProductFileName();

				// Create InputOutput information for the input file
				InputOutput inputOutput = new InputOutput(product.getProductClass().getProductType(), InputOutput.FN_TYPE_PHYSICAL,
						InputOutput.IO_TYPE_INPUT, String.valueOf(product.getId()));
				inputOutput.getFileNames().add(new IpfFileName(productFilePathAndName, productFile.getStorageType().name()));

				if (useTimeIntervals) {
					// Generate TimeInterval elements if required
					TimeInterval timeInterval = new TimeInterval(timeFormatter.format(product.getSensingStartTime()),
							timeFormatter.format(product.getSensingStopTime()), productFilePathAndName);
					inputOutput.getTimeIntervals().add(timeInterval);
				}

				// Add input file to the processing task
				processingTask.getListOfInputs().add(inputOutput);

				if (logger.isTraceEnabled()) {
					logger.trace("... added product {} to input files", product.getId());
				}
			}
		} else {
			// The product has component products
			for (Product componentProduct : product.getComponentProducts()) {
				// Recursively add input files for component products
				addIpfIOInput(componentProduct, processingTask, jobStep, useTimeIntervals);
			}
		}
	}

	/**
	 * Add input file definition of products recursively.
	 *
	 * @param productClasses   A map of products accessible by product class.
	 * @param processingTask   The Ipf_Proc element to add the input to.
	 * @param jobStep          The job step for which the Job Order is generated.
	 * @param useTimeIntervals Determines whether to generate TimeInterval elements in the input definition.
	 */
	public void addIpfIOInput(Map<String, List<Product>> productClasses, Proc processingTask, JobStep jobStep,
			Boolean useTimeIntervals) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> addIpfIOInput(<...>, {}, {}, {})", processingTask.getTaskName(), jobStep.getId(), useTimeIntervals);
		}

		for (String productClassName : productClasses.keySet()) {

			// Initialize input/output information for the product class
			InputOutput inputOutput = new InputOutput(productClassName, InputOutput.FN_TYPE_PHYSICAL, InputOutput.IO_TYPE_INPUT,
					null);

			// Add input files for the product
			for (Product product : productClasses.get(productClassName)) {
				if (product.getComponentProducts().isEmpty()) {
					for (ProductFile productFile : product.getProductFile()) {
						String filePath = productFile.getFilePath();
						String productFilePathAndName = (filePath != null && !filePath.isBlank()) ? filePath + "/" : "";
						productFilePathAndName += productFile.getProductFileName();

						if (useTimeIntervals) {
							// Generate TimeInterval elements if required
							TimeInterval timeInterval = new TimeInterval(timeFormatter.format(product.getSensingStartTime()),
									timeFormatter.format(product.getSensingStopTime()), productFilePathAndName);
							inputOutput.getTimeIntervals().add(timeInterval);
						}

						// Add the product file to the input output information
						inputOutput.getFileNames()
							.add(new IpfFileName(productFilePathAndName, productFile.getStorageType().name()));

						if (logger.isTraceEnabled()) {
							logger.trace("... added product {} to input files", product.getId());
						}
					}
				} else {
					// Not possible because the product structure is flattened by addProductToMap.
					if (logger.isTraceEnabled()) {
						logger.trace("... in 'impossible' location for product {}", product.getId());
						// TODO Maybe throw exception instead of just logging, as some larger problem might have occurred?
					}
				}
			}

			// Add the input/output information to the provided processing task
			processingTask.getListOfInputs().add(inputOutput);
		}
	}

	/**
	 * Add the product to the productClasses map. Collect all products of the same class into one map element. Add products with
	 * components recursively (otherwise, these products are seen as directories and are not handled by the wrapper/processor).
	 *
	 * @param product        The product.
	 * @param productClasses The map.
	 */
	private void addProductToMap(Product product, Map<String, List<Product>> productClasses) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> addProductToMap({}, [...])", (null == product ? "null" : product.getId()));
		}

		if (product.getComponentProducts().isEmpty()) {
			if (productClasses.containsKey(product.getProductClass().getProductType())) {
				// Collect all products of the same class into one map element
				productClasses.get(product.getProductClass().getProductType()).add(product);
			} else {
				// Add new product to map
				List<Product> plist = new ArrayList<>();
				plist.add(product);
				productClasses.put(product.getProductClass().getProductType(), plist);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("... added!");
			}
		} else {
			// Recursively add component products
			for (Product componentProduct : product.getComponentProducts()) {
				addProductToMap(componentProduct, productClasses);
			}
		}
	}

	/**
	 * Add output file definition of the product recursively.
	 *
	 * @param product        The product.
	 * @param processingTask The processing task.
	 * @param jobStep        The job step.
	 * @param baseDir        The base directory path of output data in facility/storage manager.
	 */
	public void addIpfIOOutput(Product product, Proc processingTask, JobStep jobStep, String baseDir) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> addIpfIOOutput({}, {}, {}, {})", product.getId(), processingTask.getTaskName(), jobStep.getId(),
					baseDir);
		}

		// Determine the file name type based on whether the product has component products or not
		String fileNameType = product.getComponentProducts().isEmpty() ? InputOutput.FN_TYPE_PHYSICAL
				: InputOutput.FN_TYPE_DIRECTORY;

		// Create the InputOutput information for the product
		InputOutput inputOutput = new InputOutput(product.getProductClass().getProductType(), fileNameType,
				InputOutput.IO_TYPE_OUTPUT, String.valueOf(product.getId()));

		// Determine the storage type
		String storageType = jobStep.getJob().getProcessingFacility().getDefaultStorageType().toString();

		// Generate the file name based on the product's generation time
		String fileName = "";
		if (product.getGenerationTime() != null) {
			fileName = product.generateFilename();

			// If it is a directory, remove the file extension
			if (fileNameType.equals(InputOutput.FN_TYPE_DIRECTORY)) {
				int lastDotIndex = fileName.lastIndexOf('.');
				if (lastDotIndex > 0) {
					fileName = fileName.substring(0, lastDotIndex);
				}
			}

			// Concatenate the base directory and the file name
			fileName = baseDir + fileName;
		} else {
			// Use the product class type as the file name
			fileName = baseDir + product.getProductClass().getProductType();
		}

		// Add the file name with the storage type to the InputOutput object
		inputOutput.getFileNames().add(new IpfFileName(fileName, storageType));

		// Add the InputOutput object to the processing task's list of outputs
		processingTask.getListOfOutputs().add(inputOutput);

		// Recursively process the component products
		for (Product subProduct : product.getComponentProducts()) {
			addIpfIOOutput(subProduct, processingTask, jobStep, fileName + "/");
		}
	}

	/**
	 * Send the job order as a Base64 string to the storage manager.
	 *
	 * @param kubeConfig      The processing facility configuration.
	 * @param jobOrder        The job order to send.
	 * @param jobOrderVersion The specification version of the job order file to apply.
	 * @return The job order with updated file name, or null if unsuccessful.
	 */
	public JobOrder sendJobOrderToStorageManager(KubeConfig kubeConfig, JobOrder jobOrder, JobOrderVersion jobOrderVersion) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> sendJobOrderToStorageManager({}, {})", kubeConfig, jobOrder);
		}

		// Get the storage manager URL from the provided kubeConfig
		String storageManagerUrl = kubeConfig.getStorageManagerUrl();
		if (storageManagerUrl == null || jobOrder == null) {
			logger.log(PlannerMessage.INSUFFICIENT_ORDER_DATA);
			return null;
		}

		try {
			// Create a RestTemplate for making HTTP requests
			RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
			RestTemplate restTemplate = restTemplateBuilder.setConnectTimeout(Duration.ofMillis(5000))
				.basicAuthentication(kubeConfig.getStorageManagerUser(), kubeConfig.getStorageManagerPassword())
				.build();

			// Set the REST API endpoint for submitting job orders
			String restUrl = "/joborders";

			// Create a RestJoborder object to hold the Base64 string
			RestJoborder restJoborder = new RestJoborder();

			// Set the file system type based on the storage type from kubeConfig
			switch (kubeConfig.getStorageType()) {
			case S3:
				restJoborder.setFsType(StorageType.S3.toString());
				break;
			case POSIX:
				// fall through intended
			default:
				restJoborder.setFsType(StorageType.POSIX.toString());
				break;
			}

			// Set the Base64 string of the job order in the RestJoborder object
			String b64String = jobOrder.buildBase64String(jobOrderVersion, true);
			restJoborder.setJobOrderStringBase64(b64String);

			// Send the POST request to the storage manager and get the response
			logger.log(PlannerMessage.HTTP_REQUEST, storageManagerUrl + restUrl);
			ResponseEntity<RestJoborder> response = restTemplate.postForEntity(storageManagerUrl + restUrl, restJoborder,
					RestJoborder.class);
			logger.log(PlannerMessage.HTTP_RESPONSE, response.getStatusCode());

			// Check if the response is successful and contains the uploaded flag
			if (response != null && response.getBody() != null && response.getBody().getUploaded()) {
				// Update the job order file name with the path info from the response
				jobOrder.setFileName(response.getBody().getPathInfo());
			} else {
				// TODO Maybe log?
				// Return null if the response is not successful
				return null;
			}
		} catch (Exception e) {
			logger.log(PlannerMessage.SENDING_JOB_EXCEPTION, e.getMessage());
			if (logger.isDebugEnabled()) {
				logger.debug("An exception occurred. Cause: ", e);
			}
			return null;
		}

		// Return the updated job order
		return jobOrder;
	}

}