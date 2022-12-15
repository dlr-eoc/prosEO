/**
 * BreakpointFile.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.joborder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static de.dlr.proseo.model.joborder.InputOutput.*;

/**
 * Breakpoint file information
 * 
 * Uses file name types from InputOutput class
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 * @author Thomas Bassler
 *
 */
public class BreakpointFile {
	
	/**
	 * Flag indicating whether the breakpoint file is active
	 */
	private boolean enable;
	
	/**
	 * The IPF file type
	 */
	private String fileType;
	
	/**
	 * The IPF file name type
	 */
	private String fileNameType;

	/**
	 * Breakpoint file name
	 */
	private String fileName;
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(BreakpointFile.class);

	/**
	 * Gets the flag indicating whether the breakpoint file is active
	 * 
	 * @return true, if active, false otherwise
	 */
	public boolean getEnable() {
		return enable;
	}

	/**
	 * Sets the flag indicating whether the breakpoint file is active
	 * 
	 * @param enable the flag to set
	 */
	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	/**
	 * Gets the file type
	 * 
	 * @return the fileType
	 */
	public String getFileType() {
		return fileType;
	}
	
	/**
	 * Sets the file type
	 * 
	 * @param fileType the fileType to set
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	
	/**
	 * Gets the file name type
	 * 
	 * @return the fileNameType
	 */
	public String getFileNameType() {
		return fileNameType;
	}
	
	/**
	 * Sets the file name type
	 * 
	 * @param fileNameType the file name type to set ('Physical', 'Logical', 'Stem', 'Regexp', 'Directory')
	 */
	public void setFileNameType(String fileNameType) throws IllegalArgumentException {
		if (FN_TYPE_PHYSICAL.equals(fileNameType) || FN_TYPE_LOGICAL.equals(fileNameType) || FN_TYPE_STEM.equals(fileNameType)
				|| FN_TYPE_REGEXP.equals(fileNameType) || FN_TYPE_DIRECTORY.equals(fileNameType) || FN_TYPE_ARCHIVE.equals(fileNameType) ) {
			this.fileNameType = fileNameType;
		} else {
			String message = "Invalid file name type " + fileNameType;
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
	}
	
	/**
	 * Gets the file name
	 * 
	 * @return the file name
	 */
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Sets the file name
	 * 
	 * @param fileName the file name to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Create a Breakpoint File element
	 */
	public BreakpointFile() {
	}
	/**
	 * Create  a Breakpoint File element with all attributes set
	 * @param enable flag indicating whether the breakpoint file is enabled
	 * @param fileType the file type
	 * @param fileNameType the file name type ('Physical', 'Logical', 'Stem', 'Regexp', 'Directory')
	 * @param fileName the file name
	 */
	public BreakpointFile(boolean enable, String fileType, String fileNameType, String fileName) throws IllegalArgumentException {
		setEnable(enable);
		setFileType(fileType);
		setFileNameType(fileNameType);
		setFileName(fileName);
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * 
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @param prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, Boolean prosEOAttributes) {
	    Element ioEle = doc.createElement("Enable");
	    ioEle.appendChild(doc.createTextNode(String.valueOf(enable)));
	    parentElement.appendChild(ioEle);

        Element fileTypeEle = doc.createElement("File_Type");
        fileTypeEle.appendChild(doc.createTextNode(fileType));
        ioEle.appendChild(fileTypeEle);

        Element fileNameTypeEle = doc.createElement("File_Name_Type");
        fileNameTypeEle.appendChild(doc.createTextNode(fileNameType));
        ioEle.appendChild(fileNameTypeEle);

		Element fileNameEle = doc.createElement("File_Name");
		ioEle.appendChild(fileNameEle);
        fileNameTypeEle.appendChild(doc.createTextNode(fileName));
			
	}
	

	/**
	 * Read info from XML sub tree
	 * @param thisNode XML node containing information
	 */
	public void read(Node thisNode) {
		if (thisNode != null) {
			Node child = thisNode.getFirstChild();
			while (child != null) {
				switch (child.getNodeName().toLowerCase()) {
				case "enable" : 
					this.setEnable(Boolean.parseBoolean(child.getTextContent().strip()));
					break;
				case "file_type" : 
					this.setFileType(child.getTextContent().strip());
					break;
				case "file_name_type" : 
					this.setFileNameType(child.getTextContent().strip());
					break;
				case "file_name" : 
					this.setFileName(child.getTextContent().strip());
					break;
				}
				child = child.getNextSibling();
			}
		}		
	}
}
