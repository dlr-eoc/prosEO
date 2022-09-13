package de.dlr.proseo.ui.cli.syntax2html;

import de.dlr.proseo.ui.cli.parser.CLISyntax;
import java.io.InputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.BaseConstructor;

/**
 * 
 * Converts Yaml input to HTML output.
 *
 */
public class Syntax2HTML {

	/**
	 * @param args source_file target_file
	 */
	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("usage: Syntax2HTML source_file target_file");
			System.exit(1);
		}
		
		File syntaxFile = new File(args[0]);
		File htmlDocFile = new File(args[1]);
						
		try {

			// Read CLI Syntax file (YAML)
			InputStream inputStream = new FileInputStream(syntaxFile);

			// Parse YAML document to Java object of type Documentation
			Yaml yaml = new Yaml(new Constructor(CLISyntax.class));
			CLISyntax newDoc = yaml.load(inputStream);

			// Generate StringBuilder with HTML code
			StringBuilder htmlDoc = newDoc.printHTML();

			// Write HTML-StringBuilder to HTML file
			BufferedWriter htmlWriter = new BufferedWriter(new FileWriter(htmlDocFile));
			htmlWriter.write(htmlDoc.toString());
			htmlWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
