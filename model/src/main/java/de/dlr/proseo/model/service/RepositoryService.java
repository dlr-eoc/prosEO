/**
 * RepositoryService.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.dlr.proseo.model.dao.ConfigurationRepository;
import de.dlr.proseo.model.dao.ConfiguredProcessorRepository;
import de.dlr.proseo.model.dao.FacilityRepository;
import de.dlr.proseo.model.dao.InputFilterRepository;
import de.dlr.proseo.model.dao.JobRepository;
import de.dlr.proseo.model.dao.JobStepRepository;
import de.dlr.proseo.model.dao.MissionRepository;
import de.dlr.proseo.model.dao.MonExtServiceRepository;
import de.dlr.proseo.model.dao.MonExtServiceStateOperationDayRepository;
import de.dlr.proseo.model.dao.MonExtServiceStateOperationMonthRepository;
import de.dlr.proseo.model.dao.MonExtServiceStateOperationRepository;
import de.dlr.proseo.model.dao.MonOrderStateRepository;
import de.dlr.proseo.model.dao.MonProductProductionDayRepository;
import de.dlr.proseo.model.dao.MonProductProductionHourRepository;
import de.dlr.proseo.model.dao.MonProductProductionMonthRepository;
import de.dlr.proseo.model.dao.MonServiceRepository;
import de.dlr.proseo.model.dao.MonServiceStateOperationDayRepository;
import de.dlr.proseo.model.dao.MonServiceStateOperationMonthRepository;
import de.dlr.proseo.model.dao.MonServiceStateOperationRepository;
import de.dlr.proseo.model.dao.MonServiceStateRepository;
import de.dlr.proseo.model.dao.OrbitRepository;
import de.dlr.proseo.model.dao.OrderRepository;
import de.dlr.proseo.model.dao.ClassOutputParameterRepository;
import de.dlr.proseo.model.dao.ProcessorClassRepository;
import de.dlr.proseo.model.dao.ProcessorRepository;
import de.dlr.proseo.model.dao.ProductClassRepository;
import de.dlr.proseo.model.dao.ProductFileRepository;
import de.dlr.proseo.model.dao.ProductQueryRepository;
import de.dlr.proseo.model.dao.ProductRepository;
import de.dlr.proseo.model.dao.SpacecraftRepository;
import de.dlr.proseo.model.dao.TaskRepository;

/**
 * This class autowires all available repositories and makes them accessible throughout prosEO by static methods.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Service
public class RepositoryService {
	/** The single instance of this class */
	private static RepositoryService theRepositoryService = null;

	/** The repository for the Configuration class */
	@Autowired
    private ConfigurationRepository configurationRepository;
	
	/** The repository for the ConfiguredProcessor class */
	@Autowired
    private ConfiguredProcessorRepository configuredProcessorRepository;
	
	/** The repository for the Facility class */
	@Autowired
    private FacilityRepository facilityRepository;
	
	/** The repository for the Job class */
	@Autowired
    private JobRepository jobRepository;
	
	/** The repository for the JobStep class */
	@Autowired
    private JobStepRepository jobStepRepository;
	
	/** The repository for the Mission class */
	@Autowired
    private MissionRepository missionRepository;
	
	/** The repository for the Orbit class */
	@Autowired
    private OrbitRepository orbitRepository;
	
	/** The repository for the ProcessingOrder class */
	@Autowired
    private OrderRepository orderRepository;
	
	/** The repository for the ProcessorClass class */
	@Autowired
    private ProcessorClassRepository processorClassRepository;
	
	/** The repository for the Processor class */
	@Autowired
    private ProcessorRepository processorRepository;
	
	/** The repository for the ProductClass class */
	@Autowired
    private ProductClassRepository productClassRepository;
	
	/** The repository for the ProductFile class */
	@Autowired
    private ProductFileRepository productFileRepository;
	
	/** The repository for the ProductQuery class */
	@Autowired
    private ProductQueryRepository productQueryRepository;
	
	/** The repository for the Product class */
	@Autowired
    private ProductRepository productRepository;

	/** The repository for the Spacecraft class */
	@Autowired
    private SpacecraftRepository spacecraftRepository;

	/** The repository for the Spacecraft class */
	@Autowired
    private TaskRepository taskRepository;

	/** The repository for the InputFilter class */
	@Autowired
    private InputFilterRepository inputFilterRepository;

	/** The repository for the ClassOutputParameter class */
	@Autowired
    private ClassOutputParameterRepository classOutputParameterRepository;

	/** The repository for the MonService class */
	@Autowired
    private MonServiceRepository monServiceRepository;

	/** The repository for the MonService class */
	@Autowired
    private MonExtServiceRepository monExtServiceRepository;
	
	/** The repository for the MonServiceStates class */
	@Autowired
    private MonServiceStateRepository monServiceStateRepository;

	/** The repository for the MonServiceStateOperation class */
	@Autowired
    private MonServiceStateOperationRepository monServiceStateOperationRepository;

	/** The repository for the MonServiceStateOperation class */
	@Autowired
    private MonExtServiceStateOperationRepository monExtServiceStateOperationRepository;
	
	/** The repository for the MonOrderState class */
	@Autowired
    private MonOrderStateRepository monOrderStateRepository;
	
	/** The repository for the MonProductProductionDay class */
	@Autowired
    private MonProductProductionDayRepository monProductProductionDayRepository;
	
	/** The repository for the MonProductProductionHour class */
	@Autowired
    private MonProductProductionHourRepository monProductProductionHourRepository;

	/** The repository for the MonProductProductionMonth class */
	@Autowired
    private MonProductProductionMonthRepository monProductProductionMonthRepository;

	/** The repository for the MonServiceStateOperationDay class */
	@Autowired
    private MonServiceStateOperationDayRepository monServiceStateOperationDayRepository;

	/** The repository for the MonServiceStateOperationMonth class */
	@Autowired
    private MonServiceStateOperationMonthRepository monServiceStateOperationMonthRepository;

	/** The repository for the MonExtServiceStateOperationDay class */
	@Autowired
    private MonExtServiceStateOperationDayRepository monExtServiceStateOperationDayRepository;

	/** The repository for the MonExtServiceStateOperationMonth class */
	@Autowired
    private MonExtServiceStateOperationMonthRepository monExtServiceStateOperationMonthRepository;
	

	/**
	 * Singleton constructor
	 */
	public RepositoryService() {
		super();
		theRepositoryService = this;
	}

	/**
	 * Gets the repository for the Configuration class
	 * 
	 * @return the configurationRepository
	 */
	public static ConfigurationRepository getConfigurationRepository() {
		return theRepositoryService.configurationRepository;
	}

	/**
	 * Gets the repository for the ConfiguredProcessor class
	 * 
	 * @return the configuredProcessorRepository
	 */
	public static ConfiguredProcessorRepository getConfiguredProcessorRepository() {
		return theRepositoryService.configuredProcessorRepository;
	}

	/**
	 * Gets the repository for the Facility class
	 * 
	 * @return the facilityRepository
	 */
	public static FacilityRepository getFacilityRepository() {
		return theRepositoryService.facilityRepository;
	}

	/**
	 * Gets the repository for the Job class
	 * 
	 * @return the jobRepository
	 */
	public static JobRepository getJobRepository() {
		return theRepositoryService.jobRepository;
	}

	/**
	 * Gets the repository for the JobStep class
	 * 
	 * @return the jobStepRepository
	 */
	public static JobStepRepository getJobStepRepository() {
		return theRepositoryService.jobStepRepository;
	}

	/**
	 * Gets the repository for the Mission class
	 * 
	 * @return the missionRepository
	 */
	public static MissionRepository getMissionRepository() {
		return theRepositoryService.missionRepository;
	}

	/**
	 * Gets the repository for the Orbit class
	 * 
	 * @return the orbitRepository
	 */
	public static OrbitRepository getOrbitRepository() {
		return theRepositoryService.orbitRepository;
	}

	/**
	 * Gets the repository for the ProcessingOrder class
	 * 
	 * @return the orderRepository
	 */
	public static OrderRepository getOrderRepository() {
		return theRepositoryService.orderRepository;
	}

	/**
	 * Gets the repository for the ProcessorClass class
	 * 
	 * @return the processorClassRepository
	 */
	public static ProcessorClassRepository getProcessorClassRepository() {
		return theRepositoryService.processorClassRepository;
	}

	/**
	 * Gets the repository for the Processor class
	 * 
	 * @return the processorRepository
	 */
	public static ProcessorRepository getProcessorRepository() {
		return theRepositoryService.processorRepository;
	}

	/**
	 * Gets the repository for the ProductClass class
	 * 
	 * @return the productClassRepository
	 */
	public static ProductClassRepository getProductClassRepository() {
		return theRepositoryService.productClassRepository;
	}

	/**
	 * Gets the repository for the ProductFile class
	 * 
	 * @return the productFileRepository
	 */
	public static ProductFileRepository getProductFileRepository() {
		return theRepositoryService.productFileRepository;
	}

	/**
	 * Gets the repository for the ProductQuery class
	 * 
	 * @return the productQueryRepository
	 */
	public static ProductQueryRepository getProductQueryRepository() {
		return theRepositoryService.productQueryRepository;
	}

	/**
	 * Gets the repository for the Product class
	 * 
	 * @return the productRepository
	 */
	public static ProductRepository getProductRepository() {
		return theRepositoryService.productRepository;
	}

	/**
	 * Gets the repository for the Spacecraft class
	 * 
	 * @return the spacecraftRepository
	 */
	public static SpacecraftRepository getSpacecraftRepository() {
		return theRepositoryService.spacecraftRepository;
	}

	/**
	 * Gets the repository for the Task class
	 * 
	 * @return the taskRepository
	 */
	public static TaskRepository getTaskRepository() {
		return theRepositoryService.taskRepository;
	}

	/**
	 * Gets the repository for the InputFilter class
	 * 
	 * @return the inputFilterRepository
	 */
	public static InputFilterRepository getInputFilterRepository() {
		return theRepositoryService.inputFilterRepository;
	}

	/**
	 * Gets the repository for the ClassOutputParameter class
	 * 
	 * @return the classOutputParameterRepository
	 */
	public static ClassOutputParameterRepository getClassOutputParameterRepository() {
		return theRepositoryService.classOutputParameterRepository;
	}

	/**
	 * Gets the repository for the MonService class
	 * 
	 * @return the monServiceRepository
	 */
	public static MonServiceRepository getMonServiceRepository() {
		return theRepositoryService.monServiceRepository;
	}

	/**
	 * Gets the repository for the MonService class
	 * 
	 * @return the monServiceRepository
	 */
	public static MonExtServiceRepository getMonExtServiceRepository() {
		return theRepositoryService.monExtServiceRepository;
	}

	/**
	 * Gets the repository for the MonServiceStates class
	 * 
	 * @return the monServiceRepository
	 */
	public static MonServiceStateRepository getMonServiceStateRepository() {
		return theRepositoryService.monServiceStateRepository;
	}

	/**
	 * Gets the repository for the MonServiceStateOperation class
	 * 
	 * @return the monServiceStateOperationRepository
	 */
	public static MonServiceStateOperationRepository getMonServiceStateOperationRepository() {
		return theRepositoryService.monServiceStateOperationRepository;
	}

	/**
	 * Gets the repository for the MonServiceStateOperation class
	 * 
	 * @return the monServiceStateOperationRepository
	 */
	public static MonExtServiceStateOperationRepository getMonExtServiceStateOperationRepository() {
		return theRepositoryService.monExtServiceStateOperationRepository;
	}

	/**
	 * Gets the repository for the MonOrderStateOperation class
	 * 
	 * @return the monOrderStateRepository
	 */
	public static MonOrderStateRepository getMonOrderStateRepository() {
		return theRepositoryService.monOrderStateRepository;
	}

	/**
	 * @return the monProductProductionDayRepository
	 */
	public static MonProductProductionDayRepository getMonProductProductionDayRepository() {
		return theRepositoryService.monProductProductionDayRepository;
	}

	/**
	 * @return the monProductProductionHourRepository
	 */
	public static MonProductProductionHourRepository getMonProductProductionHourRepository() {
		return theRepositoryService.monProductProductionHourRepository;
	}

	/**
	 * @return the monProductProductionMonthRepository
	 */
	public static MonProductProductionMonthRepository getMonProductProductionMonthRepository() {
		return theRepositoryService.monProductProductionMonthRepository;
	}

	/**
	 * @return the monServiceStateOperationDayRepository
	 */
	public static MonServiceStateOperationDayRepository getMonServiceStateOperationDayRepository() {
		return theRepositoryService.monServiceStateOperationDayRepository;
	}

	/**
	 * @return the monServiceStateOperationMonthRepository
	 */
	public static MonServiceStateOperationMonthRepository getMonServiceStateOperationMonthRepository() {
		return theRepositoryService.monServiceStateOperationMonthRepository;
	}

	/**
	 * @return the monExtServiceStateOperationDayRepository
	 */
	public static MonExtServiceStateOperationDayRepository getMonExtServiceStateOperationDayRepository() {
		return theRepositoryService.monExtServiceStateOperationDayRepository;
	}

	/**
	 * @return the monExtServiceStateOperationMonthRepository
	 */
	public static MonExtServiceStateOperationMonthRepository getMonExtServiceStateOperationMonthRepository() {
		return theRepositoryService.monExtServiceStateOperationMonthRepository;
	}
}
