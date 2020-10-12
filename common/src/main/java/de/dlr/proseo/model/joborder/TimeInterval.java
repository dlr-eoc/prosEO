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
 * Time interval for input elements
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
	 * The start time
	 */
	private String start;
	/**
	 * The stop time
	 */
	private String stop;
	/**
	 * The file name
	 */
	private String fileName;

	public TimeInterval() {
		
	}
	/**
	 * @param start
	 * @param stop
	 */
	public TimeInterval(String start, String stop, String fileName) {
		this.start = start;
		this.stop = stop;
		this.fileName = fileName;
	}

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
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @prosEOAttributes if true, write attributes of prosEO specific data
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
