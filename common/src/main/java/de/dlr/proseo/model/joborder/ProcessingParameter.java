/**
 * ProcessingParameter.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.joborder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.dlr.proseo.model.enums.JobOrderVersion;

/**
 * Processing_Parameter information
 *
 * For details see Generic IPF Interface Specification issue 1 revision 8 -
 * 03/08/2009 MMFI-GSEG-EOPG-TN-07-0003
 *
 * @author Ernst Melchinger
 */
public class ProcessingParameter {

	/** The parameter name (Name Job Order element) */
	private String name;

	/** The parameter value (Value Job Order element) */
	private String value;

	/**
	 * Gets the processing parameter name
	 *
	 * @return the processing parameter name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the processing parameter name
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the processing parameter value
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the processing parameter value
	 *
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * No-argument constructor
	 */
	public ProcessingParameter() {
	}

	/**
	 * Constructor with parameter name and value arguments
	 *
	 * @param name  the name to set
	 * @param value the value to set
	 */
	public ProcessingParameter(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 *
	 * @param doc              The Document
	 * @param parentElement    The node to add this as child
	 * @param jobOrderVersion  the Job Order file specification version to apply
	 * @param prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, JobOrderVersion jobOrderVersion, Boolean prosEOAttributes) {
		Element procParamEle = doc
			.createElement(jobOrderVersion == JobOrderVersion.MMFI_1_8 ? "Processing_Parameter" : "Dyn_Processing_Parameter");
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
	 *
	 * @param thisNode XML node containing information
	 */
	public void read(Node thisNode) {
		if (thisNode != null) {
			Node child = thisNode.getFirstChild();
			while (child != null) {
				switch (child.getNodeName().toLowerCase()) {
				case "name":
					this.setName(child.getTextContent().strip());
					break;
				case "value":
					this.setValue(child.getTextContent().strip());
					break;
				}
				child = child.getNextSibling();
			}
		}
	}
}