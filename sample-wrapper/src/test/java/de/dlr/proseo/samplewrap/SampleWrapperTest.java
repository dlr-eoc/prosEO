/**
 * SampleWrapperTest.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.samplewrap;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for prosEO Sample Processor Wrapper using a simple job order file.
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class SampleWrapperTest {
	
	private static final String JOB_ORDER_FILE_NAME = "src/test/resources/JobOrder.609521551_KNMI.xml";
	/**
	 * Used for setting required ENV VARS before TEST-RUN
	 * @param newenv Map<String, String> env-var key&value
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
	
		Map<String,String> map1 = new HashMap<>();
		map1.put("FS_TYPE", "POSIX");
		map1.put("JOBORDER_FILE", JOB_ORDER_FILE_NAME);
		map1.put("S3_ENDPOINT", "test");
		map1.put("S3_ACCESS_KEY", "test");
		map1.put("S3_SECRET_ACCESS_KEY", "test");
		map1.put("LOGFILE_TARGET", "test");
		map1.put("STATE_CALLBACK_ENDPOINT", "test");
		map1.put("SUCCESS_STATE", "test");
		map1.put("CONTAINER_WORKDIR", "test");
		try {
			setEnv(map1);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	 * Test method for {@link de.dlr.proseo.sampleproc.SampleProcessor#run(java.lang.String[])}.
	 */
	@Test
	public final void testRun1() {
		
		// Hashmap holding env-vars
		Map<String,String> envmap = new HashMap<>();
		
		
		/** working run using POSIX-JOF */
		int rc = (new SampleWrapper()).run(); 
		assertEquals("Return code should be 0", 0L, (long) rc);
		
		/** JOF-path is wrong */
		
		envmap.put("JOBORDER_FILE", "adadad");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		rc = (new SampleWrapper()).run(); 
		assertEquals("Return code should be 255", 255L, (long) rc);
		
		/** FS_TYPE is wrong */
		envmap.clear();
		envmap.put("JOBORDER_FILE", JOB_ORDER_FILE_NAME);
		envmap.put("FS_TYPE", "sfsf");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		rc = (new SampleWrapper()).run(); 
		assertEquals("Return code should be 255", 255L, (long) rc);
		
		/** FS_TYPE is S3, but JOBORDER_FILE refers to non s3 URI */
		envmap.clear();
		envmap.put("JOBORDER_FILE", JOB_ORDER_FILE_NAME);
		envmap.put("FS_TYPE", "S3");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		rc = (new SampleWrapper()).run(); 
		assertEquals("Return code should be 255", 255L, (long) rc);
		
		/** working run using S3-JOF */
		envmap.clear();
		envmap.put("JOBORDER_FILE", "s3://joborders/JobOrder.609521551_KNMI.xml");
		envmap.put("FS_TYPE", "S3");
		envmap.put("S3_ENDPOINT", "http://localhost:9000");
		envmap.put("S3_ACCESS_KEY", "short_access_key");
		envmap.put("S3_SECRET_ACCESS_KEY", "short_secret_key");
		envmap.put("CONTAINER_WORKDIR", "target");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		rc = (new SampleWrapper()).run(); 
		assertEquals("Return code should be 0", 0L, (long) rc);
		
		/** S3-JOF but using wrong s3-credentials*/
		envmap.clear();
		envmap.put("JOBORDER_FILE", "s3://joborders/JobOrder.609521551_KNMI.xml");
		envmap.put("FS_TYPE", "S3");
		envmap.put("S3_ENDPOINT", "http://localhost:9000");
		envmap.put("S3_ACCESS_KEY", "adad");
		envmap.put("S3_SECRET_ACCESS_KEY", "weewe");
		envmap.put("CONTAINER_WORKDIR", "target");
		try {
			setEnv(envmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		rc = (new SampleWrapper()).run(); 
		assertEquals("Return code should be 255", 255L, (long) rc);
		
		
	}
	

}
