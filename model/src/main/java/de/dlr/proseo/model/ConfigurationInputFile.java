/**
 * ConfigurationInputFile.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;

/**
 * An additional input file for a processor configuration for inclusion in generated Job Order Files
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class ConfigurationInputFile extends PersistentObject {

	/**
	 * File type according to processor-specific ICD or "LOG" (Generic IPF Interface Specification, sec. 4.2.3)
	 */
	private String fileType;
	
	/** 
	 * Type of the file names (one of { "Physical", "Logical", "Stem", "Regexp", "Directory" }; Generic IPF Interface Specification, sec. 4.2.3)
	 */
	private String fileNameType;

	/**
	 * A list of legal and valid filenames (Generic IPF Interface Specification, sec. 4.2.3)
	 */
	@ElementCollection
	private List<String> fileNames = new ArrayList<>();

	/**
	 * Gets the input file type
	 * 
	 * @return the fileType
	 */
	public String getFileType() {
		return fileType;
	}

	/**
	 * Sets the input file type
	 * 
	 * @param fileType the fileType to set
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	/**
	 * Gets the input file name type
	 * 
	 * @return the input file name type
	 */
	public String getFileNameType() {
		return fileNameType;
	}

	/**
	 * Sets the input file name type
	 * 
	 * @param fileNameType the file name type to set
	 */
	public void setFileNameType(String fileNameType) {
		this.fileNameType = fileNameType;
	}

	/**
	 * Gets the input file names
	 * 
	 * @return the file names
	 */
	public List<String> getFileNames() {
		return fileNames;
	}

	/**
	 * Sets the input file names
	 * 
	 * @param fileNames the file names to set
	 */
	public void setFileNames(List<String> fileNames) {
		this.fileNames = fileNames;
	}

	@Override
	public String toString() {
		return "ConfigurationInputFile [fileType=" + fileType + ", fileNameType=" + fileNameType + ", fileNames=" + fileNames + "]";
	}

}
