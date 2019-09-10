/**
 * Processor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * A specific version of a ProcessorClass. Each permissible combination of a specific Processor with a specific Configuration is
 * modelled as a ConfiguredProcessor. A Processor can consist of multiple tasks
 * (note: for Sentinel-5P only one task per Processor is expected).
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = @Index(unique = true, columnList = "processor_class_id, processor_version"))
public class Processor extends PersistentObject {

	/** The processor class this processor (version) belongs to */
	@ManyToOne
	private ProcessorClass processorClass;
	
	/** The processor configurations this processor version is valid for */
	@OneToMany(mappedBy = "processor")
	private Set<ConfiguredProcessor> configuredProcessors = new HashSet<>();
	
	/** 
	 * Version identification of the processor executable (Docker image; 
	 * level 1 "Version" from Generic IPF Interface Specifications, sec. 4.1.3)
	 */
	private String processorVersion;
	
	/** Indicates a test version of the processor ("Test" from Generic IPF Interface Specifications, sec. 4.1.3) */
	private Boolean isTest;
	
	/**
	 * Minimum disk space in MB, worst case estimate plus safety margin
	 * ("Min_Disk_Space" from Generic IPF Interface Specifications, sec. 4.1.3)
	 */
	private Integer minDiskSpace;
	
	/** Execution time limit in seconds (default 0 means no limit; "Max_Time" from Generic IPF Interface Specifications, sec. 4.1.3) */
	private Integer maxTime;
	
	/** 
	 * Indicates whether the processor uses a sensing time interval as main processing parameter; if "true" a start/stop time is 
	 * to be inserted into the "Sensing_Time" tag of the Job Order File
	 * ("Sensing_Time_flag" from Generic IPF Interface Specifications, sec. 4.1.3; 
	 * deviating from the specification the default value is "true", because prosEO aims primarily at time-based processing)
	 */
	private Boolean sensingTimeFlag;
	
	/** List of tasks for this processor */
	@OneToMany(mappedBy = "processor")
	private List<Task> tasks = new ArrayList<>();

	/**
	 * Gets the processor class
	 * 
	 * @return the processorClass
	 */
	public ProcessorClass getProcessorClass() {
		return processorClass;
	}

	/**
	 * Sets the processor class
	 * 
	 * @param processorClass the processorClass to set
	 */
	public void setProcessorClass(ProcessorClass processorClass) {
		this.processorClass = processorClass;
	}

	/**
	 * Gets the processor version
	 * 
	 * @return the processorVersion
	 */
	public String getProcessorVersion() {
		return processorVersion;
	}

	/**
	 * Sets the processor version
	 * 
	 * @param processorVersion the processorVersion to set
	 */
	public void setProcessorVersion(String processorVersion) {
		this.processorVersion = processorVersion;
	}

	/**
	 * Checks whether this processor version is a test/evaluation version
	 * 
	 * @return the isTest
	 */
	public Boolean getIsTest() {
		return isTest;
	}

	/**
	 * Checks whether this processor version is a test/evaluation version (convenience alias for getIsTest())
	 * 
	 * @return the isTest
	 */
	public Boolean isTest() {
		return this.getIsTest();
	}

	/**
	 * Sets whether this processor version is a test/evaluation version
	 * 
	 * @param isTest the isTest to set
	 */
	public void setIsTest(Boolean isTest) {
		this.isTest = isTest;
	}

	/**
	 * Gets the minimum required disk space in MB
	 * 
	 * @return the minDiskSpace
	 */
	public Integer getMinDiskSpace() {
		return minDiskSpace;
	}

	/**
	 * Sets the minimum required disk space in MB
	 * 
	 * @param minDiskSpace the minDiskSpace to set
	 */
	public void setMinDiskSpace(Integer minDiskSpace) {
		this.minDiskSpace = minDiskSpace;
	}

	/**
	 * Gets the maximum execution time in seconds
	 * 
	 * @return the maxTime
	 */
	public Integer getMaxTime() {
		return maxTime;
	}

	/**
	 * Sets the maximum execution time in seconds
	 * 
	 * @param maxTime the maxTime to set
	 */
	public void setMaxTime(Integer maxTime) {
		this.maxTime = maxTime;
	}

	/**
	 * Checks whether this processor is executed on time intervals
	 * 
	 * @return the sensingTimeFlag
	 */
	public Boolean getSensingTimeFlag() {
		return sensingTimeFlag;
	}

	/**
	 * Checks whether this processor is executed on time intervals (convenience alias for getSensingTimeFlag())
	 * 
	 * @return the sensingTimeFlag
	 */
	public Boolean hasSensingTimeFlag() {
		return this.getSensingTimeFlag();
	}

	/**
	 * Sets whether this processor is executed on time intervals
	 * 
	 * @param sensingTimeFlag the sensingTimeFlag to set
	 */
	public void setSensingTimeFlag(Boolean sensingTimeFlag) {
		this.sensingTimeFlag = sensingTimeFlag;
	}

	/**
	 * Gets the task list
	 * 
	 * @return the tasks
	 */
	public List<Task> getTasks() {
		return tasks;
	}

	/**
	 * Sets the task list
	 * 
	 * @param tasks the tasks to set
	 */
	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(processorClass, processorVersion);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Processor))
			return false;
		Processor other = (Processor) obj;
		return Objects.equals(processorClass, other.processorClass) && Objects.equals(processorVersion, other.processorVersion);
	}
}
