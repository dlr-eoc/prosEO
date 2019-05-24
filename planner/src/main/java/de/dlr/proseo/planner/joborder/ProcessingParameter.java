/**
 * 
 */
package de.dlr.proseo.planner.joborder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author melchinger
 *
 * Processing_Parameter information
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 */
public class ProcessingParameter {
	/**
	 * The parameter name
	 */
	private String name;
	/**
	 * The parameter value
	 */
	private String value;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
	

	public ProcessingParameter() {
	}
	
	/**
	 * @param name
	 * @param value
	 */
	public ProcessingParameter(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, Boolean prosEOAttributes) {
	    Element procParamEle = doc.createElement("Processing_Parameter");
	    parentElement.appendChild(procParamEle);

        Element nameEle = doc.createElement("Name");
        nameEle.appendChild(doc.createTextNode(name));
        procParamEle.appendChild(nameEle);

        Element valueEle = doc.createElement("Value");
        valueEle.appendChild(doc.createTextNode(value));
        procParamEle.appendChild(valueEle);
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
				case "name" : 
					this.setName(child.getTextContent().strip());
					break;
				case "value" : 
					this.setValue(child.getTextContent().strip());
					break;
				}
				child = child.getNextSibling();
			}
		}
	}
}
