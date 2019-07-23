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
import de.dlr.proseo.model.Product.Parameter;

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
	 * @return the job
	 */
	public Job getJob() {
		return job;
	}

	/**
	 * @param job the job to set
	 */
	public void setJob(Job job) {
		this.job = job;
	}

	/**
	 * @return the jobStepState
	 */
	public JobStepState getJobStepState() {
		return jobStepState;
	}

	/**
	 * @param jobStepState the jobStepState to set
	 */
	public void setJobStepState(JobStepState jobStepState) {
		this.jobStepState = jobStepState;
	}

	/**
	 * @return the outputParameters
	 */
	public Map<String, Parameter> getOutputParameters() {
		return outputParameters;
	}

	/**
	 * @param outputParameters the outputParameters to set
	 */
	public void setOutputParameters(Map<String, Parameter> outputParameters) {
		this.outputParameters = outputParameters;
	}

	/**
	 * @return the inputProductQueries
	 */
	public Set<ProductQuery> getInputProductQueries() {
		return inputProductQueries;
	}

	/**
	 * @param inputProductQueries the inputProductQueries to set
	 */
	public void setInputProductQueries(Set<ProductQuery> inputProductQueries) {
		this.inputProductQueries = inputProductQueries;
	}

	/**
	 * @return the outputProduct
	 */
	public Product getOutputProduct() {
		return outputProduct;
	}

	/**
	 * @param outputProduct the outputProduct to set
	 */
	public void setOutputProduct(Product outputProduct) {
		this.outputProduct = outputProduct;
	}

}
