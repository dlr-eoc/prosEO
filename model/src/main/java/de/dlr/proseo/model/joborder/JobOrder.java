/**
 * 
 */
package de.dlr.proseo.model.joborder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author melchinger
 *
 * IPF_Job_Order information
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 */
public class JobOrder {
	// Logger
	private static Logger logger = LoggerFactory.getLogger(JobOrder.class);
	// Error Messages
	private static final String MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER = "Error instantiating DocumentBuilder: {}";
	private static final String MSG_JOF_NOT_PARSEABLE = "JobOrder file {} not parseable ({})";
	/**
	 * 
	 */
	private Conf conf;
	/**
	 * 
	 */
	private List<Proc> listOfProcs;
	/**
	 * @return the conf
	 */
	public Conf getConf() {
		return conf;
	}
	/**
	 * @param conf the conf to set
	 */
	public void setConf(Conf conf) {
		this.conf = conf;
	}
	/**
	 * @return the listOfProcs
	 */
	public List<Proc> getListOfProcs() {
		return listOfProcs;
	}
	/**
	 * @param conf
	 */
	public JobOrder(Conf conf) {
		this.conf = conf;
		this.listOfProcs = new ArrayList<Proc>();
	}
	public JobOrder() {
		this.listOfProcs = new ArrayList<Proc>();
	}
	
	/**
	 * Build XML tree and write it to file named fileName.
	 * @param fileName the file name
	 * @prosEOAttributes if true, write attributes of prosEO specific data
	 * @return true after success, else false
	 */
	public Boolean writeXML(String fileName, Boolean prosEOAttributes) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("Ipf_Job_Order");
			doc.appendChild(rootElement);
			conf.buildXML(doc, rootElement, prosEOAttributes);
			Element listEle = doc.createElement("List_of_Ipf_Procs");
		    Attr attr = doc.createAttribute("count");
		    attr.setValue(Integer.toString(listOfProcs.size()));
		    listEle.setAttributeNode(attr); 
		    rootElement.appendChild(listEle);
		    
		    for (Proc item : listOfProcs) {
		    	item.buildXML(doc, listEle, prosEOAttributes);
		    }		    

		    TransformerFactory transformerFactory = TransformerFactory.newInstance();
		    Transformer transformer = transformerFactory.newTransformer();
		    DOMSource source = new DOMSource(doc);
		    StreamResult result = new StreamResult(new File(fileName));

		    // Output to console for testing
		    // StreamResult result = new StreamResult(System.out);

		    transformer.transform(source, result);
		    
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	

	/**
	 * Read info from XML sub tree
	 * @param thisNode XML node containing information
	 */
	public JobOrder read(String aFileName) {
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error(MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER, e.getMessage());
			return null;
		}
		Document jobOrderDoc = null;
		try {
			jobOrderDoc = docBuilder.parse(aFileName);
		} catch (SAXException | IOException e) {
			logger.error(MSG_JOF_NOT_PARSEABLE, aFileName, e.getMessage());
			return null;
		}
		// now we have the document, fill tree structure
		Node ele = jobOrderDoc.getDocumentElement();
		while (ele != null) {
			if (ele.getNodeName().equalsIgnoreCase("Ipf_Job_Order")) {
				// we are there! try it
				this.setConf(null);
				// read conf and proc
				Node child = ele.getFirstChild();
				while (child != null) {
					if (child.getNodeName().equalsIgnoreCase("Ipf_Conf")) {
						conf = new Conf();
						conf.read(child);
					} else if (child.getNodeName().equalsIgnoreCase("List_of_Ipf_Procs")) {
						Node procNode = child.getFirstChild();
						while (procNode != null) {
							if (procNode.getNodeName().equalsIgnoreCase("Ipf_Proc")) {
								Proc proc = new Proc();
								proc.read(procNode);
								this .getListOfProcs().add(proc);
							}
							procNode = procNode.getNextSibling();
						}
					}
					child = child.getNextSibling();
				}
			}
			ele = ele.getNextSibling();
		}
		return this;
	}
}
