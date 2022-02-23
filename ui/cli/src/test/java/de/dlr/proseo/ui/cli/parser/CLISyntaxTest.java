/**
 * CLISyntaxTest.java
 */
package de.dlr.proseo.ui.cli.parser;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.yaml.snakeyaml.error.YAMLException;

import de.dlr.proseo.ui.cli.CLIConfiguration;
import de.dlr.proseo.ui.cli.CommandLineInterface;

/**
 * @author thomas
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CommandLineInterface.class, properties = { "spring.main.web-application-type=NONE", "proseo.cli.start=false" } )
public class CLISyntaxTest {

	/** The configuration object for the prosEO CLI */
	@Autowired
	private CLIConfiguration config;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CLISyntaxTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link de.dlr.proseo.ui.cli.parser.CLISyntax#fromSyntaxFile(java.lang.String)}.
	 */
	@Test
	public final void testFromSyntaxFile() {
		
		try {
			CLISyntax cliSyntax = CLISyntax.fromSyntaxFile(config.getCliSyntaxFile());
			assertNotNull("No CLISyntax object generated", cliSyntax);
			logger.info("Syntax created: " + cliSyntax);
		} catch(FileNotFoundException e) {
			fail("Could not find syntax file " + config.getCliSyntaxFile());
		} catch(YAMLException e) {
			fail("YAMLException reading syntax file " + config.getCliSyntaxFile() + " (cause: " + e.getMessage() + ")");
		}
		
		logger.info("Test for testFromSyntaxFile() OK");
	}

}
