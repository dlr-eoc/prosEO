/**
 * ConfigurationFile.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;

import javax.persistence.Embeddable;

/**
 * A processor configuration file
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Embeddable
public class ConfigurationFile {

	
	/** A configuration file version identifier (level 3 "Version" from Generic IPF Interface Specifications, sec. 4.1.3) */
	private String fileVersion;
	
	/** 
	 * The POSIX file path for the configuration file (TBD: absolute according to IPF Spec or relative to some TBD mount point?; 
	 * level 3 "File_Name" from Generic IPF Interface Specifications, sec. 4.1.3)
	 */
	private String fileName;



	/**
	 * Gets the configuration file version
	 * 
	 * @return the file version
	 */
	public String getFileVersion() {
		return fileVersion;
	}

	/**
	 * Sets the configuration file version
	 * 
	 * @param fileVersion the file version to set
	 */
	public void setFileVersion(String fileVersion) {
		this.fileVersion = fileVersion;
	}

	/**
	 * Gets the configuration file path
	 * 
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets the configuration file path
	 * 
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(fileName, fileVersion);
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
		
		if (!(obj instanceof ConfigurationFile))
			return false;
		ConfigurationFile other = (ConfigurationFile) obj;
		return Objects.equals(fileName, other.fileName) && Objects.equals(fileVersion, other.fileVersion);
	}

	@Override
	public String toString() {
		return "ConfigurationFile [fileVersion=" + fileVersion + ", fileName=" + fileName + "]";
	}
}
