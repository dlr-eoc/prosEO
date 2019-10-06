/**
 * CLIParser.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import de.dlr.proseo.ui.cli.CLIConfiguration;

/**
 * Parser for the prosEO Command Line Interface
 * 
 * Command line syntax is based on a YAML specification.
 * The general command syntax is:  "proseo" command [subcommand...] [option...] [parameter...]
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class CLIParser {

	/** The configuration object for the prosEO CLI */
	@Autowired
	private CLIConfiguration config;
	
	/** CLI syntax file title */
	private String syntaxFileTitle;
	/** CLI syntax version */
	private String syntaxVersion;
	/** CLI syntax description */
	private String syntaxDescription;
	/** Options for all CLI commands (based on full name) */
	private Map<String, CLIOption> globalOptions;
	/** Options for all CLI commands (based on short form) */
	private Map<String, CLIOption> globalOptionsShortForm;
	/** Options for the top-level "proseo" command */
	private Map<String, CLIOption> topLevelOptions;
	/** Map of all CLI commands */
	private Map<String, CLICommand> commands = new HashMap<>();
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CLIParser.class);
	
	/** Allowed type names in syntax file */
	private static final String[] allowedTypes = { "string", "integer", "datetime", "boolean" };
	
	/**
	 * Inner class for command options
	 */
	private static class CLIOption {
		/** Option name */
		public String name;
		/** Option type (see allowedTypes) */
		public String type;
		/** Option description (help text) */
		public String description;
		/** Option short form */
		public Character shortForm;
	}
	
	/**
	 * Inner class for command parameters
	 */
	private static class CLIParameter {
		/** Parameter name */
		public String name;
		/** Parameter type (see allowedTypes) */
		public String type;
		/** Parameter description (help text) */
		public String description;
		/** Flag, whether parameter is optional */
		public Boolean isOptional;
		/** Flag, whether parameter can occur multiple times */
		public Boolean isRepeatable;
	}
	
	/**
	 * Inner class for CLI commands and subcommands
	 */
	private static class CLICommand {
		/** Command name */
		public String name;
		/** Command description (help text) */
		public String description;
		/** Available subcommands */
		List<CLICommand> subcommands = new ArrayList<>();
		/** Command options */
		List<CLIOption> options = new ArrayList<>();
		/** Command parameters */
		List<CLIParameter> parameters = new ArrayList<>();
	}
	
	/**
	 * Load the CLI syntax file from the given file path
	 * 
	 * @param syntaxFileName the path to a YAML format syntax file
	 * @throws FileNotFoundException if the given file could not be read
	 */
	private void initFromSyntaxFile(String syntaxFileName) throws FileNotFoundException {
	    InputStream input = new FileInputStream(new File("src/test/resources/reader/utf-8.txt"));
	    Yaml yaml = new Yaml();
	    Object data = yaml.load(input);
		if (data instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> inputSyntax = (Map<String, Object>) data;
			if (logger.isDebugEnabled()) logger.debug("Loaded syntax definition: " + inputSyntax);
		} else {
			logger.error("Parsed YAML object has unexpected type " + data.getClass());
		}
	}
}
