/**
 * TimeInterval.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.joborder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Time interval for input elements (Time_Interval Job Order element)
 * (note that in contrast to IpfFileName, file names in time intervals do not carry the FSType attribute)
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 * @author Dr. Thomas Bassler
 */
public class TimeInterval {
	/**
	 * The start time (Start element)
	 */
	private String start;
	/**
	 * The stop time (Stop element)
	 */
	private String stop;
	/**
	 * The name of the file applicable for this time interval (File_Name element)
	 */
	private String fileName;

	/**
	 * No-argument constructor
	 */
	public TimeInterval() { }
	
	/**
	 * Constructor with time interval start/stop times and file name
	 * @param start the interval start time to set
	 * @param stop the interval stop time to set
	 * @param fileName the file name to set
	 */
	public TimeInterval(String start, String stop, String fileName) {
		this.start = start;
		this.stop = stop;
		this.fileName = fileName;
	}

	/**
	 * Gets the time interval start
	 * @return the interval start time
	 */
	public String getStart() {
		return start;
	}
	/**
	 * Sets the time interval start
	 * @param start the interval start time to set
	 */
	public void setStart(String start) {
		this.start = start;
	}
	/**
	 * Gets the time interval end
	 * @return the interval stop time
	 */
	public String getStop() {
		return stop;
	}
	/**
	 * Sets the time interval end
	 * @param stop the interval stop time to set
	 */
	public void setStop(String stop) {
		this.stop = stop;
	}
	/**
	 * Gets the file name
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
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @param prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, Boolean prosEOAttributes) {
	    Element timeIntervalEle = doc.createElement("Time_Interval");
	    parentElement.appendChild(timeIntervalEle);

        Element startEle = doc.createElement("Start");
        startEle.appendChild(doc.createTextNode(start));
        timeIntervalEle.appendChild(startEle);

        Element stopEle = doc.createElement("Stop");
        stopEle.appendChild(doc.createTextNode(stop));
        timeIntervalEle.appendChild(stopEle);

	    Element fnEle = doc.createElement("File_Name");
	    fnEle.appendChild(doc.createTextNode(fileName));
	    timeIntervalEle.appendChild(fnEle);
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
				case "file_name" : 
					this.setFileName(child.getTextContent().strip());
					break;
				}
				child = child.getNextSibling();
			}
		}
	}
}
