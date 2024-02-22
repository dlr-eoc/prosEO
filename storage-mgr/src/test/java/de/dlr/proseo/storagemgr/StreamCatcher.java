package de.dlr.proseo.storagemgr;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream catcher for catching an output stream
 * 
 * @author Denys Chaykovskiy
 * 
 */
 
public class StreamCatcher {
	
	/** Output header */
	private static final String OUTPUT_HEADER = "catched: "; 
	
	/** Saved original output stream */
	final PrintStream originalSystemOut;

	/** System output content */
	ByteArrayOutputStream systemOutContent ;

	/**
	 * @param originalSystemOut
	 */
	public StreamCatcher(PrintStream originalSystemOut) {

		this.originalSystemOut = originalSystemOut;
		
		systemOutContent = new ByteArrayOutputStream();

		System.setOut(new PrintStream(systemOutContent));
	}

	/**
	 * Gets a catched output content
	 * 
	 * 
	 * @return catched output content
	 */
	public List<String> getOutput() {
		
		String capturedOutput = systemOutContent.toString();

		String[] lines = capturedOutput.split(System.lineSeparator());

		List<String> outputLines = new ArrayList<>();

		for (String line : lines) {
			outputLines.add(OUTPUT_HEADER + line.trim());
		}

		return outputLines;
	}

	/**
	 * Restores default system output
	 * 
	 */
	public void restoreDefaultOutput() {

		System.setOut(originalSystemOut);
	}
}
