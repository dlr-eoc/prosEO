/**
 * BaseMonitorTest.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.basemon;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for BaseMonitor
 * 
 * @author Dr. Thomas Bassler
 */
public class BaseMonitorTest {
	
	/** File path for transfer history file */
	private static final Path TEST_HISTORY_FILE = Paths.get("target/test/transfer.history");
	
	/** Test data for available downloads */
	private static final String[] TEST_AVAILABLE_OBJECT_IDS = {
			"obj1", "obj2", "obj3", "obj4", "obj5"
	};
	
	/** Simulated download history */
	private static final String[] TEST_DOWNLOAD_HISTORY = {
		Instant.now().minusSeconds(10 * 24 * 60 * 60).toString() + ";obj0", // 10 days old, so it should get truncated
		Instant.now().minusSeconds(100).toString() + ";" + TEST_AVAILABLE_OBJECT_IDS[0], 
		Instant.now().minusSeconds(100).toString() + ";" + TEST_AVAILABLE_OBJECT_IDS[1]	
	};
	
	/** Test objects available for download */
	private static List<TransferObject> availableObjects = new ArrayList<>();
	
	/** Set of all object IDs available for download */
	private static Set<String> expectedIdentifiers = new HashSet<>();
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(BaseMonitorTest.class);
	
	
	/**
	 * A dummy implementation of TransferObject
	 */
	private static class TestTransferObject implements TransferObject {
		
		String identifier;

		@Override
		public String getIdentifier() {
			return identifier;
		}
		
		public TestTransferObject(String identifier) {
			this.identifier = identifier;
		}
		
	}
	
	/**
	 * A dummy implementation of BaseMonitor
	 */
	private static class TestMonitor extends BaseMonitor {
		
		/** A logger for this class */
		private static Logger logger = LoggerFactory.getLogger(TestMonitor.class);
		
		/**
		 * Wait randomly for up to 10 seconds, writing log messages every second
		 * 
		 * @param methodName the method to print in the log message
		 */
		private void randomWait(String methodName) {
			int waitSeconds = (int) (Math.random() * 10 + 0.5);
			
			for (int i = waitSeconds; i > 0; --i) {
				logger.info("... {} seconds to wait in method {}", i, methodName);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.warn("... wait in method {} interrupted", methodName);
				}
			}
		}
		
		private boolean randomResult(String methodName) {
			int success = (int) (Math.random() + 0.7);
			logger.info("... method {} returns {}", methodName, success == 1 ? "SUCCESS" : "FAILED");
			return success == 1;
		}

		@Override
		protected List<TransferObject> checkAvailableDownloads(Instant referenceTimeStamp) {
			return availableObjects;
		}

		@Override
		protected boolean transferToTargetDir(TransferObject object) {
			String methodName = "transferToTargetDir(" + object.getIdentifier() + ")";
			randomWait(methodName);
			return randomResult(methodName);
		}

		@Override
		protected boolean triggerFollowOnAction(TransferObject object) {
			String methodName = "triggerFollowOnAction(" + object.getIdentifier() + ")";
			randomWait(methodName);
			return randomResult(methodName);
		}
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		Files.deleteIfExists(TEST_HISTORY_FILE);

		availableObjects.clear();
		expectedIdentifiers.clear();
		for (int i = 0; i < TEST_AVAILABLE_OBJECT_IDS.length; ++i) {
			availableObjects.add(new TestTransferObject(TEST_AVAILABLE_OBJECT_IDS[i]));
			expectedIdentifiers.add(TEST_AVAILABLE_OBJECT_IDS[i]);
		}
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		
		Files.delete(TEST_HISTORY_FILE);
		
	}
	
	private void testBase() {
		
		BaseMonitor testMonitor = new TestMonitor();
		testMonitor.setTransferHistoryFile(TEST_HISTORY_FILE);
		testMonitor.setMaxDownloadThreads(3);
		testMonitor.run(5); // 5 cycles should be sufficient for "transferring" all files "successfully"
		
		// History file should now contain all download entries exactly once
		try (BufferedReader transferHistoryFile = Files.newBufferedReader(TEST_HISTORY_FILE)) {
			while (transferHistoryFile.ready()) {
				String transferredObjectIdentifier = transferHistoryFile.readLine().split(";")[1];
				logger.info("... checking transfer history entry: " + transferredObjectIdentifier);
				if (expectedIdentifiers.contains(transferredObjectIdentifier)) {
					expectedIdentifiers.remove(transferredObjectIdentifier);
				} else {
					fail("Identifier encountered more than once: " + transferredObjectIdentifier);
				}
			}
		} catch (IOException e) {
			fail("Open/read history file failed: " + e.getMessage());
		}
		if (!expectedIdentifiers.isEmpty()) {
			fail("The following identifiers were not found: " + expectedIdentifiers);
		}
		
	}

	@Test
	public final void testWithHistoryFile() {
		
		logger.info("======== Running test WITH history file ========");
		
		try (BufferedWriter transferHistoryFile = Files.newBufferedWriter(
				TEST_HISTORY_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (int i = 0; i < TEST_DOWNLOAD_HISTORY.length; ++i) {
				transferHistoryFile.write(TEST_DOWNLOAD_HISTORY[i]);
				transferHistoryFile.newLine();
			}
		} catch (IOException e) {
			fail("Create history file failed: " + e.getMessage());
		}

		testBase();
		
	}

	@Test
	public final void testWithoutHistoryFile() {
		
		logger.info("======== Running test WITHOUT history file ========");
		
		testBase();
		
	}

}
