/**
 * JobOrderTest.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.model.joborder;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.dlr.proseo.model.enums.JobOrderVersion;


/**
 * Test class for JobOrder
 *
 * @author Dr. Thomas Bassler
 *
 */
public class JobOrderTest {

    /**
     * @throws Exception if an error occurs
     */
    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws Exception if an error occurs
     */
    @AfterAll
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws Exception if an error occurs
     */
    @BeforeEach
    public void setUp() throws Exception {
    }

    /**
     * @throws Exception if an error occurs
     */
    @AfterEach
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
            
            assertNotNull(jo2.getConf(), "JOF copy failed: Conf is null");
            assertEquals(jo.getConf().getAcquisitionStation(), jo2.getConf().getAcquisitionStation(), "JOF copy failed: Conf -> AcquisitionStation");
            assertEquals(jo.getConf().getBreakpointEnable(), jo2.getConf().getBreakpointEnable(), "JOF copy failed: Conf -> BreakpointEnable");
            assertEquals(jo.getConf().getConfigFileNames(), jo2.getConf().getConfigFileNames(), "JOF copy failed: Conf -> ConfigFileNames");
            assertEquals(jo.getConf().getDynamicProcessingParameters().size(), jo2.getConf().getDynamicProcessingParameters().size(), "JOF copy failed: Conf -> DynamicProcessingParameters");
            assertEquals(jo.getConf().getProcessingStation(), jo2.getConf().getProcessingStation(), "JOF copy failed: Conf -> ProcessingStation");
            assertEquals(jo.getConf().getProcessorName(), jo2.getConf().getProcessorName(), "JOF copy failed: Conf -> ProcessorName");
            assertEquals(jo.getConf().getSensingTime().getStart(), jo2.getConf().getSensingTime().getStart(), "JOF copy failed: Conf -> SensingTime -> Start");
            assertEquals(jo.getConf().getSensingTime().getStop(), jo2.getConf().getSensingTime().getStop(), "JOF copy failed: Conf -> SensingTime -> Stop");
            assertEquals(jo.getConf().getStderrLogLevel(), jo2.getConf().getStderrLogLevel(), "JOF copy failed: Conf -> StderrLogLevel");
            assertEquals(jo.getConf().getStdoutLogLevel(), jo2.getConf().getStdoutLogLevel(), "JOF copy failed: Conf -> StdoutLogLevel");
            assertEquals(jo.getConf().getTest(), jo2.getConf().getTest(), "JOF copy failed: Conf -> Test");
            assertEquals(jo.getConf().getVersion(), jo2.getConf().getVersion(), "JOF copy failed: Conf -> Version");
            
            JO_PROC:
            for (Proc joProc : jo.getListOfProcs()) {
                for (Proc jo2Proc : jo2.getListOfProcs()) {
                    if (joProc.getTaskName().equals(jo2Proc.getTaskName())) {
                        assertEquals(joProc.getTaskVersion(), jo2Proc.getTaskVersion(), "JOF copy failed: Proc -> TaskVersion");
                        assertEquals(joProc.getListOfInputs().size(), jo2Proc.getListOfInputs().size(), "JOF copy failed: Proc -> ListOfInputs");
                        for (InputOutput input : joProc.getListOfInputs()) {
                            for (InputOutput input2 : jo2Proc.getListOfInputs()) {
                                if (input.getFileType().equals(input2.getFileType())) {
                                    assertEquals(input.getFileNameType(), input2.getFileNameType(), "JOF copy failed: Input -> FileNameType");
                                    for (IpfFileName fileName: input.getFileNames()) {
                                        boolean found = false;
                                        for (IpfFileName fileName2: input2.getFileNames()) {
                                            if (fileName2.getFileName().equals(fileName.getFileName())) {
                                                found = true;
                                                assertEquals(fileName.getFSType(), fileName2.getFSType(), "JOF copy failed: Input -> FileName -> FSType");
                                                for (TimeInterval testTi: input.getTimeIntervals()) {
                                                    boolean tiFound = false;
                                                    for (TimeInterval testTi2: input2.getTimeIntervals()) {
                                                        if (testTi.getFileName().equals(testTi2.getFileName())) {
                                                            assertEquals(testTi.getStart(), testTi2.getStart(), "JOF copy failed: Input -> TimeInterval -> Start");
                                                            assertEquals(testTi.getStop(), testTi2.getStop(), "JOF copy failed: Input -> TimeInterval -> Stop");
                                                        }
                                                    }
                                                    assertTrue(tiFound, "JOF copy failed: Input -> TimeInterval - no match");
                                                }
                                            }
                                            
                                        }
                                        assertTrue(found, "JOF copy failed: Input -> FileName - no match");
                                    }

                                }
                            }
                        }
                        assertEquals(joProc.getListOfOutputs().size(), jo2Proc.getListOfOutputs().size(), "JOF copy failed: Proc -> ListOfOutputs");
                        break JO_PROC;
                    }
                }
                fail("JOF copy failed: Proc " + joProc.getTaskName() + " not found in copy");
            } 
        }
        
    }

}