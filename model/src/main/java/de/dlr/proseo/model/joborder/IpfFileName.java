/**
 * 
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
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}
	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/**
	 * @return the originalFileName
	 */
	public String getOriginalFileName() {
		return originalFileName;
	}
	/**
	 * @param originalFileName the originalFileName to set
	 */
	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}
	/**
	 * @return the fSType
	 */
	public String getFSType() {
		return FSType;
	}
	/**
	 * @param fSType the fSType to set
	 */
	public void setFSType(String fSType) {
		FSType = fSType;
	}
	/**
	 * @param fileName
	 * @param fSType
	 */
	public IpfFileName(String fileName, String fSType) {
		super();
		this.fileName = fileName;
		FSType = fSType;
	}
	public IpfFileName() {
		FSType = "POSIX";
	}
	/**
	 * @param fileName
	 */
	public IpfFileName(String fileName) {
		this.fileName = fileName;
		FSType = "POSIX";
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @prosEOAttributes if true, write attributes of prosEO specific data
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
