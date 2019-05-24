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
 * Sensing_Time information
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 */
public class SensingTime {
	/**
	 * The start time
	 */
	private String start;
	/**
	 * The stop time
	 */
	private String stop;
	/**
	 * @return the start
	 */
	public String getStart() {
		return start;
	}
	/**
	 * @param start the start to set
	 */
	public void setStart(String start) {
		this.start = start;
	}
	/**
	 * @return the stop
	 */
	public String getStop() {
		return stop;
	}
	/**
	 * @param stop the stop to set
	 */
	public void setStop(String stop) {
		this.stop = stop;
	}

	public SensingTime() {
		
	}
	/**
	 * @param start
	 * @param stop
	 */
	public SensingTime(String start, String stop) {
		this.start = start;
		this.stop = stop;
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @prosEOAttributes if true, write attributes of prosEO specific data
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
	 * @param thisNode XML node containing information
	 */
	public void read(Node thisNode) {
		if (thisNode != null) {
			Node child = thisNode.getFirstChild();
			while (child != null) {
				switch (child.getNodeName().toLowerCase()) {
				case "start" : 
					this.setStart(child.getTextContent().strip());
					break;
				case "stop" : 
					this.setStop(child.getTextContent().strip());
					break;
				}
				child = child.getNextSibling();
			}
		}
	}
}
