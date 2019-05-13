/**
 * SampleWrapperTest.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.samplewrap;

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
 * Test class for prosEO Sample Processor Wrapper using a simple job order file.
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class SampleWrapperTest {
	
	private static final String JOB_ORDER_FILE_NAME = "src/test/resources/sample-joborder.xml";

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
	}

	/**
	 * Test method for {@link de.dlr.proseo.sampleproc.SampleProcessor#run(java.lang.String[])}.
	 */
	@Test
	public final void testRun() {
		String[] args = { JOB_ORDER_FILE_NAME };
		int rc = (new SampleWrapper()).run(args); 
		assertEquals("Return code should be 0", 0L, (long) rc);
		
		String[] args1 = { "invalid_file_name" };
		rc = (new SampleWrapper()).run(args1); 
		assertEquals("Return code should be 255", 255L, (long) rc);
	}

}
