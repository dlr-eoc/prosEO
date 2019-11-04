/**
 * SampleProcessorTest.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.sampleproc;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for prosEO Sample Processor using a simple job order file and a dummy product.
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class SampleProcessorTest {
	
	private static final String JOB_ORDER_FILE_NAME = "src/test/resources/sample-joborder.xml";
	private static final String INPUT_FILE_NAME = "src/test/resources/sample-product-in.txt";
	private static final String[] OUTPUT_FILE_NAMES = {
			"src/test/resources/sample-product-out-1.txt",
			"src/test/resources/sample-product-out-2.txt"
	};

	/**
	 * Make sure all input files exist, and all output files are removed
	 * 
	 * @throws java.lang.Exception if any of the input fails is missing
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if (!Files.isReadable(FileSystems.getDefault().getPath(JOB_ORDER_FILE_NAME))) {
			throw new FileNotFoundException(JOB_ORDER_FILE_NAME);
		}
		if (!Files.isReadable(FileSystems.getDefault().getPath(INPUT_FILE_NAME))) {
			throw new FileNotFoundException(INPUT_FILE_NAME);
		}
		for (String outputFileName: OUTPUT_FILE_NAMES) {
			Files.deleteIfExists(FileSystems.getDefault().getPath(outputFileName));
		}
	}

	/**
	 * Currently not in use
	 * 
	 * @throws java.lang.Exception never
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Currently not in use
	 * 
	 * @throws java.lang.Exception never
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Clean up: Remove generated output products
	 * 
	 * @throws java.lang.Exception never
	 */
	@After
	public void tearDown() throws Exception {
		for (String outputFileName: OUTPUT_FILE_NAMES) {
			Files.deleteIfExists(FileSystems.getDefault().getPath(outputFileName));
		}
	}

	/**
	 * Test method for {@link de.dlr.proseo.sampleproc.SampleProcessor#run(java.lang.String[])}.
	 */
	@Test
	public final void testRun() {
		String[] args = { JOB_ORDER_FILE_NAME };
		(new SampleProcessor()).run(args); 
		
		for (String outputFileName: OUTPUT_FILE_NAMES) {
			assertTrue("File not generated: " + outputFileName, Files.exists(FileSystems.getDefault().getPath(outputFileName)));
		}
	}

}
