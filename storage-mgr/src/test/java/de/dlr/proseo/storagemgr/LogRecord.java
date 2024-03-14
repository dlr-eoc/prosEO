package de.dlr.proseo.storagemgr;

import java.time.Instant;

/**
 * LogRecord Class
 * 
 * Examples: - Intercepted: 2024-02-22 20:23:08.000 TRACE 14224 --- [ Thread-6]
 * d.dlr.proseo.storagemgr.utils.FileUtils : >>>
 * createFile(2024-02-22T19:23:08.000469Z) - Intercepted: -
 * accessed-productFileDownloadPosix.txt
 * 
 * @author Denys Chaykovskiy
 * 
 */

public class LogRecord {

	/** instant */
	private Instant timestamp; // 2024-02-22 20:23:08.000

	/** threadName */
	private String threadName; // Thread-6

	/** className */
	private String className; // d.dlr.proseo.storagemgr.utils.FileUtils

	/** content */
	private String content;	// >>> createFile(2024-02-22T19:23:08.000469Z)


	/**
	 * Constructor
	 * 
	 * @param timestamp
	 * @param threadName
	 * @param className
	 * @param content
	 */
	public LogRecord(Instant timestamp, String threadName, String className, String content) {

		this.timestamp = timestamp;
		this.threadName = threadName;
		this.className = className;
		this.content = content;
	}

	/**
	 * Gets a timestamp 
	 * 
	 * @return
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Gets a thread name
	 * 
	 * @return
	 */
	public String getThreadName() {
		return threadName;
	}

	/**
	 * Gets a class name 
	 * 
	 * @return
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Gets a content
	 * 
	 * @return
	 */
	public String getContent() {
		return content;
	}
	
	/**
	 * Prints  a log record
	 * 
	 */
	public void print() {

		System.out.println("Parsed Instant: " + getTimestamp());
		System.out.println("Parsed Thread : " + getThreadName());
		System.out.println("Parsed Class  : " + getClassName());
		System.out.println("Parsed Content: " + getContent());
	}
}
