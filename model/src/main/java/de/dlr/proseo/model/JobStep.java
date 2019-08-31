/**
 * JobStep.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Map;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * A single processor execution to produce a defined output product based on a defined set of required input product
 * (modelled as ProductQuery objects). A JobStep can be executed as soon as all its ProductQuerys are satisfied.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class JobStep extends PersistentObject {

	/** The job this job step belongs to */
	@ManyToOne
	private Job job;
	
	/**
	 * The currenet status of the job step; job steps in status INITIAL need to be released to advance to WAITING_INPUT status,
	 * job steps in status WAITING_INPUT and READY can be returned to INITIAL status. All other status transitions are automatic
	 * depending on processing progress.
	 */
	private JobStepState jobStepState;
	
	/** Additional parameter to set in the output products */
	@ElementCollection
	private Map<String, Parameter> outputParameters;
	
	/** The processing mode to run the processor(s) in (one of the modes specified for the mission) */
	private String processingMode;
	
	/** Query objects for input products */
	@OneToMany(mappedBy = "jobStep")
	private Set<ProductQuery> inputProductQueries;
	
	/** The output product of this job step */
	@OneToOne(mappedBy = "jobStep")
	private Product outputProduct;
	
	/**
	 * The possible processing states for a job step
	 */
	public enum JobStepState { INITIAL, WAITING_INPUT, READY, RUNNING, COMPLETED, FAILED }

	/**
	 * Gets the enclosing job
	 * 
	 * @return the job
	 */
	public Job getJob() {
		return job;
	}

	/**
	 * Sets the enclosing job
	 * 
	 * @param job the job to set
	 */
	public void setJob(Job job) {
		this.job = job;
	}

	/**
	 * Gets the state of the job step
	 * 
	 * @return the jobStepState
	 */
	public JobStepState getJobStepState() {
		return jobStepState;
	}

	/**
	 * Sets the state of the job step
	 * 
	 * @param jobStepState the jobStepState to set
	 */
	public void setJobStepState(JobStepState jobStepState) {
		this.jobStepState = jobStepState;
	}

	/**
	 * Gets the output parameters
	 * 
	 * @return the outputParameters
	 */
	public Map<String, Parameter> getOutputParameters() {
		return outputParameters;
	}

	/**
	 * Sets the output parameters
	 * 
	 * @param outputParameters the outputParameters to set
	 */
	public void setOutputParameters(Map<String, Parameter> outputParameters) {
		this.outputParameters = outputParameters;
	}

	/**
	 * Gets the processing mode to run the processor in
	 * 
	 * @return the processingMode
	 */
	public String getProcessingMode() {
		return processingMode;
	}

	/**
	 * Sets the processing mode to run the processor in
	 * 
	 * @param processingMode the processingMode to set
	 */
	public void setProcessingMode(String processingMode) {
		this.processingMode = processingMode;
	}

	/**
	 * Gets the product queries for the input products
	 * 
	 * @return the inputProductQueries
	 */
	public Set<ProductQuery> getInputProductQueries() {
		return inputProductQueries;
	}

	/**
	 * Sets the product queries for the input products
	 * 
	 * @param inputProductQueries the inputProductQueries to set
	 */
	public void setInputProductQueries(Set<ProductQuery> inputProductQueries) {
		this.inputProductQueries = inputProductQueries;
	}

	/**
	 * Gets the output product
	 * 
	 * @return the outputProduct
	 */
	public Product getOutputProduct() {
		return outputProduct;
	}

	/**
	 * Sets the output product
	 * 
	 * @param outputProduct the outputProduct to set
	 */
	public void setOutputProduct(Product outputProduct) {
		this.outputProduct = outputProduct;
	}

}
