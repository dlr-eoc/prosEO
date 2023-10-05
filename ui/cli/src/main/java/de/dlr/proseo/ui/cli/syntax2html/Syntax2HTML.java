/**
 * Syntax2HTML.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.syntax2html;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.ui.cli.parser.CLISyntax;

/**
 * Converts YAML syntax file to HTML for documentation purposes. Usage: Syntax2HTML source_file target_file
 *
 * @author Katharina Bassler
 */
public class Syntax2HTML {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(Syntax2HTML.class);

	/**
	 * Converts YAML syntax file to HTML for documentation purposes
	 *
	 * @param args source_file target_file
	 */
	public static void main(String... args) {
		
		logger.trace(">>> Converting YAML syntax file to HTML documentation file");

		if (args.length != 2) {
			System.out.println("usage: Syntax2HTML source_file target_file");
			System.exit(1);
		}

		Path source = Paths.get(args[0]);
		Path target = Paths.get(args[1]);

		if (!Files.exists(source))
			throw new IllegalArgumentException("source must point to the YAML syntax file");

		try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(source))) {

			// Parse YAML source file with syntax to Java object of type CLISyntax
			Yaml yaml = new Yaml(new Constructor(CLISyntax.class));
			CLISyntax syntax = yaml.load(inputStream);

			// Generate HTML documentation
			StringBuilder htmlDoc = syntax.printHTML();

			// Write HTML documentation to target file
			Files.writeString(target, htmlDoc);

			System.out.println("HTML documentation generated successfully.");

		} catch (IOException e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
		}
	}

}