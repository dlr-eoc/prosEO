/**
 * LoggingDocumentationTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.documentation;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Katharina Bassler
 *
 */
public class LoggingDocumentationTest {

	File output;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		output = new File("test.html");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		output.delete();
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.documentation.LoggingDocumentation#main(java.lang.String[])}.
	 * 
	 * @throws IOException
	 */
	@Test
	public final void testMain() throws IOException {
		LoggingDocumentation.main(new String[] { output.getPath() });

		try (BufferedReader reader = new BufferedReader(new FileReader(output))) {
			String firstLine = reader.readLine();
			assertEquals("<!DOCTYPE html>", firstLine);
		}
	}
}
