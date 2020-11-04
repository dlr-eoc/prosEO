/**
 * IpfFileName.java
 */
package de.dlr.proseo.model.joborder;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author melchinger
 *
 * File_Name information
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 */
public class IpfFileName {
	private static final String DEFAULT_FILE_SYSTEM_TYPE = "POSIX";
	/**
	 * The file name
	 */
	private String fileName;
	/**
	 * The original file name
	 */
	private String originalFileName;
	/**
	 * The file system type, default to POSIX
	 */
	private String FSType;
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
	 * @param fileName the file name to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/**
	 * Gets the original file name
	 * @return the original file name
	 */
	public String getOriginalFileName() {
		return originalFileName;
	}
	/**
	 * Sets the original file name
	 * @param originalFileName the original file name to set
	 */
	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}
	/**
	 * Gets the file system type
	 * @return the file system type
	 */
	public String getFSType() {
		return FSType;
	}
	/**
	 * Sets the file system type
	 * @param fSType the file system type to set
	 */
	public void setFSType(String fSType) {
		FSType = fSType;
	}
	/**
	 * Constructor with file name and file system type arguments
	 * @param fileName the file name to set
	 * @param fSType the file system type to set
	 */
	public IpfFileName(String fileName, String fSType) {
		super();
		this.fileName = fileName;
		FSType = fSType;
	}
	/**
	 * No-argument constructor, sets the file system type to the default value "POSIX"
	 */
	public IpfFileName() {
		FSType = DEFAULT_FILE_SYSTEM_TYPE;
	}
	/**
	 * Constructor with file name argument, sets the file system type to the default value "POSIX"
	 * @param fileName the file name to set
	 */
	public IpfFileName(String fileName) {
		this.fileName = fileName;
		FSType = DEFAULT_FILE_SYSTEM_TYPE;
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @param prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, Boolean prosEOAttributes) {
	    Element fnEle = doc.createElement("File_Name");
	    if (prosEOAttributes == true) {
	    	Attr attr = doc.createAttribute("FS_Type");
	    	attr.setValue(FSType);
	    	fnEle.setAttributeNode(attr); 
	    }
	    fnEle.appendChild(doc.createTextNode(fileName));
	    parentElement.appendChild(fnEle);
	}

	/**
	 * Read info from XML sub tree
	 * @param thisNode XML node containing information
	 */
	public void read(Node thisNode) {
		if (thisNode != null) {
			this.setFileName(thisNode.getTextContent().strip());
			NamedNodeMap nodeAttributes = (thisNode).getAttributes();
			for (int i = 0; i < nodeAttributes.getLength(); i++) {
				Node attrNode = nodeAttributes.item(i);
				switch (attrNode.getNodeName().toLowerCase()) {
				case "fs_type" : 
					this.setFSType(attrNode.getTextContent().strip());
					break;
				}
			}
		}		
	}
}
