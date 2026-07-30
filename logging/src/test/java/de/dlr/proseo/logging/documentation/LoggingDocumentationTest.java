/**
 * LoggingDocumentationTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.documentation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Katharina Bassler
 *
 */
public class LoggingDocumentationTest {

	File output;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		output = new File("test.html");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
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
