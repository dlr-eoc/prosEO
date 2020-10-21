/**
 * JobOrder.java
 * 
 * (C) 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.model.joborder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * Ipf_Job_Order information
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 * @author melchinger
 */
public class JobOrder {
	
	// Logger
	private static Logger logger = LoggerFactory.getLogger(JobOrder.class);
	
	// Error Messages
	private static final String MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER = "Error instantiating DocumentBuilder: {}";
	private static final String MSG_JOF_NOT_PARSEABLE = "JobOrder file {} not parseable ({})";
	
	/**
	 * The file name where job order is stored
	 */
	private String fileName;

	/**
	 * The Ipf_Conf part of the Job Order
	 */
	private Conf conf;
	
	/**
	 * The list of processors (List_of_Ipf_Procs element)
	 */
	private List<Proc> listOfProcs = new ArrayList<Proc>();

	/**
	 * Gets the Job Order file name
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Sets the Job Order file name
	 * @param fileName the file name to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	/**
	 * Gets the Conf Job Order element
	 * @return the Job Order configuration
	 */
	public Conf getConf() {
		return conf;
	}
	
	/**
	 * Sets the Conf Job Order element
	 * @param conf the Job Order configuration to set
	 */
	public void setConf(Conf conf) {
		this.conf = conf;
	}
	
	/**
	 * Gets the List_of_Ipf_Procs element
	 * @return the list of processors
	 */
	public List<Proc> getListOfProcs() {
		return listOfProcs;
	}
	
	/**
	 * Constructor with Ipf_Conf argument
	 * @param conf the Job Order configuration to set
	 */
	public JobOrder(Conf conf) {
		this.conf = conf;
	}
	
	/**
	 * No-argument constructor
	 */
	public JobOrder() { }
	
	/**
	 * Build XML tree and write it to file named fileName.
	 * @param fileName the file name
	 * @param prosEOAttributes if true, write attributes of prosEO specific data
	 * @return true after success, else false
	 */
	public Boolean writeXML(String fileName, Boolean prosEOAttributes) {
		try {
			FileOutputStream fout = new FileOutputStream(fileName);
			writeXMLToStream(fout, prosEOAttributes);
			fout.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Create a Base64-coded string from the XML representation of this Job Order
	 * @param prosEOAttributes if true, write attributes of prosEO specific data
	 * @return the Base64-coded string
	 */
	public String buildBase64String(Boolean prosEOAttributes) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			writeXMLToStream(baos, prosEOAttributes);	
			String xmlString = baos.toString();
			baos.close();
			byte[] bytes = java.util.Base64.getEncoder().encode(xmlString.getBytes());
			return new String(bytes);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Writes the content of the Job Order to an XML-formatted output stream
	 * @param aStream the stream to write to
	 * @param prosEOAttributes if true, write attributes of prosEO specific data 
	 * @return true, if the operation completed successfully, false otherwise
	 */
	public Boolean writeXMLToStream(OutputStream aStream, Boolean prosEOAttributes) {
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
		    StreamResult result = new StreamResult(aStream);
		    
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
	 * Read a Job Order  from an XML-formatted string
	 * @param jobOrderString the XML-formatted Job Order File
	 * @return a Job Order object
	 */
	public JobOrder read(String jobOrderString) {
		InputStream aStream = new ByteArrayInputStream(jobOrderString.getBytes());

		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error(MSG_ERROR_INSTANTIATING_DOCUMENT_BUILDER, e.getMessage());
			return null;
		}
		Document jobOrderDoc = null;
		try {
			jobOrderDoc = docBuilder.parse(aStream);
		} catch (SAXException | IOException e) {
			logger.error(MSG_JOF_NOT_PARSEABLE, e.getMessage());
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
