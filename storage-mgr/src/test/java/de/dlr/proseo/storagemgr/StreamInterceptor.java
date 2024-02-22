package de.dlr.proseo.storagemgr;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream interceptor for intercepting an output stream
 * 
 * @author Denys Chaykovskiy
 * 
 */
 
public class StreamInterceptor {
	
	/** Output header */
	private static final String OUTPUT_HEADER = "Intercepted: "; 
	
	/** Saved original output stream */
	private PrintStream originalSystemOut;

	/** Intercepted system output content */
	private ByteArrayOutputStream systemOutContent = new ByteArrayOutputStream();;

	/**
	 * @param originalSystemOut
	 */
	public StreamInterceptor(PrintStream originalSystemOut) {

		this.originalSystemOut = originalSystemOut;
		
		System.setOut(new PrintStream(systemOutContent));
	}

	/**
	 * Gets an intercepted output content
	 * 
	 * 
	 * @return intercepted output content
	 */
	public List<String> getOutput() {
		
		String interceptedOutput = systemOutContent.toString();

		String[] lines = interceptedOutput.split(System.lineSeparator());

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
