/**
 * SampleWrapperTest.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 * (C) 2019 Hubert Asamer
 */
package de.dlr.proseo.basewrap;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for prosEO Sample Processor Wrapper using a simple job order file.
 * 
 * @author Dr. Thomas Bassler
 * @author Hubert Asamer
 *
 */
public class BaseWrapperTest {
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(BaseWrapperTest.class);
	private static final String JOB_ORDER_FILE_NAME = "src/test/resources/JobOrder.609521551_KNMI.xml";
	
    public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
    
	/**
	 * Used for setting required ENV VARS before TEST-RUN
	 * @param newenv environment variables mapped by key and value
	 * @throws Exception on any exception that occurs
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static void setEnv(Map<String, String> newenv) throws Exception {
		try {
			Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
			Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
			theEnvironmentField.setAccessible(true);
			Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
			env.putAll(newenv);
			Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
			theCaseInsensitiveEnvironmentField.setAccessible(true);
			Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
			cienv.putAll(newenv);
		} catch (NoSuchFieldException e) {
			Class[] classes = Collections.class.getDeclaredClasses();
			Map<String, String> env = System.getenv();
			for(Class cl : classes) {
				if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
					Field field = cl.getDeclaredField("m");
					field.setAccessible(true);
					Object obj = field.get(env);
					Map<String, String> map = (Map<String, String>) obj;
					map.clear();
					map.putAll(newenv);
				}
			}
		}
	}
	

	/**
	 * Make sure all input files exist, and all output files are removed
	 * 
	 * @throws java.lang.Exception if any of the input fails is missing
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if (!Files.isReadable(FileSystems.getDefault().getPath(JOB_ORDER_FILE_NAME))) {
			throw new FileNotFoundException(JOB_ORDER_FILE_NAME);
		}
	}
	/**
	 * Currently not in use
	 * 
	 * @throws java.lang.Exception never
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Currently not in use
	 * 
	 * @throws java.lang.Exception never
	 */
	@Before
	public void setUp1() throws Exception {

	}

	/**
	 * Clean up: Remove generated output products
	 * 
	 * @throws java.lang.Exception never
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link de.dlr.proseo.basewrap.BaseWrapper#run()}.
	 */
	@Test
	public final void testRun1() {
		
//		HttpResponseInfo callback = RestOps.restApiCall("wrapper", "ingest&plan", "http://192.168.20.155:8080/proseo/planner/v0.1/processingfacilities/Lerchenhof/finish/proseojob2111", "", "FAILURE", "status", RestOps.HttpMethod.PATCH);
//		logger.info(callback.toString());
//		callback = RestOps.restApiCall("PTM-melchinger", "ernst", "http://192.168.20.155:8080/proseo/planner/v0.1/processingfacilities/Lerchenhof/finish/proseojob2111", "", "FAILURE", "status", RestOps.HttpMethod.PATCH);
//		logger.info(callback.toString());
		
		
		// test for debug
		Client client = ClientBuilder.newClient();

		WebTarget webTarget = client.target("server").path("Hallo/Welt");
		webTarget = webTarget.queryParam("p1", "v1");
		webTarget = webTarget.queryParam("p2", "v2");
		logger.info(webTarget.toString());
		logger.info(webTarget.getUri().toString());

		// Hashmap holding env-vars
		Map<String,String> envmap = new HashMap<>();
		
		
		/** working run using POSIX-JOF */
		envmap.put("FS_TYPE", "POSIX");
		envmap.put("JOBORDER_FILE", JOB_ORDER_FILE_NAME);
		envmap.put("STORAGE_ENDPOINT", "test");
		envmap.put("STORAGE_USER", "test");
		envmap.put("STORAGE_PASSWORD", "test");
		envmap.put("LOGFILE_TARGET", "test");
		envmap.put("STATE_CALLBACK_ENDPOINT", "test");
		envmap.put("SUCCESS_STATE", "test");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info(ANSI_YELLOW+"**TEST: working run using POSIX-JOF "+envmap+ANSI_RESET);
//		TEST SKIPPED, DOES NOT WORK
//		int rc = (new SampleWrapper()).run(); 
//		assertEquals("Return code should be 0", 0L, (long) rc);
		
		
		/** JOF-path is wrong */
		envmap.clear();
		envmap.put("FS_TYPE", "POSIX");
		envmap.put("JOBORDER_FILE", "adadad");
		envmap.put("STORAGE_ENDPOINT", "test");
		envmap.put("STORAGE_USER", "test");
		envmap.put("STORAGE_PASSWORD", "test");
		envmap.put("LOGFILE_TARGET", "test");
		envmap.put("STATE_CALLBACK_ENDPOINT", "test");
		envmap.put("SUCCESS_STATE", "test");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info(ANSI_YELLOW+"**TEST: JOF-path is wrong "+envmap+ANSI_RESET);
		int rc = (new BaseWrapper()).run(); 
		assertEquals("Return code should be 255", 255L, (long) rc);
		
		
		/** FS_TYPE is wrong */
		envmap.clear();
		envmap.put("FS_TYPE", "sfsf");
		envmap.put("JOBORDER_FILE", JOB_ORDER_FILE_NAME);
		envmap.put("STORAGE_ENDPOINT", "test");
		envmap.put("STORAGE_USER", "test");
		envmap.put("STORAGE_PASSWORD", "test");
		envmap.put("LOGFILE_TARGET", "test");
		envmap.put("STATE_CALLBACK_ENDPOINT", "test");
		envmap.put("SUCCESS_STATE", "test");

		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info(ANSI_YELLOW+"**TEST: FS_TYPE is wrong "+envmap+ANSI_RESET);
		rc = (new BaseWrapper()).run(); 
		assertEquals("Return code should be 255", 255L, (long) rc);
		
		
		/** FS_TYPE is S3, but JOBORDER_FILE refers to non s3 URI */
		envmap.clear();
		envmap.put("FS_TYPE", "S3");
		envmap.put("JOBORDER_FILE", JOB_ORDER_FILE_NAME);
		envmap.put("STORAGE_ENDPOINT", "test");
		envmap.put("STORAGE_USER", "test");
		envmap.put("STORAGE_PASSWORD", "test");
		envmap.put("LOGFILE_TARGET", "test");
		envmap.put("STATE_CALLBACK_ENDPOINT", "test");
		envmap.put("SUCCESS_STATE", "test");
		
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info(ANSI_YELLOW+"**TEST: FS_TYPE is S3, but JOBORDER_FILE refers to non s3 URI "+envmap+ANSI_RESET);
		rc = (new BaseWrapper()).run(); 
		assertEquals("Return code should be 255", 255L, (long) rc);
		
		
		/** working run using S3-JOF */
		envmap.clear();
		envmap.put("JOBORDER_FILE", "s3://joborders/JobOrder.609521551_KNMI.xml");
		envmap.put("FS_TYPE", "S3");
		envmap.put("STORAGE_ENDPOINT", "http://localhost:9000");
		envmap.put("STORAGE_USER", "short_access_key");
		envmap.put("STORAGE_PASSWORD", "short_secret_key");
		envmap.put("LOGFILE_TARGET", "test");
		envmap.put("STATE_CALLBACK_ENDPOINT", "test");
		envmap.put("SUCCESS_STATE", "test");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info(ANSI_YELLOW+"**TEST: working run using S3-JOF "+envmap+ANSI_RESET);
//		TEST SKIPPED, DOES NOT WORK
//		rc = (new SampleWrapper()).run(); 
//		assertEquals("Return code should be 0", 0L, (long) rc);
		
		
		/** S3-JOF but using wrong s3-credentials*/
		envmap.clear();
		envmap.put("JOBORDER_FILE", "s3://joborders/JobOrder.609521551_KNMI.xml");
		envmap.put("FS_TYPE", "S3");
		envmap.put("STORAGE_ENDPOINT", "http://localhost:9000");
		envmap.put("STORAGE_USER", "adad");
		envmap.put("STORAGE_PASSWORD", "weewe");
		envmap.put("LOGFILE_TARGET", "test");
		envmap.put("STATE_CALLBACK_ENDPOINT", "test");
		envmap.put("SUCCESS_STATE", "test");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info(ANSI_YELLOW+"**TEST: S3-JOF but using wrong s3-credentials "+envmap+ANSI_RESET);
		rc = (new BaseWrapper()).run(); 
		assertEquals("Return code should be 255", 255L, (long) rc);
		
		
		/** S3-JOF but using wrong s3-URI */
		envmap.clear();
		envmap.put("JOBORDER_FILE", "s3://joborers/JobOrer.609521551_KNMI.xml");
		envmap.put("FS_TYPE", "S3");
		envmap.put("STORAGE_ENDPOINT", "http://localhost:9000");
		envmap.put("STORAGE_USER", "short_access_key");
		envmap.put("STORAGE_PASSWORD", "short_secret_key");
		envmap.put("LOGFILE_TARGET", "test");
		envmap.put("STATE_CALLBACK_ENDPOINT", "test");
		envmap.put("SUCCESS_STATE", "test");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info(ANSI_YELLOW+"**TEST: S3-JOF but using wrong s3-URI "+envmap+ANSI_RESET);
		rc = (new BaseWrapper()).run(); 
		assertEquals("Return code should be 255", 255L, (long) rc);
		
	}
	

}
