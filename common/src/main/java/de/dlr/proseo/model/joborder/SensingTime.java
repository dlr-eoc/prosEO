/**
 * SensingTime.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.joborder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Sensing_Time information
 *
 * For details see Generic IPF Interface Specification issue 1 revision 8 -
 * 03/08/2009 MMFI-GSEG-EOPG-TN-07-0003
 *
 * @author Ernst Melchinger
 */
public class SensingTime {

	/** The sensing start time (Start Job Order element) */
	private String start;

	/** The sensing stop time (Stop Job Order element) */
	private String stop;

	/**
	 * Gets the sensing start time
	 *
	 * @return the sensing start time
	 */
	public String getStart() {
		return start;
	}

	/**
	 * Sets the sensing start time
	 *
	 * @param start the sensing start time to set
	 */
	public void setStart(String start) {
		this.start = start;
	}

	/**
	 * Gets the sensing stop time
	 *
	 * @return the sensing stop time
	 */
	public String getStop() {
		return stop;
	}

	/**
	 * Sets the sensing stop time
	 *
	 * @param stop the sensing stop time to set
	 */
	public void setStop(String stop) {
		this.stop = stop;
	}

	/**
	 * No-argument constructor
	 */
	public SensingTime() {
	}

	/**
	 * Constructor with sensing start and stop time arguments
	 *
	 * @param start the sensing start time to set
	 * @param stop  the sensing stop time to set
	 */
	public SensingTime(String start, String stop) {
		this.start = start;
		this.stop = stop;
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 *
	 * @param doc              The Document
	 * @param parentElement    The node to add this as child
	 * @param prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, Boolean prosEOAttributes) {
		Element sensingTimeEle = doc.createElement("Sensing_Time");
		parentElement.appendChild(sensingTimeEle);

		Element startEle = doc.createElement("Start");
		startEle.appendChild(doc.createTextNode(start));
		sensingTimeEle.appendChild(startEle);

		Element stopEle = doc.createElement("Stop");
		stopEle.appendChild(doc.createTextNode(stop));
		sensingTimeEle.appendChild(stopEle);
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
				case "start":
					this.setStart(child.getTextContent().strip());
					break;
				case "stop":
					this.setStop(child.getTextContent().strip());
					break;
				}
				child = child.getNextSibling();
			}
		}
	}
}