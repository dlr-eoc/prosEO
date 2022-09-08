/**
 * Task.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * A single, separately adressable execution step of a Processor. Tasks may be flagged as "external", in which case the input data 
 * products are forwarded to an external entity for processing, and the output products are retrieved from that external entity.
 * (Level 4 "Task" from Generic IPF Interface Specifications, sec. 4.1.3)
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class Task extends PersistentObject {
	
	/** The processor this task belongs to */
	@ManyToOne
	private Processor processor;
	
	/** 
	 * The task name (unique for the Processor, but no further restrictions; 
	 * level 5 "Name" from Generic IPF Interface Specifications, sec. 4.1.3)
	 */
	private String taskName;
	
	/** A version identifier for the task (level 5 "Version" from Generic IPF Interface Specifications, sec. 4.1.3) */
	private String taskVersion;
	
	/**
	 * Indicates whether a failure of the task results in a failure of the whole processor
	 * (level 5 "Critical" from Generic IPF Interface Specifications, sec. 4.1.3; default true)
	 */
	private Boolean isCritical = true;
	
	/**
	 * Used in case of Pools composed by more than one task. If a critical task with a criticality level of "n" fails it will
	 * cause the kill of all the tasks in the pool having criticality level less than or equal to "n". The tasks with a
	 * criticality level higher than "n" will complete their execution before the whole processor is interrupted and marked as "failed". 
	 * (Level 5 "Criticality_Level" from Generic IPF Interface Specifications, sec. 4.1.3)
	 */
	private Integer criticalityLevel;
	
	/** The number of CPUs used by the task (level 5 "Number_of_CPUs" from Generic IPF Interface Specifications, sec. 4.1.3; optional) */
	private Integer numberOfCpus;
	
	/**
	 * Minimum memory (RAM) requirement for this task in GiB (used by the Production Planner to request memory resources
	 * on a Kubernetes worker node when scheduling a task; if not set, no specific request will be made)
	 */
	private Integer minMemory;
	
	/**
	 * Intermediate output files for testing/evaluation purposes 
	 * (level 5 "List_of_Breakpoints" from Generic IPF Interface Specifications, sec. 4.1.3)
	 */
	@ElementCollection
	private List<String> breakpointFileNames = new ArrayList<>();

	/**
	 * Gets the processor this task belongs to
	 * 
	 * @return the processor
	 */
	public Processor getProcessor() {
		return processor;
	}

	/**
	 * Sets the processor this task belongs to
	 * 
	 * @param processor the processor to set
	 */
	public void setProcessor(Processor processor) {
		this.processor = processor;
	}

	/**
	 * Gets the task name
	 * 
	 * @return the taskName
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * Sets the task name
	 * 
	 * @param taskName the taskName to set
	 */
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	/**
	 * Gets the task version
	 * 
	 * @return the taskVersion
	 */
	public String getTaskVersion() {
		return taskVersion;
	}

	/**
	 * Sets the task version
	 * 
	 * @param taskVersion the taskVersion to set
	 */
	public void setTaskVersion(String taskVersion) {
		this.taskVersion = taskVersion;
	}

	/**
	 * Checks whether this task is critical
	 * 
	 * @return the isCritical
	 */
	public Boolean getIsCritical() {
		return isCritical;
	}

	/**
	 * Checks whether this task is critical (convenience alias for getIsCritical())
	 * 
	 * @return the isCritical
	 */
	public Boolean isCritical() {
		return this.getIsCritical();
	}

	/**
	 * Sets whether this task is critical
	 * 
	 * @param isCritical the isCritical to set
	 */
	public void setIsCritical(Boolean isCritical) {
		this.isCritical = isCritical;
	}

	/**
	 * Gets the criticality level
	 * 
	 * @return the criticalityLevel
	 */
	public Integer getCriticalityLevel() {
		return criticalityLevel;
	}

	/**
	 * Sets the criticality level
	 * 
	 * @param criticalityLevel the criticalityLevel to set
	 */
	public void setCriticalityLevel(Integer criticalityLevel) {
		this.criticalityLevel = criticalityLevel;
	}

	/**
	 * Gets the suggested number of CPUs
	 * 
	 * @return the numberOfCpus
	 */
	public Integer getNumberOfCpus() {
		return numberOfCpus;
	}

	/**
	 * Sets the suggested number of CPUs
	 * 
	 * @param numberOfCpus the numberOfCpus to set
	 */
	public void setNumberOfCpus(Integer numberOfCpus) {
		this.numberOfCpus = numberOfCpus;
	}

	/**
	 * Gets the minimum required memory in GiB
	 * 
	 * @return the minimum memory
	 */
	public Integer getMinMemory() {
		return minMemory;
	}

	/**
	 * Sets the minimum required memory in GiB
	 * 
	 * @param minMemory the minimum memory to set
	 */
	public void setMinMemory(Integer minMemory) {
		this.minMemory = minMemory;
	}

	/**
	 * Gets the breakpoint file names
	 * 
	 * @return the breakpointFileNames
	 */
	public List<String> getBreakpointFileNames() {
		return breakpointFileNames;
	}

	/**
	 * Sets the breakpoint file names
	 * 
	 * @param breakpointFileNames the breakpointFileNames to set
	 */
	public void setBreakpointFileNames(List<String> breakpointFileNames) {
		this.breakpointFileNames = breakpointFileNames;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(processor, taskName, taskVersion);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof Task))
			return false;
		Task other = (Task) obj;
		return Objects.equals(processor, other.processor) && Objects.equals(taskName, other.taskName)
				&& Objects.equals(taskVersion, other.taskVersion);
	}

	@Override
	public String toString() {
		return "Task [taskName=" + taskName + ", taskVersion=" + taskVersion + ", isCritical=" + isCritical + ", criticalityLevel="
				+ criticalityLevel + ", numberOfCpus=" + numberOfCpus + ", breakpointFileNames=" + breakpointFileNames + "]";
	}

}
