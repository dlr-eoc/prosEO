/**
 * CLISyntax.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;


/**
 * Representation of the prosEO Command Line Interface syntax
 * 
 * The command line syntax is based on a YAML specification, which is used to initialize a CLISyntax object. This can be
 * referenced by the CLIParser when parsing CLI commands.
 * 
 * @author Dr. Thomas Bassler
 */
public class CLISyntax {

	/** CLI syntax file title */
	private String title;
	/** CLI syntax version */
	private String version;
	/** CLI syntax description */
	private String description;
	/** Options for all CLI commands (list from YAML specification) */
	private List<CLIOption> globalOptions;
	/** Options for the top-level "proseo" command (list from YAML specification */
	private List<CLIOption> options;
	/** All second-level CLI commands (list from YAML specification) */
	private List<CLICommand> commands;
	
	/** Lookup table for options for all CLI commands (based on full name) */
	private Map<String, CLIOption> globalOptionMap = new HashMap<>();
	/** Lookup table for options for all CLI commands (based on short form) */
	private Map<String, CLIOption> globalOptionsShortForm = new HashMap<>();
	/** Lookup table for options for the top-level "proseo" command */
	private Map<String, CLIOption> topLevelOptions = new HashMap<>();
	/** Lookup table for all CLI commands */
	private Map<String, CLICommand> commandMap = new HashMap<>();
	
	/** The syntax used in this program run (the last one parsed) */
	/* package */ static CLISyntax inputSyntax;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(CLISyntax.class);
	
	/** Allowed type names in syntax file */
	/* package */ static final Set<String> allowedTypes = new HashSet<>(Arrays.asList("string", "integer", "datetime", "boolean"));
	
	/**
	 * Gets the title of the syntax file
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Gets the title of the syntax file
	 * 
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the syntax file version
	 * 
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * sets the syntax file version
	 * 
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Gets the syntax description
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the syntax description
	 * 
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the options valid for all commands
	 * 
	 * @return the globalOptions
	 */
	public List<CLIOption> getGlobalOptions() {
		return globalOptions;
	}

	/**
	 * Sets the options valid for all commands
	 * 
	 * @param globalOptions the globalOptions to set
	 */
	public void setGlobalOptions(List<CLIOption> globalOptions) {
		this.globalOptions = globalOptions;
	}

	/**
	 * Gets the top-level options
	 * 
	 * @return the options
	 */
	public List<CLIOption> getOptions() {
		return options;
	}

	/**
	 * Sets the top-level options
	 * 
	 * @param options the options to set
	 */
	public void setOptions(List<CLIOption> options) {
		this.options = options;
	}

	/**
	 * Gets the available commands
	 * 
	 * @return the commands
	 */
	public List<CLICommand> getCommands() {
		return commands;
	}

	/**
	 * Sets the available commands
	 * 
	 * @param commands the commands to set
	 */
	public void setCommands(List<CLICommand> commands) {
		this.commands = commands;
	}

	/**
	 * Gets the options valid for all commands as a map by full name
	 * 
	 * @return the globalOptionMap
	 */
	public Map<String, CLIOption> getGlobalOptionMap() {
		return globalOptionMap;
	}

	/**
	 * Sets the global option map (full name)
	 * 
	 * @param globalOptionMap the globalOptionMap to set
	 */
	public void setGlobalOptionMap(Map<String, CLIOption> globalOptionMap) {
		this.globalOptionMap = globalOptionMap;
	}

	/**
	 * Gets the options valid for all commands as a map by short form
	 * 
	 * @return the globalOptionsShortForm
	 */
	public Map<String, CLIOption> getGlobalOptionsShortForm() {
		return globalOptionsShortForm;
	}

	/**
	 * Sets the global option map (short form)
	 * 
	 * @param globalOptionsShortForm the globalOptionsShortForm to set
	 */
	public void setGlobalOptionsShortForm(Map<String, CLIOption> globalOptionsShortForm) {
		this.globalOptionsShortForm = globalOptionsShortForm;
	}

	/**
	 * Gets the top-level options as a map by full name
	 * 
	 * @return the topLevelOptions
	 */
	public Map<String, CLIOption> getTopLevelOptions() {
		return topLevelOptions;
	}

	/**
	 * Sets the top-level option map (full name)
	 * 
	 * @param topLevelOptions the topLevelOptions to set
	 */
	public void setTopLevelOptions(Map<String, CLIOption> topLevelOptions) {
		this.topLevelOptions = topLevelOptions;
	}

	/**
	 * Gets the available commands as a lookup map by command name
	 * 
	 * @return the commandMap
	 */
	public Map<String, CLICommand> getCommandMap() {
		return commandMap;
	}

	/**
	 * Sets the command lookup map
	 * 
	 * @param commandMap the commandMap to set
	 */
	public void setCommandMap(Map<String, CLICommand> commandMap) {
		this.commandMap = commandMap;
	}

	/**
	 * Load the CLI syntax file from the given file path
	 * 
	 * @param syntaxFileName the path to a YAML format syntax file
	 * @return a CLISyntax object construct from the YAML specification
	 * @throws FileNotFoundException if the given file could not be read
	 * @throws YAMLException if the given file could not be parsed
	 */
	public static CLISyntax fromSyntaxFile(String syntaxFileName) throws FileNotFoundException, YAMLException {
		if (logger.isTraceEnabled()) logger.trace(">>> fromSyntaxFile({})", syntaxFileName);
		
		// Parse YAML syntax file
	    InputStream input = CLISyntax.class.getClassLoader()
	    		  .getResourceAsStream(syntaxFileName);
	    
	    Constructor syntaxConstructor = new Constructor(CLISyntax.class);
	    TypeDescription syntaxTypeDescription = new TypeDescription(CLISyntax.class);
	    syntaxTypeDescription.addPropertyParameters("globalOptions", CLIOption.class);
	    syntaxTypeDescription.addPropertyParameters("options", CLIOption.class);
	    syntaxTypeDescription.addPropertyParameters("commands", CLICommand.class);
	    TypeDescription commandTypeDescription = new TypeDescription(CLICommand.class);
	    commandTypeDescription.addPropertyParameters("subcommands", CLICommand.class);
	    commandTypeDescription.addPropertyParameters("options", CLIOption.class);
	    commandTypeDescription.addPropertyParameters("parameters", CLIParameter.class);
	    
	    Yaml yaml = new Yaml(syntaxConstructor);
	    yaml.addTypeDescription(commandTypeDescription);
	    inputSyntax = yaml.load(input);
	    
	    // Check semantic constraints
	    // TODO
	    
	    // Return parsed syntax object
	    logger.log(UIMessage.SYNTAX_LOADED, syntaxFileName);
		//if (logger.isDebugEnabled()) logger.debug("Syntax definition: " + inputSyntax);
		
		return inputSyntax;
	}

	/**
	 * Print help information for this command on the given print stream
	 * 
	 * @param out the print stream to write to
	 */
	public void printHelp(PrintStream out) {
		out.println(description);
		out.println("Options for the 'proseo' command:");
		for (CLIOption option: options) {
			out.println(String.format("    %s --%-10s  %s", 
					(null == option.getShortForm() ? "   " : "-" + option.getShortForm() + ","), 
					option.getName(), 
					option.getDescription().replace('\n', ' ')));
		}
		out.println("Options for all commands:");
		for (CLIOption option: globalOptions) {
			out.println(String.format("    %s --%-10s  %s", 
					(null == option.getShortForm() ? "   " : "-" + option.getShortForm() + ","), 
					option.getName(), 
					option.getDescription().replace('\n', ' ')));
		}
		out.println("Commands:");
		for (CLICommand command: commands) {
			out.println(String.format("    %-16s  %s", command.getName(), command.getDescription().replace('\n', ' ')));
		}
	}
	
	@Override
	public String toString() {
		return "CLISyntax [\n  title=" + title + ",\n  version=" + version + ",\n  description=" + description + ",\n  globalOptions="
				+ globalOptions + ",\n  options=" + options + ",\n  commands=" + commands + "\n]";
	}
	
	/**
	 * Generates a StringBuilder, appends it with HTML code that prints a title and
	 * description, calls the respective printHTML() methods for all global options,
	 * options and commands from the respective Documentation class fields.
	 * 
	 * @return StringBuilder
	 */
	public StringBuilder printHTML() {

		StringBuilder htmlDoc = new StringBuilder();

		htmlDoc.append("<!DOCTYPE html><html><head>" + "<title>ProsEO CLI Documentation</title>"
				+ "<link rel=\"stylesheet\" href=\"css/syntax.css\">" + "</head>").append("<body>");

		// Title and descriptions:
		htmlDoc.append("<h1 id=\"Header\">" + this.title).append(" (" + this.version + ")</h1>")
				.append("<p>" + this.description.replace("\n\n", "</p><p>") + "</p>");

		// Global options:
		htmlDoc.append("<h2>Global Options</h2>");

		for (CLIOption globalOption : this.globalOptions) {
			globalOption.printHTML(htmlDoc);
		}

		// Options:
		htmlDoc.append("<h2>Options</h2>");

		for (CLIOption option : this.options) {
			option.printHTML(htmlDoc);
		}

		// Commands:
		htmlDoc.append("<h2>Commands</h2>");

		for (CLICommand command : this.commands) {
			command.printHTML(htmlDoc);
		}

		htmlDoc.append("</body></html>");
		return htmlDoc;
	}

}
