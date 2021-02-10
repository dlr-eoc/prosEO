package de.dlr.proseo.model.joborder;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.dlr.proseo.model.enums.JobOrderVersion;


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
		InputOutput io = new InputOutput("CFG_CO___", InputOutput.FN_TYPE_PHYSICAL, InputOutput.IO_TYPE_INPUT, null);
		io.getFileNames().add(new IpfFileName("myFile01.xml"));
		io.getFileNames().add(new IpfFileName("myFile02.xml"));
		pr.getListOfInputs().add(io);
		io = new InputOutput("CFG_START", InputOutput.FN_TYPE_PHYSICAL, InputOutput.IO_TYPE_INPUT, null);
		io.getFileNames().add(new IpfFileName("myFile03.xml", "SomethingOther"));
		io.getFileNames().add(new IpfFileName("myFile04.xml"));
		pr.getListOfInputs().add(io);
		io = new InputOutput("CFG_STOP_", InputOutput.FN_TYPE_PHYSICAL, InputOutput.IO_TYPE_OUTPUT, "1234");
		io.getFileNames().add(new IpfFileName("myFile05.xml", "SomethingOther"));
		pr.getListOfOutputs().add(io);
		jo.getListOfProcs().add(pr);
		pr = new Proc("ProcTaskName2", "08.15");
		io = new InputOutput("CFG_CH4__", InputOutput.FN_TYPE_PHYSICAL, InputOutput.IO_TYPE_INPUT, null);
		io.getFileNames().add(new IpfFileName("myFile01.xml"));
		io.getFileNames().add(new IpfFileName("myFile02.xml"));
		pr.getListOfInputs().add(io);
		io = new InputOutput("L0_______", InputOutput.FN_TYPE_PHYSICAL, InputOutput.IO_TYPE_INPUT, null);
		io.getFileNames().add(new IpfFileName("myFile03.xml", "SomethingOther"));
		io.getFileNames().add(new IpfFileName("myFile04.xml"));
		// New: List of time intervals
		TimeInterval ti = new TimeInterval("20201001_101112123456", "20201001_101213234567", "myFile03.xml");
		io.getTimeIntervals().add(ti);
		ti = new TimeInterval("20201001_101213234567", "20201001_101314345678", "myFile04.xml");
		io.getTimeIntervals().add(ti);
		pr.getListOfInputs().add(io);
		io = new InputOutput("L1B______", InputOutput.FN_TYPE_DIRECTORY, InputOutput.IO_TYPE_OUTPUT, "567");
		io.getFileNames().add(new IpfFileName("myOutDir"));
		pr.getListOfOutputs().add(io);
		jo.getListOfProcs().add(pr);
		
		for (JobOrderVersion joVersion: Arrays.asList(JobOrderVersion.MMFI_1_8, JobOrderVersion.GMES_1_1)) {
			System.out.println("Testing Job Order classes with Job Order version " + joVersion);
			
			ByteArrayOutputStream jofStream = new ByteArrayOutputStream();
			jo.writeXMLToStream(jofStream, true, joVersion);
			JobOrder jo2 = new JobOrder();
			jo2.read(jofStream.toString());
			
			assertNotNull("JOF copy failed: Conf is null", jo2.getConf());
			assertEquals("JOF copy failed: Conf -> AcquisitionStation", jo.getConf().getAcquisitionStation(),
					jo2.getConf().getAcquisitionStation());
			assertEquals("JOF copy failed: Conf -> BreakpointEnable", jo.getConf().getBreakpointEnable(),
					jo2.getConf().getBreakpointEnable());
			assertEquals("JOF copy failed: Conf -> ConfigFileNames", jo.getConf().getConfigFileNames(),
					jo2.getConf().getConfigFileNames());
			assertEquals("JOF copy failed: Conf -> DynamicProcessingParameters",
					jo.getConf().getDynamicProcessingParameters().size(), jo2.getConf().getDynamicProcessingParameters().size());
			assertEquals("JOF copy failed: Conf -> ProcessingStation", jo.getConf().getProcessingStation(),
					jo2.getConf().getProcessingStation());
			assertEquals("JOF copy failed: Conf -> ProcessorName", jo.getConf().getProcessorName(),
					jo2.getConf().getProcessorName());
			assertEquals("JOF copy failed: Conf -> SensingTime -> Start", jo.getConf().getSensingTime().getStart(),
					jo2.getConf().getSensingTime().getStart());
			assertEquals("JOF copy failed: Conf -> SensingTime -> Stop", jo.getConf().getSensingTime().getStop(),
					jo2.getConf().getSensingTime().getStop());
			assertEquals("JOF copy failed: Conf -> StderrLogLevel", jo.getConf().getStderrLogLevel(),
					jo2.getConf().getStderrLogLevel());
			assertEquals("JOF copy failed: Conf -> StdoutLogLevel", jo.getConf().getStdoutLogLevel(),
					jo2.getConf().getStdoutLogLevel());
			assertEquals("JOF copy failed: Conf -> Test", jo.getConf().getTest(), jo2.getConf().getTest());
			assertEquals("JOF copy failed: Conf -> Version", jo.getConf().getVersion(), jo2.getConf().getVersion());
			
			JO_PROC:
			for (Proc joProc : jo.getListOfProcs()) {
				for (Proc jo2Proc : jo2.getListOfProcs()) {
					if (joProc.getTaskName().equals(jo2Proc.getTaskName())) {
						assertEquals("JOF copy failed: Proc -> TaskVersion", joProc.getTaskVersion(), jo2Proc.getTaskVersion());
						assertEquals("JOF copy failed: Proc -> ListOfInputs", joProc.getListOfInputs().size(),
								jo2Proc.getListOfInputs().size());
						for (InputOutput input : joProc.getListOfInputs()) {
							for (InputOutput input2 : jo2Proc.getListOfInputs()) {
								if (input.getFileType().equals(input2.getFileType())) {
									assertEquals("JOF copy failed: Input -> FileNameType", input.getFileNameType(),
											input2.getFileNameType());
									for (IpfFileName fileName: input.getFileNames()) {
										boolean found = false;
										for (IpfFileName fileName2: input2.getFileNames()) {
											if (fileName2.getFileName().equals(fileName.getFileName())) {
												found = true;
												assertEquals("JOF copy failed: Input -> FileName -> FSType", fileName.getFSType(),
														fileName2.getFSType());
												for (TimeInterval testTi: input.getTimeIntervals()) {
													boolean tiFound = false;
													for (TimeInterval testTi2: input2.getTimeIntervals()) {
														if (testTi.getFileName().equals(testTi2.getFileName())) {
															assertEquals("JOF copy failed: Input -> TimeInterval -> Start",
																	testTi.getStart(), testTi2.getStart());
															assertEquals("JOF copy failed: Input -> TimeInterval -> Stop",
																	testTi.getStop(), testTi2.getStop());
														}
													}
													assertTrue("JOF copy failed: Input -> TimeInterval - no match", tiFound);
												}
											}
											
										}
										assertTrue("JOF copy failed: Input -> FileName - no match", found);
									}

								}
							}
						}
						assertEquals("JOF copy failed: Proc -> ListOfOutputs", joProc.getListOfOutputs().size(),
								jo2Proc.getListOfOutputs().size());
						break JO_PROC;
					}
				}
				fail("JOF copy failed: Proc " + joProc.getTaskName() + " not found in copy");
			} 
		}
		
	}

}
