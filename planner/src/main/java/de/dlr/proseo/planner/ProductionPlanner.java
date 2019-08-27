/*
 * ProductionPlanner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.planner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dlr.proseo.model.dao.ConfiguredProcessorRepository;
import de.dlr.proseo.model.dao.FacilityRepository;
import de.dlr.proseo.model.dao.JobRepository;
import de.dlr.proseo.model.dao.JobStepRepository;
import de.dlr.proseo.model.dao.ProcessorClassRepository;
import de.dlr.proseo.model.dao.ProcessorRepository;
import de.dlr.proseo.model.dao.ProductClassRepository;
import de.dlr.proseo.model.dao.ProductRepository;
import de.dlr.proseo.planner.kubernetes.KubeConfig;

/*
 * prosEO Ingestor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
@Transactional
@EnableJpaRepositories("de.dlr.proseo.model.dao")
public class ProductionPlanner {


	@Autowired
    private JobStepRepository jobSteps;
	@Autowired
    private ConfiguredProcessorRepository configuredProcessors;
	@Autowired
    private FacilityRepository facilitys;
	@Autowired
    private JobRepository jobs;
	@Autowired
    private ProcessorClassRepository processorClasses;
	@Autowired
    private ProcessorRepository processors;
	@Autowired
    private ProductClassRepository productClasses;
	@Autowired
    private ProductRepository products;

	static ApplicationContext ac;
	static ProductionPlanner thePlanner = null;
	
	static Map<String, KubeConfig> kubeConfigs = new HashMap<>();
	
	/**
	 * @return the jobSteps
	 */
	public static JobStepRepository getJobSteps() {
		return getPlanner().getJobStepsPrim();
	}
	/**
	 * @return the configuredProcessors
	 */
	public static ConfiguredProcessorRepository getConfiguredProcessors() {
		return getPlanner().getConfiguredProcessorsPrim();
	}
	/**
	 * @return the facilitys
	 */
	public static FacilityRepository getFacilitys() {
		return getPlanner().getFacilitysPrim();
	}
	/**
	 * @return the jobs
	 */
	public static JobRepository getJobs() {
		return getPlanner().getJobsPrim();
	}
	/**
	 * @return the processorClasses
	 */
	public static ProcessorClassRepository getProcessorClasses() {
		return getPlanner().getProcessorClassesPrim();
	}
	/**
	 * @return the processors
	 */
	public static ProcessorRepository getProcessors() {
		return getPlanner().getProcessorsPrim();
	}
	/**
	 * @return the productClasses
	 */
	public static ProductClassRepository getProductClasses() {
		return getPlanner().getProductClassesPrim();
	}
	/**
	 * @return the products
	 */
	public static ProductRepository getProducts() {
		return getPlanner().getProductsPrim();
	}
	
	public static ProductionPlanner getPlanner() {
		return thePlanner;		
	}
	public static KubeConfig getKubeConfig(String name) {
		if (name == null && kubeConfigs.size() == 1) {
			return (KubeConfig) kubeConfigs.values().toArray()[0];
		}
		return kubeConfigs.get(name.toLowerCase());
	}

	public static Collection<KubeConfig> getKubeConfigs() {
		return (Collection<KubeConfig>) kubeConfigs.values();
	}
	
	public static void main(String[] args) throws Exception {

		ProductionPlanner.addKubeConfig("Lerchenhof", "Testumgebung auf dem Lerchenhof", "http://192.168.20.159:8080");
		SpringApplication spa = new SpringApplication(ProductionPlanner.class);
		ac = spa.run(args);
		
		Object o = spa.getMainApplicationClass();
	}

	/**
	 * @return the ac
	 */
	public static ApplicationContext getAc() {
		return ac;
	}

	public static KubeConfig addKubeConfig(String name, String desc, String url) {
		if (getKubeConfig(name) == null) {
			KubeConfig kubeConfig = new KubeConfig(name, desc, url);
			if (kubeConfig != null && kubeConfig.connect()) {
				kubeConfigs.put(name.toLowerCase(), kubeConfig);
				return kubeConfig;
			}
		}
		return null;
	}
	
	public ProductionPlanner() {
		thePlanner = this;
	}	
	
	/**
	 * @return the jobSteps
	 */
	private JobStepRepository getJobStepsPrim() {
		return jobSteps;
	}
	/**
	 * @return the configuredProcessors
	 */
	private ConfiguredProcessorRepository getConfiguredProcessorsPrim() {
		return configuredProcessors;
	}
	/**
	 * @return the facilitys
	 */
	private FacilityRepository getFacilitysPrim() {
		return facilitys;
	}
	/**
	 * @return the jobs
	 */
	private JobRepository getJobsPrim() {
		return jobs;
	}
	/**
	 * @return the processorClasses
	 */
	private ProcessorClassRepository getProcessorClassesPrim() {
		return processorClasses;
	}
	/**
	 * @return the processors
	 */
	private ProcessorRepository getProcessorsPrim() {
		return processors;
	}
	/**
	 * @return the productClasses
	 */
	private ProductClassRepository getProductClassesPrim() {
		return productClasses;
	}
	/**
	 * @return the products
	 */
	private ProductRepository getProductsPrim() {
		return products;
	}

}

