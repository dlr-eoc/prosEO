/**
 * InputOutput.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.joborder;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Input or Output information
 *
 * For details see Generic IPF Interface Specification issue 1 revision 8 -
 * 03/08/2009 MMFI-GSEG-EOPG-TN-07-0003
 *
 * @author Ernst Melchinger *
 */
public class InputOutput {

	/** Type string for input elements */
	public final static String IO_TYPE_INPUT = "Input";

	/** Type string for output elements */
	public final static String IO_TYPE_OUTPUT = "Output";

	/** File name type for physical files */
	public final static String FN_TYPE_PHYSICAL = "Physical";

	/** File name type for logical files */
	public final static String FN_TYPE_LOGICAL = "Logical";

	/** File name type for file name stems */
	public final static String FN_TYPE_STEM = "Stem";

	/** File name type for regular expressions */
	public final static String FN_TYPE_REGEXP = "Regexp";

	/** File name type for directories */
	public final static String FN_TYPE_DIRECTORY = "Directory";

	/** File name type for ZIP archives (non-standard extension!) */
	public static final String FN_TYPE_ARCHIVE = "Archive";

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
	 * The product database ID (non-standard!)
	 */
	private String productID;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(InputOutput.class);

	/**
	 * List of file names (multiple allowed for input, at most one for output)
	 */
	private List<IpfFileName> fileNames = new ArrayList<>();

	/**
	 * List of time intervals (optional element)
	 */
	private List<TimeInterval> timeIntervals = new ArrayList<>();

	/**
	 * Gets the product ID
	 *
	 * @return the productID
	 */
	public String getProductID() {
		return productID;
	}

	/**
	 * Sets the product ID
	 *
	 * @param productID the productID to set
	 */
	public void setProductID(String productID) {
		this.productID = productID;
	}

	/**
	 * Gets the file type
	 *
	 * @return the fileType
	 */
	public String getFileType() {
		return fileType;
	}

	/**
	 * Sets the file type
	 *
	 * @param fileType the fileType to set
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	/**
	 * Gets the file name type
	 *
	 * @return the fileNameType
	 */
	public String getFileNameType() {
		return fileNameType;
	}

	/**
	 * Sets the file name type
	 *
	 * @param fileNameType the file name type to set ('Physical', 'Logical', 'Stem',
	 *                     'Regexp', 'Directory')
	 */
	public void setFileNameType(String fileNameType) throws IllegalArgumentException {
		if (FN_TYPE_PHYSICAL.equals(fileNameType) || FN_TYPE_LOGICAL.equals(fileNameType) || FN_TYPE_STEM.equals(fileNameType)
				|| FN_TYPE_REGEXP.equals(fileNameType) || FN_TYPE_DIRECTORY.equals(fileNameType)
				|| FN_TYPE_ARCHIVE.equals(fileNameType)) {
			this.fileNameType = fileNameType;
		} else {
			String message = "Invalid file name type " + fileNameType;
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Gets the list of file names
	 *
	 * @return the list of file names
	 */
	public List<IpfFileName> getFileNames() {
		return fileNames;
	}

	/**
	 * @return the timeIntervals
	 */
	public List<TimeInterval> getTimeIntervals() {
		return timeIntervals;
	}

	/**
	 * Create an Input/Output element of the given type
	 *
	 * @param type the input/output type (either 'Input' or 'Output')
	 * @throws IllegalArgumentException if the given type is not correct
	 */
	public InputOutput(String type) throws IllegalArgumentException {
		if (IO_TYPE_INPUT.equals(type) || IO_TYPE_OUTPUT.equals(type)) {
			this.type = type;
		} else {
			String message = "Invalid input/output type " + type;
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Create an Input/Output element with all attributes set
	 *
	 * @param fileType     the file type
	 * @param fileNameType the file name type ('Physical', 'Logical', 'Stem',
	 *                     'Regexp', 'Directory')
	 * @param type         the input/output type (either 'Input' or 'Output')
	 * @param productID    the product database ID
	 */
	public InputOutput(String fileType, String fileNameType, String type, String productID) throws IllegalArgumentException {
		if (IO_TYPE_INPUT.equals(type) || IO_TYPE_OUTPUT.equals(type)) {
			this.type = type;
			setFileType(fileType);
			setFileNameType(fileNameType);
			setProductID(productID);
		} else {
			String message = "Invalid input/output type " + type;
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 *
	 * @param doc              The Document
	 * @param parentElement    The node to add this as child
	 * @param prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, Boolean prosEOAttributes) {
		Element ioEle = doc.createElement(type);
		if (prosEOAttributes && productID != null && productID.length() > 0) {
			ioEle.setAttribute("Product_ID", productID);
		}
		parentElement.appendChild(ioEle);

		Element fileTypeEle = doc.createElement("File_Type");
		fileTypeEle.appendChild(doc.createTextNode(fileType));
		ioEle.appendChild(fileTypeEle);

		Element fileNameTypeEle = doc.createElement("File_Name_Type");
		fileNameTypeEle.appendChild(doc.createTextNode(fileNameType));
		ioEle.appendChild(fileNameTypeEle);

		if (IO_TYPE_INPUT.equals(type)) {
			Element fileNamesEle = doc.createElement("List_of_File_Names");
			Attr fnAttr = doc.createAttribute("count");
			fnAttr.setValue(Integer.toString(fileNames.size()));
			fileNamesEle.setAttributeNode(fnAttr);
			ioEle.appendChild(fileNamesEle);
			for (IpfFileName item : fileNames) {
				item.buildXML(doc, fileNamesEle, prosEOAttributes);
			}

			if (!timeIntervals.isEmpty()) {
				Element timeIntervalsEle = doc.createElement("List_of_Time_Intervals");
				Attr tiAttr = doc.createAttribute("count");
				tiAttr.setValue(Integer.toString(timeIntervals.size()));
				timeIntervalsEle.setAttributeNode(tiAttr);
				ioEle.appendChild(timeIntervalsEle);
				for (TimeInterval item : timeIntervals) {
					item.buildXML(doc, timeIntervalsEle, prosEOAttributes);
				}
			}
		} else {
			// An output element must have at most one file name
			if (1 < fileNames.size()) {
				String message = "Output element must have at most one file name, but has " + fileNames.size();
				logger.error(message);
				throw new IndexOutOfBoundsException(message);
			}
			if (1 == fileNames.size()) {
				fileNames.get(0).buildXML(doc, ioEle, prosEOAttributes);
			}
		}
	}

	/**
	 * Read info from XML sub tree
	 *
	 * @param thisNode XML node containing information
	 */
	public void read(Node thisNode) {
		if (thisNode != null) {
			NamedNodeMap nodeAttributes = (thisNode).getAttributes();
			for (int i = 0; i < nodeAttributes.getLength(); i++) {
				Node attrNode = nodeAttributes.item(i);
				switch (attrNode.getNodeName().toLowerCase()) {
				case "product_id":
					this.setProductID(attrNode.getTextContent().strip());
					break;
				}
			}
			Node child = thisNode.getFirstChild();
			while (child != null) {
				switch (child.getNodeName().toLowerCase()) {
				case "file_type":
					this.setFileType(child.getTextContent().strip());
					break;
				case "file_name_type":
					this.setFileNameType(child.getTextContent().strip());
					break;
				case "list_of_file_names":
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
				case "list_of_time_intervals":
					Node tiele = child.getFirstChild();
					while (tiele != null) {
						if (tiele.getNodeName().equalsIgnoreCase("Time_Interval")) {
							TimeInterval ti = new TimeInterval();
							ti.read(tiele);
							this.getTimeIntervals().add(ti);
						}
						tiele = tiele.getNextSibling();
					}
					break;
				case "file_name":
					IpfFileName fn = new IpfFileName();
					fn.read(child);
					this.getFileNames().add(fn);
					break;
				}
				child = child.getNextSibling();
			}
		}
	}
}