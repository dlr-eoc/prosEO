package de.dlr.proseo.model.joborder;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class JobOrderTest {

	/**
	 * @throws Exception if an error occurs
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws Exception if an error occurs
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws Exception if an error occurs
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws Exception if an error occurs
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test creating and reading XML job order files
	 */
	@Test
	public void test() {
		JobOrder jo = new JobOrder(null);
		Conf co = new Conf("Hugo", "0.0", "INFO", "WARNING", "false", "false", "PDGS-NP", "PDGS-GSN");
		co.getConfigFileNames().add("empty");
		co.setSensingTime(new SensingTime("00000000_000000000000", "99999999_999999999999"));
		co.getDynamicProcessingParameters().add(new ProcessingParameter("logging.root", "notice"));
		co.getDynamicProcessingParameters().add(new ProcessingParameter("logging.dumplog", "null"));
		co.getDynamicProcessingParameters().add(new ProcessingParameter("Threads", "9"));
		jo.setConf(co);
		Proc pr = new Proc("ProcTaskName1", "01.02.03");
		InputOutput io = new InputOutput("CFG_CO___", "PHYSICAL", "Input", null);
		io.getFileNames().add(new IpfFileName("myFile01.xml"));
		io.getFileNames().add(new IpfFileName("myFile02.xml"));
		pr.getListOfInputs().add(io);
		io = new InputOutput("CFG_STOP_", "PHYSICAL", "Input", null);
		io.getFileNames().add(new IpfFileName("myFile03.xml", "SomethingOther"));
		io.getFileNames().add(new IpfFileName("myFile04.xml"));
		pr.getListOfInputs().add(io);
		io = new InputOutput("CFG_STOP_", "PHYSICAL", "Output", "1234");
		io.getFileNames().add(new IpfFileName("myFile05.xml", "SomethingOther"));
		io.getFileNames().add(new IpfFileName("myFile06.xml"));
		pr.getListOfOutputs().add(io);
		jo.getListOfProcs().add(pr);
		pr = new Proc("ProcTaskName2", "08.15");
		io = new InputOutput("CFG_CO___", "PHYSICAL", "Input", null);
		io.getFileNames().add(new IpfFileName("myFile01.xml"));
		io.getFileNames().add(new IpfFileName("myFile02.xml"));
		pr.getListOfInputs().add(io);
		io = new InputOutput("CFG_STOP_", "PHYSICAL", "Input", null);
		io.getFileNames().add(new IpfFileName("myFile03.xml", "SomethingOther"));
		io.getFileNames().add(new IpfFileName("myFile04.xml"));
		pr.getListOfInputs().add(io);
		io = new InputOutput("CFG_STOP_", "PHYSICAL", "Output", "567");
		io.getFileNames().add(new IpfFileName("myFile05.xml", "SomethingOther"));
		io.getFileNames().add(new IpfFileName("myFile06.xml"));
		pr.getListOfOutputs().add(io);
		jo.getListOfProcs().add(pr);
		jo.writeXML("src/test/resources/testjo.xml", true);

		JobOrder jo2 = new JobOrder();
		jo2.read("src/test/resources/testjo.xml");
		jo2.writeXML("src/test/resources/testjocopy.xml", true);
		
	}

}
