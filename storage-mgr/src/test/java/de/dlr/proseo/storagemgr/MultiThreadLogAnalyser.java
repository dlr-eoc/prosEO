package de.dlr.proseo.storagemgr;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * MultiThreadLogAnalyser Class
 * 
 * 
 * @author Denys Chaykovskiy
 * 
 */

public class MultiThreadLogAnalyser {

	/** logs */
	private List<LogRecord> logRecords = new ArrayList<>();

	// thread messages

	/** blocking thread message */
	private String blockingThreadMsg;

	/** waiting thread message */
	private String waitingThreadMsg;

	/** non-blocking thread message */
	private String nonBlockingThreadMsg;

	// thread names

	/** blocking thread name */
	private String blockingThreadName;

	/** waiting thread name */
	private String waitingThreadName;

	/** non-blocking thread name */
	private String nonBlockingThreadName;

	

	/**
	 * Constructor
	 * 
	 * @param logs
	 * @param blockingThreadMsg
	 * @param waitingThreadMsg
	 * @param nonBlockingThreadMsg
	 */
	public MultiThreadLogAnalyser(List<String> logs, String blockingThreadMsg, String waitingThreadMsg,
			String nonBlockingThreadMsg) {

		this.blockingThreadMsg = blockingThreadMsg;
		this.waitingThreadMsg = waitingThreadMsg;
		this.nonBlockingThreadMsg = nonBlockingThreadMsg;

		for (String log : logs) {

			LogParser logParser = new LogParser(log);

			logRecords.add(logParser.parse());
		}
	}

	/**
	 * Checks if there are logs with given messages and they are from different
	 * threads
	 * 
	 * @return
	 */
	public boolean check() {

		try {

			blockingThreadName = getThreadName(blockingThreadMsg);
			waitingThreadName = getThreadName(waitingThreadMsg);
			nonBlockingThreadName = getThreadName(nonBlockingThreadMsg);

		} catch (Exception e) {

			System.out.println(e.getMessage());
			return false;
		}

		boolean expectedDifferentThreads = areDifferent(blockingThreadName, waitingThreadName, nonBlockingThreadName);

		if (!expectedDifferentThreads) {
			System.out.println("ERROR: Expected threads are not different: " + blockingThreadName + " " + waitingThreadName +
					" " + nonBlockingThreadName);
		}

		return expectedDifferentThreads;

	}

	/**
	 * Checks if 3 Strings are different
	 * 
	 * @param s1
	 * @param s2
	 * @param s3
	 * @return
	 */
	private boolean areDifferent(String s1, String s2, String s3) {

		return !s1.equals(s2) && !s1.equals(s3) && !s2.equals(s3);
	}

	/**
	 * Gets a thread name from the log, which contains a given message
	 * 
	 * @param message
	 * @return
	 * @throws IOException
	 */
	private String getThreadName(String message) throws IOException {

		for (LogRecord logRecord : logRecords) {

			if (logRecord.getContent().contains(message)) {

				return logRecord.getThreadName();
			}
		}

		throw new IOException("ERROR: Cannot find a thread with this messsage: " + message);
	}
}
