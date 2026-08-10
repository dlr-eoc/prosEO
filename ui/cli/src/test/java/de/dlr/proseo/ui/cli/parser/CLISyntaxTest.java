/**
 * CLISyntaxTest.java
 */
package de.dlr.proseo.ui.cli.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.yaml.snakeyaml.error.YAMLException;

import de.dlr.proseo.ui.cli.CLIConfiguration;
import de.dlr.proseo.ui.cli.CommandLineInterface;

/**
 * @author thomas
 */
@SpringBootTest(classes = CommandLineInterface.class, properties = { "spring.main.web-application-type=NONE",
		"proseo.cli.start=false" })
public class CLISyntaxTest {

	/** The configuration object for the prosEO CLI */
	@Autowired
	private CLIConfiguration config;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CLISyntaxTest.class);

	/**
	 * Test method for {@link de.dlr.proseo.ui.cli.parser.CLISyntax#fromSyntaxFile(java.lang.String)}.
	 */
	@Test
	public final void testFromSyntaxFile() {

		try {
			CLISyntax cliSyntax = CLISyntax.fromSyntaxFile(config.getCliSyntaxFile());
			assertNotNull(cliSyntax, "No CLISyntax object generated");
			logger.info("Syntax created: " + cliSyntax);
		} catch (FileNotFoundException e) {
			fail("Could not find syntax file " + config.getCliSyntaxFile());
		} catch (YAMLException e) {
			fail("YAMLException reading syntax file " + config.getCliSyntaxFile() + " (cause: " + e.getMessage() + ")");
		}

		logger.info("Test for testFromSyntaxFile() OK");
	}

}
