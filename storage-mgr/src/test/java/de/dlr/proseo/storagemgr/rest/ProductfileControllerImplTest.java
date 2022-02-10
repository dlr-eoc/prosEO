package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.rest.model.RestProductFS;

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
public class ProductfileControllerImplTest {

	@Autowired
	private MockMvc mockMvc;

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/productsfiles";

	/**
	 * Retrieve file from Storage Manager into locally accessible file system
	 * 
	 * GET /productfiles pathInfo="/.."
	 * 
	 * @return RestFileInfo
	 */
	@Test
	public void testGetRestFileInfoByPathInfo() throws Exception {

		String pathInfo = "/..";

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", pathInfo);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
	}

	/**
     *  Push file from local POSIX file system to Storage Manager
     *  
     *  POST /productfiles pathInfo="/.."&productId="123"&fileSize="234"  
     * 
     * @return
     *     RestFileInfo
     */
	@Test
	public void testCreateRestProductFS()  throws Exception {
		
		String pathInfo = "/.."; 
		String productId = "123"; 
		String fileSize = "234"; 
				
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(REQUEST_STRING)
				.param("pathInfo", pathInfo)
				.param("productId", productId)
				.param("fileSize", fileSize);
		
		MvcResult mvcResult = mockMvc.perform(request)
				.andExpect(status().is(201))
				.andReturn(); 
		
		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus() );
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString() );
	}
}
