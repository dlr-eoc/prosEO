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

import de.dlr.proseo.logging.messages.*;

/**
 * Collects and aggregates the messages used by the services for documentation
 * purposes.
 *
 * @author Katharina Bassler
 */
public class LoggingDocumentation {
	public static void main(String[] args) {

		//target_file = "src/site/resources/logging.html"
		if (args.length != 1) {
			System.out.println("usage: LoggingDocumentation target_file");
			System.exit(1);
		}
		
		File target = new File(args[0]);
		
		try {

			StringBuilder loggingDoc = new StringBuilder();
			
			loggingDoc.append(addBeginning())
			.append(addMessages())
			.append(addEnd());
			
			BufferedWriter htmlWriter = new BufferedWriter(new FileWriter(target));
			htmlWriter.write(loggingDoc.toString());
			htmlWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static String addBeginning() {
		return "<!DOCTYPE html>\n"
				+ "<html>\n"
				+ "<head>\n"
				+ "<title>ProsEO Messages DOcumentation\n"
				+ "</title>\n"
				+ "<link rel=\"stylesheet\" href=\"css/logging.css\">\n"
				+ "</head>\n"
				+ "<body>\n"
				+ "<h1 id=\"Header\">ProsEO Messages</h1>\n";
	}
	
	private static String addMessages() {
		StringBuilder messages = new StringBuilder();

		messages.append("<h2 id=\"GeneralMessage\">General Messages</h2>");
		messages.append(addService(GeneralMessage.class));

		messages.append("<h2 id=\"FacilityMgrMessage\">Facility Manager Messages</h2>");
		messages.append(addService(FacilityMgrMessage.class));

		messages.append("<h2 id=\"IngestorMessage\">Ingestor Messages</h2>");
		messages.append(addService(IngestorMessage.class));

		messages.append("<h2 id=\"ModelMessage\">Model Messages</h2>");
		messages.append(addService(ModelMessage.class));

		messages.append("<h2 id=\"OrderMgrMessage\">Order Manager Messages</h2>");
		messages.append(addService(OrderMgrMessage.class));

		messages.append("<h2 id=\"PlannerMessage\">Planner Messages</h2>");
		messages.append(addService(PlannerMessage.class));

		messages.append("<h2 id=\"ProcessorMgrMessage\">Processor Manager Messages</h2>");
		messages.append(addService(ProcessorMgrMessage.class));

		messages.append("<h2 id=\"SamplesMessage\">Samples Messages</h2>");
		messages.append(addService(SamplesMessage.class));

		messages.append("<h2 id=\"StorageMgrMessage\">Storage Manager Messages</h2>");
		messages.append(addService(StorageMgrMessage.class));

		messages.append("<h2 id=\"UIMessage\">User Interface Messages</h2>");
		messages.append(addService(UIMessage.class));

		messages.append("<h2 id=\"UserMgrMessage\">User Manager Messages</h2>");
		messages.append(addService(UserMgrMessage.class));

		messages.append("</table>");

		return messages.toString();
	}

	/*
	private static String addService(String service) throws ClassNotFoundException {
				
		StringBuilder messages = new StringBuilder();
		
		@SuppressWarnings("unchecked")
		Class<? extends ProseoMessage> clazz = (Class<? extends ProseoMessage>) Class.forName(service);

		ProseoMessage[] pm = clazz.getEnumConstants();
		Arrays.sort(pm, (a, b) -> a.getCode() - b.getCode());

		messages.append("<table>");

		for (ProseoMessage m : pm) {
			messages.append("<tr>").append("<td>" + m.getCode() + "</td>").append("<td>" + m.getLevel() + "</td>")
					.append("<td>" + m + "</td>").append("<td>" + m.getMessage() + "</td>")
					.append("<td>" + m.getDescription() + "</td>").append("</tr>");
		}

		messages.append("</table>");

		return messages.toString();
	}
*/	
	private static String addService(Class<? extends ProseoMessage> clazz) {
		
		StringBuilder messages = new StringBuilder();
		
		ProseoMessage[] pm = clazz.getEnumConstants();
		Arrays.sort(pm, (a, b) -> a.getCode() - b.getCode());

		messages.append("<table>")
				.append("<tr>")
				.append("<th>Code</th>")
				.append("<th>Severity</th>")
				.append("<th>Message</th>")
				.append("<th>Description</th>")
				.append("</tr>");

		for (ProseoMessage m : pm) {
			messages.append("<tr>")
					.append("<td>" + m.getCode() + "</td>")
					.append("<td>" + m.getLevel() + "</td>")
					.append("<td>" + m.getMessage() + "</td>")
					.append("<td>" + m.getDescription() + "</td>")
					.append("</tr>");
		}

		messages.append("</table>");

		return messages.toString();
	}
	
	private static String addEnd() {
		return "</body>\n"
				+ "</html>";
	}
}