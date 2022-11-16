/**
 * Syntax2HTMLTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ui.cli.syntax2html;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Test;

/**
 * @author Katharina Bassler
 *
 */
public class Syntax2HTMLTest {

	Path output = Paths.get("test.html");

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		Files.delete(output);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.logging.ui.cli.syntax2html.Syntax2HTML#main(java.lang.String...)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public final void testMain() {
		Syntax2HTML.main("src/main/resources/ui-cli-syntax.yml", "test.html");

		try (BufferedReader reader = Files.newBufferedReader(output)) {
			String firstLine = reader.readLine();
			assertTrue(firstLine.startsWith("<!DOCTYPE html><html><head><title>ProsEO CLI Documentation</title>"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}