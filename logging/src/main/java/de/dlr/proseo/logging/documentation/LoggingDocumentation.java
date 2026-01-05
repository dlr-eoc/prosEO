/**
 * LoggingDocumentation.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.documentation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import de.dlr.proseo.logging.messages.*;
import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Collects and aggregates the messages used by the services for documentation
 * purposes.
 *
 * @author Katharina Bassler
 */
public class LoggingDocumentation {

	/**
	 * Collects the message codes to ensure that no code is duplicate across
	 * services
	 */
	private static Set<Integer> messageCodes = new HashSet<Integer>();

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(LoggingDocumentation.class);
	
	public static void main(String[] args) {

		// target_file = "src/site/resources/logging.html"
		if (args.length != 1) {
			System.out.println("usage: LoggingDocumentation target_file");
			System.exit(1);
		}

		File target = new File(args[0]);

		try (BufferedWriter htmlWriter = new BufferedWriter(new FileWriter(target))) {

			StringBuilder loggingDoc = new StringBuilder();

			loggingDoc.append(addBeginning()).append(addTableOfContents()).append(addMessages()).append(addEnd());

			htmlWriter.write(loggingDoc.toString());

		} catch (IOException e) {
			if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
		}

	}

	/**
	 * Returns the beginning of the HTML documentation.
	 */
	private static String addBeginning() {
		return "<!DOCTYPE html>\n" + "<html>\n" + "<head>\n" + "<title>ProsEO Messages Documentation\n" + "</title>\n"
				+ "<link rel=\"stylesheet\" href=\"css/logging.css\">\n" + "</head>\n" + "<body>\n"
				+ "<h1 id=\"Header\">ProsEO Messages</h1>\n";
	}

	/**
	 * Returns a navigable table of contents.
	 */
	private static String addTableOfContents() {

		// Prepare a StringBuilder to collect the table of contents
		StringBuilder tableOfContents = new StringBuilder();

		// Add a heading
		tableOfContents.append("<h2>Table of Contents</h2>");

		// Add the services as an unordered list
		tableOfContents.append("<ul>");

		tableOfContents.append("<li><a href=\"#GeneralMessage\">General Messages</a></li>");
		tableOfContents.append("<li><a href=\"#ApiMonitorMessage\">ESA API Monitor Messages</a></li>");
		tableOfContents.append("<li><a href=\"#FacilityMgrMessage\">Facility Manager Messages</a></li>");
		tableOfContents.append("<li><a href=\"#GeotoolsMessage\">Geotools Messages</a></li>");
		tableOfContents.append("<li><a href=\"#IngestorMessage\">Ingestor Messages</a></li>");
		tableOfContents.append("<li><a href=\"#ModelMessage\">Model Messages</a></li>");
		tableOfContents.append("<li><a href=\"#MonitorMessage\">Monitor Messages</a></li>");
		tableOfContents.append("<li><a href=\"#NotificationMessage\">Notification Messages</a></li>");
		tableOfContents.append("<li><a href=\"#OdipMessage\">ODIP Messages</a></li>");
		tableOfContents.append("<li><a href=\"#OrderGenMessage\">Order Generator Messages</a></li>");
		tableOfContents.append("<li><a href=\"#OrderMgrMessage\">Order Manager Messages</a></li>");
		tableOfContents.append("<li><a href=\"#PlannerMessage\">Planner Messages</a></li>");
		tableOfContents.append("<li><a href=\"#PripMessage\">PRIP Messages</a></li>");
		tableOfContents.append("<li><a href=\"#ProcessorMgrMessage\">Processor Manager Messages</a></li>");
		tableOfContents.append("<li><a href=\"#ProductArchiveMgrMessage\">Product Archive Manager Messages</a></li>");
		tableOfContents.append("<li><a href=\"#ProductClassMgrMessage\">Product Class Manager Messages</a></li>");
		tableOfContents.append("<li><a href=\"#StorageMgrMessage\">Storage Manager Messages</a></li>");
		tableOfContents.append("<li><a href=\"#UIMessage\">User Interface Messages</a></li>");
		tableOfContents.append("<li><a href=\"#UserMgrMessage\">User Manager Messages</a></li>");

		tableOfContents.append("</ul>");

		// Return the table of contents in HTML format as a String.
		return tableOfContents.toString();
	}

	/**
	 * Adds the services' messages to the HTML documentation.
	 */
	private static String addMessages() {

		// Prepare a StringBuilder to collect each service's table with its messages
		StringBuilder messages = new StringBuilder();

		// For each service, add a heading and the service's message table
		messages.append("<h2 id=\"GeneralMessage\">General Messages</h2>");
		messages.append(addService(GeneralMessage.class));

		messages.append("<h2 id=\"AipClientMessage\">AIP Client Messages</h2>");
		messages.append(addService(AipClientMessage.class));

		messages.append("<h2 id=\"ApiMonitorMessage\">ESA API Monitor Messages</h2>");
		messages.append(addService(ApiMonitorMessage.class));

		messages.append("<h2 id=\"FacilityMgrMessage\">Facility Manager Messages</h2>");
		messages.append(addService(FacilityMgrMessage.class));

		messages.append("<h2 id=\"GeotoolsMessage\">Geotools Messages</h2>");
		messages.append(addService(GeotoolsMessage.class));

		messages.append("<h2 id=\"IngestorMessage\">Ingestor Messages</h2>");
		messages.append(addService(IngestorMessage.class));

		messages.append("<h2 id=\"ModelMessage\">Model Messages</h2>");
		messages.append(addService(ModelMessage.class));

		messages.append("<h2 id=\"MonitorMessage\">Monitor Messages</h2>");
		messages.append(addService(MonitorMessage.class));

		messages.append("<h2 id=\"NotificationMessage\">Notification Messages</h2>");
		messages.append(addService(NotificationMessage.class));

		messages.append("<h2 id=\"OAuthMessage\">OAuth Messages</h2>");
		messages.append(addService(OAuthMessage.class));

		messages.append("<h2 id=\"OdipMessage\">ODIP Messages</h2>");
		messages.append(addService(OdipMessage.class));

		messages.append("<h2 id=\"OrderGenMessage\">Order Generator Messages</h2>");
		messages.append(addService(OrderGenMessage.class));
		
		messages.append("<h2 id=\"OrderMgrMessage\">Order Manager Messages</h2>");
		messages.append(addService(OrderMgrMessage.class));

		messages.append("<h2 id=\"PlannerMessage\">Planner Messages</h2>");
		messages.append(addService(PlannerMessage.class));

		messages.append("<h2 id=\"PripMessage\">PRIP Messages</h2>");
		messages.append(addService(PripMessage.class));

		messages.append("<h2 id=\"ProcessorMgrMessage\">Processor Manager Messages</h2>");
		messages.append(addService(ProcessorMgrMessage.class));

		messages.append("<h2 id=\"ProductArchiveMgrMessage\">Product Archive Manager Messages</h2>");
		messages.append(addService(ProductArchiveMgrMessage.class));

		messages.append("<h2 id=\"ProductClassMgrMessage\">Product Class Manager Messages</h2>");
		messages.append(addService(ProductClassMgrMessage.class));

		messages.append("<h2 id=\"StorageMgrMessage\">Storage Manager Messages</h2>");
		messages.append(addService(StorageMgrMessage.class));

		messages.append("<h2 id=\"UIMessage\">User Interface Messages</h2>");
		messages.append(addService(UIMessage.class));

		messages.append("<h2 id=\"UserMgrMessage\">User Manager Messages</h2>");
		messages.append(addService(UserMgrMessage.class));

		// Return the collected tables and heading in HTML format as a String
		return messages.toString();
	}

	/**
	 * Adds the messages of a specific service to the HTML documentation.
	 */
	private static String addService(Class<? extends ProseoMessage> clazz) {

		// Prepare a string builder to collect the service's messages
		StringBuilder messages = new StringBuilder();

		// Retrieve the messages and sort them by code
		ProseoMessage[] pm = clazz.getEnumConstants();
		Arrays.sort(pm, (a, b) -> a.getCode() - b.getCode());

		// Start the table with column names
		messages.append("<table>")
			.append("<tr>")
			.append("<th>Code</th>")
			.append("<th>Severity</th>")
			.append("<th>Message</th>")
			.append("<th>Description</th>")
			.append("</tr>");

		// Add one line per message
		for (ProseoMessage m : pm) {
			messages.append("<tr>")
				.append("<td>" + m.getCode() + "</td>")
				.append("<td>" + m.getLevel() + "</td>")
				.append("<td>" + m.getMessage() + "</td>")
				.append("<td>" + m.getDescription() + "</td>")
				.append("</tr>");

			// Ensure that no codes are duplicate within and across services
			if (!messageCodes.add(m.getCode())) {
				throw new RuntimeException(
						"No duplicate codes allowed, check error code " + m.getCode() + " (" + m.getClass() + ")");
			}
		}

		// End the table
		messages.append("</table>");

		// Return an HTML table containing all the messages of one service as a string.
		return messages.toString();
	}

	/**
	 * Returns the end of the HTML documentation.
	 */
	private static String addEnd() {
		return "</body>\n" + "</html>";
	}
}