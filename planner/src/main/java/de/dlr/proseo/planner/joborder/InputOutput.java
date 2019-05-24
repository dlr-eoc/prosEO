package de.dlr.proseo.planner.joborder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author melchinger
 *
 * Input or Output information
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 */
public class InputOutput {
	/**
	 * The type of element (Input/Output)
	 */
	private String type;
	/**
	 * The IPF file type
	 */
	private String fileType;
	/**
	 * The IPF file name type
	 */
	private String fileNameType;
	/**
	 * List of file names
	 */
	private List<IpfFileName> fileNames;
	/**
	 * @return the fileType
	 */
	public String getFileType() {
		return fileType;
	}
	/**
	 * @param fileType the fileType to set
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	/**
	 * @return the fileNameType
	 */
	public String getFileNameType() {
		return fileNameType;
	}
	/**
	 * @param fileNameType the fileNameType to set
	 */
	public void setFileNameType(String fileNameType) {
		this.fileNameType = fileNameType;
	}
	/**
	 * @return the fileNames
	 */
	public List<IpfFileName> getFileNames() {
		return fileNames;
	}

	public InputOutput(String type) {
		this.type = type;
		this.fileNames = new ArrayList<IpfFileName>();
	}
	/**
	 * @param fileType
	 * @param fileNameType
	 */
	public InputOutput(String fileType, String fileNameType, String type) {
		this.type = type;
		this.fileType = fileType;
		this.fileNameType = fileNameType;
		this.fileNames = new ArrayList<IpfFileName>();
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, Boolean prosEOAttributes) {
	    Element ioEle = doc.createElement(type);
	    parentElement.appendChild(ioEle);

        Element fileTypeEle = doc.createElement("File_Type");
        fileTypeEle.appendChild(doc.createTextNode(fileType));
        ioEle.appendChild(fileTypeEle);

        Element fileNameTypeEle = doc.createElement("File_Name_Type");
        fileNameTypeEle.appendChild(doc.createTextNode(fileNameType));
        ioEle.appendChild(fileNameTypeEle);

	    Element fileNamesEle = doc.createElement("List_of_File_Names");
	    Attr attr = doc.createAttribute("count");
	    attr.setValue(Integer.toString(fileNames.size()));
	    fileNamesEle.setAttributeNode(attr); 
	    ioEle.appendChild(fileNamesEle);
	    
	    for (IpfFileName item : fileNames) {
	    	item.buildXML(doc, fileNamesEle, prosEOAttributes);
	    }
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
				case "file_type" : 
					this.setFileType(child.getTextContent().strip());
					break;
				case "file_name_type" : 
					this.setFileNameType(child.getTextContent().strip());
					break;
				case "list_of_file_names" : 
					Node fnele = child.getFirstChild();
					while (fnele != null) {
						if (fnele.getNodeName().equalsIgnoreCase("File_Name")) {
							IpfFileName fn = new IpfFileName();
							fn.read(fnele);
							this.getFileNames().add(fn);
						}
						fnele = fnele.getNextSibling();
					}						
					break;
				}
				child = child.getNextSibling();
			}
		}		
	}
}
