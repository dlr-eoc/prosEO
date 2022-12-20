/**
 * ConfTest.java
 */
package de.dlr.proseo.model.joborder;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.dlr.proseo.model.joborder.Conf.NonUniqueResultException;

/**
 * @author thomas
 *
 */
public class ConfTest {

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
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.joborder.Conf#getProcessingParametersByName(java.lang.String)}.
	 */
	@Test
	public final void testGetProcessingParametersByName() {
		Conf testConf = new Conf();
		
		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("a", "v1"));
		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("b", "v2"));
		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("c", "v3"));
		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("b", "v4"));
		
		List<ProcessingParameter> result = testConf.getProcessingParametersByName("b");
		
		assertEquals("Unexpected result size:", 2, result.size());
		assertEquals("Unexpected first result", "v2", result.get(0).getValue());
		assertEquals("Unexpected second result", "v4", result.get(1).getValue());
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.joborder.Conf#getUniqueProcessingParameterByName(java.lang.String)}.
	 */
	@Test
	public final void testGetUniqueProcessingParameterByName() {
		Conf testConf = new Conf();
		
		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("a", "v1"));
		
		ProcessingParameter result = testConf.getUniqueProcessingParameterByName("b");
		
		assertNull("Unexpected result:", result);
		
		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("b", "v2"));
		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("c", "v3"));
		
		result = testConf.getUniqueProcessingParameterByName("b");
		
		assertEquals("Unexpected result value:", "v2", result.getValue());

		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("b", "v4"));
		
		org.junit.Assert.assertThrows("Expected exception not thrown", 
				NonUniqueResultException.class, 
				() -> { testConf.getUniqueProcessingParameterByName("b"); } );
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.joborder.Conf#setProcessingParameterByName(java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testSetProcessingParameterByName() {
		Conf testConf = new Conf();
		
		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("a", "v1"));
		
		testConf.setProcessingParameterByName("b", "v2");
		ProcessingParameter result = testConf.getUniqueProcessingParameterByName("b");
		
		assertEquals("Unexpected result value:", "v2", result.getValue());
		
		testConf.setProcessingParameterByName("b", "v5");
		result = testConf.getUniqueProcessingParameterByName("b");
		
		assertEquals("Unexpected result value:", "v5", result.getValue());
		
		testConf.getDynamicProcessingParameters().add(new ProcessingParameter("b", "v4"));
		
		org.junit.Assert.assertThrows("Expected exception not thrown", 
				NonUniqueResultException.class, 
				() -> { testConf.setProcessingParameterByName("b", "v6"); } );
	}

}
