/**
 * 
 */
package de.dlr.proseo.model.joborder;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author melchinger
 *
 * IPF_Proc information
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 */
public class Proc {
	/**
	 * The task name
	 */
	private String taskName;
	/**
	 * The task version
	 */
	private String taskVersion;
	/**
	 * List of input definitions
	 */
	private List<InputOutput> listOfInputs;
	/**
	 * List of output definitions
	 */
	private List<InputOutput> listOfOutputs;
	/**
	 * @return the taskName
	 */
	public String getTaskName() {
		return taskName;
	}
	/**
	 * @param taskName the taskName to set
	 */
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	/**
	 * @return the taskVersion
	 */
	public String getTaskVersion() {
		return taskVersion;
	}
	/**
	 * @param taskVersion the taskVersion to set
	 */
	public void setTaskVersion(String taskVersion) {
		this.taskVersion = taskVersion;
	}
	/**
	 * @return the listOfInputs
	 */
	public List<InputOutput> getListOfInputs() {
		return listOfInputs;
	}
	/**
	 * @return the listOfOutputs
	 */
	public List<InputOutput> getListOfOutputs() {
		return listOfOutputs;
	}
	public Proc() {
		this.listOfInputs = new ArrayList<InputOutput>();
		this.listOfOutputs = new ArrayList<InputOutput>();
	}	

	/**
	 * @param taskName
	 * @param taskVersion
	 */
	public Proc(String taskName, String taskVersion) {
		this.taskName = taskName;
		this.taskVersion = taskVersion;
		this.listOfInputs = new ArrayList<InputOutput>();
		this.listOfOutputs = new ArrayList<InputOutput>();
	}	

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, Boolean prosEOAttributes) {
	    Element procEle = doc.createElement("Ipf_Proc");
	    parentElement.appendChild(procEle);

        Element taskNameEle = doc.createElement("Task_Name");
        taskNameEle.appendChild(doc.createTextNode(taskName));
        procEle.appendChild(taskNameEle);

        Element taskVersionEle = doc.createElement("Task_Version");
        taskVersionEle.appendChild(doc.createTextNode(taskVersion));
        procEle.appendChild(taskVersionEle);

	    Element listOfInputsEle = doc.createElement("List_of_Inputs");
	    Attr attr = doc.createAttribute("count");
	    attr.setValue(Integer.toString(listOfInputs.size()));
	    listOfInputsEle.setAttributeNode(attr); 
	    procEle.appendChild(listOfInputsEle);
	    
	    for (InputOutput item : listOfInputs) {
	    	item.buildXML(doc, listOfInputsEle, prosEOAttributes);
	    }

	    Element listOfOutputsEle = doc.createElement("List_of_Outputs");
	    attr = doc.createAttribute("count");
	    attr.setValue(Integer.toString(listOfOutputs.size()));
	    listOfOutputsEle.setAttributeNode(attr); 
	    procEle.appendChild(listOfOutputsEle);
	    
	    for (InputOutput item : listOfOutputs) {
	    	item.buildXML(doc, listOfOutputsEle, prosEOAttributes);
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
				case "task_name" : 
					this.setTaskName(child.getTextContent().strip());
					break;
				case "task_version" : 
					this.setTaskVersion(child.getTextContent().strip());
					break;
				case "list_of_inputs" : 
					Node iele = child.getFirstChild();
					while (iele != null) {
						if (iele.getNodeName().equalsIgnoreCase(InputOutput.IO_TYPE_INPUT)) {
							InputOutput io = new InputOutput(InputOutput.IO_TYPE_INPUT);
							io.read(iele);
							this.getListOfInputs().add(io);
						}
						iele = iele.getNextSibling();
					}						
					break;
				case "list_of_outputs" : 
					Node oele = child.getFirstChild();
					while (oele != null) {
						if (oele.getNodeName().equalsIgnoreCase(InputOutput.IO_TYPE_OUTPUT)) {
							InputOutput io = new InputOutput(InputOutput.IO_TYPE_OUTPUT);
							io.read(oele);
							this.getListOfOutputs().add(io);
						}
						oele = oele.getNextSibling();
					}						
					break;
				}
				child = child.getNextSibling();
			}
		}		
	}
}
