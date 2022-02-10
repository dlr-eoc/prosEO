package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.rest.model.RestJoborder;

/**
 * Mock Mvc test for Product Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */
/**
 * @throws Exception
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class JobOrderControllerImplTest {

	@Autowired
	private MockMvc mockMvc;

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/joborders";

	/**
	 * Download prosEO Job Order File as Base64-encoded string
	 * 
	 * GET /joborders pathInfo="/.."
	 * 
	 * @return String
	 */
	@Test
	public void testGetObjectByPathInfo() throws Exception {

		String pathInfo = "/..";

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", pathInfo);

		MvcResult mvcResult = mockMvc.perform(request)
				.andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
	}

	/**
     *  Upload prosEO Job Order File for later use in a job
     *  
     *  POST /joborders RestJoborder  
     * 
     * @return
     *     RestJoborder
     */
	@Test
	public void testCreateRestJoborder()  throws Exception {
		
		RestJoborder restJoborder = new RestJoborder(); 
		
		// populate restJoborder 
				
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(REQUEST_STRING)
				.sessionAttr("restJoborder", restJoborder);
				
		
		MvcResult mvcResult = mockMvc.perform(request)
				.andExpect(status().is(201))
				.andReturn(); 
		
		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus() );
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString() );
	}
}
